-- ============================================================
-- V1：初始表结构
--
-- 用 CREATE TABLE IF NOT EXISTS 而不是直接 CREATE：
-- 项目早期是用 sql/init.sql 手工建表的，已有环境里这些表都已存在，
-- 这里保持幂等，Flyway 记录版本时不会因为"表已存在"启动失败。
-- 全新数据库则会被完整创建。
-- ============================================================

CREATE TABLE IF NOT EXISTS `t_user` (
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

CREATE TABLE IF NOT EXISTS `t_goods` (
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

CREATE TABLE IF NOT EXISTS `t_seckill_activity` (
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

CREATE TABLE IF NOT EXISTS `t_seckill_order` (
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
    KEY `idx_activity_id` (`activity_id`),
    KEY `idx_status_create` (`status`, `create_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '秒杀订单表';
