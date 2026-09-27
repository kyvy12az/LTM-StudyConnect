package com.studyconnect.client.view.component;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.net.URL;

/** Reusable colors, images and painted controls for authentication views. */
public final class AuthTheme {
    public static final Color NAVY = new Color(7, 59, 112);
    public static final Color TEAL = new Color(0, 155, 131);
    public static final Color TEAL_DARK = new Color(0, 120, 102);
    public static final Color MINT = new Color(234, 248, 245);
    public static final Color BACKGROUND = new Color(245, 252, 250);
    public static final Color TEXT = new Color(18, 49, 83);
    public static final Color MUTED = new Color(113, 128, 150);
    public static final Color BORDER = new Color(207, 220, 231);
    public static final Color ERROR = new Color(199, 54, 54);
    public static final Color SUCCESS = new Color(16, 165, 87);
    private static final Image BACKGROUND_IMAGE = loadImage("/images/background.png");
    private static final Image LOGO_IMAGE = loadImage("/images/logo-final.png");

    private AuthTheme() { }
    public enum InputIcon { USER, MAIL }
    public static Font font(int style, int size) { return new Font("Segoe UI", style, size); }

    public static void installFrameIcon(JFrame frame) {
        if (LOGO_IMAGE != null) frame.setIconImage(LOGO_IMAGE.getScaledInstance(64, 32, Image.SCALE_SMOOTH));
    }

    public static JLabel createLogoLabel(int width, int height) {
        JLabel label = new JLabel();
        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        label.setHorizontalAlignment(SwingConstants.CENTER);
        label.setPreferredSize(new Dimension(width, height));
        label.setMinimumSize(new Dimension(width, height));
        label.setMaximumSize(new Dimension(width, height));
        if (LOGO_IMAGE == null) {
            label.setText("StudyConnect");
            label.setFont(font(Font.BOLD, 22));
            label.setForeground(NAVY);
        } else {
            double scale = Math.min((double) width / LOGO_IMAGE.getWidth(null),
                    (double) height / LOGO_IMAGE.getHeight(null));
            int w = (int) Math.round(LOGO_IMAGE.getWidth(null) * scale);
            int h = (int) Math.round(LOGO_IMAGE.getHeight(null) * scale);
            label.setIcon(new ImageIcon(LOGO_IMAGE.getScaledInstance(w, h, Image.SCALE_SMOOTH)));
        }
        return label;
    }

    public static JLabel createTitle(String text, int size) {
        JLabel label = new JLabel(text);
        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        label.setFont(font(Font.BOLD, size));
        label.setForeground(TEXT);
        return label;
    }

    public static JLabel createSubtitle(String text) {
        JLabel label = new JLabel(text);
        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        label.setFont(font(Font.PLAIN, 15));
        label.setForeground(MUTED);
        return label;
    }

    public static JPanel createCenteredWrapper(JComponent component, int height) {
        JPanel wrapper = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        wrapper.setOpaque(false);
        wrapper.setAlignmentX(Component.LEFT_ALIGNMENT);
        wrapper.setPreferredSize(new Dimension(1, height));
        wrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
        wrapper.add(component);
        return wrapper;
    }

    public static JLabel createFieldLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(font(Font.BOLD, 14));
        label.setForeground(TEXT);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    public static JLabel createErrorLabel() {
        JLabel label = new JLabel(" ");
        label.setFont(font(Font.PLAIN, 12));
        label.setForeground(ERROR);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        label.setPreferredSize(new Dimension(10, 20));
        label.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
        return label;
    }

    public static JButton createLinkButton(String text) {
        JButton button = new JButton(text);
        button.setFont(font(Font.BOLD, 13));
        button.setForeground(TEAL_DARK);
        button.setBorder(new EmptyBorder(3, 2, 3, 2));
        button.setOpaque(false);
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { if (button.isEnabled()) button.setForeground(NAVY); }
            public void mouseExited(MouseEvent e) { button.setForeground(TEAL_DARK); }
        });
        return button;
    }

    public static void configureCheckBox(JCheckBox checkBox) {
        checkBox.setOpaque(false);
        checkBox.setForeground(TEXT);
        checkBox.setFont(font(Font.PLAIN, 13));
        checkBox.setFocusPainted(false);
        checkBox.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        checkBox.setIcon(new CheckIcon(false));
        checkBox.setSelectedIcon(new CheckIcon(true));
        checkBox.setDisabledIcon(new CheckIcon(false));
        checkBox.setIconTextGap(9);
    }

    private static Image loadImage(String path) {
        URL resource = AuthTheme.class.getResource(path);
        return resource == null ? null : new ImageIcon(resource).getImage();
    }

    public static class RoundedPanel extends JPanel {
        private final int radius;
        public RoundedPanel(int radius) { this.radius = radius; setOpaque(false); }
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = antialias(graphics);
            int width = Math.max(0, getWidth() - 9), height = Math.max(0, getHeight() - 9);
            g.setColor(new Color(7, 59, 112, 18));
            g.fillRoundRect(5, 6, width, height, radius, radius);
            g.setColor(Color.WHITE);
            g.fillRoundRect(1, 1, width, height, radius, radius);
            g.setColor(BORDER);
            g.drawRoundRect(1, 1, width, height, radius, radius);
            g.dispose();
            super.paintComponent(graphics);
        }
    }

    public static class PrimaryButton extends JButton {
        public PrimaryButton(String text) {
            super(text);
            setFont(font(Font.BOLD, 16));
            setForeground(Color.WHITE);
            setBorder(new EmptyBorder(13, 20, 13, 20));
            setFocusPainted(false);
            setContentAreaFilled(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setPreferredSize(new Dimension(200, 50));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
            setAlignmentX(Component.LEFT_ALIGNMENT);
        }
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = antialias(graphics);
            Color fill = !isEnabled() ? new Color(139, 193, 183)
                    : getModel().isPressed() ? TEAL_DARK
                    : getModel().isRollover() ? new Color(0, 139, 116) : TEAL;
            g.setColor(fill);
            g.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
            g.dispose();
            super.paintComponent(graphics);
        }
    }

    public static class RoundedTextField extends JTextField {
        private final String placeholder;
        private final InputIcon icon;
        public RoundedTextField(String placeholder, InputIcon icon) {
            this.placeholder = placeholder;
            this.icon = icon;
            configureField(this);
        }
        protected void paintComponent(Graphics graphics) {
            paintInputBackground(graphics, this, hasFocus());
            super.paintComponent(graphics);
            paintPlaceholder(graphics, this, placeholder, getText().isEmpty());
            Graphics2D g = antialias(graphics);
            g.setColor(NAVY);
            g.setStroke(new BasicStroke(1.7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            int x = 24, y = getHeight() / 2;
            if (icon == InputIcon.MAIL) {
                g.drawRoundRect(x - 8, y - 6, 16, 12, 2, 2);
                g.drawLine(x - 7, y - 4, x, y + 1);
                g.drawLine(x, y + 1, x + 7, y - 4);
            } else {
                g.draw(new Ellipse2D.Double(x - 4, y - 8, 8, 8));
                g.drawArc(x - 8, y + 2, 16, 12, 0, 180);
            }
            g.dispose();
        }
        protected void paintBorder(Graphics graphics) { }
    }

    public static class PasswordFieldWithToggle extends JPanel {
        private final JPasswordField field = new JPasswordField();
        private final JButton toggle = new JButton();
        private boolean visible;
        public PasswordFieldWithToggle(String placeholder) {
            setLayout(new BorderLayout());
            setOpaque(false);
            setPreferredSize(new Dimension(200, 48));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
            setAlignmentX(Component.LEFT_ALIGNMENT);
            configureField(field);
            field.setBorder(new EmptyBorder(0, 47, 0, 5));
            field.setEchoChar('•');
            field.putClientProperty("placeholder", placeholder);
            toggle.setPreferredSize(new Dimension(44, 48));
            toggle.setBorder(null);
            toggle.setOpaque(false);
            toggle.setContentAreaFilled(false);
            toggle.setFocusPainted(false);
            toggle.setToolTipText("Hiện mật khẩu");
            toggle.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            toggle.addActionListener(event -> toggleVisibility());
            add(field, BorderLayout.CENTER);
            add(toggle, BorderLayout.EAST);
        }
        public char[] getPassword() { return field.getPassword(); }
        public JPasswordField getPasswordField() { return field; }
        public void setEnabled(boolean enabled) {
            super.setEnabled(enabled);
            if (field != null) { field.setEnabled(enabled); toggle.setEnabled(enabled); }
        }
        private void toggleVisibility() {
            visible = !visible;
            field.setEchoChar(visible ? (char) 0 : '•');
            toggle.setToolTipText(visible ? "Ẩn mật khẩu" : "Hiện mật khẩu");
            repaint();
            field.requestFocusInWindow();
        }
        protected void paintComponent(Graphics graphics) {
            paintInputBackground(graphics, this, field.hasFocus());
            super.paintComponent(graphics);
        }
        protected void paintChildren(Graphics graphics) {
            super.paintChildren(graphics);
            paintPasswordDecorations(graphics, this, field, visible,
                    (String) field.getClientProperty("placeholder"));
        }
    }

    public static class ImagePanel extends JPanel {
        public ImagePanel(boolean showFeatures) {
            setLayout(new BorderLayout());
            setBackground(MINT);
            setMinimumSize(new Dimension(410, 600));
            setPreferredSize(new Dimension(520, 760));
            if (showFeatures) add(createFeatureStrip(), BorderLayout.SOUTH);
        }
        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);

            Graphics2D g = (Graphics2D) graphics.create();

            g.setRenderingHint(
                    RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BICUBIC
            );

            g.setRenderingHint(
                    RenderingHints.KEY_RENDERING,
                    RenderingHints.VALUE_RENDER_QUALITY
            );

            g.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON
            );

            // Màu dự phòng khi không tìm thấy ảnh
            g.setColor(MINT);
            g.fillRect(0, 0, getWidth(), getHeight());

            if (BACKGROUND_IMAGE != null) {
                int imageWidth =
                        BACKGROUND_IMAGE.getWidth(null);

                int imageHeight =
                        BACKGROUND_IMAGE.getHeight(null);

                if (imageWidth > 0 && imageHeight > 0) {
                    /*
                     * COVER:
                     * Ảnh phủ kín toàn bộ sidebar và không bị kéo méo.
                     */
                    double scale = Math.max(
                            (double) getWidth() / imageWidth,
                            (double) getHeight() / imageHeight
                    );

                    int drawWidth = (int) Math.ceil(
                            imageWidth * scale
                    );

                    int drawHeight = (int) Math.ceil(
                            imageHeight * scale
                    );

                    /*
                     * Căn giữa ảnh.
                     */
                    int drawX =
                            (getWidth() - drawWidth) / 2;

                    int drawY =
                            (getHeight() - drawHeight) / 2;

                    g.drawImage(
                            BACKGROUND_IMAGE,
                            drawX,
                            drawY,
                            drawWidth,
                            drawHeight,
                            null
                    );
                }
            }

            g.dispose();
        }
        private JPanel createFeatureStrip() {
            JPanel strip = new JPanel(new GridLayout(1, 4, 6, 0)) {
                protected void paintComponent(Graphics graphics) {
                    Graphics2D g = (Graphics2D) graphics.create();
                    g.setColor(new Color(255, 255, 255, 225));
                    g.fillRect(0, 0, getWidth(), getHeight());
                    g.dispose();
                    super.paintComponent(graphics);
                }
            };
            strip.setOpaque(false);
            strip.setBorder(new EmptyBorder(13, 18, 12, 18));
            strip.setPreferredSize(new Dimension(100, 90));
            strip.add(feature(new FeatureIcon(FeatureIcon.Type.BOOK), "Học tập"));
            strip.add(feature(new FeatureIcon(FeatureIcon.Type.PEOPLE), "Kết nối"));
            strip.add(feature(new FeatureIcon(FeatureIcon.Type.CHART), "Chia sẻ"));
            strip.add(feature(new FeatureIcon(FeatureIcon.Type.TROPHY), "Phát triển"));
            return strip;
        }
    }

    private static JPanel feature(Icon icon, String text) {
        JPanel item = new JPanel();
        item.setOpaque(false);
        item.setLayout(new BoxLayout(item, BoxLayout.Y_AXIS));
        JLabel iconLabel = new JLabel(icon);
        iconLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel textLabel = new JLabel(text);
        textLabel.setFont(font(Font.PLAIN, 12));
        textLabel.setForeground(NAVY);
        textLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        item.add(iconLabel);
        item.add(Box.createVerticalStrut(3));
        item.add(textLabel);
        return item;
    }

    private static void configureField(JTextField field) {
        field.setFont(font(Font.PLAIN, 14));
        field.setForeground(TEXT);
        field.setCaretColor(TEAL_DARK);
        field.setOpaque(false);
        field.setBorder(new EmptyBorder(0, 47, 0, 15));
        field.setPreferredSize(new Dimension(200, 48));
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        field.setAlignmentX(Component.LEFT_ALIGNMENT);
    }

    private static void paintInputBackground(Graphics graphics, JComponent component, boolean focused) {
        Graphics2D g = antialias(graphics);
        g.setColor(component.isEnabled() ? Color.WHITE : new Color(247, 249, 250));
        g.fill(new RoundRectangle2D.Double(0.5, 0.5, Math.max(0, component.getWidth() - 1.5),
                Math.max(0, component.getHeight() - 1.5), 12, 12));
        g.setColor(focused ? TEAL : BORDER);
        g.setStroke(new BasicStroke(focused ? 1.5f : 1f));
        g.draw(new RoundRectangle2D.Double(1, 1, Math.max(0, component.getWidth() - 2.5),
                Math.max(0, component.getHeight() - 2.5), 12, 12));
        g.dispose();
    }

    private static void paintPlaceholder(Graphics graphics, JTextField field, String text, boolean empty) {
        if (!empty || field.hasFocus()) return;
        Graphics2D g = (Graphics2D) graphics.create();
        g.setFont(field.getFont());
        g.setColor(new Color(143, 155, 171));
        FontMetrics metrics = g.getFontMetrics();
        g.drawString(text, 47, (field.getHeight() - metrics.getHeight()) / 2 + metrics.getAscent());
        g.dispose();
    }

    private static void paintPasswordDecorations(Graphics graphics, JComponent component,
            JPasswordField field, boolean visible, String placeholder) {
        Graphics2D g = antialias(graphics);
        int y = component.getHeight() / 2;
        g.setColor(NAVY);
        g.setStroke(new BasicStroke(1.7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawRoundRect(17, y - 1, 14, 11, 2, 2);
        g.drawArc(19, y - 8, 10, 12, 0, 180);
        int x = component.getWidth() - 22;
        Path2D eye = new Path2D.Double();
        eye.moveTo(x - 9, y);
        eye.curveTo(x - 4, y - 7, x + 4, y - 7, x + 9, y);
        eye.curveTo(x + 4, y + 7, x - 4, y + 7, x - 9, y);
        g.draw(eye);
        g.fill(new Ellipse2D.Double(x - 2, y - 2, 4, 4));
        if (visible) g.drawLine(x - 8, y + 7, x + 8, y - 7);
        g.dispose();
        paintPlaceholder(graphics, field, placeholder, field.getPassword().length == 0);
    }

    private static Graphics2D antialias(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        return g;
    }

    private static class CheckIcon implements Icon {
        private final boolean selected;
        CheckIcon(boolean selected) { this.selected = selected; }
        public void paintIcon(Component component, Graphics graphics, int x, int y) {
            Graphics2D g = antialias(graphics);
            g.setColor(selected ? TEAL : Color.WHITE);
            g.fillRoundRect(x, y, 20, 20, 5, 5);
            g.setColor(selected ? TEAL : new Color(155, 174, 194));
            g.drawRoundRect(x, y, 19, 19, 5, 5);
            if (selected) {
                g.setColor(Color.WHITE);
                g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.drawLine(x + 5, y + 10, x + 9, y + 14);
                g.drawLine(x + 9, y + 14, x + 16, y + 6);
            }
            g.dispose();
        }
        public int getIconWidth() { return 20; }
        public int getIconHeight() { return 20; }
    }
}
