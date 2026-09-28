package com.studyconnect.server.network.file;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FileTransferServer {
    private final int port;
    private final ExecutorService pool = Executors.newCachedThreadPool();
    private volatile boolean running;
    private ServerSocket serverSocket;

    public FileTransferServer(int port) {
        this.port = port;
    }

    public void run() {
        if (running) return;

        try {
            serverSocket = new ServerSocket(port);
            running = true;

            System.out.println("File Server đang chạy tại cổng " + port);

            while (running) {
                Socket socket = serverSocket.accept();
                pool.submit(new FileUploadHandler(socket));
            }
        } catch (SocketException e) {
            if (running) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            stop();
        }
    }

    public void stop() {
        running = false;
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (IOException igrored) {
            }
        }
        pool.shutdownNow();
    }
}
