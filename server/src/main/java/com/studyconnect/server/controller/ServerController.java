package com.studyconnect.server.controller;

import com.studyconnect.common.dto.PostDTO;
import com.studyconnect.server.event.ServerEventListener;
import com.studyconnect.server.model.dao.PostDAO;
import com.studyconnect.server.model.dao.UserDAO;
import com.studyconnect.server.model.dto.AdminUserDTO;
import com.studyconnect.server.model.dto.ConnectedClientDTO;
import com.studyconnect.server.model.dto.DashboardSnapshot;
import com.studyconnect.server.network.file.FileTransferServer;
import com.studyconnect.server.network.session.SessionManager;
import com.studyconnect.server.network.tcp.ClientConnectionManager;
import com.studyconnect.server.network.tcp.TCPServer;
import com.studyconnect.server.view.ServerFrame;

import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

public class ServerController implements ServerEventListener {
    private static final int DEFAULT_PORT = 2006;

    private final ServerFrame view;
    private final UserDAO userDAO = new UserDAO();
    private final PostDAO postDAO = new PostDAO();
    private final SessionManager sessionManager = SessionManager.getInstance();
    private final AtomicBoolean dashboardLoading = new AtomicBoolean();
    private final AtomicBoolean usersLoading = new AtomicBoolean();
    private final AtomicBoolean postsLoading = new AtomicBoolean();
    private final Timer refreshTimer;

    private volatile TCPServer server;
    private Thread serverThread;
    private FileTransferServer fileTransferServer;
    private Thread fileTransferThread;

    public ServerController(ServerFrame view) {
        if (view == null) throw new IllegalArgumentException("ServerFrame không được null");
        this.view = view;
        initializeEvents();
        refreshTimer = new Timer(5_000, event -> refreshDashboard());
        refreshTimer.setCoalesce(true);
        refreshTimer.start();
        refreshDashboard();
        refreshUsers();
        refreshPosts();
    }

    private void initializeEvents() {
        view.addStartListener(event -> startServer());
        view.addStopListener(event -> stopServer());
        view.addClearLogListener(event -> view.clearLog());
        view.addUserRefreshListener(event -> refreshUsers());
        view.addPostRefreshListener(event -> refreshPosts());
        view.addUserDetailListener(event -> view.showSelectedUserDetail());
        view.addPostDetailListener(event -> view.showSelectedPostDetail());
        view.addUserStatusListener(event -> toggleSelectedUserStatus());
        view.addPageChangeListener(page -> {
            if (ServerFrame.USERS_PAGE.equals(page)) refreshUsers();
            if (ServerFrame.POSTS_PAGE.equals(page)) refreshPosts();
            if (ServerFrame.DASHBOARD_PAGE.equals(page)) refreshDashboard();
        });

        view.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                refreshTimer.stop();
                stopServer();
                view.dispose();
            }
        });
    }

    private void startServer() {
        TCPServer currentServer = server;
        if (currentServer != null && currentServer.isRunning()) {
            view.appendLog("Server đang chạy");
            return;
        }

        final int port;
        try {
            port = Integer.parseInt(view.getPortText());
            if (port < 1 || port >= 65535) throw new NumberFormatException();
        } catch (NumberFormatException exception) {
            view.showError("Cổng phải là số từ 1 đến 65534.");
            return;
        }

        int filePort = port + 1;
        server = new TCPServer(port, this);
        serverThread = new Thread(server::run, "studyconnect-tcp-server");
        serverThread.setDaemon(true);
        fileTransferServer = new FileTransferServer(filePort);
        fileTransferThread = new Thread(fileTransferServer::run, "studyconnect-file-server");
        fileTransferThread.setDaemon(true);

        view.setServerRunning(true);
        view.setClientCount(0);
        view.appendLog("Đang khởi động TCP Server tại cổng " + port);
        view.appendLog("Đang khởi động File Server tại cổng " + filePort);
        serverThread.start();
        fileTransferThread.start();
        refreshDashboard();
    }

    private void stopServer() {
        TCPServer currentServer = server;
        server = null;
        if (currentServer != null) currentServer.stop();

        FileTransferServer currentFileServer = fileTransferServer;
        fileTransferServer = null;
        if (currentFileServer != null) currentFileServer.stop();

        serverThread = null;
        fileTransferThread = null;
        view.setServerRunning(false);
        view.setClientCount(0);
        refreshDashboard();
    }

    private void refreshDashboard() {
        if (!dashboardLoading.compareAndSet(false, true)) return;

        new SwingWorker<DashboardSnapshot, Void>() {
            @Override
            protected DashboardSnapshot doInBackground() throws Exception {
                TCPServer currentServer = server;
                boolean running = currentServer != null && currentServer.isRunning();
                int port = currentServer == null ? parsePortOrDefault() : currentServer.getPort();
                List<ClientConnectionManager.ConnectionSnapshot> connections =
                        currentServer == null
                                ? Collections.emptyList()
                                : currentServer.getConnectionSnapshots();
                List<AdminUserDTO> users = userDAO.findAllForAdmin();
                Map<Long, String> names = new HashMap<>();
                for (AdminUserDTO user : users) names.put(user.id(), user.username());

                List<ConnectedClientDTO> clients = new ArrayList<>();
                for (ClientConnectionManager.ConnectionSnapshot connection : connections) {
                    String username = connection.userId() == null
                            ? "Chưa xác thực"
                            : names.getOrDefault(connection.userId(), "User #" + connection.userId());
                    clients.add(new ConnectedClientDTO(
                            connection.userId(),
                            username,
                            connection.ipAddress(),
                            connection.port(),
                            connection.connectedAt(),
                            connection.authenticated()
                    ));
                }

                SystemMetrics metrics = systemMetrics();
                return new DashboardSnapshot(
                        resolveLocalIp(),
                        port,
                        running,
                        connections.size(),
                        users.size(),
                        postDAO.countCreatedToday(),
                        ManagementFactory.getThreadMXBean().getThreadCount(),
                        metrics.cpuPercent(),
                        metrics.usedMemory(),
                        metrics.totalMemory(),
                        clients
                );
            }

            @Override
            protected void done() {
                dashboardLoading.set(false);
                try {
                    view.updateDashboard(get());
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException exception) {
                    view.appendLog("Không thể cập nhật tổng quan: " + rootMessage(exception));
                }
            }
        }.execute();
    }

    private void refreshUsers() {
        if (!usersLoading.compareAndSet(false, true)) return;
        view.setUserLoading(true);
        new SwingWorker<List<AdminUserDTO>, Void>() {
            @Override
            protected List<AdminUserDTO> doInBackground() throws Exception {
                Set<Long> onlineIds = sessionManager.getActiveUserIds();
                List<AdminUserDTO> result = new ArrayList<>();
                for (AdminUserDTO user : userDAO.findAllForAdmin()) {
                    result.add(user.withOnline(onlineIds.contains(user.id())));
                }
                return result;
            }

            @Override
            protected void done() {
                usersLoading.set(false);
                view.setUserLoading(false);
                try {
                    view.setUsers(get());
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException exception) {
                    view.showUserError("Không thể tải người dùng: " + rootMessage(exception));
                }
            }
        }.execute();
    }

    private void refreshPosts() {
        if (!postsLoading.compareAndSet(false, true)) return;
        view.setPostLoading(true);
        new SwingWorker<List<PostDTO>, Void>() {
            @Override
            protected List<PostDTO> doInBackground() throws Exception {
                return postDAO.findAll();
            }

            @Override
            protected void done() {
                postsLoading.set(false);
                view.setPostLoading(false);
                try {
                    view.setPosts(get());
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException exception) {
                    view.showPostError("Không thể tải bài viết: " + rootMessage(exception));
                }
            }
        }.execute();
    }

    private void toggleSelectedUserStatus() {
        AdminUserDTO user = view.getSelectedUser();
        if (user == null) {
            view.showError("Vui lòng chọn một người dùng.");
            return;
        }
        String nextStatus = "LOCKED".equalsIgnoreCase(user.status()) ? "ACTIVE" : "LOCKED";
        view.setUserLoading(true);
        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                return userDAO.updateStatus(user.id(), nextStatus);
            }

            @Override
            protected void done() {
                view.setUserLoading(false);
                try {
                    if (!get()) view.showError("Không tìm thấy người dùng cần cập nhật.");
                    refreshUsers();
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException exception) {
                    view.showError("Không thể cập nhật trạng thái: " + rootMessage(exception));
                }
            }
        }.execute();
    }

    private int parsePortOrDefault() {
        try {
            int value = Integer.parseInt(view.getPortText());
            return value > 0 && value < 65535 ? value : DEFAULT_PORT;
        } catch (NumberFormatException exception) {
            return DEFAULT_PORT;
        }
    }

    private String resolveLocalIp() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception exception) {
            return "127.0.0.1";
        }
    }

    private SystemMetrics systemMetrics() {
        java.lang.management.OperatingSystemMXBean bean =
                ManagementFactory.getOperatingSystemMXBean();
        if (bean instanceof com.sun.management.OperatingSystemMXBean os) {
            double cpu = os.getCpuLoad();
            long total = os.getTotalMemorySize();
            long used = total - os.getFreeMemorySize();
            return new SystemMetrics(
                    cpu < 0 ? 0 : (int) Math.round(cpu * 100),
                    Math.max(0, used),
                    Math.max(0, total)
            );
        }
        Runtime runtime = Runtime.getRuntime();
        long total = runtime.totalMemory();
        return new SystemMetrics(0, total - runtime.freeMemory(), total);
    }

    private String rootMessage(Throwable throwable) {
        Throwable cause = throwable;
        while (cause.getCause() != null && cause.getCause() != cause) cause = cause.getCause();
        return cause.getMessage() == null ? cause.getClass().getSimpleName() : cause.getMessage();
    }

    @Override
    public void onLog(String message) {
        SwingUtilities.invokeLater(() -> view.appendLog(message));
    }

    @Override
    public void onStatusChanged(boolean running) {
        SwingUtilities.invokeLater(() -> {
            view.setServerRunning(running);
            refreshDashboard();
        });
    }

    @Override
    public void onClientCountChanged(int clientCount) {
        SwingUtilities.invokeLater(() -> {
            view.setClientCount(clientCount);
            refreshDashboard();
            refreshUsers();
        });
    }

    @Override
    public void onPostCreated(PostDTO post) {
        SwingUtilities.invokeLater(() -> {
            view.addOrUpdatePost(post);
            refreshDashboard();
        });
    }

    private record SystemMetrics(
            int cpuPercent,
            long usedMemory,
            long totalMemory
    ) {
    }
}
