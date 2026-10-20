package com.seckill.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.seckill.common.CacheKeys;
import com.seckill.entity.Goods;
import com.seckill.exception.BusinessException;
import com.seckill.mapper.GoodsMapper;
import com.seckill.service.GoodsService;
import com.seckill.util.CacheUtil;
import com.seckill.util.JsonUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;

/**
 * 商品业务实现
 */
@Service
@RequiredArgsConstructor
public class GoodsServiceImpl extends ServiceImpl<GoodsMapper, Goods> implements GoodsService {

    private static final int MAX_PAGE_SIZE = 100;
    /** 商品详情缓存基础时长 */
    private static final Duration DETAIL_CACHE_TTL = Duration.ofMinutes(30);
    /** 空值缓存时长（要短，否则商品补录后还会一直报"不存在"） */
    private static final Duration EMPTY_CACHE_TTL = Duration.ofMinutes(1);

    private final StringRedisTemplate redisTemplate;
    private final JsonUtil jsonUtil;

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

    /**
     * 商品详情
     * <p>
     * Cache-Aside 模式（旁路缓存）：先读缓存 -> 未命中查数据库 -> 回写缓存。
     * 这是最常用的缓存模式，读多写少的场景非常合适。
     */
    @Override
    public Goods getDetail(Long id) {
        String key = CacheKeys.goodsDetail(id);

        // 1. 先查缓存
        String cached = redisTemplate.opsForValue().get(key);
        if (CacheKeys.EMPTY_PLACEHOLDER.equals(cached)) {
            // 之前查过且确实不存在，直接返回，不再打数据库（防穿透）
            throw new BusinessException(404, "商品不存在");
        }
        if (cached != null) {
            return jsonUtil.parse(cached, Goods.class);
        }

        // 2. 缓存未命中，查数据库
        Goods goods = getBaseMapper().selectById(id);

        // 3. 回写缓存
        if (goods == null) {
            // 不存在的商品也缓存一个占位符，挡掉反复用无效 id 的请求
            redisTemplate.opsForValue().set(key, CacheKeys.EMPTY_PLACEHOLDER, EMPTY_CACHE_TTL);
            throw new BusinessException(404, "商品不存在");
        }
        // TTL 加随机抖动，避免大批 key 同时失效（防雪崩）
        redisTemplate.opsForValue().set(key, jsonUtil.toJson(goods),
                CacheUtil.withJitter(DETAIL_CACHE_TTL, 300));
        return goods;
    }

    @Override
    public void updateGoods(Goods goods) {
        if (goods.getId() == null) {
            throw new BusinessException(400, "商品 id 不能为空");
        }
        int rows = getBaseMapper().updateById(goods);
        if (rows == 0) {
            throw new BusinessException(404, "商品不存在");
        }
        // 缓存一致性：写完数据库"删除"缓存，而不是更新缓存
        // 删缓存更简单也更安全：更新缓存在并发下可能写入旧值，删除则最多让下一次请求回源
        redisTemplate.delete(CacheKeys.goodsDetail(goods.getId()));
    }

    @Override
    public void deleteGoods(Long id) {
        int rows = getBaseMapper().deleteById(id);
        if (rows == 0) {
            throw new BusinessException(404, "商品不存在");
        }
        redisTemplate.delete(CacheKeys.goodsDetail(id));
    }
}
