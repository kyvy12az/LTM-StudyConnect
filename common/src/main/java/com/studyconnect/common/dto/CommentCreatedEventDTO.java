package com.studyconnect.common.dto;

import java.io.Serializable;

public class CommentCreatedEventDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private long postId;
    private CommentDTO comment;
    private int commentCount;
    private long timestamp;

    public CommentCreatedEventDTO() {
    }

    public CommentCreatedEventDTO(
            long postId,
            CommentDTO comment,
            int commentCount,
            long timestamp
    ) {
        this.postId = postId;
        this.comment = comment;
        this.commentCount = commentCount;
        this.timestamp = timestamp;
    }

    public long getPostId() {
        return postId;
    }

    public void setPostId(long postId) {
        this.postId = postId;
    }

    public CommentDTO getComment() {
        return comment;
    }

    public void setComment(CommentDTO comment) {
        this.comment = comment;
    }

    public int getCommentCount() {
        return commentCount;
    }

    public void setCommentCount(int commentCount) {
        this.commentCount = commentCount;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
