package com.seckill;

import com.seckill.common.CacheKeys;
import com.seckill.common.UserContext;
import com.seckill.exception.BusinessException;
import com.seckill.service.SeckillService;
import com.seckill.support.BaseIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

/**
 * 秒杀核心逻辑测试
 * <p>
 * 重点验证两件事：
 * 1. 并发下不超卖（Lua 原子扣减）
 * 2. 同一用户只能抢到一单（一人一单）
 */
class SeckillConcurrencyTest extends BaseIntegrationTest {

    @Autowired
    private SeckillService seckillService;

    @Test
    @DisplayName("30 个用户并发抢 10 件：恰好 10 单成功，库存为 0 不为负")
    void shouldNotOversellUnderConcurrency() throws Exception {
        Long activityId = seedActivity(10);
        preheat(activityId, 10);

        int users = 30;
        ExecutorService pool = Executors.newFixedThreadPool(users);
        CountDownLatch ready = new CountDownLatch(users);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger success = new AtomicInteger();
        AtomicInteger soldOut = new AtomicInteger();
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 1; i <= users; i++) {
            long userId = i;
            futures.add(pool.submit(() -> {
                UserContext.set(new UserContext.CurrentUser(userId, "user" + userId, 0));
                ready.countDown();
                try {
                    // 所有线程卡在同一个起跑线上，尽量制造真实并发
                    start.await();
                    seckillService.seckill(activityId);
                    success.incrementAndGet();
                } catch (BusinessException e) {
                    if (e.getCode() == 400) {
                        soldOut.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    UserContext.clear();
                }
            }));
        }

        assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
        start.countDown();
        for (Future<?> future : futures) {
            future.get(15, TimeUnit.SECONDS);
        }
        pool.shutdown();

        assertThat(success.get()).as("成功下单数应等于活动库存").isEqualTo(10);
        assertThat(soldOut.get()).as("其余请求应因库存不足失败").isEqualTo(20);
        assertThat(redisStock(activityId)).as("库存不能扣成负数").isEqualTo("0");
        assertThat(redisTemplate.opsForSet().size(CacheKeys.activityUsers(activityId)))
                .as("参与用户集合应记录 10 个人")
                .isEqualTo(10);
    }

    @Test
    @DisplayName("同一用户重复抢购：第二次返回 409，库存只扣一次")
    void shouldRejectDuplicateOrderForSameUser() {
        Long activityId = seedActivity(10);
        preheat(activityId, 10);

        UserContext.set(new UserContext.CurrentUser(100L, "tester", 0));
        try {
            assertThat(seckillService.seckill(activityId).getStatus()).isEqualTo(0);
            assertThat(redisStock(activityId)).isEqualTo("9");

            BusinessException error = catchThrowableOfType(
                    () -> seckillService.seckill(activityId), BusinessException.class);

            assertThat(error).isNotNull();
            assertThat(error.getCode()).isEqualTo(409);
            assertThat(error.getMessage()).contains("已经抢购");
            assertThat(redisStock(activityId)).as("重复请求不应再扣库存").isEqualTo("9");
        } finally {
            UserContext.clear();
        }
    }
}
