package com.studyconnect.server.model.dao;

import com.studyconnect.common.dto.CommentDTO;
import com.studyconnect.common.dto.CreateCommentDTO;
import com.studyconnect.server.model.database.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CommentDAO {
    private static final String SELECT_COMMENT_FIELDS = """
            SELECT
                c.id,
                c.post_id,
                c.author_id,
                COALESCE(u.full_name, u.username) AS author_name,
                u.avatar_url AS author_avatar_url,
                c.content,
                c.parent_comment_id,
                c.created_at
            FROM comments c
            INNER JOIN users u ON u.id = c.author_id
            """;

    // thêm 1 bình luận mới hoặc một câu trả lời trong bình luận
    public CommentDTO create(long authorId, CreateCommentDTO createCommentDTO) throws SQLException {
        String sql = """
                INSERT INTO comments (
                    post_id,
                    author_id,
                    content,
                    parent_comment_id,
                    created_at
                )
                VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP)
                """;

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)
        ) {
            statement.setLong(1, createCommentDTO.getPostId());
            statement.setLong(2, authorId);
            statement.setString(3, createCommentDTO.getContent());

            Long parentCommentId = createCommentDTO.getParentCommentId();

            if (parentCommentId == null) {
                statement.setNull(4, Types.BIGINT);
            } else {
                statement.setLong(4, parentCommentId);
            }

            int affectedRows = statement.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("Không thể tạo bình luận.");
            }

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {

                if (!generatedKeys.next()) {
                    throw new SQLException("Không lấy được ID bình luận.");
                }

                long commentId = generatedKeys.getLong(1);

                return findById(commentId)
                        .orElseThrow(() ->
                                new SQLException("Không tìm thấy bình luận vừa tạo."));
            }
        }
    }

    // lấy toàn bộ bình luận thuộc 1 bài viết
    public List<CommentDTO> findByPostId(long postId) throws SQLException {
        String sql = SELECT_COMMENT_FIELDS + """
                WHERE c.post_id = ?
                ORDER BY c.created_at ASC, c.id ASC
                """;

        List<CommentDTO> comments = new ArrayList<>();

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setLong(1, postId);

            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    comments.add(mapComment(result));
                }
            }
        }

        return comments;
    }

    // lấy 1 bình luận theo ID
    public Optional<CommentDTO> findById(long commentId) throws SQLException {
        String sql = SELECT_COMMENT_FIELDS + """
                WHERE c.id = ?
                """;

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setLong(1, commentId);

            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    return Optional.of(mapComment(result));
                }
            }
        }

        return Optional.empty();
    }

    // đếm số bình luận của một bài viết
    public int countByPostId(long postId) throws SQLException {
        String sql = """
                SELECT COUNT(*) AS total
                FROM comments
                WHERE post_id = ?
                """;

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setLong(1, postId);

            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    return result.getInt("total");
                }
            }
        }

        return 0;
    }

    // kiểm tra bình luận có tồn tại và thuộc đúng bai viết hay không
    public boolean exitsInPost(long commentId, long postId) throws SQLException {
        String sql = """
                SELECT 1
                FROM comments
                WHERE id = ? AND post_id = ?
                LIMIT 1
                """;

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql);
        ) {
            statement.setLong(1, commentId);
            statement.setLong(2, postId);

            try (ResultSet result = statement.executeQuery()) {
                return result.next();
            }
        }
    }

    private CommentDTO mapComment(ResultSet result) throws SQLException {
        Long parentCommentId = result.getObject(
                "parent_comment_id",
                Long.class
        );

        Timestamp createdAt = result.getTimestamp("created_at");

        return new CommentDTO(
                result.getLong("id"),
                result.getLong("post_id"),
                result.getLong("author_id"),
                result.getString("author_name"),
                result.getString("author_avatar_url"),
                result.getString("content"),
                parentCommentId,
                createdAt != null ? createdAt.getTime() : 0
        );
    }
}
