package com.seckill.exception;

import com.seckill.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 * <p>
 * 作用：Controller 里抛出的异常不用自己 try-catch，
 * 这里统一转成 Result 返回，保证前端拿到的永远是同一种结构。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 业务异常：我们自己主动抛的，属于"可预期"的错误 */
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e) {
        log.warn("业务异常：{}", e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    /** 参数校验异常：@Valid 校验失败时抛出 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValidException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldError() == null
                ? "参数校验失败"
                : e.getBindingResult().getFieldError().getDefaultMessage();
        log.warn("参数校验失败：{}", message);
        return Result.error(Result.BAD_REQUEST, message);
    }

    /** 兜底：其他所有未捕获的异常，避免把堆栈信息直接暴露给前端 */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        log.error("系统异常", e);
        return Result.error(Result.ERROR, "系统繁忙，请稍后再试");
    }
}
