package com.studyconnect.server.network.tcp;

import com.studyconnect.common.protocol.ActionType;
import com.studyconnect.common.protocol.Request;
import com.studyconnect.common.protocol.Response;
import com.studyconnect.common.protocol.ServerEvent;
import com.studyconnect.common.protocol.ServerFrameType;
import com.studyconnect.common.protocol.StatusCode;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.util.Objects;
import java.util.function.Consumer;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private final RequestRouter requestRouter;
    private final ClientConnectionManager connectionManager;
    private final Consumer<String> logConsumer;
    private final Runnable disconnectCallback;
    private final Object writeLock = new Object();
    private final long connectedAt = System.currentTimeMillis();

    private volatile DataInputStream input;
    private volatile DataOutputStream output;

    public ClientHandler(
            Socket socket,
            RequestRouter requestRouter,
            ClientConnectionManager connectionManager,
            Consumer<String> logConsumer,
            Runnable disconnectCallback
    ) {
        this.socket = Objects.requireNonNull(socket, "socket");
        this.requestRouter = Objects.requireNonNull(
                requestRouter,
                "requestRouter"
        );
        this.connectionManager = Objects.requireNonNull(
                connectionManager,
                "connectionManager"
        );
        this.logConsumer = logConsumer;
        this.disconnectCallback = disconnectCallback;
    }

    @Override
    public void run() {
        try {
            input = new DataInputStream(
                    socket.getInputStream()
            );
            output = new DataOutputStream(
                    socket.getOutputStream()
            );

            while (isOpen()) {
                try {
                    Request<String> request = readRequest();

                    log(
                            "[" + getRemoteAddress() + "] "
                                    + "Action: " + request.getAction()
                                    + " | RequestId: "
                                    + request.getRequestId()
                    );

                    Response<String> response =
                            requestRouter.route(request, this);

                    sendResponse(response);

                    log(
                            "[" + getRemoteAddress() + "] "
                                    + "Response: "
                                    + response.getStatusCode()
                                    + " - "
                                    + response.getMessage()
                    );
                } catch (InvalidActionException exception) {
                    log(
                            "Client gửi Action không hợp lệ: "
                                    + exception.getActionName()
                    );
                    sendResponse(Response.error(
                            exception.getRequestId(),
                            StatusCode.BAD_REQUEST,
                            "Action không hợp lệ: "
                                    + exception.getActionName()
                    ));
                } catch (IllegalArgumentException exception) {
                    log(
                            "Request không hợp lệ: "
                                    + exception.getMessage()
                    );
                    sendResponse(Response.error(
                            null,
                            StatusCode.BAD_REQUEST,
                            "Request không hợp lệ: "
                                    + exception.getMessage()
                    ));
                }
            }
        } catch (EOFException | SocketException exception) {
            log(
                    "Client đã đóng kết nối: "
                            + getRemoteAddress()
            );
        } catch (IOException exception) {
            if (isOpen()) {
                log(
                        "Lỗi giao tiếp với client "
                                + getRemoteAddress()
                                + ": "
                                + exception.getMessage()
                );
            }
        } finally {
            connectionManager.unregister(this);
            close();
            if (disconnectCallback != null) {
                disconnectCallback.run();
            }
        }
    }

    private Request<String> readRequest()
            throws IOException {
        DataInputStream currentInput = input;
        if (currentInput == null) {
            throw new EOFException("Input stream đã đóng");
        }

        String requestId = currentInput.readUTF();
        String actionName = currentInput.readUTF();
        boolean hasToken = currentInput.readBoolean();
        String token = hasToken
                ? currentInput.readUTF()
                : null;
        String data = currentInput.readUTF();
        long timestamp = currentInput.readLong();

        ActionType action;
        try {
            action = ActionType.valueOf(actionName);
        } catch (IllegalArgumentException exception) {
            throw new InvalidActionException(
                    requestId,
                    actionName
            );
        }

        Request<String> request = new Request<>(
                action,
                token,
                data
        );
        request.setRequestId(requestId);
        request.setTimestamp(timestamp);
        return request;
    }

    public void sendResponse(
            Response<String> response
    ) throws IOException {
        Objects.requireNonNull(response, "response");
        synchronized (writeLock) {
            DataOutputStream currentOutput = requireOutput();
            currentOutput.writeUTF(
                    ServerFrameType.RESPONSE.name()
            );
            currentOutput.writeUTF(
                    response.getRequestId() == null
                            ? ""
                            : response.getRequestId()
            );

            StatusCode statusCode = response.getStatusCode();
            currentOutput.writeUTF(
                    statusCode == null
                            ? StatusCode.SERVER_ERROR.name()
                            : statusCode.name()
            );
            currentOutput.writeUTF(
                    response.getMessage() == null
                            ? ""
                            : response.getMessage()
            );
            currentOutput.writeUTF(
                    response.getData() == null
                            ? ""
                            : response.getData()
            );
            currentOutput.writeLong(response.getTimestamp());
            currentOutput.flush();
        }
    }

    public void sendEvent(
            ServerEvent<String> event
    ) throws IOException {
        Objects.requireNonNull(event, "event");
        Objects.requireNonNull(event.getType(), "event.type");

        synchronized (writeLock) {
            DataOutputStream currentOutput = requireOutput();
            currentOutput.writeUTF(
                    ServerFrameType.EVENT.name()
            );
            currentOutput.writeUTF(event.getType().name());
            currentOutput.writeUTF(
                    event.getData() == null
                            ? ""
                            : event.getData()
            );
            currentOutput.writeLong(event.getTimestamp());
            currentOutput.flush();
        }
    }

    private DataOutputStream requireOutput()
            throws IOException {
        if (!isOpen() || output == null) {
            throw new IOException(
                    "Kết nối client đã đóng"
            );
        }
        return output;
    }

    public boolean isOpen() {
        return !socket.isClosed()
                && socket.isConnected();
    }

    public String getRemoteAddress() {
        return String.valueOf(
                socket.getRemoteSocketAddress()
        );
    }

    public String getRemoteHost() {
        return socket.getInetAddress() == null
                ? "--"
                : socket.getInetAddress().getHostAddress();
    }

    public int getRemotePort() {
        return socket.getPort();
    }

    public long getConnectedAt() {
        return connectedAt;
    }

    public void close() {
        if (socket.isClosed()) {
            return;
        }
        try {
            socket.close();
        } catch (IOException exception) {
            log(
                    "Không thể đóng socket: "
                            + exception.getMessage()
            );
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

        InvalidActionException(
                String requestId,
                String actionName
        ) {
            super("Action không hợp lệ: " + actionName);
            this.requestId = requestId;
            this.actionName = actionName;
        }

        String getRequestId() {
            return requestId;
        }

        String getActionName() {
            return actionName;
        }
    }
}
