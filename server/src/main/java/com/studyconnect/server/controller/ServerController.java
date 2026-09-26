package com.studyconnect.server.controller;

import com.studyconnect.server.event.ServerEventListener;
import com.studyconnect.server.network.tcp.TCPServer;
import com.studyconnect.server.view.ServerFrame;

import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class ServerController implements ServerEventListener {
    private final ServerFrame view;

    private TCPServer server;
    private Thread serverThread;

    public ServerController(ServerFrame view) {
        this.view = view;
        initializeEvents();
    }

    private void initializeEvents() {
        view.addStartListener(event -> startServer());
        view.addStopListener(event -> stopServer());
        view.addClearLogListener(event -> view.clearLog());

        view.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                stopServer();
                view.dispose();
            }
        });
    }

    private void startServer() {
        if (server != null && server.isRunning()) {
            view.appendLog("Server đang chạy");
            return;
        }

        int port;
        try {
            port = Integer.parseInt(view.getPortText());
            if (port < 1 || port > 65535) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(view, "Cổng phải là số từ 1 đến 65535.", "Cổng không hợp lệ", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // tạo server mới và chạy trong luồng riêng
        server = new TCPServer(port, this);
        serverThread = new Thread(server::run, "studyconnect-tcp-server");
        serverThread.setDaemon(true); // đặt luồng server là daemon để nó không ngăn ứng dụng kết thúc
        view.setServerRunning(true); // cập nhật giao diện để hiển thị server đang chạy
        view.setClientCount(0);
        view.appendLog("Đang khởi động server tại cổng " + port + "...");

        serverThread.start();
    }

    private void stopServer() {
        if (server != null) {
            server.stop();
            server = null;
        }

        serverThread = null;

        view.setServerRunning(false);
        view.setClientCount(0);
    }

    @Override
    public void onLog(String message) {
        SwingUtilities.invokeLater(
                () -> view.appendLog(message)
        );
    }

    @Override
    public void onStatusChanged(boolean running) {
        SwingUtilities.invokeLater(
                () -> view.setServerRunning(running)
        );
    }

    @Override
    public void onClientCountChanged(int clientCount) {
        SwingUtilities.invokeLater(
                () -> view.setClientCount(clientCount)
        );
    }
}
