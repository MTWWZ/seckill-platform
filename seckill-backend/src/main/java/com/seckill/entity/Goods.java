package com.seckill.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品实体，对应表 t_goods
 */
@Data
@TableName("t_goods")
public class Goods {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 商品名称 */
    private String name;

    /** 卖点标题 */
    private String title;

    /** 商品描述 */
    private String description;

    /** 商品图片地址 */
    private String imgUrl;

    /** 原价（金额一律用 BigDecimal，不要用 double） */
    private BigDecimal originPrice;

    /** 总库存 */
    private Integer stock;

    /** 分类 */
    private String category;

    /** 状态：0-下架 1-上架 */
    private Integer status;

    @TableLogic
    private Integer deleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
