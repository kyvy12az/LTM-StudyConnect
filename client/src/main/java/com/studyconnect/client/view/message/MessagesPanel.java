package com.studyconnect.client.view.message;

import com.studyconnect.client.view.component.AvatarView;
import com.studyconnect.client.view.component.MainTheme;
import com.studyconnect.common.dto.ConversationDTO;
import com.studyconnect.common.dto.MessageDTO;
import com.studyconnect.common.dto.MessageStatus;
import com.studyconnect.common.dto.UserDTO;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.DefaultEditorKit;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

public class MessagesPanel extends JPanel {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final long currentUserId;
    private final MainTheme.SearchField searchField =
            new MainTheme.SearchField("Tìm kiếm cuộc trò chuyện...");
    private final JPanel conversationList = verticalPanel();
    private final JPanel messageList = verticalPanel();
    private final JScrollPane messageScroll = MainTheme.scrollPane(messageList);
    private final JPanel chatContent = new JPanel(new BorderLayout());
    private final CardLayout chatLayout = new CardLayout();
    private final JPanel chatDeck = new JPanel(chatLayout);
    private final JLabel emptyState = new JLabel(
            "Chọn một cuộc trò chuyện để bắt đầu", SwingConstants.CENTER);
    private final JLabel chatName = new JLabel();
    private final JLabel chatStatus = new JLabel();
    private final JPanel avatarHost = new JPanel(new BorderLayout());
    private final JTextArea composer = new JTextArea(2, 20);
    private final JButton sendButton = createSendButton();

    private final Map<Long, ConversationDTO> conversations = new LinkedHashMap<>();
    private final Map<String, MessageDTO> messages = new LinkedHashMap<>();
    private Consumer<ConversationDTO> selectionListener;
    private Consumer<String> sendListener;
    private IntConsumer unreadCountListener;
    private long selectedUserId;

    public MessagesPanel(long currentUserId) {
        if (currentUserId <= 0) throw new IllegalArgumentException("User ID không hợp lệ");
        this.currentUserId = currentUserId;
        setLayout(new BorderLayout());
        setOpaque(false);
        setBorder(new EmptyBorder(8, 20, 16, 20));
        add(createConversationColumn(), BorderLayout.WEST);
        add(createChatColumn(), BorderLayout.CENTER);
        bindComposerKeys();
    }

    public void setConversationSelectionListener(Consumer<ConversationDTO> listener) {
        this.selectionListener = listener;
    }

    public void setSendListener(Consumer<String> listener) {
        this.sendListener = listener;
    }

    public void setUnreadCountListener(IntConsumer listener) {
        this.unreadCountListener = listener;
        notifyUnreadChanged();
    }

    public void setConversations(List<ConversationDTO> values) {
        conversations.clear();
        if (values != null) {
            values.stream()
                    .filter(item -> item != null && item.getOtherUserId() > 0)
                    .sorted(conversationComparator())
                    .forEach(item -> conversations.put(item.getOtherUserId(), item));
        }
        renderConversations();
        if (selectedUserId > 0) {
            ConversationDTO selected = conversations.get(selectedUserId);
            if (selected != null) updateHeader(selected);
        }
        notifyUnreadChanged();
    }

    public void setConversationLoading(boolean loading) {
        if (loading && conversations.isEmpty()) {
            conversationList.removeAll();
            conversationList.add(statusLabel("Đang tải hội thoại..."));
            refresh(conversationList);
        }
    }

    public void openConversation(ConversationDTO conversation) {
        if (conversation == null) return;
        selectedUserId = conversation.getOtherUserId();
        conversation.setUnreadCount(0);
        updateHeader(conversation);
        chatLayout.show(chatDeck, "chat");
        messages.clear();
        messageList.removeAll();
        messageList.add(statusLabel("Đang tải tin nhắn..."));
        renderConversations();
        notifyUnreadChanged();
    }

    public void setMessages(List<MessageDTO> values) {
        messages.clear();
        if (values != null) {
            values.stream()
                    .filter(this::hasMessageIdentity)
                    .sorted(messageComparator())
                    .forEach(item -> messages.put(messageKey(item), item));
        }
        renderMessages(true, -1);
    }

    public boolean addMessage(MessageDTO message, boolean forceScroll) {
        if (!hasMessageIdentity(message)) {
            return false;
        }
        String key = messageKey(message);
        boolean added = !messages.containsKey(key);
        boolean nearBottom = isNearBottom();
        int oldValue = messageScroll.getVerticalScrollBar().getValue();
        messages.put(key, message);
        renderMessages(forceScroll || nearBottom, oldValue);
        return added;
    }

    public void markMessagesRead(List<Long> messageIds, long readAt) {
        markMessagesRead(messageIds, List.of(), readAt);
    }

    public void markMessagesRead(
            List<Long> messageIds,
            List<String> clientMessageIds,
            long readAt
    ) {
        if ((messageIds == null || messageIds.isEmpty())
                && (clientMessageIds == null || clientMessageIds.isEmpty())) return;
        boolean changed = false;
        for (MessageDTO message : messages.values()) {
            boolean matchesServerId = messageIds != null && messageIds.contains(message.getId());
            boolean matchesClientId = clientMessageIds != null
                    && clientMessageIds.contains(message.getClientMessageId());
            if ((matchesServerId || matchesClientId)
                    && message.getSenderId() == currentUserId) {
                message.setStatus(MessageStatus.READ);
                message.setReadAt(readAt);
                changed = true;
            }
        }
        if (changed) renderMessages(false, messageScroll.getVerticalScrollBar().getValue());
    }

    public void updateMessageStatus(MessageDTO message) {
        if (!hasMessageIdentity(message)) return;
        MessageDTO current = messages.get(messageKey(message));
        if (current == null && message.getId() > 0) {
            current = messages.values().stream()
                    .filter(item -> item.getId() == message.getId())
                    .findFirst().orElse(null);
        }
        if (current != null || belongsToSelectedConversation(message)) {
            addMessage(message, false);
        }
        updateConversationForMessage(message, false);
    }

    public void updateConversationForMessage(MessageDTO message, boolean incrementUnread) {
        if (message == null) return;
        long otherId = message.getSenderId() == currentUserId
                ? message.getReceiverId() : message.getSenderId();
        ConversationDTO conversation = conversations.get(otherId);
        if (conversation == null) {
            conversation = new ConversationDTO();
            conversation.setOtherUserId(otherId);
            conversation.setOtherUserName(message.getSenderName());
            conversation.setOtherUserAvatarUrl(message.getSenderAvatarUrl());
            conversations.put(otherId, conversation);
        }
        conversation.setLastMessage(message);
        if (incrementUnread && otherId != selectedUserId) {
            conversation.setUnreadCount(conversation.getUnreadCount() + 1);
        }
        renderConversations();
        notifyUnreadChanged();
    }

    public void updateOnlineUsers(Set<Long> onlineUserIds) {
        Set<Long> safeIds = onlineUserIds == null ? Set.of() : onlineUserIds;
        conversations.values().forEach(
                conversation -> conversation.setOnline(safeIds.contains(conversation.getOtherUserId())));
        renderConversations();
        ConversationDTO selected = conversations.get(selectedUserId);
        if (selected != null) updateHeader(selected);
    }

    public void mergeOnlineUsers(List<UserDTO> onlineUsers) {
        Set<Long> onlineIds = new java.util.HashSet<>();
        if (onlineUsers != null) {
            for (UserDTO user : onlineUsers) {
                if (user == null || user.getId() <= 0 || user.getId() == currentUserId) continue;
                onlineIds.add(user.getId());
                ConversationDTO conversation = conversations.get(user.getId());
                if (conversation == null) {
                    conversation = new ConversationDTO();
                    conversation.setOtherUserId(user.getId());
                    conversations.put(user.getId(), conversation);
                }
                conversation.setOtherUsername(user.getUsername());
                conversation.setOtherUserName(user.getFullName());
                conversation.setOtherUserAvatarUrl(user.getAvatarUrl());
                conversation.setOnline(true);
            }
        }
        conversations.values().forEach(item ->
                item.setOnline(onlineIds.contains(item.getOtherUserId())));
        renderConversations();
        ConversationDTO selected = conversations.get(selectedUserId);
        if (selected != null) updateHeader(selected);
    }

    public long getSelectedUserId() {
        return selectedUserId;
    }

    public void setSending(boolean sending) {
        sendButton.setEnabled(!sending);
        sendButton.setToolTipText(sending ? "Đang gửi tin nhắn..." : "Gửi tin nhắn");
    }

    public void clearComposerIfUnchanged(String sentText) {
        if (composer.getText().trim().equals(sentText)) composer.setText("");
        composer.requestFocusInWindow();
    }

    public void showError(String message) {
        javax.swing.JOptionPane.showMessageDialog(
                this,
                message == null || message.isBlank() ? "Đã xảy ra lỗi." : message,
                "Tin nhắn",
                javax.swing.JOptionPane.ERROR_MESSAGE
        );
    }

    private JComponent createConversationColumn() {
        MainTheme.RoundedPanel column = new MainTheme.RoundedPanel(18);
        column.setLayout(new BorderLayout(0, 12));
        column.setPreferredSize(new Dimension(340, 0));
        column.setBorder(new EmptyBorder(16, 14, 14, 14));
        JPanel heading = new JPanel(new BorderLayout(0, 12));
        heading.setOpaque(false);
        JLabel title = new JLabel("Tin nhắn");
        title.setFont(MainTheme.font(Font.BOLD, 23));
        title.setForeground(MainTheme.NAVY);
        heading.add(title, BorderLayout.NORTH);
        heading.add(searchField, BorderLayout.CENTER);
        column.add(heading, BorderLayout.NORTH);
        JScrollPane scroll = MainTheme.scrollPane(conversationList);
        scroll.getViewport().setBackground(Color.WHITE);
        column.add(scroll, BorderLayout.CENTER);
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent event) { renderConversations(); }
            public void removeUpdate(DocumentEvent event) { renderConversations(); }
            public void changedUpdate(DocumentEvent event) { renderConversations(); }
        });
        return column;
    }

    private JComponent createChatColumn() {
        MainTheme.RoundedPanel column = new MainTheme.RoundedPanel(18);
        column.setLayout(new BorderLayout());
        column.setBorder(new EmptyBorder(0, 0, 0, 0));
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.setBorder(new EmptyBorder(0, 14, 0, 0));
        emptyState.setFont(MainTheme.font(Font.PLAIN, 16));
        emptyState.setForeground(MainTheme.MUTED);
        chatDeck.setOpaque(false);
        chatDeck.add(emptyState, "empty");

        chatContent.setOpaque(false);
        chatContent.add(createChatHeader(), BorderLayout.NORTH);
        messageList.setBorder(new EmptyBorder(14, 10, 14, 10));
        messageScroll.getViewport().setBackground(Color.WHITE);
        chatContent.add(messageScroll, BorderLayout.CENTER);
        chatContent.add(createComposer(), BorderLayout.SOUTH);
        chatDeck.add(chatContent, "chat");
        chatLayout.show(chatDeck, "empty");
        wrapper.add(chatDeck, BorderLayout.CENTER);
        column.add(wrapper, BorderLayout.CENTER);
        return column;
    }

    private JComponent createChatHeader() {
        JPanel header = new JPanel(new BorderLayout(10, 0));
        header.setBackground(Color.WHITE);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, MainTheme.BORDER),
                new EmptyBorder(12, 18, 12, 18)));
        avatarHost.setOpaque(false);
        avatarHost.setPreferredSize(new Dimension(54, 54));
        header.add(avatarHost, BorderLayout.WEST);
        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        chatName.setFont(MainTheme.font(Font.BOLD, 16));
        chatName.setForeground(MainTheme.NAVY);
        chatStatus.setFont(MainTheme.font(Font.PLAIN, 12));
        chatStatus.setForeground(MainTheme.MUTED);
        text.add(Box.createVerticalStrut(5));
        text.add(chatName);
        text.add(Box.createVerticalStrut(3));
        text.add(chatStatus);
        header.add(text, BorderLayout.CENTER);
        return header;
    }

    private JComponent createComposer() {
        JPanel footer = new JPanel(new BorderLayout(8, 0));
        footer.setBackground(Color.WHITE);
        footer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, MainTheme.BORDER),
                new EmptyBorder(8, 14, 8, 14)));
        composer.setLineWrap(true);
        composer.setWrapStyleWord(true);
        composer.setFont(MainTheme.font(Font.PLAIN, 14));
        composer.setBorder(new EmptyBorder(6, 10, 6, 10));
        composer.setToolTipText("Nhập tin nhắn (Enter để gửi, Shift+Enter để xuống dòng)");
        JScrollPane composerScroll = new JScrollPane(composer);
        composerScroll.setPreferredSize(new Dimension(100, 46));
        composerScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        composerScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        composerScroll.getViewport().setBackground(Color.WHITE);
        composerScroll.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(MainTheme.BORDER, 1, true),
                new EmptyBorder(1, 1, 1, 1)));
        footer.add(composerScroll, BorderLayout.CENTER);
        sendButton.setPreferredSize(new Dimension(46, 46));
        sendButton.addActionListener(event -> submit());
        footer.add(sendButton, BorderLayout.EAST);
        return footer;
    }

    private JButton createSendButton() {
        JButton button = new JButton(
                MainTheme.svgIcon(MainTheme.IconType.SEND, 20, Color.WHITE)
        ) {
            @Override
            protected void paintComponent(Graphics graphics) {
                Graphics2D g = (Graphics2D) graphics.create();
                g.setRenderingHint(
                        RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON
                );
                Color fill;
                if (!isEnabled()) {
                    fill = new Color(163, 201, 194);
                } else if (getModel().isRollover()) {
                    fill = MainTheme.TEAL_DARK;
                } else {
                    fill = MainTheme.TEAL;
                }
                g.setColor(fill);
                g.fillRoundRect(0, 0, getWidth(), getHeight(), 18, 18);
                g.dispose();
                super.paintComponent(graphics);
            }
        };
        button.setToolTipText("Gửi tin nhắn");
        button.setBorder(new EmptyBorder(10, 10, 10, 10));
        button.setContentAreaFilled(false);
        button.setOpaque(false);
        button.setFocusPainted(false);
        button.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        button.getAccessibleContext().setAccessibleName("Gửi tin nhắn");
        return button;
    }

    private void bindComposerKeys() {
        composer.getInputMap().put(KeyStroke.getKeyStroke("ENTER"), "send-message");
        composer.getActionMap().put("send-message", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent event) { submit(); }
        });
        composer.getInputMap().put(
                KeyStroke.getKeyStroke("shift ENTER"), DefaultEditorKit.insertBreakAction);
    }

    private void submit() {
        String text = composer.getText().trim();
        if (!text.isEmpty() && selectedUserId > 0 && sendListener != null) {
            sendListener.accept(text);
        }
    }

    private void renderConversations() {
        String query = searchField.getText() == null
                ? "" : searchField.getText().trim().toLowerCase();
        conversationList.removeAll();
        List<ConversationDTO> values = new ArrayList<>(conversations.values());
        values.sort(conversationComparator());
        for (ConversationDTO conversation : values) {
            String haystack = (displayName(conversation) + " "
                    + safe(conversation.getOtherUsername())).toLowerCase();
            if (!query.isEmpty() && !haystack.contains(query)) continue;
            ConversationListItem item = new ConversationListItem(
                    conversation, () -> selectConversation(conversation));
            item.setSelected(conversation.getOtherUserId() == selectedUserId);
            conversationList.add(item);
        }
        if (conversationList.getComponentCount() == 0) {
            conversationList.add(statusLabel(
                    query.isEmpty() ? "Chưa có cuộc trò chuyện" : "Không tìm thấy kết quả"));
        }
        refresh(conversationList);
    }

    private void selectConversation(ConversationDTO conversation) {
        if (selectionListener != null) selectionListener.accept(conversation);
    }

    private void renderMessages(boolean scrollBottom, int oldValue) {
        messageList.removeAll();
        List<MessageDTO> values = new ArrayList<>(messages.values());
        values.sort(messageComparator());
        LocalDate previous = null;
        for (MessageDTO message : values) {
            LocalDate date = Instant.ofEpochMilli(message.getSentAt())
                    .atZone(ZoneId.systemDefault()).toLocalDate();
            if (!date.equals(previous)) {
                JLabel separator = new JLabel(formatDate(date), SwingConstants.CENTER);
                separator.setFont(MainTheme.font(Font.PLAIN, 11));
                separator.setForeground(MainTheme.MUTED);
                separator.setBorder(new EmptyBorder(9, 0, 9, 0));
                separator.setAlignmentX(Component.CENTER_ALIGNMENT);
                separator.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
                messageList.add(separator);
                previous = date;
            }
            messageList.add(new MessageBubble(message, message.getSenderId() == currentUserId));
        }
        if (values.isEmpty()) messageList.add(statusLabel("Chưa có tin nhắn. Hãy bắt đầu trò chuyện!"));
        refresh(messageList);
        SwingUtilities.invokeLater(() -> {
            if (scrollBottom) {
                messageScroll.getVerticalScrollBar().setValue(
                        messageScroll.getVerticalScrollBar().getMaximum());
            } else if (oldValue >= 0) {
                messageScroll.getVerticalScrollBar().setValue(oldValue);
            }
        });
    }

    private void updateHeader(ConversationDTO conversation) {
        chatName.setText(displayName(conversation));
        chatStatus.setText(conversation.isOnline() ? "Đang trực tuyến" : "Ngoại tuyến");
        chatStatus.setForeground(conversation.isOnline() ? MainTheme.SUCCESS : MainTheme.MUTED);
        avatarHost.removeAll();
        AvatarView avatar = new AvatarView(displayName(conversation), 46);
        avatar.setOnline(conversation.isOnline());
        String url = conversation.getOtherUserAvatarUrl();
        if (url == null || url.isBlank()) avatar.setAvatarResource("/images/default-avatar.png");
        else if (url.startsWith("/")) avatar.setAvatarResource(url);
        else avatar.setAvatarUrl(url);
        avatarHost.add(avatar, BorderLayout.CENTER);
        refresh(avatarHost);
    }

    private boolean isNearBottom() {
        var bar = messageScroll.getVerticalScrollBar();
        return bar.getMaximum() - (bar.getValue() + bar.getVisibleAmount()) <= 90;
    }

    private Comparator<ConversationDTO> conversationComparator() {
        return Comparator.comparingLong((ConversationDTO item) ->
                item.getLastMessage() == null ? 0L : item.getLastMessage().getSentAt()).reversed();
    }

    private Comparator<MessageDTO> messageComparator() {
        return Comparator.comparingLong(MessageDTO::getSentAt)
                .thenComparingLong(MessageDTO::getId);
    }

    private boolean belongsToSelectedConversation(MessageDTO message) {
        long otherId = message.getSenderId() == currentUserId
                ? message.getReceiverId() : message.getSenderId();
        return otherId == selectedUserId;
    }

    private boolean hasMessageIdentity(MessageDTO message) {
        return message != null && (message.getId() > 0
                || (message.getClientMessageId() != null
                && !message.getClientMessageId().isBlank()));
    }

    private String messageKey(MessageDTO message) {
        if (message.getClientMessageId() != null
                && !message.getClientMessageId().isBlank()) {
            return "client:" + message.getClientMessageId();
        }
        return "server:" + message.getId();
    }

    private void notifyUnreadChanged() {
        if (unreadCountListener != null) {
            int count = conversations.values().stream()
                    .mapToInt(item -> Math.max(0, item.getUnreadCount())).sum();
            unreadCountListener.accept(count);
        }
    }

    private String formatDate(LocalDate date) {
        if (date.equals(LocalDate.now())) return "Hôm nay";
        if (date.equals(LocalDate.now().minusDays(1))) return "Hôm qua";
        return DATE.format(date);
    }

    private String displayName(ConversationDTO conversation) {
        String name = conversation.getOtherUserName();
        if (name == null || name.isBlank()) name = conversation.getOtherUsername();
        return name == null || name.isBlank() ? "Người dùng" : name;
    }

    private static JPanel verticalPanel() {
        JPanel panel = new JPanel();
        panel.setBackground(Color.WHITE);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        return panel;
    }

    private JLabel statusLabel(String text) {
        JLabel label = new JLabel(text, SwingConstants.CENTER);
        label.setFont(MainTheme.font(Font.PLAIN, 13));
        label.setForeground(MainTheme.MUTED);
        label.setBorder(new EmptyBorder(24, 8, 24, 8));
        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        label.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
        return label;
    }

    private void refresh(JComponent component) {
        component.revalidate();
        component.repaint();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
