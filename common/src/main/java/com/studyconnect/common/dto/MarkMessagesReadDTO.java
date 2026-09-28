package com.studyconnect.common.dto;

import java.io.Serializable;

public class MarkMessagesReadDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private long otherUserId;

    public MarkMessagesReadDTO() {
    }

    public MarkMessagesReadDTO(long otherUserId) {
        this.otherUserId = otherUserId;
    }

    public long getOtherUserId() {
        return otherUserId;
    }

    public void setOtherUserId(long otherUserId) {
        this.otherUserId = otherUserId;
    }
}
