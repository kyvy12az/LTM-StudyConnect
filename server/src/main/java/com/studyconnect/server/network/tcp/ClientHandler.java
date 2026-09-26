package com.studyconnect.server.network.tcp;

import com.studyconnect.common.protocol.ActionType;
import com.studyconnect.common.protocol.Request;
import com.studyconnect.common.protocol.Response;
import com.studyconnect.common.protocol.StatusCode;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.util.function.Consumer;

public class ClientHandler implements Runnable {

    private final Socket socket;
    private final RequestRouter requestRouter;
    private final Consumer<String> logConsumer;
    private final Runnable disconnectCallback;

    public ClientHandler(
            Socket socket,
            Consumer<String> logConsumer,
            Runnable disconnectCallback
    ) {
        this.socket = socket;
        this.requestRouter = new RequestRouter();
        this.logConsumer = logConsumer;
        this.disconnectCallback = disconnectCallback;
    }

    @Override
    public void run() {
        try (
                DataInputStream input = new DataInputStream(
                        socket.getInputStream()
                );

                DataOutputStream output = new DataOutputStream(
                        socket.getOutputStream()
                )
        ) {
            while (!socket.isClosed()) {
                try {
                    Request<String> request = readRequest(input);

                    log(
                            "[" + socket.getRemoteSocketAddress() + "] "
                                    + "Action: " + request.getAction()
                                    + " | RequestId: "
                                    + request.getRequestId()
                    );

                    Response<String> response =
                            requestRouter.route(request);

                    writeResponse(output, response);

                    log(
                            "[" + socket.getRemoteSocketAddress() + "] "
                                    + "Response: "
                                    + response.getStatusCode()
                                    + " - "
                                    + response.getMessage()
                    );

                } catch (InvalidActionException e) {
                    log(
                            "Client gửi Action không hợp lệ: "
                                    + e.getActionName()
                    );

                    Response<String> response = Response.error(
                            e.getRequestId(),
                            StatusCode.BAD_REQUEST,
                            "Action không hợp lệ: "
                                    + e.getActionName()
                    );

                    writeResponse(output, response);

                } catch (IllegalArgumentException e) {
                    log(
                            "Request không hợp lệ: "
                                    + e.getMessage()
                    );

                    Response<String> response = Response.error(
                            null,
                            StatusCode.BAD_REQUEST,
                            "Request không hợp lệ: "
                                    + e.getMessage()
                    );

                    writeResponse(output, response);
                }
            }

        } catch (EOFException | SocketException e) {
            log(
                    "Client đã đóng kết nối: "
                            + socket.getRemoteSocketAddress()
            );

        } catch (IOException e) {
            if (!socket.isClosed()) {
                log(
                        "Lỗi giao tiếp với client "
                                + socket.getRemoteSocketAddress()
                                + ": "
                                + e.getMessage()
                );
            }

        } finally {
            closeSocket();

            if (disconnectCallback != null) {
                disconnectCallback.run();
            }
        }
    }

    private Request<String> readRequest(
            DataInputStream input
    ) throws IOException {
        String requestId = input.readUTF();
        String actionName = input.readUTF();

        boolean hasToken = input.readBoolean();

        String token = null;

        if (hasToken) {
            token = input.readUTF();
        }

        String data = input.readUTF();
        long timestamp = input.readLong();

        ActionType action = ActionType.valueOf(actionName);

        Request<String> request = new Request<>(
                action,
                token,
                data
        );

        request.setRequestId(requestId);
        request.setTimestamp(timestamp);

        return request;
    }

    private void writeResponse(
            DataOutputStream output,
            Response<String> response
    ) throws IOException {
        output.writeUTF(
                response.getRequestId() == null
                        ? ""
                        : response.getRequestId()
        );

        StatusCode statusCode = response.getStatusCode();

        output.writeUTF(
                statusCode == null
                        ? StatusCode.SERVER_ERROR.name()
                        : statusCode.name()
        );

        output.writeUTF(
                response.getMessage() == null
                        ? ""
                        : response.getMessage()
        );

        output.writeUTF(
                response.getData() == null
                        ? ""
                        : response.getData()
        );

        output.writeLong(response.getTimestamp());
        output.flush();
    }

    private void closeSocket() {
        if (socket.isClosed()) {
            return;
        }

        try {
            socket.close();
        } catch (IOException e) {
            log("Không thể đóng socket: " + e.getMessage());
        }
    }

    private void log(String message) {
        if (logConsumer != null) {
            logConsumer.accept(message);
        } else {
            System.out.println(message);
        }
    }

    private static class InvalidActionException
            extends IllegalArgumentException {

        private final String requestId;
        private final String actionName;

        public InvalidActionException(
                String requestId,
                String actionName
        ) {
            super("Action không hợp lệ: " + actionName);

            this.requestId = requestId;
            this.actionName = actionName;
        }

        public String getRequestId() {
            return requestId;
        }

        public String getActionName() {
            return actionName;
        }
    }
}