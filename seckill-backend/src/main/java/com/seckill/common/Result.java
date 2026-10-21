package com.seckill.common;

import lombok.Data;

/**
 * 统一响应结果
 * <p>
 * 所有接口都返回这个结构，前端只需要判断 code 即可：
 * {
 *   "code": 200,
 *   "message": "操作成功",
 *   "data": { ... }
 * }
 */
@Data
public class Result<T> {

    /** 业务状态码：200-成功，其他为失败 */
    public static final int SUCCESS = 200;
    public static final int BAD_REQUEST = 400;
    public static final int UNAUTHORIZED = 401;
    /** 已登录但没有权限（比如普通用户访问管理接口） */
    public static final int FORBIDDEN = 403;
    public static final int ERROR = 500;

    private Integer code;
    private String message;
    private T data;

    private Result(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> Result<T> success() {
        return new Result<>(SUCCESS, "操作成功", null);
    }

    public static <T> Result<T> success(T data) {
        return new Result<>(SUCCESS, "操作成功", data);
    }

    public static <T> Result<T> error(Integer code, String message) {
        return new Result<>(code, message, null);
    }
}
