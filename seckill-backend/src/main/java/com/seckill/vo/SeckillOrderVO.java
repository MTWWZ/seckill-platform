package com.seckill.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 秒杀订单展示对象
 */
@Data
public class SeckillOrderVO {

    private Long id;

    private String orderNo;

    private Long activityId;

    private Long goodsId;

    /** 下单用户 id（后台订单列表会用到） */
    private Long userId;

    /** 下单用户账号 */
    private String username;

    /** 下单用户昵称 */
    private String nickname;

    /** 商品名称 */
    private String goodsName;

    private BigDecimal amount;

    private Integer status;

    /** 状态文案：待支付 / 已支付 / 已取消 / 已完成 */
    private String statusText;

    private LocalDateTime createTime;
}
