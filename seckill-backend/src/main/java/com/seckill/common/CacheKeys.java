package com.seckill.common;

/**
 * Redis key 统一定义在这里
 * <p>
 * 好处：key 不会散落在各个 Service 里，命名混乱、拼错导致缓存永远不命中。
 */
public final class CacheKeys {

    private CacheKeys() {
    }

    /** 商品详情：seckill:goods:detail:{id} */
    public static String goodsDetail(Long goodsId) {
        return "seckill:goods:detail:" + goodsId;
    }

    /**
     * 秒杀活动库存：seckill:activity:stock:{activityId}
     * <p>
     * 活动开始前把数据库里的活动库存"预热"到这里，
     * 抢购时直接扣 Redis，避免高并发下所有请求都去行锁竞争数据库。
     */
    public static String activityStock(Long activityId) {
        return "seckill:activity:stock:" + activityId;
    }

    /**
     * 参与过某活动的用户集合：seckill:activity:users:{activityId}
     * <p>
     * 用 Set 存 userId，配合 Lua 的 SISMEMBER 实现"一人一单"。
     */
    public static String activityUsers(Long activityId) {
        return "seckill:activity:users:" + activityId;
    }

    /**
     * 空值占位符
     * <p>
     * 查询结果为空时写入 Redis，用于防缓存穿透：
     * 恶意请求反复查询不存在的 id 时，直接命中这个占位符，不会打到数据库。
     */
    public static final String EMPTY_PLACEHOLDER = "__EMPTY__";
}
