package com.seckill.controller;

import com.seckill.annotation.RateLimit;
import com.seckill.common.Result;
import com.seckill.service.SeckillOrderService;
import com.seckill.service.SeckillService;
import com.seckill.vo.SeckillOrderVO;
import com.seckill.vo.SeckillResultVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 秒杀接口（需要登录）
 * <p>
 * 注意路径：/api/seckill/activity/** 是放行的浏览接口，
 * 而 /api/seckill/order/** 会被登录拦截器拦住。
 */
@RestController
@RequestMapping("/api/seckill")
@RequiredArgsConstructor
public class SeckillController {

    private final SeckillService seckillService;
    private final SeckillOrderService orderService;

    /** 抢购（异步下单：立刻返回"排队中"，订单由消费者异步生成）
     *  限流：同一用户 1 秒内最多 5 次请求，挡住脚本刷单 */
    @RateLimit(type = RateLimit.Type.USER, window = 1, count = 5)
    @PostMapping("/order/{activityId}")
    public Result<SeckillResultVO> seckill(@PathVariable Long activityId) {
        return Result.success(seckillService.seckill(activityId));
    }

    /** 查询抢购结果（前端轮询这个接口确认最终结果） */
    @GetMapping("/order/result/{activityId}")
    public Result<SeckillResultVO> result(@PathVariable Long activityId) {
        return Result.success(seckillService.queryResult(activityId));
    }

    /** 我的秒杀订单 */
    @GetMapping("/order/my")
    public Result<List<SeckillOrderVO>> myOrders() {
        return Result.success(seckillService.myOrders());
    }

    /** 支付订单（演示用：直接标记已支付，接入真实支付时换成回调） */
    @PostMapping("/order/{orderNo}/pay")
    public Result<Void> pay(@PathVariable String orderNo) {
        orderService.pay(orderNo);
        return Result.success();
    }
}
