package com.seckill.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 秒杀活动实体，对应表 t_seckill_activity
 * <p>
 * 活动库存（seckill_stock）与商品总库存（t_goods.stock）是分开维护的，
 * 秒杀只扣活动库存。
 */
@Data
@TableName("t_seckill_activity")
public class SeckillActivity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long goodsId;

    /** 秒杀价 */
    private BigDecimal seckillPrice;

    /** 活动库存（限量份数） */
    private Integer seckillStock;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    /** 状态：0-未开始 1-进行中 2-已结束 */
    private Integer status;

    /** 乐观锁版本号（阶段三会用到） */
    private Integer version;

    @TableLogic
    private Integer deleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
