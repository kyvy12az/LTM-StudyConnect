package com.studyconnect.common.dto;

import java.io.Serializable;

public class MessageDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private long id;
    private String clientMessageId;
    private long senderId;
    private String senderName;
    private String senderAvatarUrl;
    private long receiverId;
    private String content;
    private long createdAt;
    private MessageStatus status;
    private MessageDeliveryMode deliveryMode;
    private long deliveredAt;
    private long readAt;

    public MessageDTO() {
    }

    public MessageDTO(long id, long senderId, long receiverId,
                      String content, long sentAt, boolean read) {
        this.id = id;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.content = content;
        this.createdAt = sentAt;
        this.status = read ? MessageStatus.READ : MessageStatus.SENT;
        this.deliveryMode = MessageDeliveryMode.SERVER_RELAY;
    }

    public MessageDTO(long id, String clientMessageId, long senderId,
                      String senderName, String senderAvatarUrl, long receiverId,
                      String content, long createdAt, MessageStatus status,
                      MessageDeliveryMode deliveryMode, long deliveredAt, long readAt) {
        this.id = id;
        this.clientMessageId = clientMessageId;
        this.senderId = senderId;
        this.senderName = senderName;
        this.senderAvatarUrl = senderAvatarUrl;
        this.receiverId = receiverId;
        this.content = content;
        this.createdAt = createdAt;
        this.status = status;
        this.deliveryMode = deliveryMode;
        this.deliveredAt = deliveredAt;
        this.readAt = readAt;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getClientMessageId() { return clientMessageId; }
    public void setClientMessageId(String clientMessageId) { this.clientMessageId = clientMessageId; }
    public long getSenderId() { return senderId; }
    public void setSenderId(long senderId) { this.senderId = senderId; }
    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }
    public String getSenderAvatarUrl() { return senderAvatarUrl; }
    public void setSenderAvatarUrl(String senderAvatarUrl) { this.senderAvatarUrl = senderAvatarUrl; }
    public long getReceiverId() { return receiverId; }
    public void setReceiverId(long receiverId) { this.receiverId = receiverId; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    public MessageStatus getStatus() { return status; }
    public void setStatus(MessageStatus status) { this.status = status; }
    public MessageDeliveryMode getDeliveryMode() { return deliveryMode; }
    public void setDeliveryMode(MessageDeliveryMode deliveryMode) { this.deliveryMode = deliveryMode; }
    public long getDeliveredAt() { return deliveredAt; }
    public void setDeliveredAt(long deliveredAt) { this.deliveredAt = deliveredAt; }
    public long getReadAt() { return readAt; }
    public void setReadAt(long readAt) { this.readAt = readAt; }

    // Aliases kept for existing Swing components and older JSON clients.
    public long getSentAt() { return createdAt; }
    public void setSentAt(long sentAt) { this.createdAt = sentAt; }
    public boolean isRead() { return status == MessageStatus.READ; }
    public void setRead(boolean read) {
        if (read) status = MessageStatus.READ;
        else if (status == MessageStatus.READ) status = MessageStatus.DELIVERED;
    }
}
