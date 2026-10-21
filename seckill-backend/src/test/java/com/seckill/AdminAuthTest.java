package com.seckill;

import com.seckill.support.BaseIntegrationTest;
import com.seckill.util.JwtUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 管理接口的权限测试
 * <p>
 * 认证（有没有登录）和授权（是不是管理员）是两件事：
 * 普通用户拿着合法 token 也不应该能碰 /api/admin/**。
 */
@AutoConfigureMockMvc
class AdminAuthTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtil jwtUtil;

    @Test
    @DisplayName("普通用户访问管理接口：返回 403")
    void shouldForbidNormalUser() throws Exception {
        String token = jwtUtil.createToken(200L, "student01", 0);

        mockMvc.perform(get("/api/admin/order/page").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    @DisplayName("管理员访问管理接口：正常返回")
    void shouldAllowAdmin() throws Exception {
        String token = jwtUtil.createToken(1L, "admin", 1);

        mockMvc.perform(get("/api/admin/order/page").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("未登录访问管理接口：返回 401 而不是 403")
    void shouldRejectAnonymous() throws Exception {
        mockMvc.perform(get("/api/admin/order/page"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }
}
