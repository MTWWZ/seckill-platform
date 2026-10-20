package com.seckill.vo;

import lombok.Data;

/**
 * 抢购结果
 * <p>
 * 异步下单场景下，抢购接口不会直接返回订单，而是返回"排队中"，
 * 前端再通过查询接口确认最终结果。
 */
@Data
public class SeckillResultVO {

    private Long activityId;

    private String orderNo;

    /** 0-排队中 1-抢购成功 2-未抢到 */
    private Integer status;

    private String statusText;

    private String message;
}
