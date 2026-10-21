package com.seckill.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.seckill.common.CacheKeys;
import com.seckill.dto.SeckillActivityCreateDTO;
import com.seckill.entity.Goods;
import com.seckill.entity.SeckillActivity;
import com.seckill.exception.BusinessException;
import com.seckill.mapper.GoodsMapper;
import com.seckill.mapper.SeckillActivityMapper;
import com.seckill.service.SeckillActivityService;
import com.seckill.vo.SeckillActivityVO;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * 秒杀活动业务实现
 */
@Service
@RequiredArgsConstructor
public class SeckillActivityServiceImpl extends ServiceImpl<SeckillActivityMapper, SeckillActivity>
        implements SeckillActivityService {

    /** 提前几分钟开始预热 */
    private static final int PREHEAT_AHEAD_MINUTES = 5;

    private final GoodsMapper goodsMapper;
    private final StringRedisTemplate redisTemplate;
    private final MeterRegistry meterRegistry;

    @Override
    public IPage<SeckillActivityVO> pageQuery(Integer pageNum, Integer pageSize) {
        int num = (pageNum == null || pageNum < 1) ? 1 : pageNum;
        int size = (pageSize == null || pageSize < 1) ? 10 : Math.min(pageSize, 100);

        IPage<SeckillActivity> page = getBaseMapper().selectPage(new Page<>(num, size),
                new LambdaQueryWrapper<SeckillActivity>().orderByDesc(SeckillActivity::getId));

        IPage<SeckillActivityVO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        List<SeckillActivityVO> records = page.getRecords().stream()
                .map(this::toVO)
                .collect(Collectors.toList());
        result.setRecords(records);
        return result;
    }

    @Override
    public SeckillActivityVO getDetail(Long activityId) {
        SeckillActivity activity = getBaseMapper().selectById(activityId);
        if (activity == null) {
            throw new BusinessException(404, "秒杀活动不存在");
        }
        return toVO(activity);
    }

    @Override
    public Long createActivity(SeckillActivityCreateDTO dto) {
        Goods goods = goodsMapper.selectById(dto.getGoodsId());
        if (goods == null) {
            throw new BusinessException(400, "商品不存在");
        }
        if (dto.getEndTime().isBefore(dto.getStartTime())) {
            throw new BusinessException(400, "结束时间必须晚于开始时间");
        }

        SeckillActivity activity = new SeckillActivity();
        activity.setGoodsId(dto.getGoodsId());
        activity.setSeckillPrice(dto.getSeckillPrice());
        activity.setSeckillStock(dto.getSeckillStock());
        activity.setStartTime(dto.getStartTime());
        activity.setEndTime(dto.getEndTime());
        activity.setStatus(0);
        activity.setVersion(0);
        getBaseMapper().insert(activity);

        // 创建后立刻预热，保证活动开始时 Redis 里一定已有库存
        preheatStock(activity.getId());
        return activity.getId();
    }

    @Override
    public void preheatStock(Long activityId) {
        SeckillActivity activity = getBaseMapper().selectById(activityId);
        if (activity == null) {
            throw new BusinessException(404, "秒杀活动不存在");
        }
        redisTemplate.opsForValue().set(CacheKeys.activityStock(activityId),
                String.valueOf(activity.getSeckillStock()));
        registerStockGauge(activityId);
    }

    @Override
    public int preheatPendingActivities() {
        LocalDateTime now = LocalDateTime.now();
        // 结束时间还没到、且 5 分钟内就要开始的活动
        List<SeckillActivity> activities = getBaseMapper().selectList(new LambdaQueryWrapper<SeckillActivity>()
                .ge(SeckillActivity::getEndTime, now)
                .le(SeckillActivity::getStartTime, now.plusMinutes(PREHEAT_AHEAD_MINUTES)));

        int count = 0;
        for (SeckillActivity activity : activities) {
            String key = CacheKeys.activityStock(activity.getId());
            // 已经预热过的不再重复写入，避免把活动进行中被扣掉的库存又刷回初始值
            if (Boolean.FALSE.equals(redisTemplate.hasKey(key))) {
                redisTemplate.opsForValue().set(key, String.valueOf(activity.getSeckillStock()));
                count++;
            }
            registerStockGauge(activity.getId());
        }
        return count;
    }

    /**
     * 把活动实时库存暴露成 Prometheus 指标：seckill_activity_stock{activityId="1"}
     * <p>
     * 用 Gauge 而不是每次抢购时主动上报，是因为库存会被并发修改，
     * 抓取时现读 Redis 最准确，也不会给下单链路增加开销。
     * 重复注册同一个 id+标签时 Micrometer 会返回已有指标，不会重复登记。
     */
    private void registerStockGauge(Long activityId) {
        String key = CacheKeys.activityStock(activityId);
        Supplier<Number> supplier = () -> {
            String value = redisTemplate.opsForValue().get(key);
            return value == null ? 0 : Double.parseDouble(value);
        };
        Gauge.builder("seckill_activity_stock", supplier)
                .description("活动剩余的 Redis 实时库存")
                .tag("activityId", String.valueOf(activityId))
                .register(meterRegistry);
    }

    /** 实体 -> 展示对象：拼上商品信息和 Redis 实时库存 */
    private SeckillActivityVO toVO(SeckillActivity activity) {
        SeckillActivityVO vo = new SeckillActivityVO();
        vo.setId(activity.getId());
        vo.setGoodsId(activity.getGoodsId());
        vo.setSeckillPrice(activity.getSeckillPrice());
        vo.setSeckillStock(activity.getSeckillStock());
        vo.setStartTime(activity.getStartTime());
        vo.setEndTime(activity.getEndTime());

        Goods goods = goodsMapper.selectById(activity.getGoodsId());
        if (goods != null) {
            vo.setGoodsName(goods.getName());
            vo.setGoodsTitle(goods.getTitle());
            vo.setGoodsImgUrl(goods.getImgUrl());
            vo.setOriginPrice(goods.getOriginPrice());
        }

        String stock = redisTemplate.opsForValue().get(CacheKeys.activityStock(activity.getId()));
        vo.setRedisStock(stock == null ? null : Integer.valueOf(stock));

        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(activity.getStartTime())) {
            vo.setStatusText("未开始");
        } else if (now.isAfter(activity.getEndTime())) {
            vo.setStatusText("已结束");
        } else {
            vo.setStatusText("进行中");
        }
        return vo;
    }
}
