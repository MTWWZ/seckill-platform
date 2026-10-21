-- 测试库表结构：与 init.sql 保持一致，但不删表、不插入种子数据
-- 测试类自己准备需要的数据，@BeforeEach 里清空

CREATE TABLE IF NOT EXISTS `t_user` (
    `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `username`    VARCHAR(50)     NOT NULL,
    `password`    VARCHAR(100)    NOT NULL,
    `nickname`    VARCHAR(50)     DEFAULT NULL,
    `phone`       VARCHAR(20)     DEFAULT NULL,
    `avatar`      VARCHAR(255)    DEFAULT NULL,
    `role`        TINYINT         NOT NULL DEFAULT 0,
    `status`      TINYINT         NOT NULL DEFAULT 1,
    `deleted`     TINYINT         NOT NULL DEFAULT 0,
    `create_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS `t_goods` (
    `id`           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `name`         VARCHAR(100)    NOT NULL,
    `title`        VARCHAR(200)    DEFAULT NULL,
    `description`  TEXT,
    `img_url`      VARCHAR(500)    DEFAULT NULL,
    `origin_price` DECIMAL(10, 2)  NOT NULL,
    `stock`        INT             NOT NULL DEFAULT 0,
    `category`     VARCHAR(50)     DEFAULT NULL,
    `status`       TINYINT         NOT NULL DEFAULT 1,
    `deleted`      TINYINT         NOT NULL DEFAULT 0,
    `create_time`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_status` (`status`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS `t_seckill_activity` (
    `id`             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `goods_id`       BIGINT UNSIGNED NOT NULL,
    `seckill_price`  DECIMAL(10, 2)  NOT NULL,
    `seckill_stock`  INT             NOT NULL,
    `start_time`     DATETIME        NOT NULL,
    `end_time`       DATETIME        NOT NULL,
    `status`         TINYINT         NOT NULL DEFAULT 0,
    `version`        INT             NOT NULL DEFAULT 0,
    `deleted`        TINYINT         NOT NULL DEFAULT 0,
    `create_time`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_goods_id` (`goods_id`),
    KEY `idx_time` (`start_time`, `end_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS `t_seckill_order` (
    `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `order_no`    VARCHAR(64)     NOT NULL,
    `user_id`     BIGINT UNSIGNED NOT NULL,
    `activity_id` BIGINT UNSIGNED NOT NULL,
    `goods_id`    BIGINT UNSIGNED NOT NULL,
    `amount`      DECIMAL(10, 2)  NOT NULL,
    `status`      TINYINT         NOT NULL DEFAULT 0,
    `pay_time`    DATETIME        DEFAULT NULL,
    `deleted`     TINYINT         NOT NULL DEFAULT 0,
    `create_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_no` (`order_no`),
    UNIQUE KEY `uk_user_activity` (`user_id`, `activity_id`),
    KEY `idx_activity_id` (`activity_id`),
    KEY `idx_status_create` (`status`, `create_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
