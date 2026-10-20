package com.seckill.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * JSON 工具
 * <p>
 * 复用 Spring Boot 自动配置好的 ObjectMapper（已经注册了 JavaTimeModule，
 * 能正确处理 LocalDateTime），把对象和 JSON 字符串互转。
 * 缓存进 Redis 时统一用 JSON 字符串，好处是 redis-cli 里直接可读。
 */
@Component
@RequiredArgsConstructor
public class JsonUtil {

    private final ObjectMapper objectMapper;

    public String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON 序列化失败", e);
        }
    }

    public <T> T parse(String json, Class<T> clazz) {
        try {
            return objectMapper.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON 反序列化失败", e);
        }
    }
}
