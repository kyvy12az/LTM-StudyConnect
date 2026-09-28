package com.studyconnect.server.model.dao;

import com.studyconnect.common.dto.CreatePostDTO;
import com.studyconnect.common.dto.PostDTO;
import com.studyconnect.common.dto.PostLikeDTO;
import com.studyconnect.server.model.database.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PostDAO {
    private static final String SELECT_POST_FIELDS = """
            SELECT
                p.id,
                p.author_id,
                COALESCE(u.full_name, u.username) AS author_name,
                u.avatar_url AS author_avatar_url,
                p.title,
                p.content,
                p.subject,
                p.created_at,
                (
                    SELECT COUNT(*)
                    FROM post_likes pl
                    WHERE pl.post_id = p.id
                ) AS like_count,
                EXISTS (
                    SELECT 1
                    FROM post_likes current_like
                    WHERE current_like.post_id = p.id
                      AND current_like.user_id = ?
                ) AS liked_by_current_user,
                (
                    SELECT COUNT(*)
                    FROM comments c
                    WHERE c.post_id = p.id
                ) AS comment_count
            FROM posts p
            INNER JOIN users u ON u.id = p.author_id
            """;

    private final PostAttachmentDAO attachmentDAO = new PostAttachmentDAO();

    public long countCreatedToday() throws SQLException {
        String sql = """
                SELECT COUNT(*)
                FROM posts
                WHERE status <> 'DELETED'
                  AND DATE(created_at) = CURRENT_DATE
                """;
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            return result.next() ? result.getLong(1) : 0L;
        }
    }

    public PostDTO create(long authorId, CreatePostDTO createPostDTO) throws SQLException {
        String sql = """
                INSERT INTO posts (
                    author_id,
                    title,
                    content,
                    subject,
                    created_at
                )
                VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP)
                """;

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)
        ) {
            statement.setLong(1, authorId);
            statement.setString(2, createPostDTO.getTitle());
            statement.setString(3, createPostDTO.getContent());
            statement.setString(4, createPostDTO.getSubject());

            int affectedRows = statement.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("Không thể tạo bài viết.");
            }

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (!generatedKeys.next()) {
                    throw new SQLException("Không lấy được ID bài viết.");
                }

                long postId = generatedKeys.getLong(1);

                return findById(postId, authorId)
                        .orElseThrow(() ->
                                new SQLException("Không tìm thấy bài viết vừa tạo.")
                        );
            }
        }
    }

    public Optional<PostDTO> findById(long postId) throws SQLException {
        return findById(postId, 0L);
    }

    public Optional<PostDTO> findById(long postId, long viewerId) throws SQLException {
        String sql = SELECT_POST_FIELDS + """
                WHERE p.id = ?
                """;

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql);
        ) {
            statement.setLong(1, viewerId);
            statement.setLong(2, postId);

            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    return Optional.of(mapPost(result));
                }
            }
        }
        return Optional.empty();
    }

    public List<PostDTO> findAll() throws SQLException {
        return findAll(0L);
    }

    public List<PostDTO> findAll(long viewerId) throws SQLException {
        String sql = SELECT_POST_FIELDS + """
                ORDER BY p.created_at DESC, p.id DESC
                """;

        List<PostDTO> posts = new ArrayList<>();

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setLong(1, viewerId);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    posts.add(mapPost(result));
                }
            }
        }

        return posts;
    }

    public boolean exitsById(long postId) throws SQLException {
        String sql = """
                SELECT 1
                FROM posts
                WHERE id = ?
                LIMIT 1
                """;

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql);
        ) {
            statement.setLong(1, postId);

            try (ResultSet result = statement.executeQuery()) {
                return result.next();
            }
        }
    }

    public List<PostDTO> findBySubject(String subject) throws SQLException {
        return findBySubject(subject, 0L);
    }

    public List<PostDTO> findBySubject(String subject, long viewerId) throws SQLException {
        String sql = SELECT_POST_FIELDS + """
                WHERE p.subject = ?
                ORDER BY p.created_at DESC, p.id DESC
                """;

        List<PostDTO> posts = new ArrayList<>();

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql);
        ) {
            statement.setLong(1, viewerId);
            statement.setString(2, subject);

            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    posts.add(mapPost(result));
                }
            }
        }

        return posts;
    }

    public PostLikeDTO setLiked(long postId, long userId, boolean liked)
            throws SQLException {
        String mutationSql = liked
                ? "INSERT IGNORE INTO post_likes (post_id, user_id) VALUES (?, ?)"
                : "DELETE FROM post_likes WHERE post_id = ? AND user_id = ?";
        String countSql = "SELECT COUNT(*) FROM post_likes WHERE post_id = ?";

        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement statement = connection.prepareStatement(mutationSql)) {
                    statement.setLong(1, postId);
                    statement.setLong(2, userId);
                    statement.executeUpdate();
                }

                int likeCount;
                try (PreparedStatement statement = connection.prepareStatement(countSql)) {
                    statement.setLong(1, postId);
                    try (ResultSet result = statement.executeQuery()) {
                        likeCount = result.next() ? result.getInt(1) : 0;
                    }
                }

                connection.commit();
                return new PostLikeDTO(
                        postId,
                        userId,
                        liked,
                        likeCount,
                        System.currentTimeMillis()
                );
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    private PostDTO mapPost(ResultSet result) throws SQLException {
        Timestamp createdAt = result.getTimestamp("created_at");
//        String avatarUrl = result.getString("author_avatar_url");

        PostDTO post = new PostDTO(
                result.getLong("id"),
                result.getLong("author_id"),
                result.getString("author_name"),
                result.getString("author_avatar_url"),
                result.getString("title"),
                result.getString("content"),
                result.getString("subject"),
                createdAt != null ? createdAt.getTime() : 0,
                result.getInt("like_count"),
                result.getInt("comment_count")
        );

        post.setLikedByCurrentUser(
                result.getBoolean("liked_by_current_user")
        );

        post.setAttachments(attachmentDAO.findByPostId(post.getId()));

        return post;
    }
}
