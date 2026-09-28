package com.studyconnect.server.model.dao;

import com.studyconnect.common.dto.PostAttachmentDTO;
import com.studyconnect.server.model.database.DatabaseConnection;

import java.sql.*;

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
}