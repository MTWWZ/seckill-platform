package com.seckill.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.seckill.entity.User;

/**
 * 用户业务接口
 * <p>
 * 继承 IService<User> 后同样白嫖了通用业务方法，例如：
 * save / removeById / updateById / getById / list / page ...
 */
public interface UserService extends IService<User> {
}
