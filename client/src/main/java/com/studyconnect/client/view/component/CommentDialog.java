package com.studyconnect.client.view.component;

import com.studyconnect.client.model.CurrentUser;
import com.studyconnect.common.dto.CommentDTO;
import com.studyconnect.common.dto.UserDTO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Hộp thoại bình luận theo phong cách mạng xã hội.
 *
 * <p>Lớp này chỉ quản lý giao diện. Việc tải và gửi bình luận vẫn do
 * MainController xử lý thông qua các API public hiện có.</p>
 */
public class CommentDialog extends JDialog {
    private static final int MAX_RENDER_DEPTH = 4;
    private static final int REPLY_INDENT = 28;
    private static final int MAX_BUBBLE_WIDTH = 470;

    private static final Color DIALOG_BACKGROUND = new Color(255, 255, 255);
    private static final Color LIST_BACKGROUND = new Color(248, 250, 252);
    private static final Color BUBBLE_BACKGROUND = new Color(238, 242, 246);
    private static final Color BUBBLE_HOVER = new Color(232, 238, 243);
    private static final Color INPUT_BACKGROUND = new Color(241, 245, 248);
    private static final Color META_TEXT = new Color(101, 116, 139);
    private static final Color LINK_TEXT = new Color(51, 65, 85);
    private static final Color REPLY_LINE = new Color(215, 226, 232);

    private final JPanel commentsPanel = new JPanel();
    private final JScrollPane commentsScrollPane;
    private final JTextArea commentInput = new JTextArea(2, 20);
    private final JButton sendButton = MainTheme.textButton("Gửi", true);
    private final JLabel commentCountLabel = new JLabel("0 bình luận");
    private final JLabel statusLabel = new JLabel(" ", SwingConstants.LEFT);
    private final JPanel replyPanel = new JPanel(new BorderLayout(10, 0));
    private final JLabel replyLabel = new JLabel();
    private final JButton cancelReplyButton = createLinkButton("Hủy");
    private final Map<Long, CommentDTO> commentsById = new LinkedHashMap<>();

    private CommentDTO replyTarget;
    private boolean loading;

    public CommentDialog(JFrame owner, String postTitle) {
        super(owner, "Bình luận", true);

        setSize(760, 760);
        setMinimumSize(new Dimension(620, 580));
        setLocationRelativeTo(owner);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        commentsPanel.setBackground(LIST_BACKGROUND);
        commentsPanel.setLayout(new BoxLayout(commentsPanel, BoxLayout.Y_AXIS));
        commentsPanel.setBorder(new EmptyBorder(16, 20, 22, 20));

        commentsScrollPane = MainTheme.scrollPane(commentsPanel);
        configureCommentsScrollPane();
        configureInputActions();
        initializeUI(postTitle);
        updateSendButtonState();
    }

    private void initializeUI(String postTitle) {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(DIALOG_BACKGROUND);
        root.add(createHeader(postTitle), BorderLayout.NORTH);
        root.add(commentsScrollPane, BorderLayout.CENTER);
        root.add(createComposer(), BorderLayout.SOUTH);
        setContentPane(root);
    }

    private JComponent createHeader(String postTitle) {
        JPanel header = new JPanel(new BorderLayout(14, 0));
        header.setBackground(DIALOG_BACKGROUND);
        header.setBorder(new EmptyBorder(17, 22, 15, 22));

        JLabel icon = new JLabel(
                MainTheme.svgIcon(MainTheme.IconType.COMMENT, 25, MainTheme.TEAL)
        );
        icon.setVerticalAlignment(SwingConstants.TOP);
        icon.setBorder(new EmptyBorder(3, 0, 0, 0));

        JPanel words = new JPanel();
        words.setOpaque(false);
        words.setLayout(new BoxLayout(words, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Bình luận");
        title.setFont(MainTheme.font(Font.BOLD, 22));
        title.setForeground(MainTheme.NAVY);

        String safeTitle = postTitle == null || postTitle.isBlank()
                ? "Bài viết trên StudyConnect"
                : postTitle.trim();
        JLabel subtitle = new JLabel(
                "<html><div style='width:420px'>"
                        + escapeHtml(safeTitle)
                        + "</div></html>"
        );
        subtitle.setFont(MainTheme.font(Font.PLAIN, 12));
        subtitle.setForeground(MainTheme.MUTED);

        words.add(title);
        words.add(Box.createVerticalStrut(4));
        words.add(subtitle);

        commentCountLabel.setFont(MainTheme.font(Font.BOLD, 12));
        commentCountLabel.setForeground(MainTheme.TEAL_DARK);
        commentCountLabel.setBorder(new EmptyBorder(7, 12, 7, 12));
        commentCountLabel.setOpaque(true);
        commentCountLabel.setBackground(new Color(229, 248, 243));
        commentCountLabel.setVerticalAlignment(SwingConstants.TOP);

        header.add(icon, BorderLayout.WEST);
        header.add(words, BorderLayout.CENTER);
        header.add(commentCountLabel, BorderLayout.EAST);
        return header;
    }

    private void configureCommentsScrollPane() {
        commentsScrollPane.setBorder(new MatteBorder(1, 0, 1, 0, MainTheme.BORDER));
        commentsScrollPane.setBackground(LIST_BACKGROUND);
        commentsScrollPane.getViewport().setBackground(LIST_BACKGROUND);
        commentsScrollPane.getVerticalScrollBar().setUnitIncrement(18);
        commentsScrollPane.setHorizontalScrollBarPolicy(
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
        );
    }

    private JComponent createComposer() {
        JPanel container = new JPanel(new BorderLayout());
        container.setBackground(DIALOG_BACKGROUND);
        container.setBorder(new EmptyBorder(10, 18, 14, 18));

        JPanel content = new JPanel(new BorderLayout(0, 7));
        content.setOpaque(false);

        configureReplyPanel();
        content.add(replyPanel, BorderLayout.NORTH);

        JPanel inputRow = new JPanel(new BorderLayout(10, 0));
        inputRow.setOpaque(false);

        AvatarView currentAvatar = createCurrentUserAvatar(38);
        JPanel avatarHolder = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 2));
        avatarHolder.setOpaque(false);
        avatarHolder.add(currentAvatar);
        inputRow.add(avatarHolder, BorderLayout.WEST);
        inputRow.add(createInputArea(), BorderLayout.CENTER);

        configureSendButton();
        inputRow.add(sendButton, BorderLayout.EAST);
        content.add(inputRow, BorderLayout.CENTER);

        statusLabel.setFont(MainTheme.font(Font.PLAIN, 11));
        statusLabel.setForeground(MainTheme.MUTED);
        statusLabel.setBorder(new EmptyBorder(0, 52, 0, 0));
        content.add(statusLabel, BorderLayout.SOUTH);

        container.add(content, BorderLayout.CENTER);
        return container;
    }

    private void configureReplyPanel() {
        replyPanel.setOpaque(true);
        replyPanel.setBackground(new Color(238, 249, 246));
        replyPanel.setBorder(BorderFactory.createCompoundBorder(
                new MatteBorder(0, 3, 0, 0, MainTheme.TEAL),
                new EmptyBorder(8, 11, 8, 8)
        ));
        replyPanel.setVisible(false);

        replyLabel.setFont(MainTheme.font(Font.PLAIN, 12));
        replyLabel.setForeground(MainTheme.TEXT);

        cancelReplyButton.setFont(MainTheme.font(Font.BOLD, 12));
        cancelReplyButton.setForeground(MainTheme.TEAL_DARK);
        cancelReplyButton.addActionListener(event -> clearReplyTarget());

        replyPanel.add(replyLabel, BorderLayout.CENTER);
        replyPanel.add(cancelReplyButton, BorderLayout.EAST);
    }

    private JComponent createInputArea() {
        MainTheme.RoundedPanel inputBubble = new MainTheme.RoundedPanel(22);
        inputBubble.setLayout(new BorderLayout());
        inputBubble.setFill(INPUT_BACKGROUND);
        inputBubble.setBorder(new EmptyBorder(8, 14, 8, 14));
        inputBubble.setPreferredSize(new Dimension(500, 58));
        inputBubble.setMinimumSize(new Dimension(250, 48));

        commentInput.setLineWrap(true);
        commentInput.setWrapStyleWord(true);
        commentInput.setFont(MainTheme.font(Font.PLAIN, 13));
        commentInput.setForeground(MainTheme.TEXT);
        commentInput.setCaretColor(MainTheme.TEAL_DARK);
        commentInput.setBackground(INPUT_BACKGROUND);
        commentInput.setOpaque(false);
        commentInput.setBorder(null);
        commentInput.setToolTipText("Viết bình luận...");
        commentInput.putClientProperty("JTextArea.placeholderText", "Viết bình luận...");

        JScrollPane inputScroll = new JScrollPane(commentInput);
        inputScroll.setBorder(null);
        inputScroll.setOpaque(false);
        inputScroll.getViewport().setOpaque(false);
        inputScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        inputScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);

        inputBubble.add(inputScroll, BorderLayout.CENTER);
        return inputBubble;
    }

    private void configureSendButton() {
        sendButton.setFont(MainTheme.font(Font.BOLD, 12));
        sendButton.setIcon(MainTheme.svgIcon(MainTheme.IconType.SEND, 17, Color.WHITE));
        sendButton.setIconTextGap(7);
        sendButton.setPreferredSize(new Dimension(88, 48));
        sendButton.setMinimumSize(new Dimension(88, 48));
        sendButton.setToolTipText("Gửi bình luận (Ctrl+Enter)");
    }

    private void configureInputActions() {
        commentInput.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent event) { updateSendButtonState(); }
            @Override public void removeUpdate(DocumentEvent event) { updateSendButtonState(); }
            @Override public void changedUpdate(DocumentEvent event) { updateSendButtonState(); }
        });

        commentInput.getInputMap().put(
                KeyStroke.getKeyStroke("ctrl ENTER"),
                "submit-comment"
        );
        commentInput.getActionMap().put("submit-comment", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                if (sendButton.isEnabled()) {
                    sendButton.doClick();
                }
            }
        });
    }

    public void displayComments(List<CommentDTO> comments) {
        List<CommentDTO> safeComments = comments == null
                ? List.of()
                : new ArrayList<>(comments);

        runOnEdt(() -> {
            commentsById.clear();
            for (CommentDTO comment : safeComments) {
                if (comment != null && comment.getId() > 0) {
                    commentsById.put(comment.getId(), comment);
                }
            }
            renderCommentTree();
        });
    }

    public void addOrUpdateComment(CommentDTO comment) {
        if (comment == null || comment.getId() <= 0) {
            return;
        }

        runOnEdt(() -> {
            boolean isNew = !commentsById.containsKey(comment.getId());
            commentsById.put(comment.getId(), comment);
            renderCommentTree();
            if (isNew) {
                scrollToBottom();
            }
        });
    }

    public boolean hasComment(long commentId) {
        return commentsById.containsKey(commentId);
    }

    private void renderCommentTree() {
        commentsPanel.removeAll();
        updateCommentCount();

        if (commentsById.isEmpty()) {
            showEmptyState();
            refreshCommentsPanel();
            return;
        }

        Map<Long, List<CommentDTO>> children = new HashMap<>();
        List<CommentDTO> roots = new ArrayList<>();

        for (CommentDTO comment : commentsById.values()) {
            Long parentId = comment.getParentCommentId();
            if (parentId == null
                    || parentId == comment.getId()
                    || !commentsById.containsKey(parentId)) {
                roots.add(comment);
            } else {
                children.computeIfAbsent(parentId, ignored -> new ArrayList<>()).add(comment);
            }
        }

        Comparator<CommentDTO> oldestFirst = Comparator
                .comparingLong(CommentDTO::getCreatedAt)
                .thenComparingLong(CommentDTO::getId);
        roots.sort(oldestFirst);
        children.values().forEach(list -> list.sort(oldestFirst));

        Set<Long> rendered = new HashSet<>();
        Set<Long> visiting = new HashSet<>();

        for (CommentDTO root : roots) {
            renderNode(root, 0, children, rendered, visiting);
        }

        // Dữ liệu parent hỏng hoặc tạo vòng vẫn được hiển thị an toàn.
        for (CommentDTO comment : commentsById.values()) {
            if (!rendered.contains(comment.getId())) {
                renderNode(comment, 0, children, rendered, visiting);
            }
        }

        commentsPanel.add(Box.createVerticalGlue());
        refreshCommentsPanel();
    }

    private void renderNode(
            CommentDTO comment,
            int depth,
            Map<Long, List<CommentDTO>> children,
            Set<Long> rendered,
            Set<Long> visiting
    ) {
        long commentId = comment.getId();
        if (rendered.contains(commentId) || !visiting.add(commentId)) {
            return;
        }

        rendered.add(commentId);
        commentsPanel.add(createCommentItem(comment, depth));
        commentsPanel.add(Box.createVerticalStrut(depth == 0 ? 10 : 6));

        List<CommentDTO> replies = children.get(commentId);
        if (replies != null) {
            for (CommentDTO reply : replies) {
                renderNode(reply, depth + 1, children, rendered, visiting);
            }
        }
        visiting.remove(commentId);
    }

    private void showEmptyState() {
        JPanel emptyPanel = new JPanel();
        emptyPanel.setOpaque(false);
        emptyPanel.setLayout(new BoxLayout(emptyPanel, BoxLayout.Y_AXIS));
        emptyPanel.setBorder(new EmptyBorder(90, 20, 50, 20));
        emptyPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        emptyPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 260));

        JLabel icon = new JLabel(
                MainTheme.svgIcon(MainTheme.IconType.COMMENT, 42, new Color(148, 163, 184))
        );
        icon.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel title = new JLabel("Chưa có bình luận");
        title.setFont(MainTheme.font(Font.BOLD, 17));
        title.setForeground(MainTheme.NAVY);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitle = new JLabel("Hãy là người đầu tiên chia sẻ suy nghĩ của bạn.");
        subtitle.setFont(MainTheme.font(Font.PLAIN, 12));
        subtitle.setForeground(MainTheme.MUTED);
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        emptyPanel.add(icon);
        emptyPanel.add(Box.createVerticalStrut(12));
        emptyPanel.add(title);
        emptyPanel.add(Box.createVerticalStrut(6));
        emptyPanel.add(subtitle);
        commentsPanel.add(emptyPanel);
        commentsPanel.add(Box.createVerticalGlue());
    }

    private JComponent createCommentItem(CommentDTO comment, int depth) {
        int visualDepth = Math.min(Math.max(0, depth), MAX_RENDER_DEPTH);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.setBorder(new EmptyBorder(0, visualDepth * REPLY_INDENT, 0, 8));
        wrapper.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel row = new JPanel(new BorderLayout(9, 0));
        row.setOpaque(false);

        String author = safeAuthor(comment.getAuthorName());
        int avatarSize = visualDepth == 0 ? 38 : 31;
        AvatarView avatar = new AvatarView(author, avatarSize);
        configureCommentAvatar(avatar, comment.getAuthorAvatarUrl());

        JPanel avatarHolder = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        avatarHolder.setOpaque(false);
        avatarHolder.add(avatar);

        JPanel commentBody = new JPanel();
        commentBody.setOpaque(false);
        commentBody.setLayout(new BoxLayout(commentBody, BoxLayout.Y_AXIS));

        int availableWidth = Math.max(250, MAX_BUBBLE_WIDTH - visualDepth * 18);
        JComponent bubble = createBubble(comment, author, availableWidth);
        bubble.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel metadata = new JPanel(new FlowLayout(FlowLayout.LEFT, 9, 0));
        metadata.setOpaque(false);
        metadata.setBorder(new EmptyBorder(3, 10, 0, 0));
        metadata.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel timeLabel = new JLabel(formatTime(comment.getCreatedAt()));
        timeLabel.setFont(MainTheme.font(Font.PLAIN, 11));
        timeLabel.setForeground(META_TEXT);

        JButton replyButton = createLinkButton("Trả lời");
        replyButton.addActionListener(event -> selectReply(comment));

        metadata.add(timeLabel);
        metadata.add(replyButton);
        commentBody.add(bubble);
        commentBody.add(metadata);

        row.add(avatarHolder, BorderLayout.WEST);
        row.add(commentBody, BorderLayout.CENTER);

        if (visualDepth > 0) {
            JPanel replyLane = new JPanel(new BorderLayout());
            replyLane.setOpaque(false);
            replyLane.setBorder(BorderFactory.createCompoundBorder(
                    new MatteBorder(0, 2, 0, 0, REPLY_LINE),
                    new EmptyBorder(0, 10, 0, 0)
            ));
            replyLane.add(row, BorderLayout.CENTER);
            wrapper.add(replyLane, BorderLayout.CENTER);
        } else {
            wrapper.add(row, BorderLayout.CENTER);
        }

        int preferredHeight = Math.max(
                avatarSize + 8,
                wrapper.getPreferredSize().height
        );
        wrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, preferredHeight));

        return wrapper;
    }

    private JComponent createBubble(CommentDTO comment, String author, int maxWidth) {
        HoverRoundedPanel bubble = new HoverRoundedPanel(18, BUBBLE_BACKGROUND, BUBBLE_HOVER);
        bubble.setLayout(new BoxLayout(bubble, BoxLayout.Y_AXIS));
        bubble.setBorder(new EmptyBorder(8, 12, 9, 12));

        JLabel authorLabel = new JLabel(escapeHtml(author));
        authorLabel.setFont(MainTheme.font(Font.BOLD, 12));
        authorLabel.setForeground(MainTheme.NAVY);
        authorLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JTextArea body = new JTextArea(comment.getContent() == null ? "" : comment.getContent());
        body.setEditable(false);
        body.setFocusable(true);
        body.setLineWrap(true);
        body.setWrapStyleWord(true);
        body.setOpaque(false);
        body.setBorder(null);
        body.setFont(MainTheme.font(Font.PLAIN, 13));
        body.setForeground(MainTheme.TEXT);
        body.setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
        body.setAlignmentX(Component.LEFT_ALIGNMENT);

        int authorWidth = authorLabel.getFontMetrics(authorLabel.getFont()).stringWidth(author) + 2;
        int textWidth = calculateBubbleTextWidth(body, body.getText(), maxWidth);
        int contentWidth = Math.min(maxWidth, Math.max(textWidth, authorWidth));
        body.setSize(new Dimension(contentWidth, Short.MAX_VALUE));
        Dimension bodySize = body.getPreferredSize();
        body.setPreferredSize(new Dimension(contentWidth, bodySize.height));
        body.setMaximumSize(new Dimension(contentWidth, bodySize.height));

        bubble.add(authorLabel);
        bubble.add(Box.createVerticalStrut(2));
        bubble.add(body);
        Dimension preferred = bubble.getPreferredSize();
        bubble.setPreferredSize(new Dimension(contentWidth + 24, preferred.height));
        bubble.setMaximumSize(new Dimension(contentWidth + 24, preferred.height));
        return bubble;
    }

    private int calculateBubbleTextWidth(JTextArea area, String text, int maxWidth) {
        FontMetrics metrics = area.getFontMetrics(area.getFont());
        int longestLine = 54;
        for (String line : text.split("\\R", -1)) {
            longestLine = Math.max(longestLine, metrics.stringWidth(line));
        }

        int naturalWidth = longestLine + 4;
        if (text.length() > 72 || naturalWidth > maxWidth) {
            return maxWidth;
        }
        return Math.max(76, Math.min(maxWidth, naturalWidth));
    }

    private AvatarView createCurrentUserAvatar(int size) {
        UserDTO user = CurrentUser.getUser();
        String name = user == null ? "Bạn" : safeAuthor(user.getFullName());
        AvatarView avatar = new AvatarView(name, size);
        avatar.setOnline(user != null && user.isOnline());
        configureCommentAvatar(avatar, user == null ? null : user.getAvatarUrl());
        return avatar;
    }

    private void configureCommentAvatar(AvatarView avatar, String authorAvatarUrl) {
        if (authorAvatarUrl == null || authorAvatarUrl.isBlank()) {
            avatar.setAvatarResource("/images/default-avatar.png");
            return;
        }

        String avatarUrl = authorAvatarUrl.trim();
        String lower = avatarUrl.toLowerCase();
        if (lower.endsWith("default_avatar.png") || lower.endsWith("default-avatar.png")) {
            avatar.setAvatarResource("/images/default-avatar.png");
        } else if (avatarUrl.startsWith("http://") || avatarUrl.startsWith("https://")) {
            avatar.setAvatarUrl(avatarUrl);
        } else if (avatarUrl.startsWith("/")) {
            avatar.setAvatarResource(avatarUrl);
        } else {
            avatar.setAvatarResource("/images/" + avatarUrl);
        }
    }

    private JButton createLinkButton(String text) {
        JButton button = new JButton(text);
        button.setFont(MainTheme.font(Font.BOLD, 11));
        button.setForeground(LINK_TEXT);
        button.setBorder(new EmptyBorder(2, 0, 2, 0));
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setOpaque(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.putClientProperty("JButton.buttonType", "borderless");
        return button;
    }

    private void selectReply(CommentDTO comment) {
        replyTarget = comment;
        String author = safeAuthor(comment.getAuthorName());
        replyLabel.setText(
                "<html>Đang trả lời <b>" + escapeHtml(author)
                        + "</b><br><span style='color:#64748b'>"
                        + preview(comment.getContent()) + "</span></html>"
        );
        replyPanel.setVisible(true);
        replyPanel.revalidate();
        commentInput.requestFocusInWindow();
    }

    public Long getReplyParentId() {
        return replyTarget == null ? null : replyTarget.getId();
    }

    public void clearReplyTarget() {
        runOnEdt(() -> {
            replyTarget = null;
            replyLabel.setText("");
            replyPanel.setVisible(false);
            replyPanel.revalidate();
            commentInput.requestFocusInWindow();
        });
    }

    public String getCommentContent() {
        return commentInput.getText().trim();
    }

    public void clearInput() {
        runOnEdt(() -> {
            commentInput.setText("");
            updateSendButtonState();
        });
    }

    public void addSendListener(ActionListener listener) {
        if (listener != null) {
            sendButton.addActionListener(listener);
        }
    }

    public void setLoading(boolean loading) {
        runOnEdt(() -> {
            this.loading = loading;
            commentInput.setEnabled(!loading);
            cancelReplyButton.setEnabled(!loading);
            sendButton.setText(loading ? "Đang gửi..." : "Gửi");
            sendButton.setIcon(loading
                    ? null
                    : MainTheme.svgIcon(MainTheme.IconType.SEND, 17, Color.WHITE));
            updateSendButtonState();
        });
    }

    public void showStatus(String message) {
        runOnEdt(() -> {
            statusLabel.setForeground(MainTheme.MUTED);
            statusLabel.setText(message == null ? " " : message);
        });
    }

    public void showError(String message) {
        runOnEdt(() -> {
            statusLabel.setForeground(MainTheme.DANGER);
            statusLabel.setText(message == null ? "Đã xảy ra lỗi" : message);
        });
    }

    private void updateSendButtonState() {
        sendButton.setEnabled(!loading && !commentInput.getText().trim().isEmpty());
    }

    private void updateCommentCount() {
        int count = commentsById.size();
        commentCountLabel.setText(count + " bình luận");
    }

    private void scrollToBottom() {
        SwingUtilities.invokeLater(() -> {
            JScrollBar bar = commentsScrollPane.getVerticalScrollBar();
            bar.setValue(bar.getMaximum());
        });
    }

    private void refreshCommentsPanel() {
        commentsPanel.revalidate();
        commentsPanel.repaint();
    }

    private String safeAuthor(String authorName) {
        return authorName == null || authorName.isBlank()
                ? "Người dùng StudyConnect"
                : authorName.trim();
    }

    private String preview(String content) {
        if (content == null || content.isBlank()) {
            return "Bình luận";
        }
        String normalized = content.trim().replaceAll("\\s+", " ");
        if (normalized.length() > 72) {
            normalized = normalized.substring(0, 69) + "...";
        }
        return escapeHtml(normalized);
    }

    private String formatTime(long createdAt) {
        if (createdAt <= 0) return "Vừa xong";

        Instant created = Instant.ofEpochMilli(createdAt);
        Instant now = Instant.now();
        if (created.isAfter(now.plusSeconds(60))) {
            return created.atZone(ZoneId.systemDefault())
                    .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        }

        long seconds = Math.max(0, Duration.between(created, now).getSeconds());
        if (seconds < 60) return "Vừa xong";

        long minutes = seconds / 60;
        if (minutes < 60) return minutes + " phút";

        long hours = minutes / 60;
        if (hours < 24) return hours + " giờ";

        long days = hours / 24;
        if (days < 7) return days + " ngày";

        return created.atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    private String escapeHtml(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;")
                .replace("\n", "<br>");
    }

    private void runOnEdt(Runnable action) {
        if (SwingUtilities.isEventDispatchThread()) {
            action.run();
        } else {
            SwingUtilities.invokeLater(action);
        }
    }

    private static final class HoverRoundedPanel extends MainTheme.RoundedPanel {
        private final Color normal;
        private final Color hover;
        private boolean hovered;

        private HoverRoundedPanel(int radius, Color normal, Color hover) {
            super(radius);
            this.normal = normal;
            this.hover = hover;
            setFill(normal);
            setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseEntered(java.awt.event.MouseEvent event) {
                    hovered = true;
                    setFill(HoverRoundedPanel.this.hover);
                    repaint();
                }

                @Override
                public void mouseExited(java.awt.event.MouseEvent event) {
                    hovered = false;
                    setFill(HoverRoundedPanel.this.normal);
                    repaint();
                }
            });
        }
    }
}
