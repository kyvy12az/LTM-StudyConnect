package com.studyconnect.client.view.component;

import com.studyconnect.common.dto.CommentDTO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionListener;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CommentDialog extends JDialog {
    private static final int MAX_RENDER_DEPTH = 4;
    private static final int REPLY_INDENT = 32;
    private static final Color DIALOG_BACKGROUND = new Color(255, 255, 255);
    private static final Color BUBBLE_BACKGROUND = new Color(240, 242, 245);
    private static final Color INPUT_BACKGROUND = new Color(240, 242, 245);
    private static final Color META_TEXT = new Color(101, 103, 107);
    private static final Color LINK_TEXT = new Color(79, 81, 86);

    private final JPanel commentsPanel = new JPanel();
    private final JScrollPane commentsScrollPane;
    private final JTextArea commentInput = new JTextArea(2, 20);
    private final JButton sendButton =
            MainTheme.textButton("Gửi", true);
    private final JLabel statusLabel =
            new JLabel(" ", SwingConstants.LEFT);
    private final JPanel replyPanel =
            new JPanel(new BorderLayout(8, 0));
    private final JLabel replyLabel = new JLabel();
    private final JButton cancelReplyButton =
            createLinkButton("Hủy");
    private final Map<Long, CommentDTO> commentsById =
            new LinkedHashMap<>();

    private CommentDTO replyTarget;

    public CommentDialog(JFrame owner, String postTitle) {
        super(owner, "Bình luận", true);

        setSize(690, 720);
        setMinimumSize(new Dimension(560, 540));
        setLocationRelativeTo(owner);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        commentsPanel.setBackground(DIALOG_BACKGROUND);
        commentsPanel.setLayout(
                new BoxLayout(commentsPanel, BoxLayout.Y_AXIS)
        );
        commentsPanel.setBorder(new EmptyBorder(12, 16, 18, 16));

        commentsScrollPane = MainTheme.scrollPane(commentsPanel);
        configureCommentsScrollPane();
        initializeUI(postTitle);
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
        JPanel header = new JPanel();
        header.setBackground(DIALOG_BACKGROUND);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setBorder(new EmptyBorder(16, 20, 12, 20));

        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.setOpaque(false);

        JLabel title = new JLabel("Bình luận");
        title.setFont(MainTheme.font(Font.BOLD, 20));
        title.setForeground(MainTheme.NAVY);

        JLabel subtitle = new JLabel(
                "<html><div style='width:520px'>"
                        + escapeHtml(postTitle)
                        + "</div></html>"
        );
        subtitle.setFont(MainTheme.font(Font.PLAIN, 12));
        subtitle.setForeground(MainTheme.MUTED);

        titleRow.add(title, BorderLayout.WEST);
        header.add(titleRow);
        header.add(Box.createVerticalStrut(4));
        header.add(subtitle);
        header.add(Box.createVerticalStrut(12));

        JSeparator separator = new JSeparator();
        separator.setForeground(MainTheme.BORDER);
        separator.setMaximumSize(
                new Dimension(Integer.MAX_VALUE, 1)
        );
        header.add(separator);
        return header;
    }

    private void configureCommentsScrollPane() {
        commentsScrollPane.setBorder(null);
        commentsScrollPane.setBackground(DIALOG_BACKGROUND);
        commentsScrollPane.getViewport().setBackground(DIALOG_BACKGROUND);
        commentsScrollPane.getVerticalScrollBar().setUnitIncrement(16);
        commentsScrollPane.setHorizontalScrollBarPolicy(
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
        );
    }

    private JComponent createComposer() {
        JPanel container = new JPanel(new BorderLayout(0, 8));
        container.setBackground(DIALOG_BACKGROUND);
        container.setBorder(new EmptyBorder(10, 16, 14, 16));

        JSeparator separator = new JSeparator();
        separator.setForeground(MainTheme.BORDER);
        container.add(separator, BorderLayout.NORTH);

        JPanel composerBody = new JPanel(new BorderLayout(0, 8));
        composerBody.setOpaque(false);
        composerBody.setBorder(new EmptyBorder(8, 0, 0, 0));

        JPanel composerState = new JPanel();
        composerState.setOpaque(false);
        composerState.setLayout(
                new BoxLayout(composerState, BoxLayout.Y_AXIS)
        );

        configureReplyPanel();
        statusLabel.setFont(MainTheme.font(Font.PLAIN, 11));
        statusLabel.setForeground(MainTheme.MUTED);
        statusLabel.setBorder(new EmptyBorder(2, 4, 0, 4));
        statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        replyPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        composerState.add(replyPanel);
        composerState.add(statusLabel);

        JPanel inputRow = new JPanel(new BorderLayout(10, 0));
        inputRow.setOpaque(false);
        inputRow.add(createInputBubble(), BorderLayout.CENTER);

        sendButton.setFont(MainTheme.font(Font.BOLD, 12));
        sendButton.setPreferredSize(new Dimension(72, 42));
        sendButton.setMinimumSize(new Dimension(72, 42));
        inputRow.add(sendButton, BorderLayout.EAST);

        composerBody.add(composerState, BorderLayout.NORTH);
        composerBody.add(inputRow, BorderLayout.CENTER);
        container.add(composerBody, BorderLayout.CENTER);
        return container;
    }

    private void configureReplyPanel() {
        replyPanel.setOpaque(false);
        replyPanel.setVisible(false);
        replyPanel.setBorder(new EmptyBorder(0, 4, 3, 4));

        replyLabel.setFont(MainTheme.font(Font.PLAIN, 12));
        replyLabel.setForeground(MainTheme.MUTED);

        cancelReplyButton.setFont(MainTheme.font(Font.BOLD, 12));
        cancelReplyButton.addActionListener(
                event -> clearReplyTarget()
        );

        replyPanel.add(replyLabel, BorderLayout.CENTER);
        replyPanel.add(cancelReplyButton, BorderLayout.EAST);
    }

    private JComponent createInputBubble() {
        MainTheme.RoundedPanel inputBubble =
                new MainTheme.RoundedPanel(24);
        inputBubble.setLayout(new BorderLayout());
        inputBubble.setFill(INPUT_BACKGROUND);
        inputBubble.setBorder(new EmptyBorder(7, 13, 7, 13));
        inputBubble.setPreferredSize(new Dimension(460, 58));

        commentInput.setLineWrap(true);
        commentInput.setWrapStyleWord(true);
        commentInput.setFont(MainTheme.font(Font.PLAIN, 13));
        commentInput.setForeground(MainTheme.TEXT);
        commentInput.setBackground(INPUT_BACKGROUND);
        commentInput.setOpaque(false);
        commentInput.setBorder(null);
        commentInput.setToolTipText("Viết bình luận...");

        JScrollPane inputScroll = new JScrollPane(commentInput);
        inputScroll.setBorder(null);
        inputScroll.setOpaque(false);
        inputScroll.getViewport().setOpaque(false);
        inputScroll.setHorizontalScrollBarPolicy(
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
        );
        inputScroll.setVerticalScrollBarPolicy(
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED
        );

        inputBubble.add(inputScroll, BorderLayout.CENTER);
        return inputBubble;
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
                children.computeIfAbsent(
                        parentId,
                        ignored -> new ArrayList<>()
                ).add(comment);
            }
        }

        Set<Long> rendered = new HashSet<>();
        Set<Long> visiting = new HashSet<>();

        for (CommentDTO root : roots) {
            renderNode(root, 0, children, rendered, visiting);
        }

        // Dữ liệu parent bị lỗi hoặc tạo vòng vẫn được hiển thị an toàn.
        for (CommentDTO comment : commentsById.values()) {
            if (!rendered.contains(comment.getId())) {
                renderNode(comment, 0, children, rendered, visiting);
            }
        }

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

        if (rendered.contains(commentId)
                || !visiting.add(commentId)) {
            return;
        }

        rendered.add(commentId);
        commentsPanel.add(createCommentItem(comment, depth));
        commentsPanel.add(Box.createVerticalStrut(7));

        List<CommentDTO> replies = children.get(commentId);
        if (replies != null) {
            for (CommentDTO reply : replies) {
                renderNode(
                        reply,
                        depth + 1,
                        children,
                        rendered,
                        visiting
                );
            }
        }

        visiting.remove(commentId);
    }

    private void showEmptyState() {
        JPanel emptyPanel = new JPanel();
        emptyPanel.setOpaque(false);
        emptyPanel.setLayout(
                new BoxLayout(emptyPanel, BoxLayout.Y_AXIS)
        );
        emptyPanel.setBorder(new EmptyBorder(65, 20, 40, 20));
        emptyPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        emptyPanel.setMaximumSize(
                new Dimension(Integer.MAX_VALUE, 180)
        );

        JLabel title = new JLabel("Chưa có bình luận");
        title.setFont(MainTheme.font(Font.BOLD, 16));
        title.setForeground(MainTheme.NAVY);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitle = new JLabel(
                "Hãy là người đầu tiên chia sẻ suy nghĩ của bạn."
        );
        subtitle.setFont(MainTheme.font(Font.PLAIN, 12));
        subtitle.setForeground(MainTheme.MUTED);
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        emptyPanel.add(title);
        emptyPanel.add(Box.createVerticalStrut(6));
        emptyPanel.add(subtitle);
        commentsPanel.add(emptyPanel);
    }

    private JComponent createCommentItem(CommentDTO comment, int depth) {
        int visualDepth = Math.min(
                Math.max(0, depth),
                MAX_RENDER_DEPTH
        );

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.setBorder(
                new EmptyBorder(
                        0,
                        visualDepth * REPLY_INDENT,
                        0,
                        6
                )
        );
        wrapper.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setOpaque(false);

        String author = safeAuthor(comment.getAuthorName());
        int avatarSize = visualDepth == 0 ? 38 : 32;
        AvatarView avatar = new AvatarView(author, avatarSize);
        configureCommentAvatar(
                avatar,
                comment.getAuthorAvatarUrl()
        );

        JPanel commentBody = new JPanel();
        commentBody.setOpaque(false);
        commentBody.setLayout(
                new BoxLayout(commentBody, BoxLayout.Y_AXIS)
        );

        MainTheme.RoundedPanel bubble =
                new MainTheme.RoundedPanel(18);
        bubble.setLayout(new BoxLayout(bubble, BoxLayout.Y_AXIS));
        bubble.setFill(BUBBLE_BACKGROUND);
        bubble.setBorder(new EmptyBorder(8, 12, 9, 12));
        bubble.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel authorLabel = new JLabel(escapeHtml(author));
        authorLabel.setFont(MainTheme.font(Font.BOLD, 12));
        authorLabel.setForeground(MainTheme.TEXT);
        authorLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        int textWidth = Math.max(
                230,
                430 - visualDepth * REPLY_INDENT
        );

        JLabel bodyLabel = new JLabel(
                "<html><div style='width:"
                        + textWidth
                        + "px'>"
                        + escapeHtml(comment.getContent())
                        + "</div></html>"
        );
        bodyLabel.setFont(MainTheme.font(Font.PLAIN, 13));
        bodyLabel.setForeground(MainTheme.TEXT);
        bodyLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        bubble.add(authorLabel);
        bubble.add(Box.createVerticalStrut(2));
        bubble.add(bodyLabel);

        JPanel metadata = new JPanel(
                new FlowLayout(FlowLayout.LEFT, 10, 0)
        );
        metadata.setOpaque(false);
        metadata.setBorder(new EmptyBorder(3, 8, 0, 0));
        metadata.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel timeLabel = new JLabel(
                formatTime(comment.getCreatedAt())
        );
        timeLabel.setFont(MainTheme.font(Font.PLAIN, 11));
        timeLabel.setForeground(META_TEXT);

        JButton replyButton = createLinkButton("Trả lời");
        replyButton.addActionListener(
                event -> selectReply(comment)
        );

        metadata.add(timeLabel);
        metadata.add(replyButton);

        commentBody.add(bubble);
        commentBody.add(metadata);

        JPanel avatarHolder = new JPanel(
                new FlowLayout(FlowLayout.LEFT, 0, 0)
        );
        avatarHolder.setOpaque(false);
        avatarHolder.add(avatar);

        row.add(avatarHolder, BorderLayout.WEST);
        row.add(commentBody, BorderLayout.CENTER);
        wrapper.add(row, BorderLayout.CENTER);

        int preferredHeight = Math.max(
                avatarSize + 16,
                wrapper.getPreferredSize().height
        );
        wrapper.setMaximumSize(
                new Dimension(Integer.MAX_VALUE, preferredHeight)
        );
        return wrapper;
    }

    private void configureCommentAvatar(
            AvatarView avatar,
            String avatarUrl
    ) {
        if (avatarUrl == null || avatarUrl.isBlank()) {
            avatar.setAvatarResource(
                    "/images/default-avatar.png"
            );
            return;
        }

        String normalizedUrl = avatarUrl.trim();

        if (normalizedUrl.startsWith("http://")
                || normalizedUrl.startsWith("https://")) {
            avatar.setAvatarUrl(normalizedUrl);
        } else if (normalizedUrl.startsWith("/")) {
            avatar.setAvatarResource(normalizedUrl);
        } else {
            avatar.setAvatarResource(
                    "/images/" + normalizedUrl
            );
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
        button.setCursor(
                Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        );
        button.putClientProperty("JButton.buttonType", "borderless");
        return button;
    }

    private void selectReply(CommentDTO comment) {
        replyTarget = comment;
        String author = safeAuthor(comment.getAuthorName());

        replyLabel.setText(
                "<html>Đang trả lời <b>"
                        + escapeHtml(author)
                        + "</b></html>"
        );
        replyPanel.setVisible(true);
        replyPanel.revalidate();
        commentInput.requestFocusInWindow();
    }

    public Long getReplyParentId() {
        return replyTarget == null
                ? null
                : replyTarget.getId();
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
        runOnEdt(() -> commentInput.setText(""));
    }

    public void addSendListener(ActionListener listener) {
        if (listener != null) {
            sendButton.addActionListener(listener);
        }
    }

    public void setLoading(boolean loading) {
        runOnEdt(() -> {
            sendButton.setEnabled(!loading);
            commentInput.setEnabled(!loading);
            cancelReplyButton.setEnabled(!loading);
            sendButton.setText(loading ? "Đang gửi..." : "Gửi");
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
            statusLabel.setForeground(new Color(210, 45, 45));
            statusLabel.setText(
                    message == null ? "Đã xảy ra lỗi" : message
            );
        });
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

    private String formatTime(long createdAt) {
        if (createdAt <= 0) {
            return "Vừa xong";
        }

        Instant created = Instant.ofEpochMilli(createdAt);
        Instant now = Instant.now();

        if (created.isAfter(now.plusSeconds(60))) {
            return created
                    .atZone(ZoneId.systemDefault())
                    .format(
                            DateTimeFormatter.ofPattern(
                                    "dd/MM/yyyy HH:mm"
                            )
                    );
        }

        Duration duration = Duration.between(created, now);
        long seconds = Math.max(0, duration.getSeconds());

        if (seconds < 60) return "Vừa xong";

        long minutes = seconds / 60;
        if (minutes < 60) return minutes + " phút";

        long hours = minutes / 60;
        if (hours < 24) return hours + " giờ";

        long days = hours / 24;
        if (days < 7) return days + " ngày";

        return created
                .atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }

        return value
                .replace("&", "&amp;")
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
}
