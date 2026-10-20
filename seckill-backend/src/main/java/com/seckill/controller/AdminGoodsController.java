package com.seckill.controller;

import com.seckill.common.Result;
import com.seckill.entity.Goods;
import com.seckill.service.GoodsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 商品管理接口（需要登录）
 * <p>
 * 放在 /api/admin/** 而不是 /api/goods/**，
 * 因为 /api/goods/** 是游客可见的浏览接口，写操作不能混在里面。
 * 管理员角色校验会在后续阶段补上。
 */
@RestController
@RequestMapping("/api/admin/goods")
@RequiredArgsConstructor
public class AdminGoodsController {

    private final GoodsService goodsService;

    /** 新增商品 */
    @PostMapping
    public Result<Long> create(@RequestBody Goods goods) {
        goodsService.createGoods(goods);
        return Result.success(goods.getId());
    }

    /** 修改商品 */
    @PutMapping
    public Result<Void> update(@RequestBody Goods goods) {
        goodsService.updateGoods(goods);
        return Result.success();
    }

    /** 删除商品（逻辑删除） */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        goodsService.deleteGoods(id);
        return Result.success();
    }
}
