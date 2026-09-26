package com.studyconnect.common.protocol;

import java.io.Serializable;

public class Request<T> implements Serializable {
    private static final long serialVersionUID = 1L;

    private String requestId;
    private ActionType action;
    private String token; // Token của người dùng (nếu có)
    private T data; // Payload dữ liệu
    private long timestamp;

    // Constructor mặc định, tạo một requestId ngẫu nhiên và ghi lại thời gian hiện tại
    public Request() {
        this.requestId = java.util.UUID.randomUUID().toString(); // tạo một UUID ngẫu nhiên cho requestId
        this.timestamp = System.currentTimeMillis(); // ghi lại thời gian hiện tại
    }

    // Constructor với action và data
    public Request(ActionType action, T data) {
        this();
        this.action = action;
        this.data = data;
    }

    public Request(ActionType action, String token, T data) {
        this(action, data);
        this.token = token;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public ActionType getAction() {
        return action;
    }

    public void setAction(ActionType action) {
        this.action = action;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
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

    @Override
    public String toString() {
        return "Request{" +
                "requestId='" + requestId + '\'' +
                ", action=" + action +
                ", timestamp=" + timestamp +
                '}';
    }
}
