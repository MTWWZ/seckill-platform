package com.seckill.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.seckill.common.Result;
import com.seckill.service.SeckillOrderService;
import com.seckill.vo.SeckillOrderVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 订单管理接口（需要登录）
 */
@RestController
@RequestMapping("/api/admin/order")
@RequiredArgsConstructor
public class AdminOrderController {

    private final SeckillOrderService orderService;

    /** 分页查询所有秒杀订单 */
    @GetMapping("/page")
    public Result<IPage<SeckillOrderVO>> page(@RequestParam(defaultValue = "1") Integer pageNum,
                                              @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(orderService.pageQuery(pageNum, pageSize));
    }
}
