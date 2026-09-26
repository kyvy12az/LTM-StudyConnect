package com.studyconnect.client.view.auth;

import com.studyconnect.client.view.component.AuthTheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionListener;

public class RegisterFrame extends JFrame {
    private final AuthTheme.RoundedTextField fullNameField = new AuthTheme.RoundedTextField(
            "Nhập họ và tên", AuthTheme.InputIcon.USER);
    private final AuthTheme.RoundedTextField usernameField = new AuthTheme.RoundedTextField(
            "Nhập tên đăng nhập", AuthTheme.InputIcon.USER);
    private final AuthTheme.RoundedTextField emailField = new AuthTheme.RoundedTextField(
            "Nhập email của bạn", AuthTheme.InputIcon.MAIL);
    private final AuthTheme.PasswordFieldWithToggle passwordField =
            new AuthTheme.PasswordFieldWithToggle("Nhập mật khẩu");
    private final AuthTheme.PasswordFieldWithToggle confirmPasswordField =
            new AuthTheme.PasswordFieldWithToggle("Nhập lại mật khẩu");
    private final JCheckBox termsCheckBox = new JCheckBox("Tôi đồng ý với Điều khoản sử dụng");
    private final AuthTheme.PrimaryButton registerButton = new AuthTheme.PrimaryButton("Đăng ký");
    private final JButton loginButton = AuthTheme.createLinkButton("Đăng nhập");
    private final JLabel errorLabel = AuthTheme.createErrorLabel();

    public RegisterFrame() {
        setTitle("StudyConnect - Đăng ký");
        setSize(1100, 760);
        setMinimumSize(new Dimension(960, 680));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        AuthTheme.installFrameIcon(this);
        initializeUI();
    }

    private void initializeUI() {
        JPanel root = new JPanel(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridy = 0;
        constraints.fill = GridBagConstraints.BOTH;
        constraints.weighty = 1;
        constraints.gridx = 0;
        constraints.weightx = 0.48;
        root.add(new AuthTheme.ImagePanel(false), constraints);
        constraints.gridx = 1;
        constraints.weightx = 0.52;
        root.add(createFormArea(), constraints);
        setContentPane(root);
        getRootPane().setDefaultButton(registerButton);
    }

    private JPanel createFormArea() {
        JPanel area = new JPanel(new BorderLayout());
        area.setBackground(AuthTheme.BACKGROUND);
        area.setPreferredSize(new Dimension(580, 760));
        area.setMinimumSize(new Dimension(500, 600));

        JPanel cardHolder = new JPanel(new GridBagLayout());
        cardHolder.setBackground(AuthTheme.BACKGROUND);
        cardHolder.setBorder(new EmptyBorder(20, 18, 20, 18));
        cardHolder.add(createCard());

        JScrollPane scrollPane = new JScrollPane(cardHolder);
        scrollPane.setBorder(null);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(7, 0));
        area.add(scrollPane, BorderLayout.CENTER);
        return area;
    }

    private JPanel createCard() {
        AuthTheme.RoundedPanel card = new AuthTheme.RoundedPanel(28);
        card.setLayout(new GridBagLayout());
        card.setPreferredSize(new Dimension(455, 705));
        card.setMinimumSize(new Dimension(415, 690));

        JPanel contentPanel = new JPanel();
        contentPanel.setOpaque(false);
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setPreferredSize(new Dimension(365, 655));
        contentPanel.setMinimumSize(new Dimension(350, 650));
        contentPanel.setMaximumSize(new Dimension(365, 655));

        JLabel logoLabel = AuthTheme.createLogoLabel(145, 60);
        contentPanel.add(AuthTheme.createCenteredWrapper(logoLabel, 60));

        JLabel titleLabel = AuthTheme.createTitle("Tạo tài khoản", 27);
        contentPanel.add(AuthTheme.createCenteredWrapper(titleLabel, 34));
        contentPanel.add(Box.createVerticalStrut(2));

        JLabel subtitleLabel = AuthTheme.createSubtitle("Tham gia cộng đồng học tập StudyConnect");
        contentPanel.add(AuthTheme.createCenteredWrapper(subtitleLabel, 22));
        contentPanel.add(Box.createVerticalStrut(10));

        addField(contentPanel, "Họ và tên", fullNameField, 6);
        addField(contentPanel, "Tên đăng nhập", usernameField, 6);
        addField(contentPanel, "Email", emailField, 6);
        addField(contentPanel, "Mật khẩu", passwordField, 0);

        JLabel hint = new JLabel("Tối thiểu 6 ký tự");
        hint.setFont(AuthTheme.font(Font.PLAIN, 12));
        hint.setForeground(AuthTheme.MUTED);
        hint.setAlignmentX(Component.LEFT_ALIGNMENT);
        hint.setMaximumSize(new Dimension(Integer.MAX_VALUE, hint.getPreferredSize().height));
        contentPanel.add(hint);
        contentPanel.add(Box.createVerticalStrut(4));
        addField(contentPanel, "Xác nhận mật khẩu", confirmPasswordField, 5);

        AuthTheme.configureCheckBox(termsCheckBox);
        termsCheckBox.setFont(AuthTheme.font(Font.PLAIN, 12));
        termsCheckBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        termsCheckBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
        contentPanel.add(termsCheckBox);

        errorLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        errorLabel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
        contentPanel.add(errorLabel);

        registerButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        registerButton.setPreferredSize(new Dimension(365, 50));
        registerButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        contentPanel.add(registerButton);
        contentPanel.add(Box.createVerticalStrut(10));

        JSeparator separator = new JSeparator();
        separator.setForeground(AuthTheme.BORDER);
        separator.setAlignmentX(Component.LEFT_ALIGNMENT);
        separator.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        contentPanel.add(separator);
        contentPanel.add(Box.createVerticalStrut(8));

        JPanel loginPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 0));
        loginPanel.setOpaque(false);
        loginPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        loginPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        JLabel prompt = new JLabel("Đã có tài khoản?");
        prompt.setFont(AuthTheme.font(Font.PLAIN, 13));
        prompt.setForeground(AuthTheme.TEXT);
        loginPanel.add(prompt);
        loginPanel.add(loginButton);
        contentPanel.add(loginPanel);

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.anchor = GridBagConstraints.CENTER;
        card.add(contentPanel, constraints);
        return card;
    }
    private void addField(JPanel panel, String labelText, JComponent field, int gap) {
        JLabel label = AuthTheme.createFieldLabel(labelText);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        label.setMaximumSize(new Dimension(Integer.MAX_VALUE, label.getPreferredSize().height));
        field.setAlignmentX(Component.LEFT_ALIGNMENT);
        field.setPreferredSize(new Dimension(365, 48));
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        panel.add(label);
        panel.add(Box.createVerticalStrut(4));
        panel.add(field);
        panel.add(Box.createVerticalStrut(gap));
    }

    public String getFullName() { return fullNameField.getText().trim(); }
    public String getUsername() { return usernameField.getText().trim(); }
    public String getEmail() { return emailField.getText().trim(); }
    public String getPasswordText() { return new String(passwordField.getPassword()); }
    public String getConfirmPassword() { return new String(confirmPasswordField.getPassword()); }
    public boolean isTermsAccepted() { return termsCheckBox.isSelected(); }
    public void addRegisterListener(ActionListener listener) { registerButton.addActionListener(listener); }
    public void addLoginListener(ActionListener listener) { loginButton.addActionListener(listener); }

    public void setLoading(boolean loading) {
        registerButton.setEnabled(!loading);
        loginButton.setEnabled(!loading);
        fullNameField.setEnabled(!loading);
        usernameField.setEnabled(!loading);
        emailField.setEnabled(!loading);
        passwordField.setEnabled(!loading);
        confirmPasswordField.setEnabled(!loading);
        termsCheckBox.setEnabled(!loading);
        registerButton.setText(loading ? "Đang đăng ký..." : "Đăng ký");
        if (loading) errorLabel.setText(" ");
    }

    public void showError(String message) {
        errorLabel.setText("<html>" + escapeHtml(message) + "</html>");
    }

    public void showSuccess(String message) {
        JOptionPane.showMessageDialog(this, message, "Đăng ký thành công", JOptionPane.INFORMATION_MESSAGE);
    }

    private String escapeHtml(String text) {
        if (text == null || text.isBlank()) return "Đã xảy ra lỗi. Vui lòng thử lại.";
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
