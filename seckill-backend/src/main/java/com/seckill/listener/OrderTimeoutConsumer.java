package com.seckill.listener;

import com.rabbitmq.client.Channel;
import com.seckill.config.RabbitMQConfig;
import com.seckill.dto.OrderTimeoutMessage;
import com.seckill.service.SeckillOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 订单超时消费者
 * <p>
 * 消息来自延迟队列：下单 15 分钟后才被投递到这里。
 * 收到后先回数据库确认订单是否还停在"待支付"——
 * 已支付的直接忽略，未支付的取消订单并把库存加回 Redis。
 * <p>
 * 幂等：取消成功与否由"status=0 才更新"决定，重复投递不会重复回补库存。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderTimeoutConsumer {

    private final SeckillOrderService orderService;

    @RabbitListener(queues = RabbitMQConfig.SECKILL_TIMEOUT_QUEUE)
    public void handleTimeout(OrderTimeoutMessage message, Channel channel,
                              @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        try {
            boolean cancelled = orderService.cancelIfUnpaid(message.getOrderNo());
            if (cancelled) {
                log.info("订单超时未支付，已自动取消：orderNo={}", message.getOrderNo());
            } else {
                log.info("订单无需取消（不存在或已支付）：orderNo={}", message.getOrderNo());
            }
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("订单超时处理失败，消息进入死信队列：orderNo={}", message.getOrderNo(), e);
            channel.basicNack(deliveryTag, false, false);
        }
    }
}
