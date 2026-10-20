package com.seckill.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.spring.service.IService;
import com.seckill.dto.SeckillActivityCreateDTO;
import com.seckill.entity.SeckillActivity;
import com.seckill.vo.SeckillActivityVO;

/**
 * 秒杀活动业务接口
 */
public interface SeckillActivityService extends IService<SeckillActivity> {

    /** 分页查询活动 */
    IPage<SeckillActivityVO> pageQuery(Integer pageNum, Integer pageSize);

    /** 活动详情 */
    SeckillActivityVO getDetail(Long activityId);

    /** 创建活动，并立即预热库存到 Redis */
    Long createActivity(SeckillActivityCreateDTO dto);

    /** 把指定活动的库存写入 Redis */
    void preheatStock(Long activityId);

    /** 扫描"即将开始"的活动并补预热，返回本次预热数量（定时任务调用） */
    int preheatPendingActivities();
}
