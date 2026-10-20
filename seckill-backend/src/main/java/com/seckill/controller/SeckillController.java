package com.seckill.controller;

import com.seckill.common.Result;
import com.seckill.service.SeckillService;
import com.seckill.vo.SeckillOrderVO;
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

    /** 抢购 */
    @PostMapping("/order/{activityId}")
    public Result<SeckillOrderVO> seckill(@PathVariable Long activityId) {
        return Result.success(seckillService.seckill(activityId));
    }

    /** 我的秒杀订单 */
    @GetMapping("/order/my")
    public Result<List<SeckillOrderVO>> myOrders() {
        return Result.success(seckillService.myOrders());
    }
}
