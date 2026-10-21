package com.seckill.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.seckill.common.CacheKeys;
import com.seckill.common.UserContext;
import com.seckill.config.RabbitMQConfig;
import com.seckill.dto.SeckillMessage;
import com.seckill.entity.Goods;
import com.seckill.entity.SeckillActivity;
import com.seckill.entity.SeckillOrder;
import com.seckill.exception.BusinessException;
import com.seckill.mapper.GoodsMapper;
import com.seckill.mapper.SeckillActivityMapper;
import com.seckill.mapper.SeckillOrderMapper;
import com.seckill.service.SeckillService;
import com.seckill.vo.SeckillOrderVO;
import com.seckill.vo.SeckillResultVO;
import lombok.RequiredArgsConstructor;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * 秒杀业务实现
 * <p>
 * 核心思路：
 * 1. 库存扣减放在 Redis，用 Lua 脚本保证"判断 + 扣减 + 记录用户"是原子的
 * 2. 数据库只负责"可靠落库"，不承担并发扣减的压力
 * 3. Redis 与数据库都有一道"一人一单"的防线（后者是唯一索引兜底）
 */
@Service
@RequiredArgsConstructor
public class SeckillServiceImpl implements SeckillService {

    /** Lua 脚本返回值 */
    private static final long LUA_SUCCESS = 0L;
    private static final long LUA_NO_STOCK = 1L;
    private static final long LUA_REPEAT = 2L;
    private static final long LUA_NOT_READY = -1L;

    /** 抢购结果状态 */
    private static final int STATUS_QUEUING = 0;
    private static final int STATUS_SUCCESS = 1;
    private static final int STATUS_FAILED = 2;

    /** Lua 脚本只加载一次，后续走 EVALSHA 复用（避免每次传整个脚本） */
    private static final DefaultRedisScript<Long> SECKILL_SCRIPT;

    static {
        SECKILL_SCRIPT = new DefaultRedisScript<>();
        SECKILL_SCRIPT.setLocation(new ClassPathResource("scripts/seckill.lua"));
        SECKILL_SCRIPT.setResultType(Long.class);
    }

    private final StringRedisTemplate redisTemplate;
    private final RabbitTemplate rabbitTemplate;
    private final SeckillOrderMapper orderMapper;
    private final SeckillActivityMapper activityMapper;
    private final GoodsMapper goodsMapper;
    private final MeterRegistry meterRegistry;

    @Override
    public SeckillResultVO seckill(Long activityId) {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new BusinessException(401, "请先登录");
        }

        SeckillActivity activity = activityMapper.selectById(activityId);
        if (activity == null) {
            throw new BusinessException(404, "秒杀活动不存在");
        }
        checkActivityTime(activity);

        // 关键一步：把"校验库存 + 扣减库存 + 记录参与用户"合成一次原子操作。
        // 拆成多条命令的话，并发下必然超卖。
        Long result = redisTemplate.execute(SECKILL_SCRIPT,
                List.of(CacheKeys.activityStock(activityId), CacheKeys.activityUsers(activityId)),
                String.valueOf(userId));

        if (result == null) {
            countResult("error");
            throw new BusinessException(500, "秒杀失败，请稍后重试");
        }
        if (result == LUA_NOT_READY) {
            countResult("not_ready");
            throw new BusinessException(400, "活动尚未就绪，请稍后再试");
        }
        if (result == LUA_NO_STOCK) {
            countResult("sold_out");
            throw new BusinessException(400, "来晚了，库存已被抢光");
        }
        if (result == LUA_REPEAT) {
            countResult("repeat");
            throw new BusinessException(409, "您已经抢购过了，每人限购一件");
        }

        // 扣减成功 -> 不再同步写数据库，只发一条消息就返回。
        // 响应时间从"等数据库插入"降到"写一次 Redis + 发一次消息"，
        // 请求高峰由 MQ 堆住，消费者按自己的节奏落库（削峰）。
        String orderNo = generateOrderNo(userId, activityId);
        SeckillMessage message = new SeckillMessage(userId, activityId, activity.getGoodsId(),
                activity.getSeckillPrice(), orderNo);
        try {
            rabbitTemplate.convertAndSend(RabbitMQConfig.SECKILL_EXCHANGE,
                    RabbitMQConfig.SECKILL_ORDER_ROUTING_KEY, message);
        } catch (Exception e) {
            // 发消息失败：把库存和标记还回去，否则用户永远等不到订单（少卖）
            rollbackRedis(activityId, userId);
            countResult("error");
            throw new BusinessException(500, "下单失败，请稍后重试");
        }

        countResult("success");
        SeckillResultVO vo = new SeckillResultVO();
        vo.setActivityId(activityId);
        vo.setOrderNo(orderNo);
        vo.setStatus(STATUS_QUEUING);
        vo.setStatusText("排队中");
        vo.setMessage("抢购成功，订单正在生成中，请稍后查询结果");
        return vo;
    }

    @Override
    public SeckillResultVO queryResult(Long activityId) {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new BusinessException(401, "请先登录");
        }

        SeckillResultVO vo = new SeckillResultVO();
        vo.setActivityId(activityId);

        // 订单已落库 -> 抢购成功
        SeckillOrder order = orderMapper.selectOne(new LambdaQueryWrapper<SeckillOrder>()
                .eq(SeckillOrder::getUserId, userId)
                .eq(SeckillOrder::getActivityId, activityId)
                .last("limit 1"));
        if (order != null) {
            vo.setOrderNo(order.getOrderNo());
            vo.setStatus(STATUS_SUCCESS);
            vo.setStatusText("抢购成功");
            vo.setMessage("订单已生成");
            return vo;
        }

        // 订单还没落库，但用户在参与集合里 -> 还在排队（消息正在被消费）
        boolean queued = Boolean.TRUE.equals(
                redisTemplate.opsForSet().isMember(CacheKeys.activityUsers(activityId), String.valueOf(userId)));
        if (queued) {
            vo.setStatus(STATUS_QUEUING);
            vo.setStatusText("排队中");
            vo.setMessage("订单生成中，请稍后查询结果");
        } else {
            vo.setStatus(STATUS_FAILED);
            vo.setStatusText("未抢到");
            vo.setMessage("很遗憾，您没有抢到");
        }
        return vo;
    }

    @Override
    public List<SeckillOrderVO> myOrders() {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new BusinessException(401, "请先登录");
        }
        List<SeckillOrder> orders = orderMapper.selectList(new LambdaQueryWrapper<SeckillOrder>()
                .eq(SeckillOrder::getUserId, userId)
                .orderByDesc(SeckillOrder::getId));
        return orders.stream().map(o -> toVO(o, null)).collect(Collectors.toList());
    }

    private void checkActivityTime(SeckillActivity activity) {
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(activity.getStartTime())) {
            throw new BusinessException(400, "秒杀还未开始");
        }
        if (now.isAfter(activity.getEndTime())) {
            throw new BusinessException(400, "秒杀已结束");
        }
    }

    /** Redis 补偿：把扣掉的库存加回去，并移除用户的参与标记 */
    private void rollbackRedis(Long activityId, Long userId) {
        redisTemplate.opsForValue().increment(CacheKeys.activityStock(activityId));
        redisTemplate.opsForSet().remove(CacheKeys.activityUsers(activityId), String.valueOf(userId));
    }

    /** 抢购结果计数：给 Grafana 面板提供成功率 / 失败原因分布 */
    private void countResult(String result) {
        meterRegistry.counter("seckill_order_result", "result", result).increment();
    }

    private String generateOrderNo(Long userId, Long activityId) {
        return "SK" + System.currentTimeMillis()
                + String.format("%04d", ThreadLocalRandom.current().nextInt(10000))
                + userId;
    }

    private SeckillOrderVO toVO(SeckillOrder order, String goodsName) {
        SeckillOrderVO vo = new SeckillOrderVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setActivityId(order.getActivityId());
        vo.setGoodsId(order.getGoodsId());
        vo.setAmount(order.getAmount());
        vo.setStatus(order.getStatus());
        vo.setStatusText(statusText(order.getStatus()));
        vo.setCreateTime(order.getCreateTime());

        if (goodsName == null && order.getGoodsId() != null) {
            Goods goods = goodsMapper.selectById(order.getGoodsId());
            goodsName = goods == null ? null : goods.getName();
        }
        vo.setGoodsName(goodsName);
        return vo;
    }

    private String statusText(Integer status) {
        if (status == null) {
            return "未知";
        }
        return switch (status) {
            case 0 -> "待支付";
            case 1 -> "已支付";
            case 2 -> "已取消";
            case 3 -> "已完成";
            default -> "未知";
        };
    }
}
