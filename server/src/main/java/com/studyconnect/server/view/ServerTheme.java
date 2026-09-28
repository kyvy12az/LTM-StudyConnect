package com.studyconnect.server.view;

import com.formdev.flatlaf.extras.FlatSVGIcon;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;

public final class ServerTheme {
    public static final Color NAVY = new Color(8, 43, 79);
    public static final Color TEAL = new Color(0, 150, 119);
    public static final Color TEAL_DARK = new Color(0, 111, 91);
    public static final Color MINT = new Color(235, 249, 245);
    public static final Color BACKGROUND = new Color(245, 250, 252);
    public static final Color TEXT = new Color(22, 48, 77);
    public static final Color MUTED = new Color(105, 123, 145);
    public static final Color BORDER = new Color(211, 224, 232);
    public static final Color SUCCESS = new Color(13, 174, 91);
    public static final Color DANGER = new Color(220, 53, 69);

    private ServerTheme() {
    }

    public static Font font(int style, int size) {
        return new Font("Segoe UI", style, size);
    }

    public static Icon icon(String name, int size, Color color) {
        URL resource = ServerTheme.class.getResource(
                "/server-icons/" + name + ".svg"
        );
        if (resource == null) {
            return UIManager.getIcon("Tree.leafIcon");
        }
        FlatSVGIcon icon = new FlatSVGIcon(resource).derive(size, size);
        icon.setColorFilter(new FlatSVGIcon.ColorFilter(ignored -> color));
        return icon;
    }

    public static Icon imageIcon(
            String resourcePath,
            int width,
            int height
    ) {
        URL resource = ServerTheme.class.getResource(
                resourcePath
        );

        if (resource == null) {
            System.err.println(
                    "Không tìm thấy hình ảnh: "
                            + resourcePath
            );

            return UIManager.getIcon(
                    "OptionPane.informationIcon"
            );
        }

        try {
            BufferedImage source = ImageIO.read(resource);

            if (source == null) {
                throw new IOException(
                        "Không thể đọc hình ảnh"
                );
            }

            BufferedImage scaled = new BufferedImage(
                    width,
                    height,
                    BufferedImage.TYPE_INT_ARGB
            );

            Graphics2D graphics = scaled.createGraphics();

            graphics.setRenderingHint(
                    RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BICUBIC
            );
            graphics.setRenderingHint(
                    RenderingHints.KEY_RENDERING,
                    RenderingHints.VALUE_RENDER_QUALITY
            );
            graphics.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON
            );

            double scale = Math.min(
                    (double) width / source.getWidth(),
                    (double) height / source.getHeight()
            );

            int scaledWidth = Math.max(
                    1,
                    (int) Math.round(
                            source.getWidth() * scale
                    )
            );

            int scaledHeight = Math.max(
                    1,
                    (int) Math.round(
                            source.getHeight() * scale
                    )
            );

            int x = (width - scaledWidth) / 2;
            int y = (height - scaledHeight) / 2;

            graphics.drawImage(
                    source,
                    x,
                    y,
                    scaledWidth,
                    scaledHeight,
                    null
            );

            graphics.dispose();

            return new ImageIcon(scaled);

        } catch (IOException exception) {
            System.err.println(
                    "Không thể tải hình ảnh "
                            + resourcePath
                            + ": "
                            + exception.getMessage()
            );

            return UIManager.getIcon(
                    "OptionPane.informationIcon"
            );
        }
    }

    public static JPanel card(LayoutManager layout) {
        JPanel panel = new JPanel(layout);
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                new EmptyBorder(14, 16, 14, 16)
        ));
        return panel;
    }

    public static JButton primaryButton(String text) {
        JButton button = new JButton(text);
        button.setFont(font(Font.BOLD, 13));
        button.setForeground(Color.WHITE);
        button.setBackground(TEAL);
        button.setFocusPainted(false);
        button.setBorder(new EmptyBorder(9, 16, 9, 16));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

    public static JButton secondaryButton(String text) {
        JButton button = new JButton(text);
        button.setFont(font(Font.BOLD, 13));
        button.setForeground(NAVY);
        button.setBackground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                new EmptyBorder(8, 14, 8, 14)
        ));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

    public static void configureTable(JTable table) {
        table.setRowHeight(34);
        table.setShowVerticalLines(false);
        table.setGridColor(new Color(231, 238, 243));
        table.setSelectionBackground(new Color(220, 245, 238));
        table.setSelectionForeground(TEXT);
        table.setFont(font(Font.PLAIN, 13));
        table.getTableHeader().setFont(font(Font.BOLD, 12));
        table.getTableHeader().setBackground(new Color(238, 246, 249));
        table.getTableHeader().setForeground(NAVY);
        table.setFillsViewportHeight(true);
    }
}
