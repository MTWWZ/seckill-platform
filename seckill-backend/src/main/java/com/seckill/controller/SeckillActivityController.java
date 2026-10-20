package com.seckill.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.seckill.common.Result;
import com.seckill.service.SeckillActivityService;
import com.seckill.vo.SeckillActivityVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 秒杀活动浏览接口（游客可访问）
 */
@RestController
@RequestMapping("/api/seckill/activity")
@RequiredArgsConstructor
public class SeckillActivityController {

    private final SeckillActivityService activityService;

    /** 活动列表 */
    @GetMapping("/page")
    public Result<IPage<SeckillActivityVO>> page(@RequestParam(defaultValue = "1") Integer pageNum,
                                                 @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(activityService.pageQuery(pageNum, pageSize));
    }

    /** 活动详情（含商品信息与 Redis 实时库存） */
    @GetMapping("/{id}")
    public Result<SeckillActivityVO> detail(@PathVariable Long id) {
        return Result.success(activityService.getDetail(id));
    }
}
