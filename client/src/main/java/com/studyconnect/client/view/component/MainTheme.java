package com.studyconnect.client.view.component;

import com.formdev.flatlaf.extras.FlatSVGIcon;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.net.URL;

public final class MainTheme {
    public static final Color NAVY = new Color(7, 59, 112);
    public static final Color TEAL = new Color(0, 155, 131);
    public static final Color TEAL_DARK = new Color(0, 120, 102);
    public static final Color MINT = new Color(234, 248, 245);
    public static final Color BACKGROUND = new Color(245, 252, 250);
    public static final Color TEXT = new Color(18, 49, 83);
    public static final Color MUTED = new Color(113, 128, 150);
    public static final Color BORDER = new Color(207, 220, 231);
    public static final Color SUCCESS = new Color(16, 165, 87);
    public static final Color DANGER = new Color(239, 68, 68);

    private MainTheme() { }
    public static Font font(int style, int size) { return new Font("Segoe UI", style, size); }

    public enum IconType {

        HOME("home.svg"),
        CHAT("message-circle.svg"),
        GROUP("group.svg"),
        DOCUMENT("file.svg"),
        USER("user.svg"),
        SEARCH("search.svg"),
        BELL("bell.svg"),
        IMAGE("image.svg"),
        POLL("chart-bar.svg"),
        SMILE("smile.svg"),
        SEND("send.svg"),
        EDIT("square-pen.svg"),
        SHARE("share-2.svg"),
        MORE("ellipsis.svg"),
        LIKE("thumbs-up.svg"),
        COMMENT("message-circle.svg"),
        ATTACH("paperclip.svg"),
        VIDEO("video.svg"),
        PHONE("phone.svg"),
        CALENDAR("calendar-days.svg"),
        SUN("sun.svg"),
        GRADUATION_CAP("graduation-cap.svg");

        private final String fileName;

        IconType(String fileName) {
            this.fileName = fileName;
        }

        public String getResourcePath() {
            return "/icons/" + fileName;
        }
    }

    public static class RoundedPanel extends JPanel {
        private final int radius;
        private Color fill = Color.WHITE;
        public RoundedPanel(int radius) { this.radius = radius; setOpaque(false); }
        public void setFill(Color fill) { this.fill = fill; repaint(); }
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = quality(graphics);
            g.setColor(new Color(7, 59, 112, 12));
            g.fillRoundRect(3, 4, Math.max(0, getWidth() - 5), Math.max(0, getHeight() - 5), radius, radius);
            g.setColor(fill);
            g.fillRoundRect(0, 0, Math.max(0, getWidth() - 4), Math.max(0, getHeight() - 4), radius, radius);
            g.setColor(BORDER);
            g.drawRoundRect(0, 0, Math.max(0, getWidth() - 5), Math.max(0, getHeight() - 5), radius, radius);
            g.dispose();
            super.paintComponent(graphics);
        }
    }

    public static Icon svgIcon(
            IconType type,
            int size,
            Color color
    ) {
        URL resource = MainTheme.class.getResource(
                type.getResourcePath()
        );

        if (resource == null) {
            System.err.println(
                    "Không tìm thấy icon: "
                            + type.getResourcePath()
            );

            /*
             * Trả về icon rỗng để ứng dụng không bị crash.
             */
            return new EmptyIcon(size, size);
        }

        FlatSVGIcon icon = new FlatSVGIcon(resource)
                .derive(size, size);

        /*
         * Chuyển toàn bộ màu trong SVG thành màu yêu cầu.
         */
        icon.setColorFilter(
                new FlatSVGIcon.ColorFilter(
                        originalColor -> color
                )
        );

        return icon;
    }

    public static JButton iconButton(
            IconType type,
            int size,
            Color color,
            String tooltip
    ) {
        JButton button = new JButton(
                svgIcon(type, size, color)
        );

        button.setToolTipText(tooltip);
        button.setBorder(
                new EmptyBorder(7, 9, 7, 9)
        );

        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setOpaque(false);

        button.setCursor(
                Cursor.getPredefinedCursor(
                        Cursor.HAND_CURSOR
                )
        );

        button.putClientProperty(
                "JButton.buttonType",
                "borderless"
        );

        return button;
    }

    public static JButton textButton(String text, boolean primary) {
        JButton button = new JButton(text) {
            protected void paintComponent(Graphics graphics) {
                if (primary) {
                    Graphics2D g = quality(graphics);
                    g.setColor(getModel().isRollover() ? TEAL_DARK : TEAL);
                    g.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                    g.dispose();
                }
                super.paintComponent(graphics);
            }
        };
        button.setFont(font(Font.BOLD, 14));
        button.setForeground(primary ? Color.WHITE : TEAL_DARK);
        button.setBorder(new EmptyBorder(10, 16, 10, 16));
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

    public static JScrollPane scrollPane(Component view) {
        JScrollPane scroll = new JScrollPane(view);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(18);
        scroll.getVerticalScrollBar().setPreferredSize(new Dimension(7, 0));
        return scroll;
    }

    public static class SearchField extends JTextField {

        private final String placeholder;
        private final Icon searchIcon;

        public SearchField(String placeholder) {
            this.placeholder = placeholder;

            this.searchIcon = svgIcon(
                    IconType.SEARCH,
                    16,
                    NAVY
            );

            setFont(font(Font.PLAIN, 14));
            setForeground(TEXT);
            setCaretColor(TEAL);
            setOpaque(false);

            /*
             * Chừa khoảng trống bên trái cho SVG icon.
             */
            setBorder(
                    new EmptyBorder(0, 46, 0, 15)
            );

            setPreferredSize(
                    new Dimension(600, 42)
            );
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = quality(graphics);

            /*
             * Background.
             */
            g.setColor(Color.WHITE);

            g.fill(
                    new RoundRectangle2D.Double(
                            0.5,
                            0.5,
                            Math.max(0, getWidth() - 1.5),
                            Math.max(0, getHeight() - 1.5),
                            14,
                            14
                    )
            );

            /*
             * Border.
             */
            g.setColor(
                    hasFocus()
                            ? TEAL
                            : BORDER
            );

            g.draw(
                    new RoundRectangle2D.Double(
                            1,
                            1,
                            Math.max(0, getWidth() - 2.5),
                            Math.max(0, getHeight() - 2.5),
                            14,
                            14
                    )
            );

            g.dispose();

            /*
             * Vẽ text và caret của JTextField.
             */
            super.paintComponent(graphics);

            /*
             * Vẽ SVG search icon.
             */
            int iconX = 16;

            int iconY =
                    (getHeight()
                            - searchIcon.getIconHeight())
                            / 2;

            searchIcon.paintIcon(
                    this,
                    graphics,
                    iconX,
                    iconY
            );

            /*
             * Placeholder.
             */
            if (getText().isEmpty() && !hasFocus()) {
                Graphics2D placeholderGraphics =
                        quality(graphics);

                placeholderGraphics.setFont(getFont());
                placeholderGraphics.setColor(MUTED);

                FontMetrics metrics =
                        placeholderGraphics.getFontMetrics();

                int textY =
                        (getHeight() - metrics.getHeight())
                                / 2
                                + metrics.getAscent();

                placeholderGraphics.drawString(
                        placeholder,
                        46,
                        textY
                );

                placeholderGraphics.dispose();
            }
        }

        @Override
        protected void paintBorder(Graphics graphics) {
            // Border đã được vẽ trong paintComponent().
        }
    }

    public static class VerticalPanel extends JPanel implements Scrollable {
        public VerticalPanel() { setLayout(new BoxLayout(this, BoxLayout.Y_AXIS)); setOpaque(false); }
        public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        public int getScrollableUnitIncrement(Rectangle r, int o, int d) { return 18; }
        public int getScrollableBlockIncrement(Rectangle r, int o, int d) { return 100; }
        public boolean getScrollableTracksViewportWidth() { return true; }
        public boolean getScrollableTracksViewportHeight() { return false; }
    }

//    public static class LineIcon implements Icon {
//        private final IconType type; private final int size; private final Color color;
//        public LineIcon(IconType type, int size, Color color) { this.type = type; this.size = size; this.color = color; }
//        public int getIconWidth() { return size; }
//        public int getIconHeight() { return size; }
//        public void paintIcon(Component c, Graphics graphics, int x, int y) {
//            Graphics2D g = quality(graphics); g.translate(x, y); g.setColor(color);
//            g.setStroke(new BasicStroke(Math.max(1.6f, size / 12f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
//            int s = size, m = s / 2;
//            switch (type) {
//                case HOME -> { Path2D p=new Path2D.Double(); p.moveTo(2,m); p.lineTo(m,2); p.lineTo(s-2,m); g.draw(p); g.drawRect(5,m-1,s-10,m-2); }
//                case CHAT -> { g.drawRoundRect(2,3,s-4,s-8,7,7); g.drawLine(7,s-5,4,s-1); }
//                case GROUP -> { g.drawOval(m-4,2,8,8); g.drawOval(1,6,7,7); g.drawOval(s-8,6,7,7); g.drawArc(4,11,s-8,s-5,0,180); }
//                case DOCUMENT -> { g.drawRect(4,2,s-8,s-4); g.drawLine(s-8,2,s-4,7); }
//                case USER -> { g.drawOval(m-5,2,10,10); g.drawArc(3,12,s-6,s-8,0,180); }
//                case BELL -> { g.drawArc(5,3,s-10,s-7,0,180); g.drawLine(5,m,3,s-5); g.drawLine(s-5,m,s-3,s-5); g.drawLine(3,s-5,s-3,s-5); }
//                case IMAGE -> { g.drawRect(2,3,s-4,s-6); g.drawOval(s-8,6,3,3); g.drawLine(4,s-5,m,m); g.drawLine(m,m,s-4,s-5); }
//                case POLL -> { g.drawLine(4,s-3,4,m); g.drawLine(m,s-3,m,4); g.drawLine(s-4,s-3,s-4,8); }
//                case SMILE -> { g.drawOval(2,2,s-4,s-4); g.fillOval(7,8,2,2); g.fillOval(s-9,8,2,2); g.drawArc(7,8,s-14,s-10,190,160); }
//                case SEND -> { Path2D p=new Path2D.Double(); p.moveTo(2,3); p.lineTo(s-2,m); p.lineTo(2,s-3); p.lineTo(6,m); p.closePath(); g.draw(p); g.drawLine(6,m,s-3,m); }
//                case EDIT -> { g.drawRect(3,6,s-8,s-8); g.drawLine(8,s-5,s-3,3); }
//                case SHARE -> { g.drawArc(2,7,s-8,s-8,40,150); g.drawLine(s-9,5,s-3,6); g.drawLine(s-3,6,s-5,12); }
//                case MORE -> { g.fillOval(2,m-1,3,3); g.fillOval(m-1,m-1,3,3); g.fillOval(s-5,m-1,3,3); }
//                case LIKE -> { g.drawRect(2,m,5,s-m-3); g.drawLine(8,s-3,s-4,s-3); g.drawLine(s-4,s-3,s-2,m); g.drawLine(s-2,m,m,m); g.drawLine(m,m,m-1,3); }
//                case COMMENT -> { g.drawOval(2,3,s-4,s-8); g.drawLine(7,s-5,4,s-1); }
//                case ATTACH -> { g.drawArc(5,2,s-8,s-4,70,230); g.drawArc(2,5,s-7,s-7,250,230); }
//                case VIDEO -> { g.drawRoundRect(1,5,s-8,s-10,4,4); g.drawLine(s-7,m,s-1,6); g.drawLine(s-1,6,s-1,s-6); g.drawLine(s-1,s-6,s-7,m); }
//                case PHONE -> { g.drawArc(4,2,s-8,s-4,110,140); g.drawLine(5,4,2,7); g.drawLine(s-5,s-4,s-2,s-7); }
//                case CALENDAR -> { g.drawRect(2,5,s-4,s-7); g.drawLine(2,10,s-2,10); g.drawLine(7,2,7,7); g.drawLine(s-7,2,s-7,7); }
//                default -> { }
//            }
//            g.dispose();
//        }
//    }

    private static class EmptyIcon implements Icon {

        private final int width;
        private final int height;

        private EmptyIcon(int width, int height) {
            this.width = width;
            this.height = height;
        }

        @Override
        public int getIconWidth() {
            return width;
        }

        @Override
        public int getIconHeight() {
            return height;
        }

        @Override
        public void paintIcon(
                Component component,
                Graphics graphics,
                int x,
                int y
        ) {
            // Không vẽ gì khi thiếu resource.
        }
    }

    public static void installHover(JComponent component) {
        component.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { component.repaint(); }
            public void mouseExited(MouseEvent e) { component.repaint(); }
        });
    }

    private static Graphics2D quality(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        return g;
    }
}
