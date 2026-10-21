package com.seckill.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 配置
 * <p>
 * 直连交换机 + 持久化队列 + 死信队列：
 * 消费者处理失败的消息（重试耗尽后）会被投递到死信队列，方便排查和人工重放。
 * <p>
 * 另外用「TTL + 死信」实现延迟队列：
 * 下单消息落库后，往 seckill.order.delay 队列投一条消息，队列本身设了 15 分钟 TTL，
 * 到期没有被消费的消息会被死信到 seckill.order.timeout 队列，
 * 由超时消费者判断"订单是否还是待支付"，是就取消并回补库存。
 */
@Configuration
public class RabbitMQConfig {

    /** 秒杀订单交换机 */
    public static final String SECKILL_EXCHANGE = "seckill.order.exchange";
    /** 秒杀订单队列 */
    public static final String SECKILL_ORDER_QUEUE = "seckill.order.queue";
    /** 秒杀下单路由键 */
    public static final String SECKILL_ORDER_ROUTING_KEY = "seckill.order.create";

    /** 延迟队列：消息在这里躺满 TTL 后自动死信到超时队列 */
    public static final String SECKILL_DELAY_QUEUE = "seckill.order.delay.queue";
    /** 投递延迟消息用的路由键 */
    public static final String SECKILL_DELAY_ROUTING_KEY = "seckill.order.delay";

    /** 超时队列：延迟到期的消息最终落到这里 */
    public static final String SECKILL_TIMEOUT_QUEUE = "seckill.order.timeout.queue";
    /** 超时消息路由键 */
    public static final String SECKILL_TIMEOUT_ROUTING_KEY = "seckill.order.timeout";

    /** 死信交换机 */
    public static final String SECKILL_DLX_EXCHANGE = "seckill.dlx.exchange";
    /** 死信队列 */
    public static final String SECKILL_DEAD_QUEUE = "seckill.dead.queue";
    /** 死信路由键 */
    public static final String SECKILL_DEAD_ROUTING_KEY = "seckill.dead";

    /**
     * 订单超时时长（分钟）
     * <p>
     * 延迟队列的 TTL 和定时补偿任务都读这个值，改的时候两边会一起变。
     * 注意：队列的 TTL 是声明时写进 Broker 的，改了值需要先删掉旧队列才会生效。
     */
    @Value("${seckill.order.timeout-minutes:15}")
    private long timeoutMinutes;

    @Bean
    public DirectExchange seckillExchange() {
        // durable=true 表示交换机持久化，Broker 重启后不会丢
        return new DirectExchange(SECKILL_EXCHANGE, true, false);
    }

    @Bean
    public Queue seckillOrderQueue() {
        // 队列持久化 + 绑定死信交换机：处理失败的消息会进死信队列
        return QueueBuilder.durable(SECKILL_ORDER_QUEUE)
                .deadLetterExchange(SECKILL_DLX_EXCHANGE)
                .deadLetterRoutingKey(SECKILL_DEAD_ROUTING_KEY)
                .build();
    }

    @Bean
    public Binding seckillOrderBinding(Queue seckillOrderQueue, DirectExchange seckillExchange) {
        return BindingBuilder.bind(seckillOrderQueue).to(seckillExchange).with(SECKILL_ORDER_ROUTING_KEY);
    }

    /**
     * 延迟队列：没有消费者，消息在这里只是"等过期"。
     * 到期后按 x-dead-letter-* 转到超时路由键，进入超时队列。
     */
    @Bean
    public Queue seckillDelayQueue() {
        return QueueBuilder.durable(SECKILL_DELAY_QUEUE)
                .ttl((int) (timeoutMinutes * 60 * 1000))
                .deadLetterExchange(SECKILL_EXCHANGE)
                .deadLetterRoutingKey(SECKILL_TIMEOUT_ROUTING_KEY)
                .build();
    }

    @Bean
    public Binding seckillDelayBinding(Queue seckillDelayQueue, DirectExchange seckillExchange) {
        return BindingBuilder.bind(seckillDelayQueue).to(seckillExchange).with(SECKILL_DELAY_ROUTING_KEY);
    }

    @Bean
    public Queue seckillTimeoutQueue() {
        // 取消订单也可能失败（比如数据库抖动），失败同样进死信队列
        return QueueBuilder.durable(SECKILL_TIMEOUT_QUEUE)
                .deadLetterExchange(SECKILL_DLX_EXCHANGE)
                .deadLetterRoutingKey(SECKILL_DEAD_ROUTING_KEY)
                .build();
    }

    @Bean
    public Binding seckillTimeoutBinding(Queue seckillTimeoutQueue, DirectExchange seckillExchange) {
        return BindingBuilder.bind(seckillTimeoutQueue).to(seckillExchange).with(SECKILL_TIMEOUT_ROUTING_KEY);
    }

    @Bean
    public DirectExchange seckillDlxExchange() {
        return new DirectExchange(SECKILL_DLX_EXCHANGE, true, false);
    }

    @Bean
    public Queue seckillDeadQueue() {
        return QueueBuilder.durable(SECKILL_DEAD_QUEUE).build();
    }

    @Bean
    public Binding seckillDeadBinding(Queue seckillDeadQueue, DirectExchange seckillDlxExchange) {
        return BindingBuilder.bind(seckillDeadQueue).to(seckillDlxExchange).with(SECKILL_DEAD_ROUTING_KEY);
    }

    /**
     * 消息用 JSON 序列化，理由和 Redis 那里一样：
     * 默认的 JDK 序列化在管理后台里是一串乱码，看不懂也不方便跨语言。
     */
    @Bean
    public MessageConverter jacksonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }
}
