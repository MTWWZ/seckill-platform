package com.seckill.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.spring.service.IService;
import com.seckill.entity.Goods;

/**
 * 商品业务接口
 */
public interface GoodsService extends IService<Goods> {

    /** 分页查询商品（只返回上架商品） */
    IPage<Goods> pageQuery(Integer pageNum, Integer pageSize, String keyword, String category);

    /** 商品详情，不存在时抛业务异常 */
    Goods getDetail(Long id);
}
