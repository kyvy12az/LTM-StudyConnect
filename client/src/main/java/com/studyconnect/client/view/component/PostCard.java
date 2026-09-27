package com.studyconnect.client.view.component;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionListener;

public class PostCard extends MainTheme.RoundedPanel {
    private final long postId;
    private final JButton commentButton;
    private int commentCount;

    public PostCard(
            long postId,
            String author,
            String authorAvatarUrl,
            String time,
            String topic,
            String title,
            String body,
            String fileName,
            String fileDetails,
            String fileType,
            int likes,
            int comments
    ) {
        super(18);

        this.postId = postId;
        this.commentCount = Math.max(0, comments);

        commentButton = action(MainTheme.IconType.COMMENT, "Bình luận (" + commentCount + ")");

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(new EmptyBorder(16, 18, 16, 22));
        setAlignmentX(Component.LEFT_ALIGNMENT);
        setMaximumSize(
                new Dimension(Integer.MAX_VALUE, 340)
        );

        add(createHeader(author, authorAvatarUrl, time, topic));
        add(Box.createVerticalStrut(10));

        JLabel titleLabel = new JLabel(
                "<html>" + escapeHtml(title) + "</html>"
        );
        titleLabel.setFont(MainTheme.font(Font.BOLD, 16));
        titleLabel.setForeground(MainTheme.TEXT);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        add(titleLabel);
        add(Box.createVerticalStrut(5));

        JLabel bodyLabel = new JLabel(
                "<html><div style='width:620px'>"
                        + escapeHtml(body)
                        + "</div></html>"
        );
        bodyLabel.setFont(MainTheme.font(Font.PLAIN, 13));
        bodyLabel.setForeground(MainTheme.TEXT);
        bodyLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        add(bodyLabel);
        add(Box.createVerticalStrut(10));

        // hiển thị AttachmentCard nếu có file đính kèm
        if (fileName != null && !fileName.isBlank()) {
            AttachmentCard attachment = new AttachmentCard(
                    fileName.trim(),
                    fileDetails == null ? "" : fileDetails,
                    fileType == null ? "" : fileType
            );

            attachment.setAlignmentX(Component.LEFT_ALIGNMENT);
            add(attachment);
            add(Box.createVerticalStrut(9));
        }

        add(createActions(likes, comments));
    }

    private JComponent createHeader(
            String author,
            String authorAvatarUrl,
            String time,
            String topic
    ) {
        JPanel header = new JPanel(new BorderLayout(10, 0));
        header.setOpaque(false);
        header.setAlignmentX(Component.LEFT_ALIGNMENT);
        header.setMaximumSize(
                new Dimension(Integer.MAX_VALUE, 52)
        );

        String safeAuthor =
                author == null || author.isBlank()
                        ? "Người dùng StudyConnect"
                        : author;

        AvatarView avatar = new AvatarView(safeAuthor, 46);

        if (authorAvatarUrl != null && !authorAvatarUrl.isBlank()) {
            if (authorAvatarUrl.startsWith("http://") || authorAvatarUrl.startsWith("https://")) {
                avatar.setAvatarUrl(authorAvatarUrl);
            } else {
                avatar.setAvatarResource(authorAvatarUrl);
            }
        }

        JPanel identity = new JPanel();
        identity.setOpaque(false);
        identity.setLayout(
                new BoxLayout(identity, BoxLayout.Y_AXIS)
        );

        JLabel name = new JLabel(safeAuthor);
        name.setFont(MainTheme.font(Font.BOLD, 14));
        name.setForeground(MainTheme.NAVY);

        JLabel when = new JLabel(
                (time == null ? "Vừa xong" : time)
                        + "  ·  Nhóm học tập"
        );
        when.setFont(MainTheme.font(Font.PLAIN, 11));
        when.setForeground(MainTheme.MUTED);

        identity.add(name);
        identity.add(Box.createVerticalStrut(3));
        identity.add(when);

        JPanel person = new JPanel(new BorderLayout(10, 0));
        person.setOpaque(false);
        person.add(avatar, BorderLayout.WEST);
        person.add(identity, BorderLayout.CENTER);

        JLabel tag = new JLabel(
                topic == null || topic.isBlank()
                        ? "#  Chung"
                        : topic
        );
        tag.setOpaque(true);
        tag.setBackground(new Color(225, 248, 242));
        tag.setForeground(MainTheme.TEAL_DARK);
        tag.setFont(MainTheme.font(Font.BOLD, 12));
        tag.setBorder(new EmptyBorder(7, 13, 7, 13));

        header.add(person, BorderLayout.CENTER);
        header.add(tag, BorderLayout.EAST);

        return header;
    }

    private JComponent createActions(int likes, int comments) {
        JPanel actions = new JPanel(new BorderLayout());
        actions.setOpaque(false);
        actions.setAlignmentX(Component.LEFT_ALIGNMENT);
        actions.setMaximumSize(
                new Dimension(Integer.MAX_VALUE, 30)
        );

        JPanel left = new JPanel(
                new FlowLayout(FlowLayout.LEFT, 6, 0)
        );
        left.setOpaque(false);
        left.add(action(
                MainTheme.IconType.LIKE,
                String.valueOf(Math.max(0, likes))
        ));
        left.add(commentButton);
        JPanel right = new JPanel(
                new FlowLayout(FlowLayout.RIGHT, 4, 0)
        );
        right.setOpaque(false);
        right.add(action(MainTheme.IconType.SHARE, ""));
        right.add(action(MainTheme.IconType.MORE, ""));

        actions.add(left, BorderLayout.WEST);
        actions.add(right, BorderLayout.EAST);

        return actions;
    }

    private JButton action(
            MainTheme.IconType type,
            String text
    ) {
        JButton button = new JButton(
                text,
                MainTheme.svgIcon(type, 18, MainTheme.NAVY)
        );

        button.setFont(MainTheme.font(Font.PLAIN, 12));
        button.setForeground(MainTheme.NAVY);
        button.setBorder(new EmptyBorder(3, 5, 3, 8));
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setCursor(
                Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        );

        return button;
    }

    public long getPostId() {
        return postId;
    }

    public void addCommentListener(ActionListener listener) {
        commentButton.addActionListener(listener);
    }

    @Deprecated
    public void addCommentListerner(ActionListener listener) {
        addCommentListener(listener);
    }

    public void setCommentCount(int commentCount) {
        this.commentCount = Math.max(0, commentCount);
        commentButton.setText("Bình luận (" + this.commentCount + ")");
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }

        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\n", "<br>");
    }
}
