CREATE DATABASE IF NOT EXISTS jingqu_db
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE jingqu_db;

CREATE TABLE IF NOT EXISTS admins (
    id BIGINT NOT NULL AUTO_INCREMENT,
    username VARCHAR(64) NOT NULL UNIQUE,
    password VARCHAR(256) NOT NULL,
    real_name VARCHAR(128),
    role VARCHAR(32) NOT NULL DEFAULT 'ADMIN',
    status TINYINT NOT NULL DEFAULT 1,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    last_login DATETIME NULL,
    deleted TINYINT DEFAULT 0,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS visitor_interactions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    visitor_id VARCHAR(64) NOT NULL,
    session_id VARCHAR(64) NULL,
    question TEXT NULL,
    answer TEXT NULL,
    interaction_type VARCHAR(32) DEFAULT 'QA',
    scenic_spot VARCHAR(128) NULL,
    route_target VARCHAR(64) NULL,
    retrieved_docs_count INT DEFAULT 0,
    fallback_used TINYINT DEFAULT 0,
    model_latency_ms BIGINT DEFAULT 0,
    final_answer_source VARCHAR(32) DEFAULT 'fallback',
    knowledge_sources TEXT NULL,
    interaction_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    PRIMARY KEY (id),
    INDEX idx_visitor_id (visitor_id),
    INDEX idx_interaction_time (interaction_time),
    INDEX idx_scenic_spot (scenic_spot)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS knowledge_base (
    id BIGINT NOT NULL AUTO_INCREMENT,
    question_pattern TEXT NULL,
    answer TEXT NOT NULL,
    keywords VARCHAR(512) NULL,
    category VARCHAR(64) DEFAULT 'GENERAL',
    priority INT DEFAULT 0,
    status TINYINT DEFAULT 1,
    view_count INT DEFAULT 0,
    success_count INT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    PRIMARY KEY (id),
    INDEX idx_category (category),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS emergency_notifications (
    id BIGINT NOT NULL AUTO_INCREMENT,
    title VARCHAR(256) NOT NULL,
    content TEXT NOT NULL,
    notification_type VARCHAR(32) DEFAULT 'INFO',
    target_scope VARCHAR(32) DEFAULT 'ALL',
    target_spot VARCHAR(128) NULL,
    push_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    expiry_time DATETIME NULL,
    status TINYINT DEFAULT 1,
    read_count INT DEFAULT 0,
    created_by VARCHAR(64) NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    PRIMARY KEY (id),
    INDEX idx_notification_status (status),
    INDEX idx_push_time (push_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS daily_statistics (
    id BIGINT NOT NULL AUTO_INCREMENT,
    stat_date DATE NOT NULL UNIQUE,
    total_interactions INT DEFAULT 0,
    total_visitors INT DEFAULT 0,
    peak_hour INT DEFAULT 0,
    popular_qa JSON NULL,
    hotspot_spots JSON NULL,
    hourly_data JSON NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_stat_date (stat_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS online_visitors (
    id BIGINT NOT NULL AUTO_INCREMENT,
    visitor_id VARCHAR(64) NOT NULL UNIQUE,
    session_id VARCHAR(64) NULL,
    device_info VARCHAR(512) NULL,
    current_spot VARCHAR(128) NULL,
    connected_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    last_active_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    is_online TINYINT DEFAULT 1,
    PRIMARY KEY (id),
    UNIQUE KEY uk_visitor_id (visitor_id),
    INDEX idx_is_online (is_online)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
