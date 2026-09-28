package com.studyconnect.common.dto;

import java.io.Serializable;

public class ConversationDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private long otherUserId;
    private String otherUsername;
    private String otherUserName;
    private String otherUserAvatarUrl;
    private boolean online;
    private MessageDTO lastMessage;
    private int unreadCount;

    public ConversationDTO() {
    }

    public long getOtherUserId() { return otherUserId; }
    public void setOtherUserId(long otherUserId) { this.otherUserId = otherUserId; }
    public String getOtherUsername() { return otherUsername; }
    public void setOtherUsername(String otherUsername) { this.otherUsername = otherUsername; }
    public String getOtherUserName() { return otherUserName; }
    public void setOtherUserName(String otherUserName) { this.otherUserName = otherUserName; }
    public String getOtherUserAvatarUrl() { return otherUserAvatarUrl; }
    public void setOtherUserAvatarUrl(String otherUserAvatarUrl) { this.otherUserAvatarUrl = otherUserAvatarUrl; }
    public boolean isOnline() { return online; }
    public void setOnline(boolean online) { this.online = online; }
    public MessageDTO getLastMessage() { return lastMessage; }
    public void setLastMessage(MessageDTO lastMessage) { this.lastMessage = lastMessage; }
    public int getUnreadCount() { return unreadCount; }
    public void setUnreadCount(int unreadCount) { this.unreadCount = unreadCount; }
}
