package com.studyconnect.server.model.dao;

import com.studyconnect.common.dto.ConversationDTO;
import com.studyconnect.common.dto.MessageDTO;
import com.studyconnect.common.dto.MessageDeliveryMode;
import com.studyconnect.common.dto.MessageStatus;
import com.studyconnect.server.model.database.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class MessageDAO {
    private static final String MESSAGE_COLUMNS = """
            m.id, m.client_message_id, m.sender_id, m.receiver_id, m.content,
            m.delivery_mode, m.status, m.created_at, m.delivered_at, m.read_at,
            sender.full_name AS sender_name,
            sender.username AS sender_username,
            sender.avatar_url AS sender_avatar_url
            """;

    public boolean userExists(long userId) throws SQLException {
        String sql = "SELECT 1 FROM users WHERE id = ? AND status = 'ACTIVE' LIMIT 1";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next();
            }
        }
    }

    public MessageDTO saveOrGet(
            String clientMessageId,
            long senderId,
            long receiverId,
            String content,
            MessageDeliveryMode deliveryMode,
            MessageStatus status
    ) throws SQLException {
        Optional<MessageDTO> existing = findByClientMessageId(clientMessageId);
        if (existing.isPresent()) return existing.get();

        String sql = """
                INSERT INTO messages (
                    client_message_id, sender_id, receiver_id, content,
                    delivery_mode, status, created_at, delivered_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;
        long now = System.currentTimeMillis();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, clientMessageId);
            statement.setLong(2, senderId);
            statement.setLong(3, receiverId);
            statement.setString(4, content);
            statement.setString(5, deliveryMode.name());
            statement.setString(6, status.name());
            statement.setTimestamp(7, new Timestamp(now));
            if (status == MessageStatus.DELIVERED || status == MessageStatus.READ) {
                statement.setTimestamp(8, new Timestamp(now));
            } else {
                statement.setTimestamp(8, null);
            }
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) throw new SQLException("Không lấy được ID tin nhắn vừa tạo");
                return findById(connection, keys.getLong(1));
            }
        } catch (SQLIntegrityConstraintViolationException duplicate) {
            return findByClientMessageId(clientMessageId)
                    .orElseThrow(() -> duplicate);
        }
    }

    public Optional<MessageDTO> findByClientMessageId(String clientMessageId)
            throws SQLException {
        String sql = "SELECT " + MESSAGE_COLUMNS + """
                FROM messages m
                JOIN users sender ON sender.id = m.sender_id
                WHERE m.client_message_id = ?
                LIMIT 1
                """;
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, clientMessageId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Optional.of(mapMessage(result)) : Optional.empty();
            }
        }
    }

    public MessageDTO updateStatus(
            String clientMessageId,
            MessageStatus status,
            long timestamp
    ) throws SQLException {
        String sql;
        if (status == MessageStatus.READ) {
            sql = """
                    UPDATE messages
                       SET status = 'READ', read_at = ?,
                           delivered_at = COALESCE(delivered_at, ?)
                     WHERE client_message_id = ?
                    """;
        } else if (status == MessageStatus.DELIVERED) {
            sql = """
                    UPDATE messages
                       SET status = CASE WHEN status = 'READ' THEN status ELSE 'DELIVERED' END,
                           delivered_at = COALESCE(delivered_at, ?)
                     WHERE client_message_id = ?
                    """;
        } else {
            sql = "UPDATE messages SET status = ? WHERE client_message_id = ?";
        }
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            if (status == MessageStatus.READ) {
                statement.setTimestamp(1, new Timestamp(timestamp));
                statement.setTimestamp(2, new Timestamp(timestamp));
                statement.setString(3, clientMessageId);
            } else if (status == MessageStatus.DELIVERED) {
                statement.setTimestamp(1, new Timestamp(timestamp));
                statement.setString(2, clientMessageId);
            } else {
                statement.setString(1, status.name());
                statement.setString(2, clientMessageId);
            }
            statement.executeUpdate();
        }
        return findByClientMessageId(clientMessageId)
                .orElseThrow(() -> new SQLException("Không tìm thấy tin nhắn để cập nhật"));
    }

    public List<MessageDTO> findConversation(
            long currentUserId,
            long otherUserId,
            Long beforeMessageId,
            int limit
    ) throws SQLException {
        String beforeClause = beforeMessageId == null ? "" : " AND m.id < ? ";
        String sql = "SELECT " + MESSAGE_COLUMNS + """
                FROM messages m
                JOIN users sender ON sender.id = m.sender_id
                WHERE ((m.sender_id = ? AND m.receiver_id = ?)
                    OR (m.sender_id = ? AND m.receiver_id = ?))
                """ + beforeClause + " ORDER BY m.created_at DESC, m.id DESC LIMIT ?";
        List<MessageDTO> messages = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            int index = 1;
            statement.setLong(index++, currentUserId);
            statement.setLong(index++, otherUserId);
            statement.setLong(index++, otherUserId);
            statement.setLong(index++, currentUserId);
            if (beforeMessageId != null) statement.setLong(index++, beforeMessageId);
            statement.setInt(index, limit);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) messages.add(mapMessage(result));
            }
        }
        Collections.reverse(messages);
        return messages;
    }

    public List<ConversationDTO> findConversations(long currentUserId)
            throws SQLException {
        String sql = """
                SELECT m.id, m.client_message_id, m.sender_id, m.receiver_id, m.content,
                       m.delivery_mode, m.status, m.created_at, m.delivered_at, m.read_at,
                       sender.full_name AS sender_name,
                       sender.username AS sender_username,
                       sender.avatar_url AS sender_avatar_url,
                       other.id AS other_user_id,
                       other.username AS other_username,
                       other.full_name AS other_user_name,
                       other.avatar_url AS other_avatar_url,
                       (SELECT COUNT(*)
                          FROM messages unread
                         WHERE unread.sender_id = other.id
                           AND unread.receiver_id = ?
                           AND unread.status <> 'READ') AS unread_count
                  FROM users other
                  LEFT JOIN messages m ON m.id = (
                       SELECT m2.id
                         FROM messages m2
                        WHERE ((m2.sender_id = ? AND m2.receiver_id = other.id)
                            OR (m2.sender_id = other.id AND m2.receiver_id = ?))
                        ORDER BY m2.created_at DESC, m2.id DESC
                        LIMIT 1)
                  LEFT JOIN users sender ON sender.id = m.sender_id
                 WHERE other.id <> ?
                   AND other.status = 'ACTIVE'
                 ORDER BY (m.id IS NULL), m.created_at DESC, m.id DESC,
                          COALESCE(other.full_name, other.username)
                """;
        List<ConversationDTO> conversations = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int index = 1; index <= 4; index++) statement.setLong(index, currentUserId);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    ConversationDTO conversation = new ConversationDTO();
                    conversation.setOtherUserId(result.getLong("other_user_id"));
                    conversation.setOtherUsername(result.getString("other_username"));
                    conversation.setOtherUserName(result.getString("other_user_name"));
                    conversation.setOtherUserAvatarUrl(result.getString("other_avatar_url"));
                    conversation.setUnreadCount(result.getInt("unread_count"));
                    if (result.getObject("id") != null) conversation.setLastMessage(mapMessage(result));
                    conversations.add(conversation);
                }
            }
        }
        return conversations;
    }

    public ReadResult markRead(long receiverId, long senderId) throws SQLException {
        String selectSql = """
                SELECT id, client_message_id FROM messages
                 WHERE sender_id = ? AND receiver_id = ? AND status <> 'READ'
                 ORDER BY id
                """;
        String updateSql = """
                UPDATE messages
                   SET status = 'READ', read_at = ?,
                       delivered_at = COALESCE(delivered_at, ?)
                 WHERE sender_id = ? AND receiver_id = ? AND status <> 'READ'
                """;
        long readAt = System.currentTimeMillis();
        List<Long> messageIds = new ArrayList<>();
        List<String> clientMessageIds = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement select = connection.prepareStatement(selectSql)) {
                    select.setLong(1, senderId);
                    select.setLong(2, receiverId);
                    try (ResultSet result = select.executeQuery()) {
                        while (result.next()) {
                            messageIds.add(result.getLong("id"));
                            clientMessageIds.add(result.getString("client_message_id"));
                        }
                    }
                }
                if (!messageIds.isEmpty()) {
                    try (PreparedStatement update = connection.prepareStatement(updateSql)) {
                        Timestamp timestamp = new Timestamp(readAt);
                        update.setTimestamp(1, timestamp);
                        update.setTimestamp(2, timestamp);
                        update.setLong(3, senderId);
                        update.setLong(4, receiverId);
                        update.executeUpdate();
                    }
                }
                connection.commit();
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
        }
        return new ReadResult(messageIds, clientMessageIds, readAt);
    }

    private MessageDTO findById(Connection connection, long messageId) throws SQLException {
        String sql = "SELECT " + MESSAGE_COLUMNS + """
                FROM messages m
                JOIN users sender ON sender.id = m.sender_id
                WHERE m.id = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, messageId);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) throw new SQLException("Không tìm thấy tin nhắn vừa tạo");
                return mapMessage(result);
            }
        }
    }

    private MessageDTO mapMessage(ResultSet result) throws SQLException {
        Timestamp createdAt = result.getTimestamp("created_at");
        Timestamp deliveredAt = result.getTimestamp("delivered_at");
        Timestamp readAt = result.getTimestamp("read_at");
        String senderName = result.getString("sender_name");
        if (senderName == null || senderName.isBlank()) senderName = result.getString("sender_username");
        return new MessageDTO(
                result.getLong("id"),
                result.getString("client_message_id"),
                result.getLong("sender_id"),
                senderName,
                result.getString("sender_avatar_url"),
                result.getLong("receiver_id"),
                result.getString("content"),
                createdAt == null ? 0L : createdAt.getTime(),
                MessageStatus.valueOf(result.getString("status")),
                MessageDeliveryMode.valueOf(result.getString("delivery_mode")),
                deliveredAt == null ? 0L : deliveredAt.getTime(),
                readAt == null ? 0L : readAt.getTime()
        );
    }

    public record ReadResult(
            List<Long> messageIds,
            List<String> clientMessageIds,
            long readAt
    ) {
    }
}
