-- ============================================================
-- 校园限量抢购平台 - 数据库初始化脚本
-- 执行方式：mysql -u root -p < init.sql
-- 说明：脚本可重复执行（先删表再建表）
-- ============================================================

CREATE DATABASE IF NOT EXISTS `seckill` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE `seckill`;

-- 注意删除顺序：先删有外键依赖的从表
DROP TABLE IF EXISTS `t_seckill_order`;
DROP TABLE IF EXISTS `t_seckill_activity`;
DROP TABLE IF EXISTS `t_goods`;
DROP TABLE IF EXISTS `t_user`;

-- ------------------------------------------------------------
-- 1. 用户表
-- ------------------------------------------------------------
CREATE TABLE `t_user` (
    `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `username`    VARCHAR(50)     NOT NULL COMMENT '登录账号',
    `password`    VARCHAR(100)    NOT NULL COMMENT '密码（BCrypt 加密后的密文）',
    `nickname`    VARCHAR(50)     DEFAULT NULL COMMENT '昵称',
    `phone`       VARCHAR(20)     DEFAULT NULL COMMENT '手机号',
    `avatar`      VARCHAR(255)    DEFAULT NULL COMMENT '头像地址',
    `role`        TINYINT         NOT NULL DEFAULT 0 COMMENT '角色：0-普通用户 1-管理员',
    `status`      TINYINT         NOT NULL DEFAULT 1 COMMENT '状态：0-禁用 1-正常',
    `deleted`     TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除 1-已删除',
    `create_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '用户表';

-- ------------------------------------------------------------
-- 2. 商品表
-- ------------------------------------------------------------
CREATE TABLE `t_goods` (
    `id`           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '商品ID',
    `name`         VARCHAR(100)    NOT NULL COMMENT '商品名称',
    `title`        VARCHAR(200)    DEFAULT NULL COMMENT '卖点标题',
    `description`  TEXT            COMMENT '商品描述',
    `img_url`      VARCHAR(500)    DEFAULT NULL COMMENT '商品图片地址',
    `origin_price` DECIMAL(10, 2)  NOT NULL COMMENT '原价',
    `stock`        INT             NOT NULL DEFAULT 0 COMMENT '总库存',
    `category`     VARCHAR(50)     DEFAULT NULL COMMENT '分类',
    `status`       TINYINT         NOT NULL DEFAULT 1 COMMENT '状态：0-下架 1-上架',
    `deleted`      TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除 1-已删除',
    `create_time`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_status` (`status`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '商品表';

-- ------------------------------------------------------------
-- 3. 秒杀活动表
--    活动库存与商品总库存分开维护，秒杀只扣活动库存
-- ------------------------------------------------------------
CREATE TABLE `t_seckill_activity` (
    `id`             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '活动ID',
    `goods_id`       BIGINT UNSIGNED NOT NULL COMMENT '商品ID',
    `seckill_price`  DECIMAL(10, 2)  NOT NULL COMMENT '秒杀价',
    `seckill_stock`  INT             NOT NULL COMMENT '活动库存（限量份数）',
    `start_time`     DATETIME        NOT NULL COMMENT '开始时间',
    `end_time`       DATETIME        NOT NULL COMMENT '结束时间',
    `status`         TINYINT         NOT NULL DEFAULT 0 COMMENT '状态：0-未开始 1-进行中 2-已结束',
    `version`        INT             NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    `deleted`        TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除 1-已删除',
    `create_time`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_goods_id` (`goods_id`),
    KEY `idx_time` (`start_time`, `end_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '秒杀活动表';

-- ------------------------------------------------------------
-- 4. 秒杀订单表
--    uk_user_activity 从数据库层面挡住"同一用户重复下单"
-- ------------------------------------------------------------
CREATE TABLE `t_seckill_order` (
    `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '订单ID',
    `order_no`    VARCHAR(64)     NOT NULL COMMENT '订单号',
    `user_id`     BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    `activity_id` BIGINT UNSIGNED NOT NULL COMMENT '活动ID',
    `goods_id`    BIGINT UNSIGNED NOT NULL COMMENT '商品ID',
    `amount`      DECIMAL(10, 2)  NOT NULL COMMENT '成交金额',
    `status`      TINYINT         NOT NULL DEFAULT 0 COMMENT '状态：0-待支付 1-已支付 2-已取消 3-已完成',
    `pay_time`    DATETIME        DEFAULT NULL COMMENT '支付时间',
    `deleted`     TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除 1-已删除',
    `create_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_no` (`order_no`),
    UNIQUE KEY `uk_user_activity` (`user_id`, `activity_id`),
    KEY `idx_activity_id` (`activity_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '秒杀订单表';

-- ------------------------------------------------------------
-- 初始化数据：商品
-- ------------------------------------------------------------
INSERT INTO `t_goods` (`name`, `title`, `description`, `img_url`, `origin_price`, `stock`, `category`, `status`)
VALUES ('校园联名卫衣', '校庆限定联名款', '校庆限定联名卫衣，限量发售，售完即止。',
        'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=campus%20limited%20edition%20hoodie%20product%20photo%20clean%20studio%20background&image_size=square',
        299.00, 100, '服饰', 1),
       ('限量款运动鞋', '联名限量配色', '校园联名限量配色运动鞋，全球限量 500 双。',
        'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=limited%20edition%20sneakers%20product%20photo%20clean%20background&image_size=square',
        899.00, 50, '鞋靴', 1),
       ('校园音乐节门票', '校园音乐节 VIP 票', '年度校园音乐节 VIP 区门票，限量 200 张。',
        'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=music%20festival%20ticket%20design%20flat%20lay&image_size=square',
        199.00, 200, '演出', 1),
       ('图书馆纪念徽章', '校图书馆限定徽章', '图书馆开馆纪念限定金属徽章。',
        'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=metal%20enamel%20pin%20badge%20product%20photo&image_size=square',
        39.90, 300, '文创', 1);

-- ------------------------------------------------------------
-- 初始化数据：用户
-- 注意：阶段一还没接入密码加密，这里暂存明文；
--       阶段③ 接入 JWT 登录后会改成 BCrypt 密文。
-- ------------------------------------------------------------
INSERT INTO `t_user` (`username`, `password`, `nickname`, `role`, `status`)
VALUES ('admin', '123456', '管理员', 1, 1),
       ('student01', '123456', '小明', 0, 1),
       ('student02', '123456', '小红', 0, 1);
