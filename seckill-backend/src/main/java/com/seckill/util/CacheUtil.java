package com.seckill.util;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 缓存相关工具
 */
public final class CacheUtil {

    private CacheUtil() {
    }

    /**
     * 给基础过期时间加一个随机抖动
     * <p>
     * 如果一批 key 的 TTL 完全相同，它们会在同一秒集体失效，
     * 请求瞬间全部打到数据库 —— 这就是"缓存雪崩"。
     * 加随机值可以把失效时间错开。
     */
    public static Duration withJitter(Duration base, int jitterSeconds) {
        return base.plusSeconds(ThreadLocalRandom.current().nextLong(jitterSeconds + 1L));
    }
}
