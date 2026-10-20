package com.seckill.task;

import com.seckill.service.SeckillActivityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 秒杀库存预热定时任务
 * <p>
 * 每 30 秒扫一次：把"5 分钟内就要开始"的活动库存写进 Redis。
 * 这样活动一开始，请求就能直接扣 Redis，不用等第一次访问才去数据库加载。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SeckillPreheatTask {

    private final SeckillActivityService activityService;

    @Scheduled(fixedDelay = 30_000, initialDelay = 10_000)
    public void preheat() {
        try {
            int count = activityService.preheatPendingActivities();
            if (count > 0) {
                log.info("秒杀库存预热完成：本次预热 {} 个活动", count);
            }
        } catch (Exception e) {
            // 定时任务里异常必须自己兜住，否则一次失败会导致后续调度中断
            log.error("秒杀库存预热失败", e);
        }
    }
}
