package com.seckill.service;

import com.seckill.vo.SeckillOrderVO;

import java.util.List;

/**
 * 秒杀业务接口
 */
public interface SeckillService {

    /** 抢购：Lua 原子扣减库存 + 一人一单，成功则落库生成订单 */
    SeckillOrderVO seckill(Long activityId);

    /** 查询当前登录用户的秒杀订单 */
    List<SeckillOrderVO> myOrders();
}
