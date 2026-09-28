package com.studyconnect.common.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class MessageReadEventDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private long readerId;
    private List<Long> messageIds = new ArrayList<>();
    private List<String> clientMessageIds = new ArrayList<>();
    private long readAt;

    public MessageReadEventDTO() {
    }

    public MessageReadEventDTO(long readerId, List<Long> messageIds, long readAt) {
        this(readerId, messageIds, null, readAt);
    }

    public MessageReadEventDTO(long readerId, List<Long> messageIds,
                               List<String> clientMessageIds, long readAt) {
        this.readerId = readerId;
        this.messageIds = messageIds == null ? new ArrayList<>() : new ArrayList<>(messageIds);
        this.clientMessageIds = clientMessageIds == null
                ? new ArrayList<>() : new ArrayList<>(clientMessageIds);
        this.readAt = readAt;
    }

    public long getReaderId() { return readerId; }
    public void setReaderId(long readerId) { this.readerId = readerId; }
    public List<Long> getMessageIds() { return messageIds; }
    public void setMessageIds(List<Long> messageIds) {
        this.messageIds = messageIds == null ? new ArrayList<>() : messageIds;
    }
    public List<String> getClientMessageIds() { return clientMessageIds; }
    public void setClientMessageIds(List<String> clientMessageIds) {
        this.clientMessageIds = clientMessageIds == null ? new ArrayList<>() : clientMessageIds;
    }
    public long getReadAt() { return readAt; }
    public void setReadAt(long readAt) { this.readAt = readAt; }
}
