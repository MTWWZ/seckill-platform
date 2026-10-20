package com.seckill.controller;

import com.seckill.common.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Redis 连通性自检接口
 */
@RestController
@RequestMapping("/api/redis")
@RequiredArgsConstructor
public class RedisController {

    private final StringRedisTemplate redisTemplate;

    /** 写一个 key 再读回来，用于确认 Redis 链路正常 */
    @GetMapping("/ping")
    public Result<Map<String, Object>> ping() {
        String key = "seckill:ping";
        String value = "hello-redis-" + System.currentTimeMillis();

        redisTemplate.opsForValue().set(key, value, Duration.ofMinutes(5));
        String readBack = redisTemplate.opsForValue().get(key);
        Long ttlSeconds = redisTemplate.getExpire(key);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("written", value);
        data.put("readBack", readBack);
        data.put("match", value.equals(readBack));
        data.put("ttlSeconds", ttlSeconds);
        data.put("time", LocalDateTime.now().toString());
        return Result.success(data);
    }
}
