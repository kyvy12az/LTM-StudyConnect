package com.studyconnect.common.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class PostDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private long id;
    private long authorId;
    private String authorName;
    private String authorAvatarUrl;
    private String title;
    private String content;
    private String subject;
    private long createdAt;
    private int likeCount;
    private boolean likedByCurrentUser;
    private int commentCount;
    private List<PostAttachmentDTO> attachments = new ArrayList<>();

    public PostDTO() {
    }

    public PostDTO(long id, long authorId, String authorName, String authorAvatarUrl, String title, String content, String subject, long createdAt, int likeCount, int commentCount) {
        this.id = id;
        this.authorId = authorId;
        this.authorName = authorName;
        this.authorAvatarUrl = authorAvatarUrl;
        this.title = title;
        this.content = content;
        this.subject = subject;
        this.createdAt = createdAt;
        this.likeCount = likeCount;
        this.commentCount = commentCount;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
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

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public int getLikeCount() {
        return likeCount;
    }

    public void setLikeCount(int likeCount) {
        this.likeCount = likeCount;
    }

    public boolean isLikedByCurrentUser() {
        return likedByCurrentUser;
    }

    public void setLikedByCurrentUser(boolean likedByCurrentUser) {
        this.likedByCurrentUser = likedByCurrentUser;
    }

    public int getCommentCount() {
        return commentCount;
    }

    public void setCommentCount(int commentCount) {
        this.commentCount = commentCount;
    }

    public String getAuthorAvatarUrl() {
        return authorAvatarUrl;
    }

    public void setAuthorAvatarUrl(String authorAvatarUrl) {
        this.authorAvatarUrl = authorAvatarUrl;
    }

    public List<PostAttachmentDTO> getAttachments() {
        return attachments;
    }

    public void setAttachments(
            List<PostAttachmentDTO> attachments
    ) {
        this.attachments = attachments == null
                ? new ArrayList<>()
                : attachments;
    }

    @Override
    public String toString() {
        return "PostDTO{" +
                "id=" + id +
                ", authorId=" + authorId +
                ", authorName='" + authorName + '\'' +
                ", title='" + title + '\'' +
                ", subject='" + subject + '\'' +
                ", createdAt=" + createdAt +
                ", likeCount=" + likeCount +
                ", likedByCurrentUser=" + likedByCurrentUser +
                ", commentCount=" + commentCount +
                '}';
    }
}
