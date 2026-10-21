package com.seckill;

import com.jayway.jsonpath.JsonPath;
import com.seckill.support.BaseIntegrationTest;
import com.seckill.util.JwtUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 接口层测试：鉴权与限流
 * <p>
 * 走完整的 MockMvc 链路（拦截器 + 切面 + 全局异常处理），
 * 验证的是"真实 HTTP 请求会得到什么响应码"。
 */
@AutoConfigureMockMvc
class SeckillApiTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtil jwtUtil;

    @Test
    @DisplayName("不带 token 访问需要登录的接口：返回 401")
    void shouldRejectRequestWithoutToken() throws Exception {
        mockMvc.perform(get("/api/user/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    @DisplayName("token 非法：返回 401，不会放行到业务层")
    void shouldRejectInvalidToken() throws Exception {
        mockMvc.perform(get("/api/user/me").header("Authorization", "Bearer not-a-real-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    @DisplayName("1 秒内连打 10 次抢购：5 次通过限流，其余返回 429")
    void shouldRateLimitBurstRequests() throws Exception {
        Long activityId = seedActivity(10);
        preheat(activityId, 10);

        String token = jwtUtil.createToken(99L, "tester", 0);
        List<Integer> codes = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            String body = mockMvc.perform(post("/api/seckill/order/" + activityId)
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();
            codes.add(JsonPath.read(body, "$.code"));
        }

        assertThat(codes).filteredOn(code -> code == 200)
                .as("第一次抢购成功").hasSize(1);
        assertThat(codes).filteredOn(code -> code == 409)
                .as("后续重复下单被一人一单拦住").hasSize(4);
        assertThat(codes).filteredOn(code -> code == 429)
                .as("超过 5 次/秒的部分被限流").hasSize(5);
    }
}
