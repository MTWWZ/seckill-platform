package com.seckill.listener;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rabbitmq.client.Channel;
import com.seckill.config.RabbitMQConfig;
import com.seckill.dto.OrderTimeoutMessage;
import com.seckill.dto.SeckillMessage;
import com.seckill.entity.SeckillOrder;
import com.seckill.mapper.SeckillOrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 秒杀订单消费者
 * <p>
 * 抢购接口发完消息就返回，真正的落库在这里异步做：
 * 请求高峰期 MQ 先把消息堆起来，消费者按自己的节奏慢慢处理，数据库不会被压垮（削峰）。
 * <p>
 * 可靠性设计：
 * 1. 手动 ACK —— 只有落库成功才确认消息
 * 2. 幂等 —— 先查一次 + 数据库唯一索引兜底，重复投递不会生成重复订单
 * 3. 处理失败 Nack 且不重新入队 —— 消息进入死信队列，避免死循环
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SeckillOrderConsumer {

    /** 订单状态：待支付 */
    private static final int ORDER_STATUS_UNPAID = 0;

    private final SeckillOrderMapper orderMapper;
    private final RabbitTemplate rabbitTemplate;

    @RabbitListener(queues = RabbitMQConfig.SECKILL_ORDER_QUEUE)
    public void handleSeckillOrder(SeckillMessage message, Channel channel,
                                   @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        try {
            // 幂等第一道：消息可能因网络抖动被重复投递，先查一次
            Long exists = orderMapper.selectCount(new LambdaQueryWrapper<SeckillOrder>()
                    .eq(SeckillOrder::getUserId, message.getUserId())
                    .eq(SeckillOrder::getActivityId, message.getActivityId()));
            if (exists != null && exists > 0) {
                channel.basicAck(deliveryTag, false);
                return;
            }

            SeckillOrder order = new SeckillOrder();
            order.setOrderNo(message.getOrderNo());
            order.setUserId(message.getUserId());
            order.setActivityId(message.getActivityId());
            order.setGoodsId(message.getGoodsId());
            order.setAmount(message.getAmount());
            order.setStatus(ORDER_STATUS_UNPAID);
            orderMapper.insert(order);

            // 订单落库后投一条延迟消息：15 分钟没支付就自动取消并回补库存。
            // 这里失败不影响用户下单（订单已经生成了），定时任务会兜底扫描。
            try {
                rabbitTemplate.convertAndSend(RabbitMQConfig.SECKILL_EXCHANGE,
                        RabbitMQConfig.SECKILL_DELAY_ROUTING_KEY,
                        new OrderTimeoutMessage(message.getOrderNo(), message.getUserId(), message.getActivityId()));
            } catch (Exception e) {
                log.error("投递超时消息失败，将由定时任务兜底：orderNo={}", message.getOrderNo(), e);
            }

            channel.basicAck(deliveryTag, false);
            log.info("秒杀订单已生成：orderNo={} userId={} activityId={}",
                    message.getOrderNo(), message.getUserId(), message.getActivityId());
        } catch (DuplicateKeyException e) {
            // 幂等第二道：数据库唯一索引 uk_user_activity 挡住了重复插入，同样算处理成功
            log.warn("重复消息已被唯一索引拦下：orderNo={}", message.getOrderNo());
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("秒杀订单落库失败，消息进入死信队列：{}", message, e);
            // requeue=false：不重新入队，交给死信交换机，避免失败消息无限循环
            channel.basicNack(deliveryTag, false, false);
        }
    }
}
