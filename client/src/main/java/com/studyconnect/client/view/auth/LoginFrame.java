package com.studyconnect.client.view.auth;

import com.studyconnect.client.view.component.AuthTheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionListener;

public class LoginFrame extends JFrame {

    private final AuthTheme.RoundedTextField accountField =
            new AuthTheme.RoundedTextField(
                    "Nhập tên đăng nhập hoặc email",
                    AuthTheme.InputIcon.USER
            );

    private final AuthTheme.PasswordFieldWithToggle passwordField =
            new AuthTheme.PasswordFieldWithToggle("Nhập mật khẩu");

    private final JCheckBox rememberCheckBox =
            new JCheckBox("Ghi nhớ đăng nhập");

    private final AuthTheme.PrimaryButton loginButton =
            new AuthTheme.PrimaryButton("Đăng nhập");

    private final JButton registerButton =
            AuthTheme.createLinkButton("Đăng ký ngay");

    private final JButton forgotPasswordButton =
            AuthTheme.createLinkButton("Quên mật khẩu?");

    private final JLabel errorLabel =
            AuthTheme.createErrorLabel();

    private final JLabel connectionLabel =
            new JLabel("●  Đã kết nối Server");

    public LoginFrame() {
        initializeFrame();
        initializeUI();
    }

    private void initializeFrame() {
        setTitle("StudyConnect - Đăng nhập");

        /*
         * Ảnh background có tỉ lệ dọc nên không nên để cửa sổ
         * quá rộng như 1100x760.
         */
        setSize(940, 720);
        setMinimumSize(new Dimension(880, 670));

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        AuthTheme.installFrameIcon(this);
    }

    private void initializeUI() {
        JPanel root = new JPanel(new GridBagLayout());
        root.setBackground(AuthTheme.BACKGROUND);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1;

        // Sidebar chứa background.png
        AuthTheme.ImagePanel sidebar =
                new AuthTheme.ImagePanel(true);

        gbc.gridx = 0;
        gbc.weightx = 0.48;
        root.add(sidebar, gbc);

        // Khu vực form đăng nhập
        gbc.gridx = 1;
        gbc.weightx = 0.52;
        root.add(createFormArea(), gbc);

        setContentPane(root);
        getRootPane().setDefaultButton(loginButton);

        forgotPasswordButton.addActionListener(event ->
                showError(
                        "Vui lòng liên hệ quản trị viên để đặt lại mật khẩu."
                )
        );
    }

    private JPanel createFormArea() {
        JPanel area = new JPanel(new BorderLayout());
        area.setBackground(AuthTheme.BACKGROUND);
        area.setBorder(new EmptyBorder(22, 24, 16, 24));

        /*
         * Wrapper giúp card luôn nằm giữa khu vực bên phải.
         */
        JPanel cardWrapper = new JPanel(new GridBagLayout());
        cardWrapper.setOpaque(false);

        GridBagConstraints cardConstraints = new GridBagConstraints();
        cardConstraints.gridx = 0;
        cardConstraints.gridy = 0;
        cardConstraints.fill = GridBagConstraints.NONE;
        cardConstraints.anchor = GridBagConstraints.CENTER;

        cardWrapper.add(createCard(), cardConstraints);
        area.add(cardWrapper, BorderLayout.CENTER);

        area.add(createConnectionStatus(), BorderLayout.SOUTH);

        return area;
    }

    private JPanel createCard() {
        AuthTheme.RoundedPanel card = new AuthTheme.RoundedPanel(26);
        card.setLayout(new GridBagLayout());
        card.setPreferredSize(new Dimension(410, 560));
        card.setMinimumSize(new Dimension(380, 550));
        card.setMaximumSize(new Dimension(430, 580));

        JPanel contentPanel = new JPanel();
        contentPanel.setOpaque(false);
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setPreferredSize(new Dimension(340, 507));
        contentPanel.setMinimumSize(new Dimension(330, 507));
        contentPanel.setMaximumSize(new Dimension(340, 507));

        JLabel logoLabel = AuthTheme.createLogoLabel(155, 72);
        contentPanel.add(AuthTheme.createCenteredWrapper(logoLabel, 72));
        contentPanel.add(Box.createVerticalStrut(3));

        JLabel titleLabel = AuthTheme.createTitle("Đăng nhập", 28);
        contentPanel.add(AuthTheme.createCenteredWrapper(titleLabel, 40));
        contentPanel.add(Box.createVerticalStrut(5));

        JLabel subtitleLabel = AuthTheme.createSubtitle("Chào mừng bạn quay trở lại!");
        contentPanel.add(AuthTheme.createCenteredWrapper(subtitleLabel, 24));
        contentPanel.add(Box.createVerticalStrut(23));

        addField(contentPanel, "Tên đăng nhập hoặc email", accountField, 15);
        addField(contentPanel, "Mật khẩu", passwordField, 9);

        AuthTheme.configureCheckBox(rememberCheckBox);
        forgotPasswordButton.setBorder(new EmptyBorder(3, 8, 3, 0));

        JPanel optionsPanel = new JPanel(new BorderLayout(12, 0));
        optionsPanel.setOpaque(false);
        optionsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        optionsPanel.setPreferredSize(new Dimension(340, 30));
        optionsPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        optionsPanel.add(rememberCheckBox, BorderLayout.WEST);
        optionsPanel.add(forgotPasswordButton, BorderLayout.EAST);
        contentPanel.add(optionsPanel);
        contentPanel.add(Box.createVerticalStrut(4));

        errorLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        errorLabel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
        contentPanel.add(errorLabel);
        contentPanel.add(Box.createVerticalStrut(4));

        loginButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        loginButton.setPreferredSize(new Dimension(340, 50));
        loginButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        contentPanel.add(loginButton);
        contentPanel.add(Box.createVerticalStrut(20));

        JSeparator separator = new JSeparator();
        separator.setForeground(AuthTheme.BORDER);
        separator.setBackground(AuthTheme.BORDER);
        separator.setAlignmentX(Component.LEFT_ALIGNMENT);
        separator.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        contentPanel.add(separator);
        contentPanel.add(Box.createVerticalStrut(15));

        JPanel registerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 0));
        registerPanel.setOpaque(false);
        registerPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        registerPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));

        JLabel registerPrompt = new JLabel("Chưa có tài khoản?");
        registerPrompt.setFont(AuthTheme.font(Font.PLAIN, 13));
        registerPrompt.setForeground(AuthTheme.TEXT);
        registerPanel.add(registerPrompt);
        registerPanel.add(registerButton);
        contentPanel.add(registerPanel);

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.anchor = GridBagConstraints.CENTER;
        card.add(contentPanel, constraints);
        return card;
    }

    private JPanel createConnectionStatus() {
        JPanel wrapper = new JPanel(new FlowLayout(
                FlowLayout.CENTER,
                0,
                0
        ));

        wrapper.setOpaque(false);
        wrapper.setBorder(new EmptyBorder(12, 0, 2, 0));

        connectionLabel.setFont(
                AuthTheme.font(Font.PLAIN, 12)
        );

        connectionLabel.setForeground(AuthTheme.SUCCESS);
        connectionLabel.setHorizontalAlignment(
                SwingConstants.CENTER
        );

        connectionLabel.setBorder(
                new EmptyBorder(7, 16, 7, 16)
        );

        wrapper.add(connectionLabel);

        return wrapper;
    }

    private void addField(
            JPanel panel,
            String labelText,
            JComponent field,
            int bottomGap
    ) {
        JLabel label = AuthTheme.createFieldLabel(labelText);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        label.setMaximumSize(new Dimension(Integer.MAX_VALUE, label.getPreferredSize().height));

        field.setAlignmentX(Component.LEFT_ALIGNMENT);
        field.setPreferredSize(new Dimension(340, 48));
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));

        panel.add(label);
        panel.add(Box.createVerticalStrut(6));
        panel.add(field);
        panel.add(Box.createVerticalStrut(bottomGap));
    }

    public String getAccount() {
        return accountField.getText().trim();
    }

    public String getPasswordText() {
        return new String(passwordField.getPassword());
    }

    public boolean isRememberSelected() {
        return rememberCheckBox.isSelected();
    }

    public void addLoginListener(ActionListener listener) {
        loginButton.addActionListener(listener);
    }

    public void addRegisterListener(ActionListener listener) {
        registerButton.addActionListener(listener);
    }

    public void setConnectionStatus(
            boolean connected,
            String message
    ) {
        connectionLabel.setForeground(
                connected
                        ? AuthTheme.SUCCESS
                        : AuthTheme.ERROR
        );

        String statusMessage = message;

        if (statusMessage == null || statusMessage.isBlank()) {
            statusMessage = connected
                    ? "Đã kết nối Server"
                    : "Mất kết nối Server";
        }

        connectionLabel.setText(
                (connected ? "●  " : "●  ")
                        + statusMessage
        );
    }

    public void setLoading(boolean loading) {
        loginButton.setEnabled(!loading);
        registerButton.setEnabled(!loading);
        forgotPasswordButton.setEnabled(!loading);
        rememberCheckBox.setEnabled(!loading);
        accountField.setEnabled(!loading);
        passwordField.setEnabled(!loading);

        loginButton.setText(
                loading
                        ? "Đang đăng nhập..."
                        : "Đăng nhập"
        );

        if (loading) {
            errorLabel.setText(" ");
        }
    }

    public void showError(String message) {
        errorLabel.setText(
                "<html>"
                        + escapeHtml(message)
                        + "</html>"
        );
    }

    public void clearError() {
        errorLabel.setText(" ");
    }

    private String escapeHtml(String text) {
        if (text == null || text.isBlank()) {
            return "Đã xảy ra lỗi. Vui lòng thử lại.";
        }

        return text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}