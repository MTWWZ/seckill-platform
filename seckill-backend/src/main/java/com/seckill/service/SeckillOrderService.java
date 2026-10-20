package com.seckill.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.spring.service.IService;
import com.seckill.entity.SeckillOrder;
import com.seckill.vo.SeckillOrderVO;

/**
 * 秒杀订单查询服务（面向后台管理）
 */
public interface SeckillOrderService extends IService<SeckillOrder> {

    /** 分页查询所有秒杀订单，并补齐用户名与商品名 */
    IPage<SeckillOrderVO> pageQuery(Integer pageNum, Integer pageSize);
}
