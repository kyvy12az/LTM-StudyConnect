package com.studyconnect.common.dto;

import java.io.Serializable;

public class CommentDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private long id;
    private long postId;
    private long authorId;
    private String authorName;
    private String authorAvatarUrl;
    private String content;
    private Long parentCommentId;
    private long createdAt;

    public CommentDTO() {
    }

    public CommentDTO(long id, long postId, long authorId, String authorName, String authorAvatarUrl, String content, Long parentCommentId, long createdAt) {
        this.id = id;
        this.postId = postId;
        this.authorId = authorId;
        this.authorName = authorName;
        this.authorAvatarUrl = authorAvatarUrl;
        this.content = content;
        this.parentCommentId = parentCommentId;
        this.createdAt = createdAt;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getPostId() {
        return postId;
    }

    public void setPostId(long postId) {
        this.postId = postId;
    }

    public long getAuthorId() {
        return authorId;
    }

    public void setAuthorId(long authorId) {
        this.authorId = authorId;
    }

    public String getAuthorName() {
        return authorName;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public String getAuthorAvatarUrl() {
        return authorAvatarUrl;
    }

    public void setAuthorAvatarUrl(String authorAvatarUrl) {
        this.authorAvatarUrl = authorAvatarUrl;
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

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "CommentDTO{" +
                "id=" + id +
                ", postId=" + postId +
                ", authorName='" + authorName + '\'' +
                ", parentCommentId=" + parentCommentId +
                ", createdAt=" + createdAt +
                '}';
    }
}
