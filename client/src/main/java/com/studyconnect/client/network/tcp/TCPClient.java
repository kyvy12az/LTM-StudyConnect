package com.studyconnect.client.network.tcp;

import com.studyconnect.common.protocol.ActionType;
import com.studyconnect.common.protocol.Request;
import com.studyconnect.common.protocol.Response;
import com.studyconnect.common.protocol.StatusCode;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class TCPClient implements AutoCloseable {

    private final String serverHost;
    private final int serverPort;

    private Socket socket;
    private DataInputStream input;
    private DataOutputStream output;

    public TCPClient(String serverHost, int serverPort) {
        this.serverHost = serverHost;
        this.serverPort = serverPort;
    }

    public void connect() throws IOException {
        if (isConnected()) {
            System.out.println("Client đã kết nối.");
            return;
        }

        socket = new Socket(serverHost, serverPort);

        output = new DataOutputStream(
                socket.getOutputStream()
        );

        input = new DataInputStream(
                socket.getInputStream()
        );

        System.out.println("Đã kết nối đến server " + serverHost + ":" + serverPort
        );
    }

    public synchronized Response<String> sendRequest(Request<String> request) throws IOException {
        if (!isConnected()) {
            throw new IllegalStateException("Client chưa kết nối đến server");
        }

        writeRequest(request);

        return readResponse();
    }


     // gửi request theo thứ tự mà server sẽ đọc
    private void writeRequest(Request<String> request) throws IOException {
        output.writeUTF(request.getRequestId());
        output.writeUTF(request.getAction().name());

        boolean hasToken = request.getToken() != null;
        output.writeBoolean(hasToken);

        if (hasToken) {
            output.writeUTF(request.getToken());
        }

        output.writeUTF(request.getData() == null ? "" : request.getData());

        output.writeLong(request.getTimestamp());
        output.flush();
    }

    // đọc response theo thứ tự server đã gửi
    private Response<String> readResponse() throws IOException {
        String requestId = input.readUTF();
        String statusName = input.readUTF();
        String message = input.readUTF();
        String data = input.readUTF();
        long timestamp = input.readLong();

        StatusCode statusCode = StatusCode.valueOf(statusName);

        Response<String> response = new Response<>(requestId, statusCode, message, data);

        response.setTimestamp(timestamp);

        return response;
    }

    public boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }

    @Override
    public void close() {
        closeInput();
        closeOutput();
        closeSocket();

        System.out.println("Client đã đóng kết nối.");
    }

    private void closeInput() {
        if (input != null) {
            try {
                input.close();
            } catch (IOException ignored) {
            }
        }
    }

    private void closeOutput() {
        if (output != null) {
            try {
                output.close();
            } catch (IOException ignored) {
            }
        }
    }

    private void closeSocket() {
        if (socket != null && !socket.isClosed()) {
            try {
                socket.close();
            } catch (IOException ignored) {
            }
        }
    }
}