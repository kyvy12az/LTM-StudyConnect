package com.studyconnect.common.dto;

import java.io.Serializable;

public class SendMessageDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String clientMessageId;
    private long senderId;
    private String senderName;
    private String senderAvatarUrl;
    private long receiverId;
    private String content;
    private long createdAt;
    private MessageDeliveryMode deliveryMode;
    private String peerToken;
    private String senderPeerToken;

    public SendMessageDTO() {
    }

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
    public MessageDeliveryMode getDeliveryMode() { return deliveryMode; }
    public void setDeliveryMode(MessageDeliveryMode deliveryMode) { this.deliveryMode = deliveryMode; }
    public String getPeerToken() { return peerToken; }
    public void setPeerToken(String peerToken) { this.peerToken = peerToken; }
    public String getSenderPeerToken() { return senderPeerToken; }
    public void setSenderPeerToken(String senderPeerToken) { this.senderPeerToken = senderPeerToken; }
}
