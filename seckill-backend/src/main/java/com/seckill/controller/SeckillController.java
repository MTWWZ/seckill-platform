package com.seckill.controller;

import com.seckill.common.Result;
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

    /** 抢购（异步下单：立刻返回"排队中"，订单由消费者异步生成） */
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
}
