CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(32) NOT NULL,
    password_hash VARCHAR(120) NOT NULL,
    nickname VARCHAR(64) NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    UNIQUE KEY uk_sys_user_username (username)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS ai_chat_message (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    conversation_id VARCHAR(128) NOT NULL,
    message_type VARCHAR(20) NOT NULL,
    message_text LONGTEXT NOT NULL,
    created_at DATETIME NOT NULL,
    KEY idx_ai_chat_message_conversation_id (conversation_id),
    KEY idx_ai_chat_message_created_at (created_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
