package com.studyconnect.server.model.dao;

import com.studyconnect.common.dto.PostAttachmentDTO;
import com.studyconnect.server.model.database.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PostAttachmentDAO {

    public PostAttachmentDTO createTemporary(
            long uploaderId,
            String originalName,
            String storedName,
            String mimeType,
            String attachmentType,
            long sizeBytes,
            String storagePath
    ) throws SQLException {
        String sql = """
                INSERT INTO post_attachments (
                    post_id,
                    uploader_id,
                    original_name,
                    stored_name,
                    mime_type,
                    attachment_type,
                    size_bytes,
                    storage_path,
                    status
                )
                VALUES (
                    NULL,
                    ?, ?, ?, ?, ?, ?, ?,
                    'TEMPORARY'
                )
                """;

        try (
                Connection connection =
                        DatabaseConnection.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(
                                sql,
                                Statement.RETURN_GENERATED_KEYS
                        )
        ) {
            statement.setLong(
                    1,
                    uploaderId
            );

            statement.setString(
                    2,
                    originalName
            );

            statement.setString(
                    3,
                    storedName
            );

            statement.setString(
                    4,
                    mimeType
            );

            statement.setString(
                    5,
                    attachmentType
            );

            statement.setLong(
                    6,
                    sizeBytes
            );

            statement.setString(
                    7,
                    storagePath
            );

            int affectedRows =
                    statement.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException(
                        "Không thể lưu thông tin attachment"
                );
            }

            try (
                    ResultSet generatedKeys =
                            statement.getGeneratedKeys()
            ) {
                if (!generatedKeys.next()) {
                    throw new SQLException(
                            "Không lấy được ID attachment"
                    );
                }

                long attachmentId =
                        generatedKeys.getLong(1);

                return new PostAttachmentDTO(
                        attachmentId,
                        originalName,
                        mimeType,
                        attachmentType,
                        sizeBytes
                );
            }
        }
    }

    public void attachToPost(
            long postId,
            long uploaderId,
            List<Long> attachmentIds
    ) throws SQLException {
        if (attachmentIds == null
                || attachmentIds.isEmpty()) {
            return;
        }

        List<Long> uniqueIds = attachmentIds.stream()
                .filter(id -> id != null && id > 0)
                .distinct()
                .toList();

        if (uniqueIds.isEmpty()) {
            return;
        }

        String placeholders = String.join(
                ",",
                java.util.Collections.nCopies(
                        uniqueIds.size(),
                        "?"
                )
        );

        String sql = """
            UPDATE post_attachments
            SET
                post_id = ?,
                status = 'ACTIVE'
            WHERE uploader_id = ?
              AND post_id IS NULL
              AND status = 'TEMPORARY'
              AND id IN (%s)
            """.formatted(placeholders);

        try (
                Connection connection =
                        DatabaseConnection.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {
            int parameterIndex = 1;

            statement.setLong(
                    parameterIndex++,
                    postId
            );

            statement.setLong(
                    parameterIndex++,
                    uploaderId
            );

            for (Long attachmentId : uniqueIds) {
                statement.setLong(
                        parameterIndex++,
                        attachmentId
                );
            }

            int updatedRows =
                    statement.executeUpdate();

            if (updatedRows != uniqueIds.size()) {
                throw new SQLException(
                        "Một hoặc nhiều tệp đính kèm "
                                + "không hợp lệ hoặc không thuộc người dùng"
                );
            }
        }
    }

    public List<PostAttachmentDTO> findByPostId(
            long postId
    ) throws SQLException {
        String sql = """
            SELECT
                id,
                original_name,
                mime_type,
                attachment_type,
                size_bytes
            FROM post_attachments
            WHERE post_id = ?
              AND status = 'ACTIVE'
            ORDER BY id ASC
            """;

        List<PostAttachmentDTO> attachments = new ArrayList<>();

        try (
                Connection connection =
                        DatabaseConnection.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {
            statement.setLong(1, postId);

            try (
                    ResultSet result =
                            statement.executeQuery()
            ) {
                while (result.next()) {
                    attachments.add(
                            new PostAttachmentDTO(
                                    result.getLong("id"),
                                    result.getString(
                                            "original_name"
                                    ),
                                    result.getString(
                                            "mime_type"
                                    ),
                                    result.getString(
                                            "attachment_type"
                                    ),
                                    result.getLong(
                                            "size_bytes"
                                    )
                            )
                    );
                }
            }
        }

        return attachments;
    }

    public Optional<String> findActiveStoragePath(
            long attachmentId
    ) throws SQLException {
        if (attachmentId <= 0) {
            return Optional.empty();
        }

        String sql = """
            SELECT storage_path
            FROM post_attachments
            WHERE id = ?
              AND post_id IS NOT NULL
              AND status = 'ACTIVE'
            """;

        try (
                Connection connection =
                        DatabaseConnection.getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {
            statement.setLong(1, attachmentId);

            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    return Optional.empty();
                }

                String storagePath = result.getString("storage_path");
                if (storagePath == null || storagePath.isBlank()) {
                    return Optional.empty();
                }
                return Optional.of(storagePath);
            }
        }
    }
}
