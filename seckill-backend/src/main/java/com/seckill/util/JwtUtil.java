package com.seckill.util;

import com.seckill.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 工具类：生成 token / 解析 token
 */
@Component
@RequiredArgsConstructor
public class JwtUtil {

    private final JwtProperties jwtProperties;

    private SecretKey key() {
        return Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    /** 生成 token，把用户 id / 用户名 / 角色写进载荷 */
    public String createToken(Long userId, String username, Integer role) {
        Date now = new Date();
        Date expire = new Date(now.getTime() + jwtProperties.getExpireMinutes() * 60_000L);
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("username", username)
                .claim("role", role)
                .issuedAt(now)
                .expiration(expire)
                .signWith(key())
                .compact();
    }

    /** 解析 token，签名不合法或已过期都返回 null（不抛异常给上层） */
    public Claims parseToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(key())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception e) {
            return null;
        }
    }
}
