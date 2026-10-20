package com.seckill.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 秒杀活动展示对象
 * <p>
 * 把活动、商品、Redis 实时库存三份数据拼在一起，
 * 前端一次请求就能拿到渲染所需的所有字段。
 */
@Data
public class SeckillActivityVO {

    private Long id;

    private Long goodsId;

    /** 商品名称 */
    private String goodsName;

    /** 商品卖点标题 */
    private String goodsTitle;

    /** 商品图片 */
    private String goodsImgUrl;

    /** 商品原价 */
    private BigDecimal originPrice;

    /** 秒杀价 */
    private BigDecimal seckillPrice;

    /** 数据库里的活动库存（初始值） */
    private Integer seckillStock;

    /** Redis 里的实时库存（null 表示还没预热） */
    private Integer redisStock;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    /** 根据当前时间计算的状态文案：未开始 / 进行中 / 已结束 */
    private String statusText;
}
