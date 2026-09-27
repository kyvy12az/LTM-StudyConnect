package com.studyconnect.common.protocol;

import java.io.Serializable;

public class ServerEvent<T> implements Serializable {
    private static final long serialVersionUID = 1L;

    private ServerEventType type;
    private T data;
    private long timestamp;

    public ServerEvent() {
        this.timestamp = System.currentTimeMillis();
    }

    public ServerEvent(ServerEventType type, T data) {
        this();
        this.type = type;
        this.data = data;
    }

    public ServerEventType getType() {
        return type;
    }

    public void setType(ServerEventType type) {
        this.type = type;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
