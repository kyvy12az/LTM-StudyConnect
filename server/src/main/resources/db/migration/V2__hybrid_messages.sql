USE studyconnect;

-- Chạy một lần cho database đã được tạo từ schema cũ.
ALTER TABLE messages
    ADD COLUMN client_message_id VARCHAR(36) NULL AFTER id,
    ADD COLUMN delivery_mode VARCHAR(20) NOT NULL DEFAULT 'SERVER_RELAY' AFTER content,
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'SENT' AFTER delivery_mode,
    ADD COLUMN created_at TIMESTAMP NULL AFTER status,
    ADD COLUMN delivered_at TIMESTAMP NULL AFTER created_at;

UPDATE messages
   SET client_message_id = UUID()
 WHERE client_message_id IS NULL;

UPDATE messages
   SET created_at = COALESCE(sent_at, CURRENT_TIMESTAMP),
       status = CASE WHEN is_read = TRUE THEN 'READ' ELSE 'SENT' END,
       delivered_at = CASE WHEN is_read = TRUE THEN COALESCE(read_at, sent_at) ELSE NULL END;

UPDATE messages
   SET content = ''
 WHERE content IS NULL;

ALTER TABLE messages
    MODIFY COLUMN client_message_id VARCHAR(36) NOT NULL,
    MODIFY COLUMN content TEXT NOT NULL,
    MODIFY COLUMN created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    MODIFY COLUMN read_at TIMESTAMP NULL,
    CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
    ADD CONSTRAINT uk_messages_client_message_id UNIQUE (client_message_id),
    ADD INDEX idx_messages_hybrid_conversation (sender_id, receiver_id, created_at),
    ADD INDEX idx_messages_receiver_status (receiver_id, status);

-- Các cột cũ được giữ lại để migration không làm mất dữ liệu và để rollback an toàn.
