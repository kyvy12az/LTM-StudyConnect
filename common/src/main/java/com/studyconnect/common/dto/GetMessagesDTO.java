package com.studyconnect.common.dto;

import java.io.Serializable;

public class GetMessagesDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private long otherUserId;
    private Long beforeMessageId;
    private int limit;

    public GetMessagesDTO() {
    }

    public GetMessagesDTO(long otherUserId, Long beforeMessageId, int limit) {
        this.otherUserId = otherUserId;
        this.beforeMessageId = beforeMessageId;
        this.limit = limit;
    }

    public long getOtherUserId() {
        return otherUserId;
    }

    public void setOtherUserId(long otherUserId) {
        this.otherUserId = otherUserId;
    }

    public Long getBeforeMessageId() {
        return beforeMessageId;
    }

    public void setBeforeMessageId(Long beforeMessageId) {
        this.beforeMessageId = beforeMessageId;
    }

    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }
}
