package com.seckill.config;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.QueueInformation;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.function.Supplier;

/**
 * 自定义监控指标
 * <p>
 * Actuator 自带的指标覆盖了 HTTP、JVM、连接池，
 * 这里再补一个业务相关的：各队列的积压消息数。
 * Grafana 每 15 秒抓一次时才会真正去问 Broker，平时不产生额外开销。
 */
@Slf4j
@Configuration
public class MetricsConfig {

    /** 四个队列的积压数量都暴露出来，方便判断消费者是否跟得上 */
    private static final List<String> QUEUES = List.of(
            RabbitMQConfig.SECKILL_ORDER_QUEUE,
            RabbitMQConfig.SECKILL_DELAY_QUEUE,
            RabbitMQConfig.SECKILL_TIMEOUT_QUEUE,
            RabbitMQConfig.SECKILL_DEAD_QUEUE);

    /**
     * 这里自己 new RabbitAdmin 而不是注入：
     * 测试上下文里并没有 RabbitAdmin 这个 bean（消费者被关闭），
     * 注入会直接导致 MetricsConfig 创建失败；而查询队列信息只需要 ConnectionFactory。
     */
    private final RabbitAdmin rabbitAdmin;
    private final MeterRegistry meterRegistry;

    public MetricsConfig(ConnectionFactory connectionFactory, MeterRegistry meterRegistry) {
        this.rabbitAdmin = new RabbitAdmin(connectionFactory);
        this.meterRegistry = meterRegistry;
    }

    @PostConstruct
    public void bindQueueGauges() {
        for (String queue : QUEUES) {
            Supplier<Number> supplier = () -> queueDepth(queue);
            Gauge.builder("seckill_queue_messages", supplier)
                    .description("队列中待消费的消息数")
                    .tag("queue", queue)
                    .register(meterRegistry);
        }
    }

    private double queueDepth(String queue) {
        try {
            QueueInformation info = rabbitAdmin.getQueueInfo(queue);
            return info == null ? 0 : info.getMessageCount();
        } catch (Exception e) {
            // 队列还没声明出来时（比如首次启动瞬间）不抛异常，避免整次抓取失败
            log.debug("读取队列积压失败：queue={}", queue, e);
            return 0;
        }
    }
}
