package com.studyconnect.common.dto;

import java.io.Serializable;

public class CreateCommentDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private long postId;
    private String content;
    private Long parentCommentId;

    public CreateCommentDTO() {
    }

    public CreateCommentDTO(long postId, String content, Long parentCommentId) {
        this.postId = postId;
        this.content = content;
        this.parentCommentId = parentCommentId;
    }

    public CreateCommentDTO(long postId, String content) {
        this(postId, content, null);
    }

    public long getPostId() {
        return postId;
    }

    public void setPostId(long postId) {
        this.postId = postId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Long getParentCommentId() {
        return parentCommentId;
    }

    public void setParentCommentId(Long parentCommentId) {
        this.parentCommentId = parentCommentId;
    }

    @Override
    public String toString() {
        return "CreateCommentDTO{" +
                "postId=" + postId +
                ", parentCommentId=" + parentCommentId +
                '}';
    }
}
