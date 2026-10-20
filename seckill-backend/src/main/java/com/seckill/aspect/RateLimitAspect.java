package com.seckill.aspect;

import com.seckill.annotation.RateLimit;
import com.seckill.common.UserContext;
import com.seckill.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;

/**
 * 限流切面
 * <p>
 * 拦截标注了 @RateLimit 的方法，用 Redis + Lua 做固定窗口计数限流。
 * 限流放在业务逻辑之前，被限流的请求根本不会走到秒杀扣减，能有效挡住刷子。
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class RateLimitAspect {

    /** 被限流时返回的业务码 */
    private static final int CODE_TOO_MANY_REQUESTS = 429;

    private static final DefaultRedisScript<Long> RATE_LIMIT_SCRIPT;

    static {
        RATE_LIMIT_SCRIPT = new DefaultRedisScript<>();
        RATE_LIMIT_SCRIPT.setLocation(new ClassPathResource("scripts/rate_limit.lua"));
        RATE_LIMIT_SCRIPT.setResultType(Long.class);
    }

    private final StringRedisTemplate redisTemplate;

    @Around("@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        String key = buildKey(rateLimit);

        Long remain = redisTemplate.execute(RATE_LIMIT_SCRIPT,
                List.of(key),
                String.valueOf(rateLimit.window()),
                String.valueOf(rateLimit.count()));

        if (remain == null) {
            throw new BusinessException(500, "限流服务异常，请稍后重试");
        }
        if (remain < 0) {
            log.warn("触发限流：key={} 阈值={}/{}秒", key, rateLimit.count(), rateLimit.window());
            throw new BusinessException(CODE_TOO_MANY_REQUESTS, "请求过于频繁，请稍后再试");
        }

        return joinPoint.proceed();
    }

    private String buildKey(RateLimit rateLimit) {
        String suffix = switch (rateLimit.type()) {
            case IP -> "ip:" + getClientIp();
            case GLOBAL -> "global";
            case USER -> "user:" + UserContext.getUserId();
        };
        return rateLimit.prefix() + suffix;
    }

    /** 取客户端真实 IP，兼容 Nginx 反向代理 */
    private String getClientIp() {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return "unknown";
        }
        HttpServletRequest request = attributes.getRequest();
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            // X-Forwarded-For 可能是 "客户端IP, 代理1, 代理2"，取第一个
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
