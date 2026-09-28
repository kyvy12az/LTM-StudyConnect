package com.studyconnect.common.dto;

import java.io.Serializable;

public class MessageAckDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String clientMessageId;
    private long messageId;
    private MessageStatus status;
    private long timestamp;
    private String errorMessage;

    public MessageAckDTO() {
    }

    public MessageAckDTO(String clientMessageId, long messageId,
                         MessageStatus status, long timestamp, String errorMessage) {
        this.clientMessageId = clientMessageId;
        this.messageId = messageId;
        this.status = status;
        this.timestamp = timestamp;
        this.errorMessage = errorMessage;
    }

    public String getClientMessageId() { return clientMessageId; }
    public void setClientMessageId(String clientMessageId) { this.clientMessageId = clientMessageId; }
    public long getMessageId() { return messageId; }
    public void setMessageId(long messageId) { this.messageId = messageId; }
    public MessageStatus getStatus() { return status; }
    public void setStatus(MessageStatus status) { this.status = status; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}
