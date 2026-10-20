package com.seckill.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 秒杀下单消息体
 * <p>
 * 抢购接口只负责"扣 Redis + 发消息"，真正的订单落库交给消费者异步完成。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SeckillMessage implements Serializable {

    private Long userId;

    private Long activityId;

    private Long goodsId;

    private BigDecimal amount;

    /** 订单号在发消息前就生成好，消费者直接落库，也方便前端凭它查询 */
    private String orderNo;
}
