package com.studyconnect.server.service;

import com.studyconnect.common.dto.CommentDTO;
import com.studyconnect.common.dto.CreateCommentDTO;
import com.studyconnect.server.model.dao.CommentDAO;
import com.studyconnect.server.model.dao.PostDAO;

import java.sql.SQLException;
import java.util.List;
import java.util.NoSuchElementException;

public class CommentService {
    private static final int MAX_COMMENT_LENGTH = 2_000;

    private final CommentDAO commentDAO;
    private final PostDAO postDAO;

    public CommentService() {
        this(new CommentDAO(), new PostDAO());
    }

    public CommentService(CommentDAO commentDAO, PostDAO postDAO) {
        if (commentDAO == null) {
            throw new IllegalArgumentException("CommentDAO không được null.");
        }

        if (postDAO == null) {
            throw new IllegalArgumentException("PostDAO không được null.");
        }

        this.commentDAO = commentDAO;
        this.postDAO = postDAO;
    }

    public CommentDTO createComment(long authorId, CreateCommentDTO createCommentDTO) throws SQLException {
        validateAuthorId(authorId);

        if (createCommentDTO == null) {
            throw new IllegalArgumentException("Dữ liệu bình luận không được để trống.");
        }

        long postId = createCommentDTO.getPostId();
        validatePostId(postId);

        if (!postDAO.exitsById(postId)) {
            throw new NoSuchElementException("Bài viết không tồn tại.");
        }

        String content = normalize(createCommentDTO.getContent());
        validateContent(content);

        Long parentCommentId = createCommentDTO.getParentCommentId();
        if (parentCommentId != null) {
            validateParentComment(parentCommentId, postId);
        }

        createCommentDTO.setContent(content);

        return commentDAO.create(authorId, createCommentDTO);
    }

    // lấy danh sách bình luận của một bài viết
    public List<CommentDTO> getCommentsByPostId(long postId) throws SQLException {
        validatePostId(postId);

        if (!postDAO.exitsById(postId)) {
            throw new NoSuchElementException("Bài viết không tồn tại.");
        }

        return commentDAO.findByPostId(postId);
    }

    // lấy một bình luận theo ID
    public CommentDTO getCommentById(long commentId) throws SQLException {
        validateCommentId(commentId);

        return commentDAO.findById(commentId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy bình luận."));
    }

    public int countComments(long postId) throws SQLException {
        validatePostId(postId);
        return commentDAO.countByPostId(postId);
    }

    private void validatePostId(long postId) {
        if (postId <= 0) {
            throw new IllegalArgumentException("ID bài viết không hợp lệ.");
        }
    }

    private void validateParentComment(long parentCommentId, long postId) throws SQLException {
        validateCommentId(parentCommentId);

        boolean parentExists = commentDAO.exitsInPost(parentCommentId, postId);
        if (!parentExists) {
            throw new NoSuchElementException("Bình luận cha không tồn tại trong bài viết này.");
        }
    }

    private void validateCommentId(long commentId) {
        if (commentId <= 0) {
            throw new IllegalArgumentException("ID bình luận không hợp lệ.");
        }
    }

    private void validateContent(String content) {
        if (content.isEmpty()) {
            throw new IllegalArgumentException("Nội dung bình luận không được để trống.");
        }

        if (content.length() > MAX_COMMENT_LENGTH) {
            throw new IllegalArgumentException("Nội dung bình luận không được vượt quá " + MAX_COMMENT_LENGTH + " ký tự.");
        }
    }

    private void validateAuthorId(long authorId) {
        if (authorId <= 0) {
            throw new IllegalArgumentException("ID tác giả không hợp lệ.");
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
