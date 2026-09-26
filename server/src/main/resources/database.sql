CREATE DATABASE IF NOT EXISTS studyconnect
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE studyconnect;

-- ==============================
-- BẢNG NGƯỜI DÙNG
-- ==============================

CREATE TABLE IF NOT EXISTS users (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,

    username VARCHAR(50) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL,
    avatar_url VARCHAR(500),

    role ENUM('USER', 'ADMIN') NOT NULL DEFAULT 'USER',
    status ENUM('ACTIVE', 'LOCKED') NOT NULL DEFAULT 'ACTIVE',

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL
        DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,
    last_seen DATETIME,

    CONSTRAINT uk_users_username UNIQUE (username),
    CONSTRAINT uk_users_email UNIQUE (email)
) ENGINE = InnoDB;


-- ==============================
-- BẢNG BÀI ĐĂNG
-- ==============================

CREATE TABLE IF NOT EXISTS posts (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,

    author_id BIGINT UNSIGNED NOT NULL,

    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    subject VARCHAR(100),
    attachment_url VARCHAR(500),

    status ENUM('VISIBLE', 'HIDDEN', 'DELETED')
        NOT NULL DEFAULT 'VISIBLE',

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL
        DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_posts_author
        FOREIGN KEY (author_id)
        REFERENCES users(id)
        ON DELETE CASCADE,

    INDEX idx_posts_author (author_id),
    INDEX idx_posts_subject (subject),
    INDEX idx_posts_created_at (created_at)
) ENGINE = InnoDB;


-- ==============================
-- BẢNG BÌNH LUẬN
-- ==============================

CREATE TABLE IF NOT EXISTS comments (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,

    post_id BIGINT UNSIGNED NOT NULL,
    author_id BIGINT UNSIGNED NOT NULL,

    /*
     * NULL: bình luận trực tiếp vào bài viết.
     * Có giá trị: trả lời một bình luận khác.
     */
    parent_comment_id BIGINT UNSIGNED NULL,

    content TEXT NOT NULL,

    status ENUM('VISIBLE', 'HIDDEN', 'DELETED')
        NOT NULL DEFAULT 'VISIBLE',

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL
        DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_comments_post
        FOREIGN KEY (post_id)
        REFERENCES posts(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_comments_author
        FOREIGN KEY (author_id)
        REFERENCES users(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_comments_parent
        FOREIGN KEY (parent_comment_id)
        REFERENCES comments(id)
        ON DELETE CASCADE,

    INDEX idx_comments_post (post_id),
    INDEX idx_comments_author (author_id),
    INDEX idx_comments_parent (parent_comment_id),
    INDEX idx_comments_created_at (created_at)
) ENGINE = InnoDB;


-- ==============================
-- BẢNG TIN NHẮN
-- ==============================

CREATE TABLE IF NOT EXISTS messages (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,

    sender_id BIGINT UNSIGNED NOT NULL,
    receiver_id BIGINT UNSIGNED NOT NULL,

    message_type ENUM('TEXT', 'FILE', 'SYSTEM')
        NOT NULL DEFAULT 'TEXT',

    content TEXT,
    file_name VARCHAR(255),
    file_url VARCHAR(500),

    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    sent_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    read_at DATETIME,

    CONSTRAINT fk_messages_sender
        FOREIGN KEY (sender_id)
        REFERENCES users(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_messages_receiver
        FOREIGN KEY (receiver_id)
        REFERENCES users(id)
        ON DELETE CASCADE,

    INDEX idx_messages_sender (sender_id),
    INDEX idx_messages_receiver (receiver_id),
    INDEX idx_messages_conversation (
        sender_id,
        receiver_id,
        sent_at
    ),
    INDEX idx_messages_sent_at (sent_at)
) ENGINE = InnoDB;