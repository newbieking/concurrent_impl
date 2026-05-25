-- ============================================================
-- 【数据库初始化脚本】企业级并发与安全示例项目
-- 
-- 【业务背景】
-- 本脚本用于创建示例项目所需的数据库表结构
-- 模拟电商订单系统的业务场景
-- 
-- 【使用方法】
-- 1. 创建数据库：CREATE DATABASE concurrent_demo;
-- 2. 使用数据库：USE concurrent_demo;
-- 3. 执行本脚本
-- ============================================================

-- -----------------------------------------------------------
-- 用户表
-- 【业务说明】存储用户基本信息和账户余额
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS `user` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
    `username` VARCHAR(50) NOT NULL COMMENT '用户名',
    `password` VARCHAR(100) NOT NULL COMMENT '密码',
    `phone` VARCHAR(20) COMMENT '手机号',
    `email` VARCHAR(100) COMMENT '邮箱',
    `balance` DECIMAL(10,2) DEFAULT 0.00 COMMENT '账户余额',
    `status` TINYINT DEFAULT 1 COMMENT '状态：1-正常 0-禁用',
    `login_error_count` INT DEFAULT 0 COMMENT '登录错误次数',
    `lock_time` DATETIME COMMENT '锁定时间',
    `version` INT DEFAULT 0 COMMENT '乐观锁版本号',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY `uk_username` (`username`),
    UNIQUE KEY `uk_phone` (`phone`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- -----------------------------------------------------------
-- 商品表
-- 【业务说明】存储商品信息和库存
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS `product` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '商品ID',
    `name` VARCHAR(200) NOT NULL COMMENT '商品名称',
    `description` TEXT COMMENT '商品描述',
    `price` DECIMAL(10,2) NOT NULL COMMENT '商品价格',
    `stock` INT NOT NULL COMMENT '库存数量',
    `lock_stock` INT DEFAULT 0 COMMENT '锁定库存（已下单未支付）',
    `category_id` BIGINT COMMENT '分类ID',
    `status` TINYINT DEFAULT 1 COMMENT '状态：1-上架 0-下架',
    `version` INT DEFAULT 0 COMMENT '乐观锁版本号',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX `idx_category` (`category_id`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品表';

-- -----------------------------------------------------------
-- 订单表
-- 【业务说明】存储订单主信息
-- 
-- 【状态流转】
-- 0-待支付 -> 1-已支付 -> 2-已发货 -> 3-已完成
-- 0-待支付 -> 4-已取消
-- 1-已支付 -> 5-已退款
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS `orders` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '订单ID',
    `order_no` VARCHAR(32) NOT NULL COMMENT '订单号',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `total_amount` DECIMAL(10,2) NOT NULL COMMENT '订单总金额',
    `pay_amount` DECIMAL(10,2) COMMENT '实付金额',
    `status` TINYINT DEFAULT 0 COMMENT '订单状态：0-待支付 1-已支付 2-已发货 3-已完成 4-已取消 5-已退款',
    `pay_type` TINYINT COMMENT '支付方式：1-支付宝 2-微信 3-银行卡',
    `pay_time` DATETIME COMMENT '支付时间',
    `ship_time` DATETIME COMMENT '发货时间',
    `receive_time` DATETIME COMMENT '收货时间',
    `cancel_time` DATETIME COMMENT '取消时间',
    `cancel_reason` VARCHAR(200) COMMENT '取消原因',
    `idempotent_key` VARCHAR(64) COMMENT '幂等键（防重复提交）',
    `remark` VARCHAR(500) COMMENT '订单备注',
    `version` INT DEFAULT 0 COMMENT '乐观锁版本号',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY `uk_order_no` (`order_no`),
    UNIQUE KEY `uk_idempotent_key` (`idempotent_key`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_status` (`status`),
    INDEX `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单表';

-- -----------------------------------------------------------
-- 订单明细表
-- 【业务说明】存储订单中的商品明细
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS `order_item` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '明细ID',
    `order_id` BIGINT NOT NULL COMMENT '订单ID',
    `order_no` VARCHAR(32) NOT NULL COMMENT '订单号',
    `product_id` BIGINT NOT NULL COMMENT '商品ID',
    `product_name` VARCHAR(200) NOT NULL COMMENT '商品名称（冗余）',
    `product_price` DECIMAL(10,2) NOT NULL COMMENT '商品单价（下单时价格）',
    `quantity` INT NOT NULL COMMENT '购买数量',
    `subtotal` DECIMAL(10,2) NOT NULL COMMENT '小计金额',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX `idx_order_id` (`order_id`),
    INDEX `idx_order_no` (`order_no`),
    INDEX `idx_product_id` (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单明细表';

-- -----------------------------------------------------------
-- 账户流水表
-- 【业务说明】记录用户账户的资金变动流水
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS `account_log` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '流水ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `order_no` VARCHAR(32) COMMENT '关联订单号',
    `amount` DECIMAL(10,2) NOT NULL COMMENT '变动金额（正数-收入，负数-支出）',
    `balance` DECIMAL(10,2) NOT NULL COMMENT '变动后余额',
    `type` TINYINT NOT NULL COMMENT '类型：1-充值 2-消费 3-退款 4-提现',
    `description` VARCHAR(200) COMMENT '流水描述',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_order_no` (`order_no`),
    INDEX `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='账户流水表';

-- -----------------------------------------------------------
-- 消息表（本地消息表模式）
-- 【业务说明】用于实现分布式事务的最终一致性
-- 
-- 【使用场景】
-- 1. 订单创建后，发送消息通知库存服务扣减库存
-- 2. 消息发送失败时，通过定时任务重试
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS `message` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '消息ID',
    `message_id` VARCHAR(64) NOT NULL COMMENT '消息唯一标识',
    `exchange` VARCHAR(100) COMMENT '交换机',
    `routing_key` VARCHAR(100) COMMENT '路由键',
    `content` TEXT COMMENT '消息内容（JSON格式）',
    `status` TINYINT DEFAULT 0 COMMENT '状态：0-待发送 1-已发送 2-已确认 3-发送失败',
    `retry_count` INT DEFAULT 0 COMMENT '重试次数',
    `max_retry_count` INT DEFAULT 3 COMMENT '最大重试次数',
    `next_retry_time` DATETIME COMMENT '下次重试时间',
    `error_message` VARCHAR(500) COMMENT '错误信息',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY `uk_message_id` (`message_id`),
    INDEX `idx_status` (`status`),
    INDEX `idx_next_retry_time` (`next_retry_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='消息表';

-- -----------------------------------------------------------
-- 操作日志表
-- 【业务说明】记录用户的关键操作，用于审计和问题追踪
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS `operation_log` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '日志ID',
    `user_id` BIGINT COMMENT '操作用户ID',
    `username` VARCHAR(50) COMMENT '操作用户名',
    `module` VARCHAR(50) COMMENT '操作模块',
    `operation` VARCHAR(100) COMMENT '操作描述',
    `method` VARCHAR(200) COMMENT '请求方法',
    `params` TEXT COMMENT '请求参数',
    `ip` VARCHAR(50) COMMENT 'IP地址',
    `status` TINYINT COMMENT '操作状态：1-成功 0-失败',
    `error_msg` TEXT COMMENT '错误信息',
    `cost_time` BIGINT COMMENT '耗时（毫秒）',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='操作日志表';

-- -----------------------------------------------------------
-- 初始化测试数据
-- -----------------------------------------------------------

-- 插入测试用户
INSERT INTO `user` (`username`, `password`, `phone`, `balance`) VALUES
('testuser1', '$2a$10$xxxx', '13800138001', 10000.00),
('testuser2', '$2a$10$xxxx', '13800138002', 5000.00),
('testuser3', '$2a$10$xxxx', '13800138003', 2000.00);

-- 插入测试商品
INSERT INTO `product` (`name`, `description`, `price`, `stock`) VALUES
('iPhone 15 Pro', 'Apple iPhone 15 Pro 256GB', 8999.00, 100),
('MacBook Pro', 'Apple MacBook Pro 14英寸 M3 Pro', 14999.00, 50),
('AirPods Pro', 'Apple AirPods Pro 第二代', 1799.00, 200),
('iPad Air', 'Apple iPad Air M1芯片', 4799.00, 80),
('Apple Watch', 'Apple Watch Series 9', 2999.00, 150);
