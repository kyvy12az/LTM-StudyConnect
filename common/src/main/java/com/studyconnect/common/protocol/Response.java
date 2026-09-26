package com.studyconnect.common.protocol;

import java.io.Serializable;

public class Response<T> implements Serializable {
    private static final long serialVersionUID = 1L;

    private String requestId;
    private StatusCode statusCode;
    private String message;
    private T data;
    private long timestamp;

    public Response() {
        this.timestamp = System.currentTimeMillis();
    }

    // constructor để tạo một Response với tất cả các trường
    public Response(String requestId, StatusCode statusCode, String message, T data) {
        this();
        this.requestId = requestId;
        this.statusCode = statusCode;
        this.message = message;
        this.data = data;
    }

    // constructor để tạo một Response thành công với dữ liệu
    public static <T> Response<T> success(String requestId, String message, T data) {
        return new Response<T>(requestId, StatusCode.SUCCESS, message, data);
    }

    // constructor để tạo một Response lỗi với mã trạng thái và thông điệp
    public static <T> Response<T> error(String resquestId, StatusCode statusCode, String message) {
        return new Response<T>(resquestId, statusCode, message, null);
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public StatusCode getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(StatusCode statusCode) {
        this.statusCode = statusCode;
    }

    public boolean isSuccess() {
        return StatusCode.SUCCESS.equals(statusCode);
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
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
        return "Response{" +
                "requestId='" + requestId + '\'' +
                ", statusCode=" + statusCode +
                ", message='" + message + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
