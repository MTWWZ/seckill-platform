package com.seckill.exception;

import lombok.Getter;

/**
 * 业务异常
 * <p>
 * 业务逻辑中主动抛出，比如"库存不足""账号已存在"，
 * 由 GlobalExceptionHandler 统一捕获并转换成 Result 返回给前端。
 */
@Getter
public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(String message) {
        this(500, message);
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }
}
