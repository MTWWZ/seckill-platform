package com.seckill.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.seckill.dto.UserLoginDTO;
import com.seckill.dto.UserRegisterDTO;
import com.seckill.entity.User;
import com.seckill.exception.BusinessException;
import com.seckill.mapper.UserMapper;
import com.seckill.service.UserService;
import com.seckill.util.JwtUtil;
import com.seckill.vo.LoginVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 用户业务实现
 * <p>
 * ServiceImpl<UserMapper, User> 已经把 IService 的通用方法实现好了，
 * 我们这里只需要补"这个项目特有的业务逻辑"。
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Override
    public void register(UserRegisterDTO dto) {
        Long exists = getBaseMapper().selectCount(
                new LambdaQueryWrapper<User>().eq(User::getUsername, dto.getUsername()));
        if (exists != null && exists > 0) {
            throw new BusinessException("用户名已存在");
        }

        User user = new User();
        user.setUsername(dto.getUsername());
        // 只存密文，绝不存明文
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setNickname(StringUtils.hasText(dto.getNickname()) ? dto.getNickname() : dto.getUsername());
        user.setPhone(dto.getPhone());
        user.setRole(0);
        user.setStatus(1);
        save(user);
    }

    @Override
    public LoginVO login(UserLoginDTO dto) {
        User user = getBaseMapper().selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, dto.getUsername()));
        // 账号不存在 和 密码错误 返回同一句提示，避免暴露"这个账号存不存在"
        if (user == null) {
            throw new BusinessException("用户名或密码错误");
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BusinessException("账号已被禁用，请联系管理员");
        }
        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }

        String token = jwtUtil.createToken(user.getId(), user.getUsername(), user.getRole());
        return LoginVO.builder()
                .token(token)
                .userId(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .role(user.getRole())
                .build();
    }
}
