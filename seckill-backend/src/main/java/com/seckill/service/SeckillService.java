package com.seckill.service;

import com.seckill.vo.SeckillOrderVO;
import com.seckill.vo.SeckillResultVO;

import java.util.List;

/**
 * 秒杀业务接口
 */
public interface SeckillService {

    /** 抢购：Lua 原子扣减库存 + 一人一单，成功后发消息异步生成订单 */
    SeckillResultVO seckill(Long activityId);

    /** 查询抢购结果：排队中 / 抢购成功 / 未抢到 */
    SeckillResultVO queryResult(Long activityId);

    /** 查询当前登录用户的秒杀订单 */
    List<SeckillOrderVO> myOrders();
}
