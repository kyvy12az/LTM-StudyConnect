package com.studyconnect.server.network.file;

import com.studyconnect.common.dto.PostAttachmentDTO;
import com.studyconnect.common.util.JsonUtils;
import com.studyconnect.server.model.dao.PostAttachmentDAO;
import com.studyconnect.server.network.session.SessionManager;

import java.io.*;
import java.net.Socket;
import java.nio.file.*;
import java.sql.SQLException;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public class FileUploadHandler implements Runnable {
    private static final long MAX_IMAGE_SIZE =
            10L * 1024 * 1024;

    private static final long MAX_DOCUMENT_SIZE =
            25L * 1024 * 1024;

    private static final Set<String> IMAGE_EXTENSIONS =
            Set.of(
                    "png",
                    "jpg",
                    "jpeg",
                    "gif",
                    "webp"
            );

    private static final Set<String> DOCUMENT_EXTENSIONS =
            Set.of(
                    "pdf",
                    "doc",
                    "docx",
                    "xls",
                    "xlsx",
                    "ppt",
                    "pptx",
                    "txt"
            );

    private final Socket socket;
    private final SessionManager sessionManager;
    private final PostAttachmentDAO attachmentDAO;
    private final Path uploadDirectory;

    public FileUploadHandler(Socket socket) {
        this.socket = socket;
        this.sessionManager =
                SessionManager.getInstance();
        this.attachmentDAO =
                new PostAttachmentDAO();

        this.uploadDirectory = Paths.get(
                "server-data",
                "uploads",
                "posts"
        ).toAbsolutePath().normalize();
    }

    @Override
    public void run() {
        Path temporaryFile = null;
        Path destinationFile = null;

        try (
                Socket currentSocket = socket;

                DataInputStream input =
                        new DataInputStream(
                                new BufferedInputStream(
                                        currentSocket.getInputStream()
                                )
                        );

                DataOutputStream output =
                        new DataOutputStream(
                                new BufferedOutputStream(
                                        currentSocket.getOutputStream()
                                )
                        )
        ) {
            try {
                String operation = input.readUTF();

                if (!"UPLOAD_POST_ATTACHMENT".equals(operation)) {
                    throw new IllegalArgumentException(
                            "Thao tác truyền file không hợp lệ"
                    );
                }

                String token = input.readUTF();

                Long uploaderId =
                        sessionManager.getUserId(token);

                if (uploaderId == null) {
                    throw new SecurityException(
                            "Phiên đăng nhập không hợp lệ hoặc đã hết hạn"
                    );
                }

                String receivedName = input.readUTF();
                String receivedMimeType = input.readUTF();
                long fileLength = input.readLong();

                FileInformation information =
                        validateFile(
                                receivedName,
                                receivedMimeType,
                                fileLength
                        );

                Files.createDirectories(uploadDirectory);

                String storedName =
                        UUID.randomUUID()
                                + "."
                                + information.extension();

                temporaryFile = uploadDirectory.resolve(
                        storedName + ".part"
                ).normalize();

                destinationFile = uploadDirectory.resolve(
                        storedName
                ).normalize();

                ensureInsideUploadDirectory(temporaryFile);
                ensureInsideUploadDirectory(destinationFile);

                receiveFile(
                        input,
                        temporaryFile,
                        fileLength
                );

                moveCompletedFile(
                        temporaryFile,
                        destinationFile
                );

                temporaryFile = null;

                PostAttachmentDTO attachment =
                        attachmentDAO.createTemporary(
                                uploaderId,
                                information.originalName(),
                                storedName,
                                information.mimeType(),
                                information.attachmentType(),
                                fileLength,
                                destinationFile.toString()
                        );

                sendSuccess(
                        output,
                        attachment
                );

                System.out.println(
                        "[FILE] User "
                                + uploaderId
                                + " đã upload "
                                + information.originalName()
                                + " ("
                                + fileLength
                                + " bytes)"
                );

            } catch (SecurityException exception) {
                deleteQuietly(temporaryFile);
                deleteQuietly(destinationFile);

                sendError(
                        output,
                        exception.getMessage()
                );

            } catch (
                    IllegalArgumentException
                    | IOException
                    | SQLException exception
            ) {
                deleteQuietly(temporaryFile);
                deleteQuietly(destinationFile);

                sendError(
                        output,
                        exception.getMessage() == null
                                ? "Không thể upload tệp"
                                : exception.getMessage()
                );
            }

        } catch (IOException exception) {
            deleteQuietly(temporaryFile);
            deleteQuietly(destinationFile);

            System.err.println(
                    "Lỗi kết nối File Server: "
                            + exception.getMessage()
            );
        }
    }

    private FileInformation validateFile(
            String receivedName,
            String receivedMimeType,
            long fileLength
    ) {
        if (receivedName == null
                || receivedName.isBlank()) {
            throw new IllegalArgumentException(
                    "Tên tệp không hợp lệ"
            );
        }

        if (fileLength <= 0) {
            throw new IllegalArgumentException(
                    "Tệp không có dữ liệu"
            );
        }

        /*
         * getFileName() giúp loại bỏ những phần như:
         * ../../file.pdf
         */
        String originalName;

        try {
            originalName = Paths.get(
                    receivedName
            ).getFileName().toString();
        } catch (InvalidPathException exception) {
            throw new IllegalArgumentException(
                    "Tên tệp không hợp lệ"
            );
        }

        if (originalName.isBlank()
                || originalName.length() > 255) {
            throw new IllegalArgumentException(
                    "Tên tệp không hợp lệ"
            );
        }

        String extension = getExtension(
                originalName
        );

        String attachmentType;
        long maximumSize;

        if (IMAGE_EXTENSIONS.contains(extension)) {
            attachmentType = "IMAGE";
            maximumSize = MAX_IMAGE_SIZE;

        } else if (
                DOCUMENT_EXTENSIONS.contains(extension)
        ) {
            attachmentType = "DOCUMENT";
            maximumSize = MAX_DOCUMENT_SIZE;

        } else {
            throw new IllegalArgumentException(
                    "Định dạng tệp không được hỗ trợ: "
                            + extension
            );
        }

        if (fileLength > maximumSize) {
            long maximumMb =
                    maximumSize / (1024 * 1024);

            throw new IllegalArgumentException(
                    "Tệp không được vượt quá "
                            + maximumMb
                            + " MB"
            );
        }

        String mimeType =
                receivedMimeType == null
                        || receivedMimeType.isBlank()
                        ? "application/octet-stream"
                        : receivedMimeType.trim();

        return new FileInformation(
                originalName,
                extension,
                mimeType,
                attachmentType
        );
    }

    private void receiveFile(
            DataInputStream input,
            Path destination,
            long fileLength
    ) throws IOException {
        try (
                OutputStream fileOutput =
                        new BufferedOutputStream(
                                Files.newOutputStream(
                                        destination,
                                        StandardOpenOption.CREATE_NEW,
                                        StandardOpenOption.WRITE
                                )
                        )
        ) {
            byte[] buffer = new byte[8192];
            long remaining = fileLength;

            while (remaining > 0) {
                int expectedLength =
                        (int) Math.min(
                                buffer.length,
                                remaining
                        );

                int read = input.read(
                        buffer,
                        0,
                        expectedLength
                );

                if (read < 0) {
                    throw new EOFException(
                            "Client đã ngắt kết nối "
                                    + "trước khi upload hoàn tất"
                    );
                }

                fileOutput.write(
                        buffer,
                        0,
                        read
                );

                remaining -= read;
            }

            fileOutput.flush();
        }
    }

    private void moveCompletedFile(
            Path temporaryFile,
            Path destinationFile
    ) throws IOException {
        try {
            Files.move(
                    temporaryFile,
                    destinationFile,
                    StandardCopyOption.ATOMIC_MOVE
            );
        } catch (
                AtomicMoveNotSupportedException exception
        ) {
            Files.move(
                    temporaryFile,
                    destinationFile
            );
        }
    }

    private void ensureInsideUploadDirectory(
            Path file
    ) {
        if (!file.startsWith(uploadDirectory)) {
            throw new SecurityException(
                    "Đường dẫn lưu tệp không hợp lệ"
            );
        }
    }

    private String getExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');

        if (dotIndex < 0
                || dotIndex == fileName.length() - 1) {
            throw new IllegalArgumentException(
                    "Tệp không có phần mở rộng"
            );
        }

        return fileName
                .substring(dotIndex + 1)
                .toLowerCase(Locale.ROOT);
    }

    private void sendSuccess(
            DataOutputStream output,
            PostAttachmentDTO attachment
    ) throws IOException {
        output.writeBoolean(true);
        output.writeUTF("Upload tệp thành công");
        output.writeUTF(
                JsonUtils.toJson(attachment)
        );
        output.flush();
    }

    private void sendError(
            DataOutputStream output,
            String message
    ) {
        try {
            output.writeBoolean(false);
            output.writeUTF(
                    message == null
                            || message.isBlank()
                            ? "Không thể upload tệp"
                            : message
            );
            output.writeUTF("");
            output.flush();

        } catch (IOException exception) {
            System.err.println(
                    "Không thể gửi lỗi upload: "
                            + exception.getMessage()
            );
        }
    }

    private void deleteQuietly(Path file) {
        if (file == null) {
            return;
        }

        try {
            Files.deleteIfExists(file);
        } catch (IOException ignored) {
        }
    }

    private record FileInformation(
            String originalName,
            String extension,
            String mimeType,
            String attachmentType
    ) {
    }
}