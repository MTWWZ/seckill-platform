package com.seckill.task;

import com.seckill.service.SeckillOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 超时订单补偿任务
 * <p>
 * 正常路径是 RabbitMQ 延迟队列到点后取消订单。
 * 但如果投递消息失败、或者 Broker 重启把延迟消息弄丢了，
 * 这层定时扫描能保证"超时未支付的订单最终一定被取消"，库存不会一直被占着。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SeckillOrderTimeoutTask {

    private final SeckillOrderService orderService;

    @Scheduled(fixedDelay = 60_000, initialDelay = 30_000)
    public void sweepTimeoutOrders() {
        try {
            int count = orderService.cancelTimeoutOrders();
            if (count > 0) {
                log.info("超时订单补偿：本次取消 {} 单并回补库存", count);
            }
        } catch (Exception e) {
            // 定时任务内部自己兜住异常，避免调度中断
            log.error("超时订单补偿任务失败", e);
        }
    }
}
