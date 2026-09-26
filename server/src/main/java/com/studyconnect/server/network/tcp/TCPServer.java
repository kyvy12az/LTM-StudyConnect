package com.studyconnect.server.network.tcp;

import com.studyconnect.server.event.ServerEventListener;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;


public class TCPServer {
    private final int port;
    private final ServerEventListener eventListener;
    private final ExecutorService clientPool;
    private final Set<Socket> connectedClients;

    private final AtomicBoolean stopped = new AtomicBoolean(false);

    private volatile ServerSocket serverSocket;
    private volatile boolean running;

    public TCPServer(int port, ServerEventListener eventListener) {
        this.port = port;
        this.eventListener = eventListener;
        this.clientPool = Executors.newCachedThreadPool();
        this.connectedClients = ConcurrentHashMap.newKeySet();
    }

    // contructor dùng khi muốn chạy server bằng console
    public TCPServer(int port) {
        this(port, null);
    }

    // khởi động server và chờ client kết nối
    public void run() {
        if (running) {
            log("Server đã chạy.");
            return;
        }

        if (stopped.get()) {
            log("Server này đã được dừng. " + "Hãy tạo đối tượng TCPServer mới.");
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
                    Socket clientSocket = serverSocket.accept();

//                    System.out.println("Client kết nối: " + clientSocket.getRemoteSocketAddress());

                    if (!running) {
                        closeSocket(clientSocket);
                        break;
                    }

                    connectedClients.add(clientSocket);

                    log("Client kết nối: " + clientSocket.getRemoteSocketAddress());
                    notifyClientCountChanged();

                    ClientHandler handler = new ClientHandler(
                        clientSocket, this::log,
                        () -> clientDisconnected(
                                clientSocket
                        )
                    );
                    clientPool.submit(handler);
                } catch (SocketException e) {
                    // socket bị đóng khi stop() được gọi
                    if (running) {
                        log("Lỗi khi chấp nhận client: " + e.getMessage());
                    }
                    break;
                } catch (RejectedExecutionException e) {
                    log("Không thể tạo luồng xử lý client: " + e.getMessage());
                }
            }
        } catch (IOException e) {
           log("Không thể khởi động server tại cổng " + port + ": " + e.getMessage());
        } finally {
            shutdown();
        }
    }

    public void stop() {
        shutdown();
    }

    private synchronized void shutdown() {
        if (!stopped.compareAndSet(false, true)) {
            return; // đã dừng rồi
        }

        running = false;

        closeServerSocket();
        closeAllClients();

        clientPool.shutdownNow();

        notifyClientCountChanged();
        notifyStatusChanged(false);

        log("TCP Server đã dừng.");
    }

    private void clientDisconnected(Socket clientSocket) {
        boolean removed = connectedClients.remove(clientSocket);
        closeSocket(clientSocket);
        if (removed) {
            log("Client đã đóng kết nối: " + clientSocket.getRemoteSocketAddress());
            notifyClientCountChanged();
        }
    }

    private void closeServerSocket() {
        if (serverSocket == null || serverSocket.isClosed()) {
            return;
        }

        try {
            serverSocket.close();
        } catch (IOException e) {
            log("Lỗi khi đóng ServerSocket: " + e.getMessage());
        }
    }

    private void closeAllClients() {
        for (Socket clientSocket : connectedClients) {
            closeSocket(clientSocket);
        }

        connectedClients.clear();
    }

    private void closeSocket(Socket socket) {
        if (socket == null || socket.isClosed()) return;

        try {
            socket.close();
        } catch (IOException ignored) {
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
                    connectedClients.size()
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
        return connectedClients.size();
    }
}
