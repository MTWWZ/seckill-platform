package com.seckill.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 密码加密器配置
 */
@Configuration
public class PasswordConfig {

    /**
     * BCrypt 是"单向加盐哈希"，同一个密码每次加密结果都不同，
     * 只能通过 matches() 校验，无法反推原文，比 MD5 安全得多。
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
