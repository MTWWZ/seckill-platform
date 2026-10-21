-- ============================================================
-- 测试库初始化脚本（只建库，表由测试启动时自动创建）
-- 执行方式：mysql -u root -p < init-test.sql
-- 说明：单测使用独立的 seckill_test 库 + Redis 15 号库，
--       不会碰开发库里的演示数据
-- ============================================================

CREATE DATABASE IF NOT EXISTS `seckill_test`
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_general_ci;
