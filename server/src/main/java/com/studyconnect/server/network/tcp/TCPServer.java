package com.studyconnect.server.network.tcp;

import com.studyconnect.server.event.ServerEventListener;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

public class TCPServer {
    private final int port;
    private final ServerEventListener eventListener;
    private final ExecutorService clientPool;
    private final ClientConnectionManager connectionManager;
    private final RequestRouter requestRouter;
    private final AtomicBoolean stopped =
            new AtomicBoolean(false);

    private volatile ServerSocket serverSocket;
    private volatile boolean running;

    public TCPServer(
            int port,
            ServerEventListener eventListener
    ) {
        this.port = port;
        this.eventListener = eventListener;
        this.clientPool = Executors.newCachedThreadPool();
        this.connectionManager =
                new ClientConnectionManager();
        this.requestRouter =
                new RequestRouter(connectionManager);
    }

    public TCPServer(int port) {
        this(port, null);
    }

    public void run() {
        if (running) {
            log("Server đã chạy.");
            return;
        }
        if (stopped.get()) {
            log(
                    "Server này đã được dừng. "
                            + "Hãy tạo đối tượng TCPServer mới."
            );
            return;
        }

        try {
            serverSocket = new ServerSocket(port);
            running = true;
            log("================================");
            log("Server đang chạy tại cổng " + port);
            log("Server đang chờ client...");
            log("================================");
            notifyStatusChanged(true);

            while (running) {
                try {
                    Socket clientSocket =
                            serverSocket.accept();
                    if (!running) {
                        closeSocket(clientSocket);
                        break;
                    }

                    ClientHandler handler = new ClientHandler(
                            clientSocket,
                            requestRouter,
                            connectionManager,
                            this::log,
                            this::notifyClientCountChanged
                    );
                    connectionManager.register(handler);

                    log(
                            "Client kết nối: "
                                    + handler.getRemoteAddress()
                    );
                    notifyClientCountChanged();

                    try {
                        clientPool.submit(handler);
                    } catch (RejectedExecutionException exception) {
                        connectionManager.unregister(handler);
                        handler.close();
                        log(
                                "Không thể tạo luồng xử lý client: "
                                        + exception.getMessage()
                        );
                    }
                } catch (SocketException exception) {
                    if (running) {
                        log(
                                "Lỗi khi chấp nhận client: "
                                        + exception.getMessage()
                        );
                    }
                    break;
                }
            }
        } catch (IOException exception) {
            log(
                    "Không thể khởi động server tại cổng "
                            + port
                            + ": "
                            + exception.getMessage()
            );
        } finally {
            shutdown();
        }
    }

    public void stop() {
        shutdown();
    }

    private synchronized void shutdown() {
        if (!stopped.compareAndSet(false, true)) {
            return;
        }

        running = false;
        closeServerSocket();
        connectionManager.closeAll();
        clientPool.shutdownNow();
        notifyClientCountChanged();
        notifyStatusChanged(false);
        log("TCP Server đã dừng.");
    }

    private void closeServerSocket() {
        ServerSocket currentServerSocket = serverSocket;
        if (currentServerSocket == null
                || currentServerSocket.isClosed()) {
            return;
        }
        try {
            currentServerSocket.close();
        } catch (IOException exception) {
            log(
                    "Lỗi khi đóng ServerSocket: "
                            + exception.getMessage()
            );
        }
    }

    private void closeSocket(Socket socket) {
        if (socket == null || socket.isClosed()) {
            return;
        }
        try {
            socket.close();
        } catch (IOException exception) {
            log(
                    "Lỗi khi đóng socket: "
                            + exception.getMessage()
            );
        }
    }

    private void log(String message) {
        if (eventListener != null) {
            eventListener.onLog(message);
        } else {
            System.out.println(message);
        }
    }

    private void notifyStatusChanged(boolean status) {
        if (eventListener != null) {
            eventListener.onStatusChanged(status);
        }
    }

    private void notifyClientCountChanged() {
        if (eventListener != null) {
            eventListener.onClientCountChanged(
                    connectionManager.getConnectionCount()
            );
        }
    }

    public boolean isRunning() {
        return running;
    }

    public int getPort() {
        return port;
    }

    public int getClientCount() {
        return connectionManager.getConnectionCount();
    }
}
