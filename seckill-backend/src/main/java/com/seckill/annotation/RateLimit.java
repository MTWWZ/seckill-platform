package com.seckill.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 接口限流注解
 * <p>
 * 用法：加在 Controller 方法上即可，由 RateLimitAspect 用 Redis + Lua 实现固定窗口限流。
 * 例：@RateLimit(type = RateLimit.Type.USER, window = 1, count = 5)
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {

    /** 限流维度 */
    enum Type {
        /** 按用户限流（需要登录，取当前登录用户 id） */
        USER,
        /** 按客户端 IP 限流 */
        IP,
        /** 全局限流（所有请求共享一个计数） */
        GLOBAL
    }

    Type type() default Type.USER;

    /** 时间窗口，单位：秒 */
    int window() default 1;

    /** 窗口内允许的最大请求数 */
    int count() default 5;

    /** Redis key 前缀 */
    String prefix() default "seckill:rate:";
}
