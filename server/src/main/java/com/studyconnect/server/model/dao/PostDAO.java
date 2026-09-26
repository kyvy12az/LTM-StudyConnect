package com.studyconnect.server.model.dao;

import com.studyconnect.common.dto.CreatePostDTO;
import com.studyconnect.common.dto.PostDTO;
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
                0 AS like_count,
                (
                    SELECT COUNT(*)
                    FROM comments c
                    WHERE c.post_id = p.id
                ) AS comment_count
            FROM posts p
            INNER JOIN users u ON u.id = p.author_id
            """;

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

                return findById(postId)
                        .orElseThrow(() ->
                                new SQLException("Không tìm thấy bài viết vừa tạo.")
                        );
            }
        }
    }

    public Optional<PostDTO> findById(long postId) throws SQLException {
        String sql = SELECT_POST_FIELDS + """
                WHERE p.id = ?
                """;

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql);
        ) {
            statement.setLong(1, postId);

            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    return Optional.of(mapPost(result));
                }
            }
        }
        return Optional.empty();
    }

    public List<PostDTO> findAll() throws SQLException {
        String sql = SELECT_POST_FIELDS + """
                ORDER BY p.created_at DESC, p.id DESC
                """;

        List<PostDTO> posts = new ArrayList<>();

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet result = statement.executeQuery()
        ) {
            while (result.next()) {
                posts.add(mapPost(result));
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
        String sql = SELECT_POST_FIELDS + """
                WHERE p.subject = ?
                ORDER BY p.created_at DESC, p.id DESC
                """;

        List<PostDTO> posts = new ArrayList<>();

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql);
        ) {
            statement.setString(1, subject);

            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    posts.add(mapPost(result));
                }
            }
        }

        return posts;
    }

    private PostDTO mapPost(ResultSet result) throws SQLException {
        Timestamp createdAt = result.getTimestamp("created_at");
        String avatarUrl = result.getString("author_avatar_url");

        return new PostDTO(
                result.getLong("id"),
                result.getLong("author_id"),
                result.getString("author_name"),
                avatarUrl,
                result.getString("title"),
                result.getString("content"),
                result.getString("subject"),
                createdAt != null ? createdAt.getTime() : 0,
                result.getInt("like_count"),
                result.getInt("comment_count")
        );
    }
}
