package com.seckill.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.seckill.entity.Goods;
import com.seckill.exception.BusinessException;
import com.seckill.mapper.GoodsMapper;
import com.seckill.service.GoodsService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 商品业务实现
 */
@Service
public class GoodsServiceImpl extends ServiceImpl<GoodsMapper, Goods> implements GoodsService {

    private static final int MAX_PAGE_SIZE = 100;

    @Override
    public IPage<Goods> pageQuery(Integer pageNum, Integer pageSize, String keyword, String category) {
        // 防御非法分页参数：页码至少 1，每页条数上限 100，避免被一次拉走全表
        int num = (pageNum == null || pageNum < 1) ? 1 : pageNum;
        int size = (pageSize == null || pageSize < 1) ? 10 : Math.min(pageSize, MAX_PAGE_SIZE);

        LambdaQueryWrapper<Goods> wrapper = new LambdaQueryWrapper<Goods>()
                .eq(Goods::getStatus, 1)
                .like(StringUtils.hasText(keyword), Goods::getName, keyword)
                .eq(StringUtils.hasText(category), Goods::getCategory, category)
                .orderByDesc(Goods::getId);

        // selectPage 由 PaginationInnerInterceptor 插件改写 SQL，自动加 LIMIT
        return getBaseMapper().selectPage(new Page<>(num, size), wrapper);
    }

    @Override
    public Goods getDetail(Long id) {
        Goods goods = getBaseMapper().selectById(id);
        if (goods == null) {
            throw new BusinessException(404, "商品不存在");
        }
        return goods;
    }
}
