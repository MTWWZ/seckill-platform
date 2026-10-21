package com.seckill.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.spring.service.IService;
import com.seckill.entity.SeckillOrder;
import com.seckill.vo.SeckillOrderVO;

/**
 * 秒杀订单服务
 * <p>
 * 面向后台的查询 + 面向用户的支付 / 取消。
 */
public interface SeckillOrderService extends IService<SeckillOrder> {

    /** 分页查询所有秒杀订单，并补齐用户名与商品名 */
    IPage<SeckillOrderVO> pageQuery(Integer pageNum, Integer pageSize);

    /** 支付订单（演示用，直接把状态从待支付改成已支付；重复支付幂等返回） */
    void pay(String orderNo);

    /**
     * 超时未支付则取消订单并回补 Redis 库存
     *
     * @return true=本次真的取消了；false=订单不存在或已经不是待支付（重复消息不会重复回补）
     */
    boolean cancelIfUnpaid(String orderNo);

    /** 扫一遍超时未支付的订单，返回本次取消的数量（定时任务兜底用） */
    int cancelTimeoutOrders();
}
