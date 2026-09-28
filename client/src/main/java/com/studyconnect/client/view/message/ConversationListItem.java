package com.studyconnect.client.view.message;

import com.studyconnect.client.view.component.AvatarView;
import com.studyconnect.client.view.component.MainTheme;
import com.studyconnect.common.dto.ConversationDTO;
import com.studyconnect.common.dto.MessageDTO;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class ConversationListItem extends JPanel {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM");

    private final ConversationDTO conversation;
    private boolean selected;

    public ConversationListItem(ConversationDTO conversation, Runnable onSelected) {
        this.conversation = conversation;
        setLayout(new BorderLayout(10, 0));
        setOpaque(true);
        setBorder(new EmptyBorder(10, 10, 10, 10));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, 78));
        setPreferredSize(new Dimension(310, 78));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        AvatarView avatar = new AvatarView(displayName(), 48);
        avatar.setOnline(conversation.isOnline());
        applyAvatar(avatar, conversation.getOtherUserAvatarUrl());
        add(avatar, BorderLayout.WEST);

        JPanel center = new JPanel();
        center.setOpaque(false);
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        JLabel name = new JLabel(displayName());
        name.setFont(MainTheme.font(Font.BOLD, 14));
        name.setForeground(MainTheme.NAVY);
        name.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel preview = new JLabel(preview());
        preview.setFont(MainTheme.font(Font.PLAIN, 12));
        preview.setForeground(MainTheme.MUTED);
        preview.setAlignmentX(Component.LEFT_ALIGNMENT);
        center.add(Box.createVerticalStrut(4));
        center.add(name);
        center.add(Box.createVerticalStrut(5));
        center.add(preview);
        add(center, BorderLayout.CENTER);

        JPanel right = new JPanel();
        right.setOpaque(false);
        right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));
        JLabel time = new JLabel(formatTime());
        time.setFont(MainTheme.font(Font.PLAIN, 11));
        time.setForeground(MainTheme.MUTED);
        time.setAlignmentX(Component.RIGHT_ALIGNMENT);
        right.add(time);
        right.add(Box.createVerticalStrut(7));
        if (conversation.getUnreadCount() > 0) {
            JLabel badge = new JLabel(String.valueOf(conversation.getUnreadCount()));
            badge.setOpaque(true);
            badge.setBackground(MainTheme.DANGER);
            badge.setForeground(Color.WHITE);
            badge.setFont(MainTheme.font(Font.BOLD, 11));
            badge.setBorder(BorderFactory.createEmptyBorder(3, 7, 3, 7));
            badge.setAlignmentX(Component.RIGHT_ALIGNMENT);
            right.add(badge);
        }
        add(right, BorderLayout.EAST);

        MouseAdapter click = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                onSelected.run();
            }
        };
        addMouseListener(click);
        addClickListener(this, click);
        updateBackground();
    }

    public long getOtherUserId() {
        return conversation.getOtherUserId();
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
        updateBackground();
    }

    private void updateBackground() {
        setBackground(selected ? new Color(224, 247, 242) : Color.WHITE);
    }

    private void addClickListener(Component component, MouseAdapter listener) {
        if (component != this) component.addMouseListener(listener);
        if (component instanceof JPanel panel) {
            for (Component child : panel.getComponents()) {
                addClickListener(child, listener);
            }
        }
    }

    private String displayName() {
        String name = conversation.getOtherUserName();
        if (name == null || name.isBlank()) name = conversation.getOtherUsername();
        return name == null || name.isBlank() ? "Người dùng" : name;
    }

    private String preview() {
        MessageDTO message = conversation.getLastMessage();
        if (message == null || message.getContent() == null) return "";
        String text = message.getContent().replace('\n', ' ').trim();
        return text.length() > 38 ? text.substring(0, 38) + "…" : text;
    }

    private String formatTime() {
        MessageDTO message = conversation.getLastMessage();
        if (message == null || message.getSentAt() <= 0) return "";
        Instant instant = Instant.ofEpochMilli(message.getSentAt());
        LocalDate date = instant.atZone(ZoneId.systemDefault()).toLocalDate();
        return date.equals(LocalDate.now())
                ? TIME.format(instant.atZone(ZoneId.systemDefault()))
                : DATE.format(date);
    }

    private void applyAvatar(AvatarView avatar, String url) {
        if (url == null || url.isBlank()) {
            avatar.setAvatarResource("/images/default-avatar.png");
        } else if (url.startsWith("/")) {
            avatar.setAvatarResource(url);
        } else {
            avatar.setAvatarUrl(url);
        }
    }
}
