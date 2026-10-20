package com.seckill.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT 配置项，读取 application.yml 中 jwt.* 的值
 */
@Data
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    /** 签名密钥 */
    private String secret;

    /** token 有效期（分钟） */
    private long expireMinutes = 120;

    /** 携带 token 的请求头名称 */
    private String header = "Authorization";
}
