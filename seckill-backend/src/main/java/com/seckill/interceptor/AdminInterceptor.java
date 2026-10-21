package com.seckill.interceptor;

import com.seckill.common.Result;
import com.seckill.common.UserContext;
import com.seckill.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 管理员权限拦截器
 * <p>
 * 认证和授权分开做两层：
 * 1. LoginInterceptor 负责"你是谁"——校验 JWT，把用户放进 UserContext
 * 2. 这里负责"你能不能"——只放行 role=1 的账号访问 /api/admin/**
 * <p>
 * 顺序很重要：WebMvcConfig 里本拦截器注册在登录拦截器之后，
 * 所以执行到这里时 UserContext 一定已经填好了。
 */
@Component
public class AdminInterceptor implements HandlerInterceptor {

    /** 管理员角色值，与 t_user.role 保持一致 */
    private static final int ROLE_ADMIN = 1;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 跨域预检不需要权限判断
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        UserContext.CurrentUser user = UserContext.get();
        if (user == null || user.getUserId() == null) {
            throw new BusinessException(Result.UNAUTHORIZED, "未登录，请先登录");
        }
        if (user.getRole() == null || user.getRole() != ROLE_ADMIN) {
            throw new BusinessException(Result.FORBIDDEN, "需要管理员权限");
        }
        return true;
    }
}
