package com.seckill.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.seckill.common.CacheKeys;
import com.seckill.common.UserContext;
import com.seckill.entity.Goods;
import com.seckill.entity.SeckillOrder;
import com.seckill.entity.User;
import com.seckill.exception.BusinessException;
import com.seckill.mapper.GoodsMapper;
import com.seckill.mapper.SeckillOrderMapper;
import com.seckill.mapper.UserMapper;
import com.seckill.service.SeckillOrderService;
import com.seckill.vo.SeckillOrderVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 秒杀订单实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SeckillOrderServiceImpl extends ServiceImpl<SeckillOrderMapper, SeckillOrder>
        implements SeckillOrderService {

    private static final int MAX_PAGE_SIZE = 100;

    /** 订单状态 */
    private static final int STATUS_UNPAID = 0;
    private static final int STATUS_PAID = 1;
    private static final int STATUS_CANCELLED = 2;

    /** 定时补偿一次最多处理多少单，避免一次性拉太多 */
    private static final int SWEEP_BATCH = 200;

    private final UserMapper userMapper;
    private final GoodsMapper goodsMapper;
    private final StringRedisTemplate redisTemplate;

    @Value("${seckill.order.timeout-minutes:15}")
    private long timeoutMinutes;

    @Override
    public IPage<SeckillOrderVO> pageQuery(Integer pageNum, Integer pageSize) {
        int num = (pageNum == null || pageNum < 1) ? 1 : pageNum;
        int size = (pageSize == null || pageSize < 1) ? 10 : Math.min(pageSize, MAX_PAGE_SIZE);

        IPage<SeckillOrder> page = getBaseMapper().selectPage(new Page<>(num, size),
                new LambdaQueryWrapper<SeckillOrder>().orderByDesc(SeckillOrder::getId));

        IPage<SeckillOrderVO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        List<SeckillOrderVO> records = page.getRecords().stream()
                .map(this::toVO)
                .collect(Collectors.toList());
        result.setRecords(records);
        return result;
    }

    @Override
    public void pay(String orderNo) {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new BusinessException(401, "请先登录");
        }

        SeckillOrder order = getBaseMapper().selectOne(new LambdaQueryWrapper<SeckillOrder>()
                .eq(SeckillOrder::getOrderNo, orderNo)
                .last("limit 1"));
        // 别人的订单按"不存在"处理，不暴露订单归属
        if (order == null || !userId.equals(order.getUserId())) {
            throw new BusinessException(404, "订单不存在");
        }
        if (order.getStatus() != null && order.getStatus() == STATUS_PAID) {
            // 重复支付直接当成功，前端重试无副作用
            return;
        }
        if (order.getStatus() == null || order.getStatus() != STATUS_UNPAID) {
            throw new BusinessException(400, "订单已取消，无法支付");
        }

        // where status=0 是防并发的关键：支付和超时取消同时发生时，只有一方能改成功
        int rows = getBaseMapper().update(null, new LambdaUpdateWrapper<SeckillOrder>()
                .eq(SeckillOrder::getId, order.getId())
                .eq(SeckillOrder::getStatus, STATUS_UNPAID)
                .set(SeckillOrder::getStatus, STATUS_PAID)
                .set(SeckillOrder::getPayTime, LocalDateTime.now()));
        if (rows == 0) {
            throw new BusinessException(409, "订单状态已变更，请下拉刷新");
        }
        log.info("订单支付成功：orderNo={} userId={} amount={}", orderNo, userId, order.getAmount());
    }

    @Override
    public boolean cancelIfUnpaid(String orderNo) {
        SeckillOrder order = getBaseMapper().selectOne(new LambdaQueryWrapper<SeckillOrder>()
                .eq(SeckillOrder::getOrderNo, orderNo)
                .last("limit 1"));
        if (order == null) {
            return false;
        }
        return cancelOrder(order);
    }

    @Override
    public int cancelTimeoutOrders() {
        LocalDateTime deadline = LocalDateTime.now().minusMinutes(timeoutMinutes);
        List<SeckillOrder> orders = getBaseMapper().selectList(new LambdaQueryWrapper<SeckillOrder>()
                .eq(SeckillOrder::getStatus, STATUS_UNPAID)
                .lt(SeckillOrder::getCreateTime, deadline)
                .last("limit " + SWEEP_BATCH));

        int cancelled = 0;
        for (SeckillOrder order : orders) {
            if (cancelOrder(order)) {
                cancelled++;
            }
        }
        return cancelled;
    }

    /**
     * 取消订单并回补库存
     * <p>
     * 幂等靠"status=0 才更新"这个条件：延迟消息和定时任务可能同时盯上同一单，
     * 只有真正把状态改掉的那一次才会去 Redis 加库存，不会重复回补导致超卖。
     * <p>
     * 注意：Redis 里该用户的参与标记保留 —— 数据库唯一索引 uk_user_activity 还在，
     * 取消后用户也不能把同一场活动再抢一次，两边行为保持一致。
     */
    private boolean cancelOrder(SeckillOrder order) {
        if (order.getStatus() == null || order.getStatus() != STATUS_UNPAID) {
            return false;
        }

        int rows = getBaseMapper().update(null, new LambdaUpdateWrapper<SeckillOrder>()
                .eq(SeckillOrder::getId, order.getId())
                .eq(SeckillOrder::getStatus, STATUS_UNPAID)
                .set(SeckillOrder::getStatus, STATUS_CANCELLED));
        if (rows == 0) {
            return false;
        }

        String stockKey = CacheKeys.activityStock(order.getActivityId());
        if (Boolean.TRUE.equals(redisTemplate.hasKey(stockKey))) {
            redisTemplate.opsForValue().increment(stockKey);
        }
        log.info("超时未支付，订单已取消并回补库存：orderNo={} activityId={}",
                order.getOrderNo(), order.getActivityId());
        return true;
    }

    private SeckillOrderVO toVO(SeckillOrder order) {
        SeckillOrderVO vo = new SeckillOrderVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setActivityId(order.getActivityId());
        vo.setGoodsId(order.getGoodsId());
        vo.setUserId(order.getUserId());
        vo.setAmount(order.getAmount());
        vo.setStatus(order.getStatus());
        vo.setStatusText(statusText(order.getStatus()));
        vo.setCreateTime(order.getCreateTime());

        User user = userMapper.selectById(order.getUserId());
        if (user != null) {
            vo.setUsername(user.getUsername());
            vo.setNickname(user.getNickname());
        }

        Goods goods = goodsMapper.selectById(order.getGoodsId());
        if (goods != null) {
            vo.setGoodsName(goods.getName());
        }
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
