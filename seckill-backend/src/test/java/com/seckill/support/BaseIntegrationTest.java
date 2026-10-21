package com.seckill.support;

import com.seckill.common.CacheKeys;
import com.seckill.entity.SeckillActivity;
import com.seckill.mapper.SeckillActivityMapper;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

/**
 * 集成测试基类
 * <p>
 * 用独立的 seckill_test 库 + Redis 15 号库，跑测试不会弄脏开发环境的数据。
 * RabbitTemplate 用 mock 替代：只验证业务判断，不真的往开发环境的队列里投消息。
 */
@SpringBootTest
@ActiveProfiles("test")
public abstract class BaseIntegrationTest {

    @MockitoBean
    protected RabbitTemplate rabbitTemplate;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @Autowired
    protected SeckillActivityMapper activityMapper;

    @Autowired
    protected StringRedisTemplate redisTemplate;

    /** 每个用例开始前回到干净状态：清订单、清活动、清测试 Redis 库 */
    @BeforeEach
    void resetState() {
        jdbcTemplate.update("DELETE FROM t_seckill_order");
        jdbcTemplate.update("DELETE FROM t_seckill_activity");
        Set<String> keys = redisTemplate.keys("seckill:*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    /** 造一场已开始的活动 */
    protected Long seedActivity(int stock) {
        SeckillActivity activity = new SeckillActivity();
        activity.setGoodsId(1L);
        activity.setSeckillPrice(new BigDecimal("99.00"));
        activity.setSeckillStock(stock);
        activity.setStartTime(LocalDateTime.now().minusMinutes(10));
        activity.setEndTime(LocalDateTime.now().plusHours(1));
        activity.setStatus(1);
        activity.setVersion(0);
        activityMapper.insert(activity);
        return activity.getId();
    }

    /** 模拟「库存已经预热到 Redis」 */
    protected void preheat(Long activityId, int stock) {
        redisTemplate.opsForValue().set(CacheKeys.activityStock(activityId), String.valueOf(stock));
    }

    protected String redisStock(Long activityId) {
        return redisTemplate.opsForValue().get(CacheKeys.activityStock(activityId));
    }
}
