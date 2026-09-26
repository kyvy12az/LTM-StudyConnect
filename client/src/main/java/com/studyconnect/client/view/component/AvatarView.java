package com.studyconnect.client.view.component;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.net.URL;

public class AvatarView extends JComponent {

    private final String displayName;
    private final int avatarSize;

    private BufferedImage avatarImage;
    private boolean online;

    public AvatarView(
            String displayName,
            int avatarSize
    ) {
        this.displayName = displayName == null
                ? ""
                : displayName.trim();

        this.avatarSize = avatarSize;

        Dimension componentSize = new Dimension(
                avatarSize + 8,
                avatarSize + 8
        );

        setPreferredSize(componentSize);
        setMinimumSize(componentSize);
        setMaximumSize(componentSize);
        setOpaque(false);
    }

    public void setOnline(boolean online) {
        this.online = online;
        repaint();
    }

    public boolean isOnline() {
        return online;
    }

    /**
     * Gán ảnh avatar đã được tải sẵn.
     */
    public void setAvatarImage(
            BufferedImage avatarImage
    ) {
        this.avatarImage = avatarImage;
        repaint();
    }

    /**
     * Tải avatar từ resources, ví dụ:
     * /images/avatar-default.png
     */
    public void setAvatarResource(
            String resourcePath
    ) {
        if (resourcePath == null
                || resourcePath.isBlank()) {
            avatarImage = null;
            repaint();
            return;
        }

        URL resource = AvatarView.class.getResource(
                resourcePath
        );

        if (resource == null) {
            System.err.println(
                    "Không tìm thấy avatar: "
                            + resourcePath
            );

            avatarImage = null;
            repaint();
            return;
        }

        try {
            avatarImage = ImageIO.read(resource);
        } catch (IOException exception) {
            avatarImage = null;

            System.err.println(
                    "Không thể đọc avatar: "
                            + exception.getMessage()
            );
        }

        repaint();
    }

    /**
     * Tải avatar từ đường dẫn URL.
     * Việc tải được chạy nền để không làm treo Swing.
     */
    public void setAvatarUrl(String avatarUrl) {
        if (avatarUrl == null
                || avatarUrl.isBlank()) {
            avatarImage = null;
            repaint();
            return;
        }

        SwingWorker<BufferedImage, Void> worker =
                new SwingWorker<>() {

                    @Override
                    protected BufferedImage doInBackground()
                            throws Exception {
                        URL url = URI.create(
                                avatarUrl.trim()
                        ).toURL();

                        return ImageIO.read(url);
                    }

                    @Override
                    protected void done() {
                        try {
                            avatarImage = get();
                        } catch (Exception exception) {
                            avatarImage = null;

                            System.err.println(
                                    "Không thể tải avatar: "
                                            + exception.getMessage()
                            );
                        }

                        repaint();
                    }
                };

        worker.execute();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);

        Graphics2D g2 =
                (Graphics2D) graphics.create();

        g2.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON
        );

        g2.setRenderingHint(
                RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BICUBIC
        );

        g2.setRenderingHint(
                RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_QUALITY
        );

        int x = Math.max(
                0,
                (getWidth() - avatarSize) / 2
        );

        int y = Math.max(
                0,
                (getHeight() - avatarSize) / 2
        );

        Shape avatarShape = new Ellipse2D.Double(
                x,
                y,
                avatarSize,
                avatarSize
        );

        // Nền avatar
        g2.setColor(new Color(188, 231, 239));
        g2.fill(avatarShape);

        if (avatarImage != null) {
            Shape oldClip = g2.getClip();
            g2.setClip(avatarShape);

            drawCoverImage(
                    g2,
                    avatarImage,
                    x,
                    y,
                    avatarSize
            );

            g2.setClip(oldClip);
        } else {
            paintInitials(
                    g2,
                    x,
                    y
            );
        }

        // Viền trắng
        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(2f));
        g2.draw(avatarShape);

        if (online) {
            paintOnlineIndicator(
                    g2,
                    x,
                    y
            );
        }

        g2.dispose();
    }

    private void drawCoverImage(
            Graphics2D g2,
            BufferedImage image,
            int x,
            int y,
            int size
    ) {
        double scale = Math.max(
                (double) size / image.getWidth(),
                (double) size / image.getHeight()
        );

        int drawWidth = (int) Math.round(
                image.getWidth() * scale
        );

        int drawHeight = (int) Math.round(
                image.getHeight() * scale
        );

        int drawX = x + (size - drawWidth) / 2;
        int drawY = y + (size - drawHeight) / 2;

        g2.drawImage(
                image,
                drawX,
                drawY,
                drawWidth,
                drawHeight,
                null
        );
    }

    private void paintInitials(
            Graphics2D g2,
            int x,
            int y
    ) {
        String initials = createInitials(
                displayName
        );

        int fontSize = Math.max(
                12,
                avatarSize / 3
        );

        g2.setFont(
                MainTheme.font(
                        Font.BOLD,
                        fontSize
                )
        );

        g2.setColor(MainTheme.NAVY);

        FontMetrics metrics =
                g2.getFontMetrics();

        int textX = x
                + (avatarSize
                - metrics.stringWidth(initials)) / 2;

        int textY = y
                + (avatarSize
                - metrics.getHeight()) / 2
                + metrics.getAscent();

        g2.drawString(
                initials,
                textX,
                textY
        );
    }

    private void paintOnlineIndicator(
            Graphics2D g2,
            int x,
            int y
    ) {
        int indicatorSize = Math.max(
                10,
                avatarSize / 5
        );

        int indicatorX =
                x + avatarSize - indicatorSize + 2;

        int indicatorY =
                y + avatarSize - indicatorSize + 2;

        g2.setColor(Color.WHITE);

        g2.fillOval(
                indicatorX - 2,
                indicatorY - 2,
                indicatorSize + 4,
                indicatorSize + 4
        );

        g2.setColor(MainTheme.SUCCESS);

        g2.fillOval(
                indicatorX,
                indicatorY,
                indicatorSize,
                indicatorSize
        );
    }

    private String createInitials(String name) {
        if (name == null || name.isBlank()) {
            return "?";
        }

        String[] words = name
                .trim()
                .split("\\s+");

        if (words.length == 1) {
            String word = words[0];

            return word.substring(
                    0,
                    Math.min(2, word.length())
            ).toUpperCase();
        }

        String first =
                words[0].substring(0, 1);

        String last =
                words[words.length - 1]
                        .substring(0, 1);

        return (first + last).toUpperCase();
    }
}