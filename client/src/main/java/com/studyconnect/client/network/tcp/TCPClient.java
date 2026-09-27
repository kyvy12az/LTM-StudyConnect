package com.studyconnect.client.network.tcp;

import com.studyconnect.common.protocol.ActionType;
import com.studyconnect.common.protocol.Request;
import com.studyconnect.common.protocol.Response;
import com.studyconnect.common.protocol.ServerEvent;
import com.studyconnect.common.protocol.ServerEventType;
import com.studyconnect.common.protocol.ServerFrameType;
import com.studyconnect.common.protocol.StatusCode;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

public class TCPClient implements AutoCloseable {
    private static final long REQUEST_TIMEOUT_SECONDS = 15;

    private final String serverHost;
    private final int serverPort;
    private final Object lifecycleLock = new Object();
    private final Object writeLock = new Object();
    private final ConcurrentMap<
            String,
            CompletableFuture<Response<String>>
            > pendingRequests = new ConcurrentHashMap<>();
    private final ConcurrentMap<
            ServerEventType,
            CopyOnWriteArrayList<Consumer<ServerEvent<String>>>
            > eventListeners = new ConcurrentHashMap<>();
    private final ExecutorService eventDispatcher =
            Executors.newSingleThreadExecutor(runnable -> {
                Thread thread = new Thread(
                        runnable,
                        "studyconnect-event-dispatcher"
                );
                thread.setDaemon(true);
                return thread;
            });

    private volatile Socket socket;
    private volatile DataInputStream input;
    private volatile DataOutputStream output;
    private volatile Thread readerThread;
    private volatile boolean running;

    public TCPClient(String serverHost, int serverPort) {
        this.serverHost = serverHost;
        this.serverPort = serverPort;
    }

    public void connect() throws IOException {
        synchronized (lifecycleLock) {
            if (isConnected()) {
                System.out.println("Client đã kết nối.");
                return;
            }
            if (eventDispatcher.isShutdown()) {
                throw new IllegalStateException(
                        "TCPClient đã đóng và không thể kết nối lại"
                );
            }

            Socket newSocket = new Socket(serverHost, serverPort);
            try {
                socket = newSocket;
                output = new DataOutputStream(
                        newSocket.getOutputStream()
                );
                input = new DataInputStream(
                        newSocket.getInputStream()
                );
                running = true;

                readerThread = new Thread(
                        this::readerLoop,
                        "studyconnect-tcp-reader"
                );
                readerThread.setDaemon(true);
                readerThread.start();
            } catch (IOException exception) {
                closeQuietly(newSocket);
                socket = null;
                input = null;
                output = null;
                throw exception;
            }
        }

        System.out.println(
                "Đã kết nối đến server "
                        + serverHost
                        + ":"
                        + serverPort
        );
    }

    public Response<String> sendRequest(
            Request<String> request
    ) throws IOException {
        Objects.requireNonNull(request, "request");
        if (request.getAction() == null) {
            throw new IllegalArgumentException(
                    "Action không được null"
            );
        }
        if (request.getRequestId() == null
                || request.getRequestId().isBlank()) {
            throw new IllegalArgumentException(
                    "RequestId không được để trống"
            );
        }
        if (!isConnected()) {
            throw new IllegalStateException(
                    "Client chưa kết nối đến server"
            );
        }

        String requestId = request.getRequestId();
        CompletableFuture<Response<String>> future =
                new CompletableFuture<>();

        if (pendingRequests.putIfAbsent(
                requestId,
                future
        ) != null) {
            throw new IllegalStateException(
                    "RequestId đang chờ xử lý: " + requestId
            );
        }

        try {
            writeRequest(request);
            return future.get(
                    REQUEST_TIMEOUT_SECONDS,
                    TimeUnit.SECONDS
            );
        } catch (TimeoutException exception) {
            throw new SocketTimeoutException(
                    "Hết thời gian chờ phản hồi cho request "
                            + requestId
            );
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IOException(
                    "Đã bị gián đoạn khi chờ phản hồi",
                    exception
            );
        } catch (ExecutionException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof IOException ioException) {
                throw ioException;
            }
            throw new IOException(
                    "Không thể nhận phản hồi từ server",
                    cause
            );
        } finally {
            pendingRequests.remove(requestId, future);
        }
    }

    public void addServerEventListener(
            ServerEventType type,
            Consumer<ServerEvent<String>> listener
    ) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(listener, "listener");
        eventListeners.computeIfAbsent(
                type,
                ignored -> new CopyOnWriteArrayList<>()
        ).addIfAbsent(listener);
    }

    public void removeServerEventListener(
            ServerEventType type,
            Consumer<ServerEvent<String>> listener
    ) {
        if (type == null || listener == null) {
            return;
        }
        CopyOnWriteArrayList<Consumer<ServerEvent<String>>> listeners =
                eventListeners.get(type);
        if (listeners == null) {
            return;
        }
        listeners.remove(listener);
        if (listeners.isEmpty()) {
            eventListeners.remove(type, listeners);
        }
    }

    private void writeRequest(Request<String> request)
            throws IOException {
        synchronized (writeLock) {
            if (!isConnected() || output == null) {
                throw new IOException(
                        "Kết nối đến server đã đóng"
                );
            }

            output.writeUTF(request.getRequestId());
            output.writeUTF(request.getAction().name());

            boolean hasToken = request.getToken() != null;
            output.writeBoolean(hasToken);
            if (hasToken) {
                output.writeUTF(request.getToken());
            }

            output.writeUTF(
                    request.getData() == null
                            ? ""
                            : request.getData()
            );
            output.writeLong(request.getTimestamp());
            output.flush();
        }
    }

    private void readerLoop() {
        Throwable failure = null;
        try {
            while (running) {
                readServerFrame();
            }
        } catch (EOFException | SocketException exception) {
            failure = exception;
            if (running) {
                System.err.println(
                        "Kết nối TCP đã đóng: "
                                + exception.getMessage()
                );
            }
        } catch (IOException | RuntimeException exception) {
            failure = exception;
            if (running) {
                System.err.println(
                        "Lỗi reader TCP: "
                                + exception.getMessage()
                );
            }
        } finally {
            handleReaderStopped(failure);
        }
    }

    private void readServerFrame() throws IOException {
        DataInputStream currentInput = input;
        if (currentInput == null) {
            throw new EOFException("Input stream đã đóng");
        }

        String frameName = currentInput.readUTF();
        ServerFrameType frameType;
        try {
            frameType = ServerFrameType.valueOf(frameName);
        } catch (IllegalArgumentException exception) {
            throw new IOException(
                    "Loại frame server không hợp lệ: "
                            + frameName,
                    exception
            );
        }

        switch (frameType) {
            case RESPONSE -> readResponseFrame(currentInput);
            case EVENT -> readEventFrame(currentInput);
        }
    }

    private void readResponseFrame(
            DataInputStream currentInput
    ) throws IOException {
        String requestId = currentInput.readUTF();
        String statusName = currentInput.readUTF();
        String message = currentInput.readUTF();
        String data = currentInput.readUTF();
        long timestamp = currentInput.readLong();

        StatusCode statusCode;
        try {
            statusCode = StatusCode.valueOf(statusName);
        } catch (IllegalArgumentException exception) {
            throw new IOException(
                    "StatusCode không hợp lệ: " + statusName,
                    exception
            );
        }

        Response<String> response = new Response<>(
                requestId,
                statusCode,
                message,
                data
        );
        response.setTimestamp(timestamp);

        CompletableFuture<Response<String>> future =
                pendingRequests.remove(requestId);
        if (future != null) {
            future.complete(response);
        } else {
            System.err.println(
                    "Không tìm thấy request đang chờ: "
                            + requestId
            );
        }
    }

    private void readEventFrame(
            DataInputStream currentInput
    ) throws IOException {
        String eventName = currentInput.readUTF();
        String data = currentInput.readUTF();
        long timestamp = currentInput.readLong();

        ServerEventType eventType;
        try {
            eventType = ServerEventType.valueOf(eventName);
        } catch (IllegalArgumentException exception) {
            throw new IOException(
                    "Loại event không hợp lệ: " + eventName,
                    exception
            );
        }

        ServerEvent<String> event =
                new ServerEvent<>(eventType, data);
        event.setTimestamp(timestamp);
        dispatchEvent(event);
    }

    private void dispatchEvent(ServerEvent<String> event) {
        CopyOnWriteArrayList<Consumer<ServerEvent<String>>> listeners =
                eventListeners.get(event.getType());
        if (listeners == null || listeners.isEmpty()) {
            return;
        }

        try {
            eventDispatcher.execute(() -> {
                for (Consumer<ServerEvent<String>> listener : listeners) {
                    try {
                        listener.accept(event);
                    } catch (RuntimeException exception) {
                        System.err.println(
                                "Listener server event bị lỗi: "
                                        + exception.getMessage()
                        );
                    }
                }
            });
        } catch (RejectedExecutionException exception) {
            if (running) {
                System.err.println(
                        "Không thể dispatch server event: "
                                + exception.getMessage()
                );
            }
        }
    }

    private void handleReaderStopped(Throwable failure) {
        running = false;
        closeTransport();

        IOException exception = new IOException(
                "Kết nối đến server đã đóng",
                failure
        );
        failPendingRequests(exception);
    }

    private void failPendingRequests(Throwable failure) {
        for (CompletableFuture<Response<String>> future
                : pendingRequests.values()) {
            future.completeExceptionally(failure);
        }
        pendingRequests.clear();
    }

    public boolean isConnected() {
        Socket currentSocket = socket;
        return running
                && currentSocket != null
                && currentSocket.isConnected()
                && !currentSocket.isClosed();
    }

    @Override
    public void close() {
        running = false;
        closeTransport();
        failPendingRequests(
                new IOException("TCPClient đã đóng")
        );
        eventListeners.clear();
        eventDispatcher.shutdownNow();

        Thread currentReader = readerThread;
        if (currentReader != null
                && currentReader != Thread.currentThread()) {
            currentReader.interrupt();
        }

        System.out.println("Client đã đóng kết nối.");
    }

    private void closeTransport() {
        synchronized (lifecycleLock) {
            closeQuietly(socket);
            closeQuietly(input);
            closeQuietly(output);
            socket = null;
            input = null;
            output = null;
        }
    }

    private void closeQuietly(AutoCloseable resource) {
        if (resource == null) {
            return;
        }
        try {
            resource.close();
        } catch (Exception exception) {
            System.err.println(
                    "Không thể đóng tài nguyên TCP: "
                            + exception.getMessage()
            );
        }
    }
}
