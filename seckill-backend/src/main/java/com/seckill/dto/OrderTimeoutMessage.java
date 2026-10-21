package com.seckill.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 订单超时消息体
 * <p>
 * 延迟队列只负责"等 15 分钟"，到点后把这个消息交给超时消费者，
 * 由它回数据库判断订单是否还停在待支付。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderTimeoutMessage implements Serializable {

    private String orderNo;

    private Long userId;

    private Long activityId;
}
