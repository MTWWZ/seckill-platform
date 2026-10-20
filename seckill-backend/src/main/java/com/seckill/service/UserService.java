package com.seckill.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.seckill.dto.UserLoginDTO;
import com.seckill.dto.UserRegisterDTO;
import com.seckill.entity.User;
import com.seckill.vo.LoginVO;

/**
 * 用户业务接口
 * <p>
 * 继承 IService<User> 后同样白嫖了通用业务方法，例如：
 * save / removeById / updateById / getById / list / page ...
 */
public interface UserService extends IService<User> {

    /** 注册新用户 */
    void register(UserRegisterDTO dto);

    /** 登录，成功返回 token 与用户信息 */
    LoginVO login(UserLoginDTO dto);
}
