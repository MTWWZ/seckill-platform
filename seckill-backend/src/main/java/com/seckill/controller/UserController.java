package com.seckill.controller;

import com.seckill.common.Result;
import com.seckill.common.UserContext;
import com.seckill.dto.UserLoginDTO;
import com.seckill.dto.UserRegisterDTO;
import com.seckill.entity.User;
import com.seckill.service.UserService;
import com.seckill.vo.LoginVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 用户接口
 * <p>
 * 阶段一先跑通"数据库 -> Mapper -> Service -> Controller -> JSON"这条链路，
 * 阶段三会在此基础上加注册、登录、JWT 鉴权。
 */
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    /** 构造器注入（@RequiredArgsConstructor 自动生成构造方法） */
    private final UserService userService;

    /** 注册 */
    @PostMapping("/register")
    public Result<Void> register(@RequestBody @Valid UserRegisterDTO dto) {
        userService.register(dto);
        return Result.success();
    }

    /** 登录 */
    @PostMapping("/login")
    public Result<LoginVO> login(@RequestBody @Valid UserLoginDTO dto) {
        return Result.success(userService.login(dto));
    }

    /** 获取当前登录用户（演示拦截器 + ThreadLocal） */
    @GetMapping("/me")
    public Result<UserContext.CurrentUser> me() {
        return Result.success(UserContext.get());
    }

    /** 查询用户列表（需要登录） */
    @GetMapping("/list")
    public Result<List<User>> list() {
        return Result.success(userService.list());
    }
}
