package com.seckill.vo;

import lombok.Builder;
import lombok.Data;

/**
 * 登录成功后返回给前端的数据
 * <p>
 * 单独定义 VO 而不是直接返回 User，是为了控制"哪些字段能出去"。
 */
@Data
@Builder
public class LoginVO {

    /** 后续请求要带在 Authorization 头里的凭证 */
    private String token;

    private Long userId;

    private String username;

    private String nickname;

    private Integer role;
}
