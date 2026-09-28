package com.studyconnect.common.dto;

import java.io.Serializable;

public class PostLikeDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private long postId;
    private long userId;
    private boolean liked;
    private int likeCount;
    private long updatedAt;

    public PostLikeDTO() {
    }

    public PostLikeDTO(
            long postId,
            long userId,
            boolean liked,
            int likeCount,
            long updatedAt
    ) {
        this.postId = postId;
        this.userId = userId;
        this.liked = liked;
        this.likeCount = likeCount;
        this.updatedAt = updatedAt;
    }

    public long getPostId() {
        return postId;
    }

    public void setPostId(long postId) {
        this.postId = postId;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public boolean isLiked() {
        return liked;
    }

    public void setLiked(boolean liked) {
        this.liked = liked;
    }

    public int getLikeCount() {
        return likeCount;
    }

    public void setLikeCount(int likeCount) {
        this.likeCount = likeCount;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }
}
