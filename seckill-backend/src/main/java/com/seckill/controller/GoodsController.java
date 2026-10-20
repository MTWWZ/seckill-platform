package com.seckill.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.seckill.common.Result;
import com.seckill.entity.Goods;
import com.seckill.service.GoodsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 商品接口（浏览类接口，游客可访问）
 */
@RestController
@RequestMapping("/api/goods")
@RequiredArgsConstructor
public class GoodsController {

    private final GoodsService goodsService;

    /** 分页查询商品 */
    @GetMapping("/page")
    public Result<IPage<Goods>> page(@RequestParam(defaultValue = "1") Integer pageNum,
                                     @RequestParam(defaultValue = "10") Integer pageSize,
                                     @RequestParam(required = false) String keyword,
                                     @RequestParam(required = false) String category) {
        return Result.success(goodsService.pageQuery(pageNum, pageSize, keyword, category));
    }

    /** 商品详情 */
    @GetMapping("/{id}")
    public Result<Goods> detail(@PathVariable Long id) {
        return Result.success(goodsService.getDetail(id));
    }
}
