package com.studyconnect.common.dto;

import java.io.Serializable;

public class CreateMessageDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private long receiverId;
    private String content;

    public CreateMessageDTO() {
    }

    public CreateMessageDTO(long receiverId, String content) {
        this.receiverId = receiverId;
        this.content = content;
    }

    public long getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(long receiverId) {
        this.receiverId = receiverId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
