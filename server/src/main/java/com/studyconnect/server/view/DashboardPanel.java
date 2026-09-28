package com.studyconnect.server.view;

import com.studyconnect.server.model.dto.ConnectedClientDTO;
import com.studyconnect.server.model.dto.DashboardSnapshot;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionListener;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class DashboardPanel extends JPanel {

    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern(
                    "dd/MM/yyyy HH:mm:ss"
            );

    private final JTextField ipField =
            new JTextField("--", 12);

    private final JTextField portField =
            new JTextField("2006", 6);

    private final JLabel serverStatus =
            new JLabel("● Đã dừng");

    private final JButton startButton =
            ServerTheme.primaryButton("Khởi động");

    private final JButton stopButton =
            ServerTheme.secondaryButton("Dừng Server");

    private final JLabel onlineValue = statisticValue();
    private final JLabel userValue = statisticValue();
    private final JLabel postValue = statisticValue();
    private final JLabel threadValue = statisticValue();

    private final DefaultTableModel clientModel =
            new DefaultTableModel(
                    new Object[]{
                            "STT",
                            "Người dùng",
                            "Địa chỉ IP",
                            "Cổng",
                            "Thời gian kết nối",
                            "Trạng thái"
                    },
                    0
            ) {
                @Override
                public boolean isCellEditable(
                        int row,
                        int column
                ) {
                    return false;
                }
            };

    private final JTable clientTable =
            new JTable(clientModel);

    private final ActivityChart activityChart =
            new ActivityChart();

    private final JProgressBar cpuBar =
            progressBar();

    private final JProgressBar ramBar =
            progressBar();

    private final JLabel ramDetails =
            new JLabel("--");

    private final JTextArea logArea =
            new JTextArea();

    private final JButton clearLogButton =
            ServerTheme.secondaryButton("Xóa log");

    public DashboardPanel() {
        setLayout(new GridBagLayout());
        setBackground(ServerTheme.BACKGROUND);

        initializeComponents();
        createLayout();

        setServerRunning(false);
    }

    private void initializeComponents() {
        configureServerField(ipField, false);
        configureServerField(portField, true);

        stopButton.setForeground(ServerTheme.DANGER);

        clientTable.setAutoCreateRowSorter(true);
        clientTable.setAutoResizeMode(
                JTable.AUTO_RESIZE_ALL_COLUMNS
        );

        logArea.setEditable(false);
        logArea.setLineWrap(false);
        logArea.setTabSize(4);
        logArea.setFont(
                new Font(
                        Font.MONOSPACED,
                        Font.PLAIN,
                        12
                )
        );
        logArea.setForeground(ServerTheme.TEXT);
        logArea.setBackground(Color.WHITE);
        logArea.setBorder(
                new EmptyBorder(6, 8, 6, 8)
        );

        ramDetails.setFont(
                ServerTheme.font(Font.PLAIN, 11)
        );
        ramDetails.setForeground(ServerTheme.MUTED);
    }

    private void createLayout() {
        GridBagConstraints constraints =
                new GridBagConstraints();

        constraints.gridx = 0;
        constraints.weightx = 1;
        constraints.fill = GridBagConstraints.BOTH;

        // Thanh điều khiển server
        constraints.gridy = 0;
        constraints.weighty = 0;
        constraints.insets =
                new Insets(0, 0, 12, 0);

        add(
                createServerControl(),
                constraints
        );

        // Các thẻ thống kê
        constraints.gridy = 1;
        constraints.weighty = 0;
        constraints.insets =
                new Insets(0, 0, 12, 0);

        add(
                createStatistics(),
                constraints
        );

        // Bảng client và biểu đồ
        constraints.gridy = 2;
        constraints.weighty = 0.64;
        constraints.insets =
                new Insets(0, 0, 12, 0);

        add(
                createMiddleSection(),
                constraints
        );

        // Nhật ký server
        constraints.gridy = 3;
        constraints.weighty = 0.36;
        constraints.insets =
                new Insets(0, 0, 0, 0);

        add(
                createLogSection(),
                constraints
        );
    }

    private JComponent createServerControl() {
        JPanel panel = ServerTheme.card(
                new BorderLayout(20, 0)
        );

        panel.setPreferredSize(
                new Dimension(0, 76)
        );
        panel.setMinimumSize(
                new Dimension(0, 70)
        );

        JLabel title = new JLabel(
                "TCP Server",
                ServerTheme.icon(
                        "server",
                        30,
                        ServerTheme.TEAL_DARK
                ),
                SwingConstants.LEFT
        );

        title.setFont(
                ServerTheme.font(Font.BOLD, 19)
        );
        title.setForeground(ServerTheme.NAVY);
        title.setIconTextGap(12);

        JPanel details = new JPanel(
                new FlowLayout(
                        FlowLayout.LEFT,
                        10,
                        2
                )
        );
        details.setOpaque(false);

        details.add(fieldLabel("Địa chỉ IP:"));
        details.add(ipField);
        details.add(fieldLabel("Cổng:"));
        details.add(portField);
        details.add(Box.createHorizontalStrut(6));
        details.add(serverStatus);

        JPanel buttons = new JPanel(
                new FlowLayout(
                        FlowLayout.RIGHT,
                        10,
                        0
                )
        );
        buttons.setOpaque(false);
        buttons.add(startButton);
        buttons.add(stopButton);

        panel.add(title, BorderLayout.WEST);
        panel.add(details, BorderLayout.CENTER);
        panel.add(buttons, BorderLayout.EAST);

        return panel;
    }

    private JLabel fieldLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(
                ServerTheme.font(Font.PLAIN, 12)
        );
        label.setForeground(ServerTheme.TEXT);
        return label;
    }

    private void configureServerField(
            JTextField field,
            boolean editable
    ) {
        field.setEditable(editable);
        field.setHorizontalAlignment(
                SwingConstants.CENTER
        );
        field.setFont(
                ServerTheme.font(Font.PLAIN, 12)
        );
        field.setPreferredSize(
                new Dimension(
                        editable ? 78 : 130,
                        34
                )
        );

        field.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(
                                ServerTheme.BORDER
                        ),
                        new EmptyBorder(
                                5,
                                9,
                                5,
                                9
                        )
                )
        );
    }

    private JComponent createStatistics() {
        JPanel panel = new JPanel(
                new GridLayout(1, 4, 12, 0)
        );

        panel.setOpaque(false);
        panel.setPreferredSize(
                new Dimension(0, 105)
        );
        panel.setMinimumSize(
                new Dimension(0, 92)
        );

        panel.add(
                statCard(
                        "Client trực tuyến",
                        onlineValue,
                        "users",
                        new Color(231, 250, 243),
                        ServerTheme.TEAL
                )
        );

        panel.add(
                statCard(
                        "Tổng người dùng",
                        userValue,
                        "user",
                        new Color(235, 245, 255),
                        new Color(37, 99, 235)
                )
        );

        panel.add(
                statCard(
                        "Bài viết hôm nay",
                        postValue,
                        "file",
                        new Color(255, 247, 230),
                        new Color(234, 116, 23)
                )
        );

        panel.add(
                statCard(
                        "Luồng đang chạy",
                        threadValue,
                        "chart",
                        new Color(247, 237, 255),
                        new Color(147, 51, 234)
                )
        );

        return panel;
    }

    private JPanel statCard(
            String title,
            JLabel value,
            String icon,
            Color background,
            Color iconColor
    ) {
        JPanel card = ServerTheme.card(
                new BorderLayout(14, 0)
        );

        card.setBackground(background);

        JLabel iconLabel = new JLabel(
                ServerTheme.icon(
                        icon,
                        36,
                        iconColor
                )
        );
        iconLabel.setHorizontalAlignment(
                SwingConstants.CENTER
        );
        iconLabel.setPreferredSize(
                new Dimension(52, 52)
        );

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(
                new BoxLayout(
                        text,
                        BoxLayout.Y_AXIS
                )
        );

        JLabel caption = new JLabel(title);
        caption.setForeground(ServerTheme.MUTED);
        caption.setFont(
                ServerTheme.font(Font.PLAIN, 13)
        );

        value.setAlignmentX(
                Component.LEFT_ALIGNMENT
        );
        caption.setAlignmentX(
                Component.LEFT_ALIGNMENT
        );

        text.add(caption);
        text.add(Box.createVerticalStrut(5));
        text.add(value);

        card.add(iconLabel, BorderLayout.WEST);
        card.add(text, BorderLayout.CENTER);

        return card;
    }

    private JComponent createMiddleSection() {
        JPanel panel = new JPanel(
                new GridBagLayout()
        );
        panel.setOpaque(false);
        panel.setMinimumSize(
                new Dimension(0, 235)
        );

        GridBagConstraints constraints =
                new GridBagConstraints();

        constraints.gridy = 0;
        constraints.weighty = 1;
        constraints.fill = GridBagConstraints.BOTH;

        constraints.gridx = 0;
        constraints.weightx = 1.05;
        constraints.insets =
                new Insets(0, 0, 0, 6);

        panel.add(
                createClientSection(),
                constraints
        );

        constraints.gridx = 1;
        constraints.weightx = 1;
        constraints.insets =
                new Insets(0, 6, 0, 0);

        panel.add(
                createActivitySection(),
                constraints
        );

        return panel;
    }

    private JComponent createClientSection() {
        JPanel clients = ServerTheme.card(
                new BorderLayout(0, 10)
        );

        clients.add(
                sectionTitle("Client đang kết nối"),
                BorderLayout.NORTH
        );

        ServerTheme.configureTable(clientTable);
        configureClientColumns();

        JScrollPane scrollPane =
                new JScrollPane(clientTable);

        scrollPane.setBorder(
                BorderFactory.createLineBorder(
                        ServerTheme.BORDER
                )
        );

        scrollPane.getViewport().setBackground(
                Color.WHITE
        );

        clients.add(
                scrollPane,
                BorderLayout.CENTER
        );

        return clients;
    }

    private void configureClientColumns() {
        clientTable.getColumnModel()
                .getColumn(0)
                .setPreferredWidth(42);

        clientTable.getColumnModel()
                .getColumn(1)
                .setPreferredWidth(115);

        clientTable.getColumnModel()
                .getColumn(2)
                .setPreferredWidth(110);

        clientTable.getColumnModel()
                .getColumn(3)
                .setPreferredWidth(65);

        clientTable.getColumnModel()
                .getColumn(4)
                .setPreferredWidth(155);

        clientTable.getColumnModel()
                .getColumn(5)
                .setPreferredWidth(105);
    }

    private JComponent createActivitySection() {
        JPanel activity = ServerTheme.card(
                new BorderLayout(0, 10)
        );

        activity.add(
                sectionTitle("Hoạt động hệ thống"),
                BorderLayout.NORTH
        );

        activity.add(
                activityChart,
                BorderLayout.CENTER
        );

        JPanel metrics = new JPanel(
                new GridLayout(2, 1, 0, 7)
        );
        metrics.setOpaque(false);
        metrics.setPreferredSize(
                new Dimension(0, 48)
        );

        metrics.add(
                metricRow(
                        "CPU",
                        cpuBar,
                        null
                )
        );

        metrics.add(
                metricRow(
                        "RAM",
                        ramBar,
                        ramDetails
                )
        );

        activity.add(
                metrics,
                BorderLayout.SOUTH
        );

        return activity;
    }

    private JPanel metricRow(
            String name,
            JProgressBar bar,
            JLabel details
    ) {
        JPanel row = new JPanel(
                new BorderLayout(10, 0)
        );
        row.setOpaque(false);

        JLabel label = new JLabel(name);
        label.setFont(
                ServerTheme.font(Font.BOLD, 12)
        );
        label.setForeground(ServerTheme.NAVY);
        label.setPreferredSize(
                new Dimension(34, 18)
        );

        row.add(label, BorderLayout.WEST);
        row.add(bar, BorderLayout.CENTER);

        if (details != null) {
            details.setPreferredSize(
                    new Dimension(105, 18)
            );
            details.setHorizontalAlignment(
                    SwingConstants.RIGHT
            );

            row.add(
                    details,
                    BorderLayout.EAST
            );
        }

        return row;
    }

    private JComponent createLogSection() {
        JPanel panel = ServerTheme.card(
                new BorderLayout(0, 8)
        );

        panel.setMinimumSize(
                new Dimension(0, 145)
        );

        JPanel header = new JPanel(
                new BorderLayout()
        );
        header.setOpaque(false);

        header.add(
                sectionTitle("Nhật ký Server"),
                BorderLayout.WEST
        );

        clearLogButton.setPreferredSize(
                new Dimension(92, 34)
        );

        header.add(
                clearLogButton,
                BorderLayout.EAST
        );

        JScrollPane logScroll =
                new JScrollPane(logArea);

        logScroll.setBorder(
                BorderFactory.createLineBorder(
                        ServerTheme.BORDER
                )
        );

        logScroll.getVerticalScrollBar()
                .setUnitIncrement(16);

        panel.add(header, BorderLayout.NORTH);
        panel.add(logScroll, BorderLayout.CENTER);

        return panel;
    }

    private JLabel sectionTitle(String text) {
        JLabel label = new JLabel(text);
        label.setFont(
                ServerTheme.font(Font.BOLD, 15)
        );
        label.setForeground(ServerTheme.NAVY);
        return label;
    }

    private static JLabel statisticValue() {
        JLabel label = new JLabel("0");

        label.setFont(
                ServerTheme.font(Font.BOLD, 27)
        );
        label.setForeground(ServerTheme.NAVY);

        return label;
    }

    private static JProgressBar progressBar() {
        JProgressBar bar =
                new JProgressBar(0, 100);

        bar.setStringPainted(true);
        bar.setForeground(ServerTheme.SUCCESS);
        bar.setBackground(
                new Color(226, 233, 238)
        );
        bar.setBorderPainted(false);
        bar.setPreferredSize(
                new Dimension(100, 18)
        );

        return bar;
    }

    public String getPortText() {
        return portField.getText().trim();
    }

    public void setServerRunning(boolean running) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(
                    () -> setServerRunning(running)
            );
            return;
        }

        startButton.setEnabled(!running);
        stopButton.setEnabled(running);
        portField.setEnabled(!running);

        serverStatus.setText(
                running
                        ? "●  Đang hoạt động"
                        : "●  Đã dừng"
        );

        serverStatus.setForeground(
                running
                        ? ServerTheme.SUCCESS
                        : ServerTheme.DANGER
        );

        serverStatus.setBackground(
                running
                        ? new Color(224, 248, 234)
                        : new Color(255, 235, 238)
        );

        serverStatus.setOpaque(true);
        serverStatus.setBorder(
                new EmptyBorder(
                        6,
                        11,
                        6,
                        11
                )
        );
    }

    public void setClientCount(int count) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(
                    () -> setClientCount(count)
            );
            return;
        }

        onlineValue.setText(
                String.valueOf(
                        Math.max(0, count)
                )
        );
    }

    public void setSnapshot(
            DashboardSnapshot snapshot
    ) {
        if (snapshot == null) {
            return;
        }

        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(
                    () -> setSnapshot(snapshot)
            );
            return;
        }

        ipField.setText(snapshot.serverIp());

        if (!portField.hasFocus()) {
            portField.setText(
                    String.valueOf(
                            snapshot.serverPort()
                    )
            );
        }

        setServerRunning(
                snapshot.serverRunning()
        );

        onlineValue.setText(
                String.valueOf(
                        snapshot.onlineClients()
                )
        );

        userValue.setText(
                String.valueOf(
                        snapshot.totalUsers()
                )
        );

        postValue.setText(
                String.valueOf(
                        snapshot.postsToday()
                )
        );

        threadValue.setText(
                String.valueOf(
                        snapshot.threadCount()
                )
        );

        cpuBar.setValue(
                snapshot.cpuPercent()
        );

        ramBar.setValue(
                memoryPercent(
                        snapshot.usedMemoryBytes(),
                        snapshot.totalMemoryBytes()
                )
        );

        ramDetails.setText(
                formatBytes(
                        snapshot.usedMemoryBytes()
                )
                        + " / "
                        + formatBytes(
                        snapshot.totalMemoryBytes()
                )
        );

        setClients(snapshot.clients());

        activityChart.addSample(
                snapshot.onlineClients()
        );
    }

    private void setClients(
            List<ConnectedClientDTO> clients
    ) {
        clientModel.setRowCount(0);

        if (clients == null) {
            return;
        }

        int index = 1;

        for (ConnectedClientDTO client : clients) {
            clientModel.addRow(
                    new Object[]{
                            index++,
                            valueOrDefault(
                                    client.username(),
                                    "Chưa đăng nhập"
                            ),
                            valueOrDefault(
                                    client.ipAddress(),
                                    "--"
                            ),
                            client.port(),
                            formatTime(
                                    client.connectedAt()
                            ),
                            client.authenticated()
                                    ? "● Trực tuyến"
                                    : "Chưa xác thực"
                    }
            );
        }
    }

    public void appendLog(String message) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(
                    () -> appendLog(message)
            );
            return;
        }

        String time = java.time.LocalTime.now()
                .format(
                        DateTimeFormatter.ofPattern(
                                "HH:mm:ss"
                        )
                );

        logArea.append(
                "["
                        + time
                        + "] "
                        + valueOrDefault(message, "")
                        + System.lineSeparator()
        );

        logArea.setCaretPosition(
                logArea.getDocument().getLength()
        );
    }

    public void clearLog() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::clearLog);
            return;
        }

        logArea.setText("");
    }

    public void addStartListener(
            ActionListener listener
    ) {
        startButton.addActionListener(listener);
    }

    public void addStopListener(
            ActionListener listener
    ) {
        stopButton.addActionListener(listener);
    }

    public void addClearLogListener(
            ActionListener listener
    ) {
        clearLogButton.addActionListener(listener);
    }

    private String valueOrDefault(
            String value,
            String defaultValue
    ) {
        return value == null || value.isBlank()
                ? defaultValue
                : value;
    }

    private String formatTime(long value) {
        if (value <= 0) {
            return "--";
        }

        return Instant.ofEpochMilli(value)
                .atZone(ZoneId.systemDefault())
                .format(DATE_TIME);
    }

    private int memoryPercent(
            long used,
            long total
    ) {
        if (total <= 0) {
            return 0;
        }

        return (int) Math.max(
                0,
                Math.min(
                        100,
                        used * 100 / total
                )
        );
    }

    private String formatBytes(long bytes) {
        if (bytes <= 0) {
            return "--";
        }

        double gigabytes =
                bytes
                        / (1024.0 * 1024 * 1024);

        return String.format(
                "%.1f GB",
                gigabytes
        );
    }

    private static final class ActivityChart
            extends JPanel {

        private final List<Integer> samples =
                new ArrayList<>();

        private ActivityChart() {
            setOpaque(false);
            setMinimumSize(
                    new Dimension(240, 130)
            );
        }

        private void addSample(int value) {
            samples.add(Math.max(0, value));

            while (samples.size() > 24) {
                samples.remove(0);
            }

            repaint();
        }

        @Override
        protected void paintComponent(
                Graphics graphics
        ) {
            super.paintComponent(graphics);

            Graphics2D g =
                    (Graphics2D) graphics.create();

            g.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON
            );

            int left = 34;
            int right = 12;
            int top = 15;
            int bottom = 22;

            int width = Math.max(
                    1,
                    getWidth() - left - right
            );

            int height = Math.max(
                    1,
                    getHeight() - top - bottom
            );

            int maximum = Math.max(
                    5,
                    samples.stream()
                            .mapToInt(Integer::intValue)
                            .max()
                            .orElse(5)
            );

            g.setFont(
                    ServerTheme.font(Font.PLAIN, 10)
            );

            for (int i = 0; i <= 4; i++) {
                int y =
                        top + height * i / 4;

                int value =
                        maximum
                                - maximum * i / 4;

                g.setColor(
                        new Color(225, 235, 240)
                );

                g.drawLine(
                        left,
                        y,
                        left + width,
                        y
                );

                g.setColor(ServerTheme.MUTED);
                g.drawString(
                        String.valueOf(value),
                        5,
                        y + 4
                );
            }

            if (samples.isEmpty()) {
                g.setColor(ServerTheme.MUTED);
                g.drawString(
                        "Chưa có dữ liệu hoạt động",
                        left + 12,
                        top + height / 2
                );

                g.dispose();
                return;
            }

            g.setStroke(new BasicStroke(2.3f));
            g.setColor(ServerTheme.TEAL);

            int previousX = left;
            int previousY =
                    top
                            + height
                            - samples.get(0)
                            * height
                            / maximum;

            g.fillOval(
                    previousX - 3,
                    previousY - 3,
                    6,
                    6
            );

            for (int i = 1;
                 i < samples.size();
                 i++) {

                int x =
                        left
                                + i
                                * width
                                / Math.max(
                                1,
                                samples.size() - 1
                        );

                int y =
                        top
                                + height
                                - samples.get(i)
                                * height
                                / maximum;

                g.drawLine(
                        previousX,
                        previousY,
                        x,
                        y
                );

                g.fillOval(
                        x - 3,
                        y - 3,
                        6,
                        6
                );

                previousX = x;
                previousY = y;
            }

            g.dispose();
        }
    }
}