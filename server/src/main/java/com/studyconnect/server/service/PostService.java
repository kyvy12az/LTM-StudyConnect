package com.studyconnect.server.service;

import com.studyconnect.common.dto.CreatePostDTO;
import com.studyconnect.common.dto.PostDTO;
import com.studyconnect.server.model.dao.PostDAO;

import java.sql.SQLException;
import java.util.List;
import java.util.NoSuchElementException;

public class PostService {
    private static final int MIN_TITLE_LENGTH = 3;
    private static final int MAX_TITLE_LENGTH = 200;
    private static final int MAX_CONTENT_LENGTH = 20_000;
    private static final int MAX_SUBJECT_LENGTH = 100;

    private static final String DEFAULT_SUBJECT = "Khác";

    private final PostDAO postDAO;

    public PostService() {
        this(new PostDAO());
    }

    public PostService (PostDAO postDAO) {
        if (postDAO == null) {
            throw new IllegalArgumentException("PostDAO không được null.");
        }
        this.postDAO = postDAO;
    }

    public PostDTO createPost(long authorId, CreatePostDTO createPostDTO) throws SQLException {
        validateAuthorId(authorId);

        if (createPostDTO == null) {
            throw new IllegalArgumentException(
                    "Dữ liệu bài viết không được để trống."
            );
        }

        String title = normalize(createPostDTO.getTitle());
        String content = normalize(createPostDTO.getContent());
        String subject = normalize(createPostDTO.getSubject());

        validateTitle(title);
        validateContent(content);

        if (subject.isEmpty()) {
            subject = DEFAULT_SUBJECT;
        }

        validateSubject(subject);

        // cập nhật lại dữ liệu đã chuẩn hóa vào DTO
        createPostDTO.setTitle(title);
        createPostDTO.setContent(content);
        createPostDTO.setSubject(subject);

        return postDAO.create(authorId, createPostDTO);
    }

    public List<PostDTO> getAllPosts() throws SQLException {
        return postDAO.findAll();
    }

    public PostDTO getPostById(long postId) throws SQLException {
        validatePostId(postId);

        return postDAO.findById(postId).orElseThrow(() -> new NoSuchElementException("Không tìm thấy bài viết."));
    }

    public List<PostDTO> getPostsBySubject(String subject) throws SQLException {
        String normalizedSubject = normalize(subject);

        if (normalizedSubject.isEmpty() || normalizedSubject.equalsIgnoreCase("Tất cả")) {
            return postDAO.findAll();
        }

        validateSubject(normalizedSubject);

        return postDAO.findBySubject(normalizedSubject);
    }

    public boolean postExists(long postId) throws SQLException {
        validatePostId(postId);
        return postDAO.exitsById(postId);
    }

    private void validatePostId(long postId) {
        if (postId <= 0) {
            throw new IllegalArgumentException("ID bài viết không hợp lệ.");
        }
    }

    private void validateAuthorId(long authorId) {
        if (authorId <= 0) {
            throw new IllegalArgumentException("Người dùng chưa đăng nhập hoặc không hợp lệ.");
        }
    }

    private void validateTitle(String title) {
        if (title.isEmpty()) {
            throw new IllegalArgumentException("Tiêu đề bài viết không được để trống.");
        }

        if (title.length() < MIN_TITLE_LENGTH) {
            throw new IllegalArgumentException("Tiêu đề phải có ít nhất " + MIN_TITLE_LENGTH + " ký tự.");
        }

        if (title.length() > MAX_TITLE_LENGTH) {
            throw new IllegalArgumentException("Tiêu đề không được vượt quá " + MAX_TITLE_LENGTH + " ký tự.");
        }
    }

    private void validateContent(String content) {
        if (content.isEmpty()) {
            throw new IllegalArgumentException("Nội dung bài viết không được để trống.");
        }

        if (content.length() > MAX_CONTENT_LENGTH) {
            throw new IllegalArgumentException("Nội dung bài viết không được vượt quá " + MAX_CONTENT_LENGTH + " ký tự.");
        }
    }

    private void validateSubject(String subject) {
        if (subject.length() > MAX_SUBJECT_LENGTH) {
            throw new IllegalArgumentException("Chủ đề bài viết không được vượt quá " + MAX_SUBJECT_LENGTH + " ký tự.");
        }
    }

    // chỉ loại bỏ khoảng trắng ở đầu và cuối
    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
