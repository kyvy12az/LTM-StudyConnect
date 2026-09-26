package com.studyconnect.server.view;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class ServerFrame extends JFrame {
    private final JTextField portField;

    private final JButton startButton;
    private final JButton stopButton;
    private final JButton clearLogButton;

    private final JLabel statusLabel;
    private final JLabel clientCountLabel;

    private final JTextArea logArea;

    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");

    public ServerFrame() {
        setTitle("StudyConnect - Server Management");
        setSize(850, 560);
        setMinimumSize(new Dimension(700, 450));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        portField = new JTextField("2006", 8);
        startButton = new JButton("Khởi động Server");
        stopButton = new JButton("Dừng Server");
        clearLogButton = new JButton("Xóa Log");

        statusLabel = new JLabel("Đã dừng", SwingConstants.CENTER);
        clientCountLabel = new JLabel("0", SwingConstants.CENTER);

        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));

        initializeUI();
        setServerRunning(false);
    }

    private void initializeUI() {
        setLayout(new BorderLayout(10, 10));

        JPanel headerPanel = createHeaderPanel();
        JPanel informationPanel = createInformationPanel();
        JScrollPane logScrollPane = createLogPanel();

        JPanel northPanel = new JPanel(new BorderLayout(10, 10));
        northPanel.add(headerPanel, BorderLayout.NORTH);
        northPanel.add(informationPanel, BorderLayout.CENTER);

        add(northPanel, BorderLayout.NORTH);
        add(logScrollPane, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottomPanel.add(clearLogButton);

        add(bottomPanel, BorderLayout.SOUTH);

        ((JPanel) getContentPane()).setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
    }

    private JPanel createHeaderPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        JLabel titleLabel = new JLabel("Quản lý Server");
        titleLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 20));

        JLabel portLabel = new JLabel("Cổng:");

        panel.add(titleLabel);
        panel.add(new JLabel("      "));
        panel.add(portLabel);
        panel.add(portField);
        panel.add(startButton);
        panel.add(stopButton);

        return panel;
    }

    private JPanel createInformationPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 2, 10, 0));

        JPanel statusPanel = createCard("Trạng thái Server", statusLabel);
        JPanel clientPanel = createCard("Client đang kết nối", clientCountLabel);

        panel.add(statusPanel);
        panel.add(clientPanel);

        return panel;
    }

    private JPanel createCard(String title, JLabel valueLabel) {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(210, 210, 210)), BorderFactory.createEmptyBorder(12, 12, 12, 12)));

        JLabel titleLabel = new JLabel(title, SwingConstants.CENTER);
        titleLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));

        valueLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));

        panel.add(titleLabel, BorderLayout.NORTH);
        panel.add(valueLabel, BorderLayout.CENTER);

        return panel;
    }

    private JScrollPane createLogPanel() {
        JScrollPane scrollPane = new JScrollPane(logArea);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Nhật ký hoạt động"));

        return scrollPane;
    }

    public String getPortText() {
        return portField.getText().trim();
    }

    public void setServerRunning(boolean running) {
        startButton.setEnabled(!running);
        stopButton.setEnabled(running);
        portField.setEnabled(!running);

        if (running) {
            statusLabel.setText("Đang chạy");
            statusLabel.setForeground(new Color(25, 135, 84));
        } else {
            statusLabel.setText("Đã dừng");
            statusLabel.setForeground(new Color(190, 50, 50));
        }
    }

    public void setClientCount(int count) {
        clientCountLabel.setText(String.valueOf(count));
    }

    public void appendLog(String message) {
        String currentTime = LocalTime.now().format(timeFormatter);

        logArea.append("[" + currentTime + "] " + message + System.lineSeparator());
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    public void clearLog() {
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
}
