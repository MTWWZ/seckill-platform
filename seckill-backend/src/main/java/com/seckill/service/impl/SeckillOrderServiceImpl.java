package com.seckill.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.seckill.entity.Goods;
import com.seckill.entity.SeckillOrder;
import com.seckill.entity.User;
import com.seckill.mapper.GoodsMapper;
import com.seckill.mapper.SeckillOrderMapper;
import com.seckill.mapper.UserMapper;
import com.seckill.service.SeckillOrderService;
import com.seckill.vo.SeckillOrderVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 秒杀订单查询实现
 */
@Service
@RequiredArgsConstructor
public class SeckillOrderServiceImpl extends ServiceImpl<SeckillOrderMapper, SeckillOrder>
        implements SeckillOrderService {

    private static final int MAX_PAGE_SIZE = 100;

    private final UserMapper userMapper;
    private final GoodsMapper goodsMapper;

    @Override
    public IPage<SeckillOrderVO> pageQuery(Integer pageNum, Integer pageSize) {
        int num = (pageNum == null || pageNum < 1) ? 1 : pageNum;
        int size = (pageSize == null || pageSize < 1) ? 10 : Math.min(pageSize, MAX_PAGE_SIZE);

        IPage<SeckillOrder> page = getBaseMapper().selectPage(new Page<>(num, size),
                new LambdaQueryWrapper<SeckillOrder>().orderByDesc(SeckillOrder::getId));

        IPage<SeckillOrderVO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        List<SeckillOrderVO> records = page.getRecords().stream()
                .map(this::toVO)
                .collect(Collectors.toList());
        result.setRecords(records);
        return result;
    }

    private SeckillOrderVO toVO(SeckillOrder order) {
        SeckillOrderVO vo = new SeckillOrderVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setActivityId(order.getActivityId());
        vo.setGoodsId(order.getGoodsId());
        vo.setUserId(order.getUserId());
        vo.setAmount(order.getAmount());
        vo.setStatus(order.getStatus());
        vo.setStatusText(statusText(order.getStatus()));
        vo.setCreateTime(order.getCreateTime());

        User user = userMapper.selectById(order.getUserId());
        if (user != null) {
            vo.setUsername(user.getUsername());
            vo.setNickname(user.getNickname());
        }

        Goods goods = goodsMapper.selectById(order.getGoodsId());
        if (goods != null) {
            vo.setGoodsName(goods.getName());
        }
        return vo;
    }

    private String statusText(Integer status) {
        if (status == null) {
            return "未知";
        }
        return switch (status) {
            case 0 -> "待支付";
            case 1 -> "已支付";
            case 2 -> "已取消";
            case 3 -> "已完成";
            default -> "未知";
        };
    }
}
