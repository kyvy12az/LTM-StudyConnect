package com.studyconnect.server.service;

import com.studyconnect.common.dto.ConversationDTO;
import com.studyconnect.common.dto.GetMessagesDTO;
import com.studyconnect.common.dto.MessageDTO;
import com.studyconnect.common.dto.MessageDeliveryMode;
import com.studyconnect.common.dto.MessageReadEventDTO;
import com.studyconnect.common.dto.MessageStatus;
import com.studyconnect.common.dto.SendMessageDTO;
import com.studyconnect.server.model.dao.MessageDAO;

import java.sql.SQLException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;

public class MessageService {
    private static final int DEFAULT_PAGE_SIZE = 50;
    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_CONTENT_LENGTH = 5000;

    private final MessageDAO messageDAO;

    public MessageService() {
        this(new MessageDAO());
    }

    public MessageService(MessageDAO messageDAO) {
        this.messageDAO = messageDAO;
    }

    public MessageDTO relayMessage(long senderId, SendMessageDTO request)
            throws SQLException {
        ValidatedMessage validated = validate(senderId, request);
        return messageDAO.saveOrGet(
                validated.clientMessageId(),
                senderId,
                validated.receiverId(),
                validated.content(),
                MessageDeliveryMode.SERVER_RELAY,
                MessageStatus.SENT
        );
    }

    public MessageDTO syncP2PMessage(long senderId, SendMessageDTO request)
            throws SQLException {
        ValidatedMessage validated = validate(senderId, request);
        return messageDAO.saveOrGet(
                validated.clientMessageId(),
                senderId,
                validated.receiverId(),
                validated.content(),
                MessageDeliveryMode.P2P,
                MessageStatus.DELIVERED
        );
    }

    public MessageDTO markDelivered(String clientMessageId) throws SQLException {
        requireClientMessageId(clientMessageId);
        return messageDAO.updateStatus(
                clientMessageId, MessageStatus.DELIVERED, System.currentTimeMillis());
    }

    public List<MessageDTO> getMessages(long currentUserId, GetMessagesDTO request)
            throws SQLException {
        if (request == null || request.getOtherUserId() <= 0) {
            throw new IllegalArgumentException("Người trò chuyện không hợp lệ");
        }
        if (request.getOtherUserId() == currentUserId) {
            throw new IllegalArgumentException("Không thể mở hội thoại với chính mình");
        }
        if (!messageDAO.userExists(request.getOtherUserId())) {
            throw new NoSuchElementException("Người trò chuyện không tồn tại");
        }
        int limit = request.getLimit() <= 0
                ? DEFAULT_PAGE_SIZE
                : Math.min(request.getLimit(), MAX_PAGE_SIZE);
        Long beforeId = request.getBeforeMessageId();
        if (beforeId != null && beforeId <= 0) {
            throw new IllegalArgumentException("Mã phân trang không hợp lệ");
        }
        return messageDAO.findConversation(
                currentUserId, request.getOtherUserId(), beforeId, limit);
    }

    public List<ConversationDTO> getConversations(long currentUserId, Set<Long> onlineUserIds)
            throws SQLException {
        List<ConversationDTO> conversations = messageDAO.findConversations(currentUserId);
        Set<Long> online = onlineUserIds == null ? Set.of() : onlineUserIds;
        conversations.forEach(item -> item.setOnline(online.contains(item.getOtherUserId())));
        return conversations;
    }

    public MessageReadEventDTO markMessagesRead(long currentUserId, long otherUserId)
            throws SQLException {
        if (otherUserId <= 0 || otherUserId == currentUserId) {
            throw new IllegalArgumentException("Người trò chuyện không hợp lệ");
        }
        MessageDAO.ReadResult result = messageDAO.markRead(currentUserId, otherUserId);
        return new MessageReadEventDTO(
                currentUserId,
                result.messageIds(),
                result.clientMessageIds(),
                result.readAt()
        );
    }

    private ValidatedMessage validate(long senderId, SendMessageDTO request)
            throws SQLException {
        if (senderId <= 0) throw new IllegalArgumentException("Người gửi không hợp lệ");
        if (request == null || request.getReceiverId() <= 0) {
            throw new IllegalArgumentException("Người nhận không hợp lệ");
        }
        if (senderId == request.getReceiverId()) {
            throw new IllegalArgumentException("Không thể gửi tin nhắn cho chính mình");
        }
        String clientMessageId = requireClientMessageId(request.getClientMessageId());
        String content = request.getContent() == null ? "" : request.getContent().trim();
        if (content.isEmpty()) {
            throw new IllegalArgumentException("Nội dung tin nhắn không được để trống");
        }
        if (content.length() > MAX_CONTENT_LENGTH) {
            throw new IllegalArgumentException("Tin nhắn không được vượt quá 5000 ký tự");
        }
        if (!messageDAO.userExists(request.getReceiverId())) {
            throw new NoSuchElementException("Người nhận không tồn tại hoặc đã bị khóa");
        }
        return new ValidatedMessage(clientMessageId, request.getReceiverId(), content);
    }

    private String requireClientMessageId(String value) {
        String normalized = value == null ? "" : value.trim();
        try {
            if (normalized.length() != 36) throw new IllegalArgumentException();
            UUID.fromString(normalized);
            return normalized;
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("clientMessageId không phải UUID hợp lệ");
        }
    }

    private record ValidatedMessage(
            String clientMessageId,
            long receiverId,
            String content
    ) {
    }
}
