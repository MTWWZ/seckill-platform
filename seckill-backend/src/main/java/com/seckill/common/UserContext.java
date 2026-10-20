package com.seckill.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 当前登录用户上下文
 * <p>
 * 拦截器解析 token 后把用户信息放进来，Controller / Service 随时取用，
 * 不用每个接口都写一遍"从请求头解析 token"。
 */
public class UserContext {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CurrentUser {
        private Long userId;
        private String username;
        private Integer role;
    }

    /** 每个请求都是一个独立线程，所以用 ThreadLocal 隔离 */
    private static final ThreadLocal<CurrentUser> HOLDER = new ThreadLocal<>();

    public static void set(CurrentUser user) {
        HOLDER.set(user);
    }

    public static CurrentUser get() {
        return HOLDER.get();
    }

    public static Long getUserId() {
        CurrentUser user = HOLDER.get();
        return user == null ? null : user.getUserId();
    }

    /** 必须清理，否则线程复用时会串数据 */
    public static void clear() {
        HOLDER.remove();
    }
}
