-- 景区导览AI数字人数据库初始化脚本

-- 创建数据库
CREATE DATABASE IF NOT EXISTS jingqu_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE jingqu_db;

-- 管理员表
CREATE TABLE IF NOT EXISTS `admins` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `username` VARCHAR(64) NOT NULL UNIQUE COMMENT '用户名',
    `password` VARCHAR(256) NOT NULL COMMENT '密码（BCrypt加密）',
    `real_name` VARCHAR(128) COMMENT '真实姓名',
    `role` VARCHAR(32) NOT NULL DEFAULT 'ADMIN' COMMENT '角色：SUPER_ADMIN/ADMIN',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-禁用 1-启用',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `last_login` DATETIME COMMENT '最后登录时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除：0-未删除 1-已删除',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='管理员表';

-- 游客交互记录表
CREATE TABLE IF NOT EXISTS `visitor_interactions` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `visitor_id` VARCHAR(64) NOT NULL COMMENT '游客唯一标识（设备ID）',
    `session_id` VARCHAR(64) COMMENT '会话ID',
    `question` TEXT COMMENT '游客提问内容',
    `answer` TEXT COMMENT 'AI数字人回答内容',
    `interaction_type` VARCHAR(32) DEFAULT 'QA' COMMENT '交互类型：QA/GREETING/GUIDANCE',
    `scenic_spot` VARCHAR(128) COMMENT '当前景点名称',
    `interaction_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '交互时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除：0-未删除 1-已删除',
    PRIMARY KEY (`id`),
    INDEX `idx_visitor_id` (`visitor_id`),
    INDEX `idx_interaction_time` (`interaction_time`),
    INDEX `idx_scenic_spot` (`scenic_spot`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='游客交互记录表';

-- 知识库表
CREATE TABLE IF NOT EXISTS `knowledge_base` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `question_pattern` TEXT COMMENT '问题模式（支持模糊匹配）',
    `answer` TEXT NOT NULL COMMENT '标准回答',
    `keywords` VARCHAR(512) COMMENT '关键词，逗号分隔',
    `category` VARCHAR(64) DEFAULT 'GENERAL' COMMENT '分类：景点介绍/路线规划/餐饮服务/紧急求助',
    `priority` INT DEFAULT 0 COMMENT '优先级（数字越大优先级越高）',
    `status` TINYINT DEFAULT 1 COMMENT '状态：0-禁用 1-启用',
    `view_count` INT DEFAULT 0 COMMENT '被查询次数',
    `success_count` INT DEFAULT 0 COMMENT '成功匹配次数',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除：0-未删除 1-已删除',
    PRIMARY KEY (`id`),
    INDEX `idx_category` (`category`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识库表';

-- 紧急通知表
CREATE TABLE IF NOT EXISTS `emergency_notifications` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `title` VARCHAR(256) NOT NULL COMMENT '通知标题',
    `content` TEXT NOT NULL COMMENT '通知内容',
    `notification_type` VARCHAR(32) DEFAULT 'INFO' COMMENT '通知类型：EMERGENCY/INFO/UPDATE',
    `target_scope` VARCHAR(32) DEFAULT 'ALL' COMMENT '推送范围：ALL/SCENIC_AREA/SPOT',
    `target_spot` VARCHAR(128) COMMENT '指定景点（可选）',
    `push_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '推送时间',
    `expiry_time` DATETIME COMMENT '过期时间',
    `status` TINYINT DEFAULT 1 COMMENT '状态：0-待推送 1-已推送 2-已过期',
    `read_count` INT DEFAULT 0 COMMENT '已读人数',
    `created_by` VARCHAR(64) COMMENT '创建人ID',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除：0-未删除 1-已删除',
    PRIMARY KEY (`id`),
    INDEX `idx_status` (`status`),
    INDEX `idx_push_time` (`push_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='紧急通知表';

-- 每日统计数据表
CREATE TABLE IF NOT EXISTS `daily_statistics` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `stat_date` DATE NOT NULL UNIQUE COMMENT '统计日期',
    `total_interactions` INT DEFAULT 0 COMMENT '今日总交互次数',
    `total_visitors` INT DEFAULT 0 COMMENT '今日独立访客数',
    `peak_hour` INT DEFAULT 0 COMMENT '高峰时段（小时：0-23）',
    `popular_qa` JSON COMMENT '热门问答TOP10 JSON',
    `hotspot_spots` JSON COMMENT '热门景点分布JSON',
    `hourly_data` JSON COMMENT '每小时交互数据JSON',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_stat_date` (`stat_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='每日统计数据表';

-- 在线游客记录表（用于实时统计）
CREATE TABLE IF NOT EXISTS `online_visitors` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `visitor_id` VARCHAR(64) NOT NULL UNIQUE COMMENT '游客唯一标识',
    `session_id` VARCHAR(64) COMMENT 'WebSocket会话ID',
    `device_info` VARCHAR(512) COMMENT '设备信息',
    `current_spot` VARCHAR(128) COMMENT '当前位置',
    `connected_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '连接时间',
    `last_active_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '最后活跃时间',
    `is_online` TINYINT DEFAULT 1 COMMENT '是否在线：0-离线 1-在线',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_visitor_id` (`visitor_id`),
    INDEX `idx_is_online` (`is_online`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='在线游客记录表';

