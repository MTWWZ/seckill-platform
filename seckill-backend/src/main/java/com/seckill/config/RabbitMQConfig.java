package com.seckill.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 配置
 * <p>
 * 直连交换机 + 持久化队列 + 死信队列：
 * 消费者处理失败的消息（重试耗尽后）会被投递到死信队列，方便排查和人工重放。
 */
@Configuration
public class RabbitMQConfig {

    /** 秒杀订单交换机 */
    public static final String SECKILL_EXCHANGE = "seckill.order.exchange";
    /** 秒杀订单队列 */
    public static final String SECKILL_ORDER_QUEUE = "seckill.order.queue";
    /** 秒杀下单路由键 */
    public static final String SECKILL_ORDER_ROUTING_KEY = "seckill.order.create";

    /** 死信交换机 */
    public static final String SECKILL_DLX_EXCHANGE = "seckill.dlx.exchange";
    /** 死信队列 */
    public static final String SECKILL_DEAD_QUEUE = "seckill.dead.queue";
    /** 死信路由键 */
    public static final String SECKILL_DEAD_ROUTING_KEY = "seckill.dead";

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
