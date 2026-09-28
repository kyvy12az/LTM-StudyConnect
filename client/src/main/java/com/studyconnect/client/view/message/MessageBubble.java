package com.studyconnect.client.view.message;

import com.studyconnect.client.view.component.AvatarView;
import com.studyconnect.client.view.component.MainTheme;
import com.studyconnect.common.dto.MessageDTO;
import com.studyconnect.common.dto.MessageDeliveryMode;
import com.studyconnect.common.dto.MessageStatus;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class MessageBubble extends JPanel {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
    private static final Color OUTGOING = new Color(0, 166, 139);
    private static final Color OUTGOING_HOVER = new Color(0, 135, 112);
    private static final Color INCOMING = new Color(228, 230, 235);
    private static final Color INCOMING_HOVER = new Color(216, 218, 223);
    private static final int MAX_TEXT_WIDTH = 360;
    private static final int MIN_TEXT_WIDTH = 34;

    public MessageBubble(
            MessageDTO message,
            boolean mine
    ) {
        super(new BorderLayout());

        setOpaque(false);

        setAlignmentX(Component.CENTER_ALIGNMENT);

        setMaximumSize(
                new Dimension(
                        Integer.MAX_VALUE,
                        Integer.MAX_VALUE
                )
        );

        setBorder(new EmptyBorder(2, 4, 2, 4));

        JPanel unit = new JPanel(
                new BorderLayout(7, 0)
        );
        unit.setOpaque(false);

        if (!mine) {
            JPanel avatarSlot = new JPanel(
                    new BorderLayout()
            );

            avatarSlot.setOpaque(false);
            avatarSlot.setBorder(
                    new EmptyBorder(0, 0, 18, 0)
            );

            AvatarView avatar = createAvatar(message);

            avatarSlot.add(
                    avatar,
                    BorderLayout.SOUTH
            );

            unit.add(
                    avatarSlot,
                    BorderLayout.WEST
            );
        }

        JPanel stack = new JPanel();
        stack.setOpaque(false);
        stack.setLayout(
                new BoxLayout(
                        stack,
                        BoxLayout.Y_AXIS
                )
        );

        BubblePanel bubble = new BubblePanel(mine);
        bubble.setLayout(new BorderLayout());
        bubble.setBorder(
                new EmptyBorder(9, 13, 9, 13)
        );

        bubble.setAlignmentX(
                mine
                        ? Component.RIGHT_ALIGNMENT
                        : Component.LEFT_ALIGNMENT
        );

        JTextArea content = createContent(
                message,
                mine
        );

        bubble.add(content, BorderLayout.CENTER);

        String tooltip = deliveryTooltip(message);
        bubble.setToolTipText(tooltip);
        content.setToolTipText(tooltip);

        stack.add(bubble);
        stack.add(Box.createVerticalStrut(3));

        JLabel metadata = new JLabel(
                metadataText(message, mine),
                mine
                        ? JLabel.RIGHT
                        : JLabel.LEFT
        );

        metadata.setFont(
                MainTheme.font(Font.PLAIN, 10)
        );

        metadata.setForeground(
                message.getStatus() == MessageStatus.FAILED
                        ? MainTheme.DANGER
                        : MainTheme.MUTED
        );

        metadata.setAlignmentX(
                mine
                        ? Component.RIGHT_ALIGNMENT
                        : Component.LEFT_ALIGNMENT
        );

        stack.add(metadata);
        unit.add(stack, BorderLayout.CENTER);

        add(
                unit,
                mine
                        ? BorderLayout.EAST
                        : BorderLayout.WEST
        );

        int rowHeight = unit.getPreferredSize().height + 8;

        setPreferredSize(
                new Dimension(0, rowHeight)
        );

        setMinimumSize(
                new Dimension(0, rowHeight)
        );

        setMaximumSize(
                new Dimension(
                        Integer.MAX_VALUE,
                        rowHeight
                )
        );
    }

    private JTextArea createContent(MessageDTO message, boolean mine) {
        String text = message.getContent() == null ? "" : message.getContent();
        JTextArea area = new JTextArea(text);
        area.setEditable(false);
        area.setFocusable(true);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setOpaque(false);
        area.setBorder(null);
        area.setFont(MainTheme.font(Font.PLAIN, 14));
        area.setForeground(mine ? Color.WHITE : new Color(5, 5, 5));
        area.setCaretColor(area.getForeground());
        area.setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));

        int width = calculateTextWidth(area, text);
        area.setSize(new Dimension(width, Short.MAX_VALUE));
        Dimension preferred = area.getPreferredSize();
        area.setPreferredSize(new Dimension(width, preferred.height));
        area.setMaximumSize(new Dimension(width, preferred.height));
        return area;
    }

    private int calculateTextWidth(JTextArea area, String text) {
        FontMetrics metrics = area.getFontMetrics(area.getFont());
        int longestLine = MIN_TEXT_WIDTH;
        String[] lines = text.split("\\R", -1);
        for (String line : lines) {
            longestLine = Math.max(longestLine, metrics.stringWidth(line));
        }
        int naturalWidth = longestLine + 4;
        if (text.length() > 55) naturalWidth = MAX_TEXT_WIDTH;
        return Math.max(MIN_TEXT_WIDTH, Math.min(MAX_TEXT_WIDTH, naturalWidth));
    }

    private AvatarView createAvatar(MessageDTO message) {
        String name = message.getSenderName();
        if (name == null || name.isBlank()) name = "Người dùng";
        AvatarView avatar = new AvatarView(name, 30);
        String url = message.getSenderAvatarUrl();
        if (url == null || url.isBlank()) {
            avatar.setAvatarResource("/images/default-avatar.png");
        } else if (url.startsWith("/")) {
            avatar.setAvatarResource(url);
        } else {
            avatar.setAvatarUrl(url);
        }
        return avatar;
    }

    private String metadataText(MessageDTO message, boolean mine) {
        String time = formatTime(message.getCreatedAt());
        if (!mine) return time;
        String separator = time.isBlank() ? "" : "  ·  ";
        return time + separator + statusText(message.getStatus());
    }

    private String statusText(MessageStatus status) {
        if (status == null) return "Đang gửi...";
        return switch (status) {
            case SENDING -> "Đang gửi...";
            case SENT -> "Đã gửi";
            case DELIVERED -> "Đã nhận";
            case READ -> "Đã xem";
            case FAILED -> "Không gửi được";
        };
    }

    private String deliveryTooltip(MessageDTO message) {
        MessageDeliveryMode mode = message.getDeliveryMode();
        if (mode == MessageDeliveryMode.P2P) {
            return "Gửi trực tiếp qua kết nối P2P";
        }
        if (mode == MessageDeliveryMode.SERVER_RELAY) {
            return "Gửi qua StudyConnect Server";
        }
        return null;
    }

    private String formatTime(long epochMillis) {
        if (epochMillis <= 0) return "";
        return TIME.format(
                Instant.ofEpochMilli(epochMillis)
                        .atZone(ZoneId.systemDefault())
        );
    }

    private static final class BubblePanel extends JPanel {
        private final Color normal;
        private final Color hover;
        private boolean hovered;

        private BubblePanel(boolean mine) {
            normal = mine ? OUTGOING : INCOMING;
            hover = mine ? OUTGOING_HOVER : INCOMING_HOVER;
            setOpaque(false);
            MouseAdapter hoverListener = new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent event) {
                    hovered = true;
                    repaint();
                }

                @Override public void mouseExited(MouseEvent event) {
                    hovered = false;
                    repaint();
                }
            };
            addMouseListener(hoverListener);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON
            );
            g.setColor(hovered ? hover : normal);
            g.fillRoundRect(0, 0, getWidth(), getHeight(), 22, 22);
            g.dispose();
            super.paintComponent(graphics);
        }
    }
}
