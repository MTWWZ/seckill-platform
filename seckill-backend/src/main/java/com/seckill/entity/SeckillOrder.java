package com.seckill.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 秒杀订单实体，对应表 t_seckill_order
 */
@Data
@TableName("t_seckill_order")
public class SeckillOrder {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 订单号 */
    private String orderNo;

    private Long userId;

    private Long activityId;

    private Long goodsId;

    /** 成交金额 */
    private BigDecimal amount;

    /** 状态：0-待支付 1-已支付 2-已取消 3-已完成 */
    private Integer status;

    private LocalDateTime payTime;

    @TableLogic
    private Integer deleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
