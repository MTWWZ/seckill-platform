package com.seckill.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户实体，对应表 t_user
 */
@Data
@TableName("t_user")
public class User {

    /** 主键，数据库自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 登录账号 */
    private String username;

    /**
     * 密码
     * WRITE_ONLY：只能被反序列化（接收前端传来的密码），
     * 不会被序列化输出，避免接口把密码返回给前端。
     */
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    private String nickname;

    private String phone;

    private String avatar;

    /** 角色：0-普通用户 1-管理员 */
    private Integer role;

    /** 状态：0-禁用 1-正常 */
    private Integer status;

    /** 逻辑删除标记：0-未删除 1-已删除（配合 application.yml 里的配置生效） */
    @TableLogic
    private Integer deleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
