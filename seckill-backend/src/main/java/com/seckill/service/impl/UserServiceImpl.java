package com.seckill.service.impl;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.seckill.entity.User;
import com.seckill.mapper.UserMapper;
import com.seckill.service.UserService;
import org.springframework.stereotype.Service;

/**
 * 用户业务实现
 * <p>
 * ServiceImpl<UserMapper, User> 已经把 IService 的通用方法实现好了，
 * 我们这里只需要补"这个项目特有的业务逻辑"。
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {
}
