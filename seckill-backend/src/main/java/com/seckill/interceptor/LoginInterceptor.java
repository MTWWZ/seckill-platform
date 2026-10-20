package com.seckill.interceptor;

import com.seckill.common.Result;
import com.seckill.common.UserContext;
import com.seckill.config.JwtProperties;
import com.seckill.exception.BusinessException;
import com.seckill.util.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 登录拦截器
 * <p>
 * 校验请求头里的 JWT，通过则把用户信息写入 UserContext，失败则抛业务异常。
 */
@Component
@RequiredArgsConstructor
public class LoginInterceptor implements HandlerInterceptor {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtUtil jwtUtil;
    private final JwtProperties jwtProperties;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 跨域预检请求直接放行
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String token = request.getHeader(jwtProperties.getHeader());
        if (token != null && token.startsWith(BEARER_PREFIX)) {
            token = token.substring(BEARER_PREFIX.length());
        }
        if (token == null || token.isBlank()) {
            throw new BusinessException(Result.UNAUTHORIZED, "未登录，请先登录");
        }

        Claims claims = jwtUtil.parseToken(token);
        if (claims == null) {
            throw new BusinessException(Result.UNAUTHORIZED, "登录已过期，请重新登录");
        }

        UserContext.set(new UserContext.CurrentUser(
                Long.valueOf(claims.getSubject()),
                claims.get("username", String.class),
                claims.get("role", Integer.class)));
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        // 请求结束一定要清理，防止线程池复用时串到下一个请求
        UserContext.clear();
    }
}
