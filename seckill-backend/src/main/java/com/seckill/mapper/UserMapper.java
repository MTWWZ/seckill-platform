package com.seckill.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.seckill.entity.User;

/**
 * 用户 Mapper
 * <p>
 * 继承 BaseMapper<User> 后，就白嫖了单表增删改查的能力：
 * insert / deleteById / updateById / selectById / selectList ...
 * 复杂 SQL 再自己写方法 + XML。
 */
public interface UserMapper extends BaseMapper<User> {
}
