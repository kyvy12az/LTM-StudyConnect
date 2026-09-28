package com.studyconnect.client.network.file;

import com.studyconnect.common.dto.PostAttachmentDTO;
import com.studyconnect.common.util.JsonUtils;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;

public class FileTransferClient {
    private final String host;
    private final int port;

    public FileTransferClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public PostAttachmentDTO upload(File file, String authToken) throws IOException {
        if (file == null || !file.isFile()) {
            throw new IllegalArgumentException("Tệp được chọn không hợp lệ");
        }

        if (authToken == null || authToken.isBlank()) {
            throw new IllegalArgumentException("Bạn chưa đăng nhập");
        }

        String mimeType = Files.probeContentType(file.toPath());
        if (mimeType == null) {
            mimeType = "application/octet-stream";
        }

        try (
                Socket socket = new Socket(host, port);
                DataOutputStream output = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
                DataInputStream input = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
                InputStream fileInput = new BufferedInputStream(Files.newInputStream(file.toPath()));
        ) {
            output.writeUTF("UPLOAD_POST_ATTACHMENT");
            output.writeUTF(authToken);
            output.writeUTF(file.getName());
            output.writeUTF(mimeType);
            output.writeLong(file.length());

            byte[] buffer = new byte[8192];
            long remaining = file.length();

            while (remaining > 0) {
                int read = fileInput.read(
                        buffer, 0,
                        (int) Math.min(buffer.length, remaining)
                );
                if (read < 0) throw new EOFException("Tệp kết thúc bất thường");

                output.write(buffer, 0, read);
                remaining -= read;
            }

            output.flush();

            boolean success = input.readBoolean();
            String message = input.readUTF();
            String responseJson = input.readUTF();

            if (!success) throw new IOException(message);

            return JsonUtils.fromJson(responseJson, PostAttachmentDTO.class);
        }
    }

    public byte[] download(
            long attachmentId,
            String authToken
    ) throws IOException {
        if (attachmentId <= 0) {
            throw new IllegalArgumentException(
                    "ID tệp đính kèm không hợp lệ"
            );
        }

        if (authToken == null || authToken.isBlank()) {
            throw new IllegalArgumentException(
                    "Bạn chưa đăng nhập"
            );
        }

        try (
                Socket socket = new Socket(host, port);

                DataOutputStream output =
                        new DataOutputStream(
                                new BufferedOutputStream(
                                        socket.getOutputStream()
                                )
                        );

                DataInputStream input =
                        new DataInputStream(
                                new BufferedInputStream(
                                        socket.getInputStream()
                                )
                        )
        ) {
            socket.setSoTimeout(30_000);

            output.writeUTF(
                    "DOWNLOAD_POST_ATTACHMENT"
            );

            output.writeUTF(authToken);
            output.writeLong(attachmentId);
            output.flush();

            boolean success = input.readBoolean();
            String message = input.readUTF();

            if (!success) {
                throw new IOException(message);
            }

            long fileSize = input.readLong();

            long maximumSize = 50L * 1024 * 1024;

            if (fileSize <= 0 || fileSize > maximumSize) {
                throw new IOException(
                        "Kích thước tệp tải xuống không hợp lệ: "
                                + fileSize
                );
            }

            ByteArrayOutputStream fileOutput =
                    new ByteArrayOutputStream(
                            (int) fileSize
                    );

            byte[] buffer = new byte[8192];
            long remaining = fileSize;

            while (remaining > 0) {
                int read = input.read(
                        buffer,
                        0,
                        (int) Math.min(
                                buffer.length,
                                remaining
                        )
                );

                if (read < 0) {
                    throw new EOFException(
                            "Kết nối kết thúc khi chưa tải đủ tệp"
                    );
                }

                fileOutput.write(buffer, 0, read);
                remaining -= read;
            }

            return fileOutput.toByteArray();
        }
    }
}
