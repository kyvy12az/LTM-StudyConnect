package com.studyconnect.server.view;

import com.studyconnect.server.model.dto.AdminUserDTO;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class UserManagementPanel extends JPanel {
    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final JTextField searchField = new JTextField();
    private final JComboBox<String> onlineFilter = new JComboBox<>(
            new String[]{"Tất cả kết nối", "Online", "Offline"}
    );
    private final JComboBox<String> statusFilter = new JComboBox<>(
            new String[]{"Tất cả trạng thái", "ACTIVE", "LOCKED"}
    );
    private final JButton refreshButton = ServerTheme.secondaryButton("Làm mới");
    private final JButton detailButton = ServerTheme.secondaryButton("Xem chi tiết");
    private final JButton toggleStatusButton = ServerTheme.primaryButton("Khóa / Mở khóa");
    private final JLabel statusLabel = new JLabel(" ");
    private final DefaultTableModel tableModel = new DefaultTableModel(
            new Object[]{"ID", "Username", "Họ tên", "Email", "Avatar", "Tài khoản", "Kết nối", "Lần cuối hoạt động"},
            0
    ) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return columnIndex == 4 ? Icon.class : Object.class;
        }
    };
    private final JTable table = new JTable(tableModel);
    private final List<AdminUserDTO> users = new ArrayList<>();
    private final List<AdminUserDTO> visibleUsers = new ArrayList<>();
    private final Map<String, Icon> avatarCache = new ConcurrentHashMap<>();
    private final Set<String> loadingAvatars = ConcurrentHashMap.newKeySet();
    private final Icon defaultAvatar = loadDefaultAvatar();

    public UserManagementPanel() {
        setLayout(new BorderLayout(0, 12));
        setBackground(ServerTheme.BACKGROUND);
        setBorder(new EmptyBorder(4, 4, 16, 10));
        add(createToolbar(), BorderLayout.NORTH);
        ServerTheme.configureTable(table);
        table.setRowHeight(40);
        table.setAutoCreateRowSorter(true);
        add(new JScrollPane(table), BorderLayout.CENTER);
        statusLabel.setForeground(ServerTheme.MUTED);
        statusLabel.setFont(ServerTheme.font(Font.PLAIN, 12));
        add(statusLabel, BorderLayout.SOUTH);
        installFilters();
    }

    private JComponent createToolbar() {
        JPanel container = ServerTheme.card(new BorderLayout(10, 0));
        searchField.putClientProperty("JTextField.placeholderText", "Tìm username, họ tên hoặc email...");
        searchField.setPreferredSize(new Dimension(330, 38));

        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        filters.setOpaque(false);
        filters.add(searchField);
        filters.add(onlineFilter);
        filters.add(statusFilter);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        actions.add(refreshButton);
        actions.add(detailButton);
        actions.add(toggleStatusButton);
        container.add(filters, BorderLayout.CENTER);
        container.add(actions, BorderLayout.EAST);
        return container;
    }

    private void installFilters() {
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent event) { applyFilters(); }
            public void removeUpdate(DocumentEvent event) { applyFilters(); }
            public void changedUpdate(DocumentEvent event) { applyFilters(); }
        });
        onlineFilter.addActionListener(event -> applyFilters());
        statusFilter.addActionListener(event -> applyFilters());
    }

    public void setUsers(List<AdminUserDTO> newUsers) {
        users.clear();
        if (newUsers != null) users.addAll(newUsers);
        applyFilters();
    }

    private void applyFilters() {
        String query = searchField.getText().trim().toLowerCase(Locale.ROOT);
        String online = String.valueOf(onlineFilter.getSelectedItem());
        String status = String.valueOf(statusFilter.getSelectedItem());
        visibleUsers.clear();

        for (AdminUserDTO user : users) {
            String searchable = (safe(user.username()) + " "
                    + safe(user.fullName()) + " "
                    + safe(user.email())).toLowerCase(Locale.ROOT);
            boolean matchesQuery = query.isEmpty() || searchable.contains(query);
            boolean matchesOnline = "Tất cả kết nối".equals(online)
                    || ("Online".equals(online) && user.online())
                    || ("Offline".equals(online) && !user.online());
            boolean matchesStatus = "Tất cả trạng thái".equals(status)
                    || status.equalsIgnoreCase(user.status());
            if (matchesQuery && matchesOnline && matchesStatus) {
                visibleUsers.add(user);
            }
        }

        tableModel.setRowCount(0);
        for (AdminUserDTO user : visibleUsers) {
            tableModel.addRow(new Object[]{
                    user.id(),
                    user.username(),
                    user.fullName(),
                    user.email(),
                    avatarFor(user.avatarUrl()),
                    user.status(),
                    user.online() ? "Online" : "Offline",
                    formatTime(user.lastSeen())
            });
        }
        statusLabel.setText("Hiển thị " + visibleUsers.size() + " / " + users.size() + " người dùng");
    }

    public AdminUserDTO getSelectedUser() {
        int selected = table.getSelectedRow();
        if (selected < 0) return null;
        int modelRow = table.convertRowIndexToModel(selected);
        if (modelRow < 0 || modelRow >= visibleUsers.size()) return null;
        return visibleUsers.get(modelRow);
    }

    public void showSelectedUserDetail(Component parent) {
        AdminUserDTO user = getSelectedUser();
        if (user == null) {
            JOptionPane.showMessageDialog(parent, "Vui lòng chọn một người dùng.", "Người dùng", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        String avatar = user.avatarUrl() == null || user.avatarUrl().isBlank()
                ? "/images/default-avatar.png"
                : user.avatarUrl();
        String message = "ID: " + user.id()
                + "\nUsername: " + user.username()
                + "\nHọ tên: " + user.fullName()
                + "\nEmail: " + user.email()
                + "\nAvatar: " + avatar
                + "\nTrạng thái: " + user.status()
                + "\nKết nối: " + (user.online() ? "Online" : "Offline")
                + "\nLần cuối hoạt động: " + formatTime(user.lastSeen());
        JOptionPane.showMessageDialog(parent, message, "Chi tiết người dùng", JOptionPane.INFORMATION_MESSAGE);
    }

    public void setLoading(boolean loading) {
        refreshButton.setEnabled(!loading);
        statusLabel.setText(loading ? "Đang tải dữ liệu người dùng..." : statusLabel.getText());
    }

    public void showError(String message) {
        statusLabel.setForeground(ServerTheme.DANGER);
        statusLabel.setText(message == null ? "Không thể tải người dùng." : message);
    }

    public void addRefreshListener(ActionListener listener) { refreshButton.addActionListener(listener); }
    public void addDetailListener(ActionListener listener) { detailButton.addActionListener(listener); }
    public void addToggleStatusListener(ActionListener listener) { toggleStatusButton.addActionListener(listener); }

    private String formatTime(long value) {
        if (value <= 0) return "--";
        return Instant.ofEpochMilli(value).atZone(ZoneId.systemDefault()).format(DATE_TIME);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private Icon avatarFor(String avatarUrl) {
        if (avatarUrl == null || avatarUrl.isBlank()) return defaultAvatar;
        Icon cached = avatarCache.get(avatarUrl);
        if (cached != null) return cached;
        if (loadingAvatars.add(avatarUrl)) {
            new SwingWorker<Icon, Void>() {
                @Override
                protected Icon doInBackground() {
                    try {
                        URL resource;
                        if (avatarUrl.startsWith("http://")
                                || avatarUrl.startsWith("https://")) {
                            URLConnection connection = new URL(avatarUrl).openConnection();
                            connection.setConnectTimeout(4_000);
                            connection.setReadTimeout(4_000);
                            try (java.io.InputStream input = connection.getInputStream()) {
                                BufferedImage image = ImageIO.read(input);
                                return image == null ? defaultAvatar : scaleAvatar(image);
                            }
                        }
                        resource = UserManagementPanel.class.getResource(avatarUrl);
                        if (resource == null) return defaultAvatar;
                        BufferedImage image = ImageIO.read(resource);
                        return image == null ? defaultAvatar : scaleAvatar(image);
                    } catch (IOException exception) {
                        return defaultAvatar;
                    }
                }

                @Override
                protected void done() {
                    try {
                        avatarCache.put(avatarUrl, get());
                    } catch (Exception exception) {
                        avatarCache.put(avatarUrl, defaultAvatar);
                    } finally {
                        loadingAvatars.remove(avatarUrl);
                        tableModel.fireTableDataChanged();
                    }
                }
            }.execute();
        }
        return defaultAvatar;
    }

    private Icon loadDefaultAvatar() {
        URL resource = UserManagementPanel.class.getResource(
                "/images/default-avatar.png"
        );
        if (resource == null) return UIManager.getIcon("Tree.leafIcon");
        try {
            BufferedImage image = ImageIO.read(resource);
            return image == null ? UIManager.getIcon("Tree.leafIcon") : scaleAvatar(image);
        } catch (IOException exception) {
            return UIManager.getIcon("Tree.leafIcon");
        }
    }

    private Icon scaleAvatar(BufferedImage image) {
        Image scaled = image.getScaledInstance(30, 30, Image.SCALE_SMOOTH);
        return new ImageIcon(scaled);
    }
}
