package com.seckill.controller;

import com.seckill.common.Result;
import com.seckill.dto.SeckillActivityCreateDTO;
import com.seckill.service.SeckillActivityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 秒杀活动管理接口（需要登录）
 */
@RestController
@RequestMapping("/api/admin/seckill/activity")
@RequiredArgsConstructor
public class AdminSeckillActivityController {

    private final SeckillActivityService activityService;

    /** 创建活动（会自动预热库存） */
    @PostMapping
    public Result<Long> create(@RequestBody @Valid SeckillActivityCreateDTO dto) {
        return Result.success(activityService.createActivity(dto));
    }

    /** 手动预热某个活动的库存 */
    @PostMapping("/{id}/preheat")
    public Result<Void> preheat(@PathVariable Long id) {
        activityService.preheatStock(id);
        return Result.success();
    }

    /** 手动触发一次批量预热（和定时任务做同一件事） */
    @PostMapping("/preheat-pending")
    public Result<Integer> preheatPending() {
        return Result.success(activityService.preheatPendingActivities());
    }
}
