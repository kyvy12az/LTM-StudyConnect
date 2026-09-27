package com.studyconnect.common.dto;

import java.io.Serializable;

public class PostCreatedEventDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private PostDTO post;
    private long timestamp;

    public PostCreatedEventDTO() {
    }

    public PostCreatedEventDTO(PostDTO post, long timestamp) {
        this.post = post;
        this.timestamp = timestamp;
    }

    public PostDTO getPost() {
        return post;
    }

    public void setPost(PostDTO post) {
        this.post = post;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
