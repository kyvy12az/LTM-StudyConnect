package com.studyconnect.client.view.component;

import com.studyconnect.client.network.file.FileTransferClient;
import com.studyconnect.common.dto.PostAttachmentDTO;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

public class PostCard extends MainTheme.RoundedPanel {
    private static final int MAX_IMAGE_WIDTH = 620;
    private static final int MAX_IMAGE_HEIGHT = 420;

    private final long postId;
    private final JButton likeButton;
    private final JButton commentButton;
    private int likeCount;
    private boolean liked;
    private int commentCount;

    public PostCard(
            long postId,
            String author,
            String authorAvatarUrl,
            String time,
            String topic,
            String title,
            String body,
            List<PostAttachmentDTO> attachments,
            FileTransferClient fileTransferClient,
            String authToken,
            int likes,
            boolean likedByCurrentUser,
            int comments
    ) {
        super(18);

        this.postId = postId;
        this.likeCount = Math.max(0, likes);
        this.liked = likedByCurrentUser;
        this.commentCount = Math.max(0, comments);
        likeButton = action(MainTheme.IconType.LIKE, "");
        updateLikeButton();
        commentButton = action(
                MainTheme.IconType.COMMENT,
                "Bình luận (" + commentCount + ")"
        );

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(new EmptyBorder(16, 18, 16, 22));
        setAlignmentX(Component.LEFT_ALIGNMENT);
        setMaximumSize(
                new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE)
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

        addAttachments(
                attachments,
                fileTransferClient,
                authToken
        );
        add(createActions());
    }

    private void addAttachments(
            List<PostAttachmentDTO> attachments,
            FileTransferClient fileTransferClient,
            String authToken
    ) {
        List<PostAttachmentDTO> safeAttachments = attachments == null
                ? Collections.emptyList()
                : attachments;

        for (PostAttachmentDTO attachment : safeAttachments) {
            if (attachment == null || attachment.getId() <= 0) {
                continue;
            }

            JComponent component;
            if (isImageAttachment(
                    attachment.getOriginalName(),
                    attachment.getMimeType()
            )) {
                component = createImageAttachment(
                        attachment,
                        fileTransferClient,
                        authToken
                );
            } else {
                component = new AttachmentCard(
                        valueOrEmpty(attachment.getOriginalName()),
                        formatAttachmentSize(attachment.getSizeBytes()),
                        resolveFileType(
                                attachment.getOriginalName(),
                                attachment.getMimeType()
                        )
                );
            }

            component.setAlignmentX(Component.LEFT_ALIGNMENT);
            add(component);
            add(Box.createVerticalStrut(9));
        }
    }

    private JComponent createImageAttachment(
            PostAttachmentDTO attachment,
            FileTransferClient fileTransferClient,
            String authToken
    ) {
        JPanel imageContainer = new JPanel(
                new FlowLayout(FlowLayout.LEFT, 0, 0)
        );
        imageContainer.setOpaque(false);
        imageContainer.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel imageLabel = new JLabel("Đang tải ảnh...");
        imageLabel.setFont(MainTheme.font(Font.PLAIN, 12));
        imageLabel.setForeground(MainTheme.MUTED);
        imageLabel.setHorizontalAlignment(SwingConstants.LEFT);
        imageLabel.setVerticalAlignment(SwingConstants.TOP);
        imageLabel.setBorder(new EmptyBorder(8, 0, 8, 0));
        imageContainer.add(imageLabel);

        if (fileTransferClient == null) {
            showImageError(imageLabel, null);
            return imageContainer;
        }

        new SwingWorker<ImageIcon, Void>() {
            @Override
            protected ImageIcon doInBackground() throws Exception {
                byte[] bytes = fileTransferClient.download(
                        attachment.getId(),
                        authToken
                );
                BufferedImage source = ImageIO.read(
                        new ByteArrayInputStream(bytes)
                );
                if (source == null) {
                    throw new IOException("Dữ liệu không phải ảnh hợp lệ");
                }
                return new ImageIcon(scaleImage(source));
            }

            @Override
            protected void done() {
                try {
                    ImageIcon icon = get();
                    imageLabel.setText("");
                    imageLabel.setIcon(icon);
                    imageLabel.setBorder(null);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    showImageError(imageLabel, exception);
                } catch (ExecutionException exception) {
                    showImageError(imageLabel, exception.getCause());
                }

                imageContainer.revalidate();
                imageContainer.repaint();
                PostCard.this.revalidate();
                PostCard.this.repaint();
            }
        }.execute();

        return imageContainer;
    }

    private BufferedImage scaleImage(BufferedImage source) {
        double scale = Math.min(
                1.0,
                Math.min(
                        (double) MAX_IMAGE_WIDTH / source.getWidth(),
                        (double) MAX_IMAGE_HEIGHT / source.getHeight()
                )
        );

        if (scale >= 1.0) {
            return source;
        }

        int width = Math.max(1, (int) Math.round(source.getWidth() * scale));
        int height = Math.max(1, (int) Math.round(source.getHeight() * scale));
        int imageType = source.getColorModel().hasAlpha()
                ? BufferedImage.TYPE_INT_ARGB
                : BufferedImage.TYPE_INT_RGB;
        BufferedImage scaled = new BufferedImage(width, height, imageType);
        Graphics2D graphics = scaled.createGraphics();
        try {
            graphics.setRenderingHint(
                    RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BICUBIC
            );
            graphics.setRenderingHint(
                    RenderingHints.KEY_RENDERING,
                    RenderingHints.VALUE_RENDER_QUALITY
            );
            graphics.drawImage(source, 0, 0, width, height, null);
        } finally {
            graphics.dispose();
        }
        return scaled;
    }

    private void showImageError(JLabel label, Throwable throwable) {
        label.setIcon(null);
        label.setText("Không thể tải ảnh.");
        label.setForeground(new Color(190, 55, 55));
        if (throwable != null && throwable.getMessage() != null) {
            label.setToolTipText(throwable.getMessage());
        }
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
        header.setMaximumSize(new Dimension(Integer.MAX_VALUE, 52));

        String safeAuthor = author == null || author.isBlank()
                ? "Người dùng StudyConnect"
                : author;
        AvatarView avatar = new AvatarView(safeAuthor, 46);
        if (authorAvatarUrl != null && !authorAvatarUrl.isBlank()) {
            if (authorAvatarUrl.startsWith("http://")
                    || authorAvatarUrl.startsWith("https://")) {
                avatar.setAvatarUrl(authorAvatarUrl);
            } else {
                avatar.setAvatarResource(authorAvatarUrl);
            }
        }

        JPanel identity = new JPanel();
        identity.setOpaque(false);
        identity.setLayout(new BoxLayout(identity, BoxLayout.Y_AXIS));
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
                topic == null || topic.isBlank() ? "#  Chung" : topic
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

    private JComponent createActions() {
        JPanel actions = new JPanel(new BorderLayout());
        actions.setOpaque(false);
        actions.setAlignmentX(Component.LEFT_ALIGNMENT);
        actions.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        left.setOpaque(false);
        left.add(likeButton);
        left.add(commentButton);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        right.setOpaque(false);
        right.add(action(MainTheme.IconType.SHARE, ""));
        right.add(action(MainTheme.IconType.MORE, ""));
        actions.add(left, BorderLayout.WEST);
        actions.add(right, BorderLayout.EAST);
        return actions;
    }

    private JButton action(MainTheme.IconType type, String text) {
        JButton button = new JButton(
                text,
                MainTheme.svgIcon(type, 18, MainTheme.NAVY)
        );
        button.setFont(MainTheme.font(Font.PLAIN, 12));
        button.setForeground(MainTheme.NAVY);
        button.setBorder(new EmptyBorder(3, 5, 3, 8));
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

    private static String resolveFileType(String fileName, String mimeType) {
        String mime = mimeType == null
                ? ""
                : mimeType.toLowerCase(Locale.ROOT);
        String name = fileName == null
                ? ""
                : fileName.toLowerCase(Locale.ROOT);

        if (mime.equals("application/pdf") || name.endsWith(".pdf")) return "PDF";
        if (mime.contains("wordprocessingml") || name.endsWith(".docx")) return "DOCX";
        if (mime.equals("application/msword") || name.endsWith(".doc")) return "DOC";
        if (mime.contains("spreadsheetml") || name.endsWith(".xlsx")) return "XLSX";
        if (mime.equals("application/vnd.ms-excel") || name.endsWith(".xls")) return "XLS";
        if (mime.contains("presentationml") || name.endsWith(".pptx")) return "PPTX";
        if (mime.startsWith("image/")
                || name.matches(".*\\.(png|jpg|jpeg|gif|webp)$")) return "IMAGE";
        if (name.endsWith(".zip")) return "ZIP";
        if (name.endsWith(".rar")) return "RAR";
        if (name.endsWith(".txt")) return "TXT";
        return "FILE";
    }

    private static boolean isImageAttachment(String fileName, String mimeType) {
        return "IMAGE".equals(resolveFileType(fileName, mimeType));
    }

    private String formatAttachmentSize(long bytes) {
        if (bytes < 1024) return Math.max(0, bytes) + " B";
        if (bytes < 1024L * 1024) {
            return String.format(Locale.ROOT, "%.1f KB", bytes / 1024.0);
        }
        return String.format(
                Locale.ROOT,
                "%.1f MB",
                bytes / (1024.0 * 1024.0)
        );
    }

    private String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }

    public long getPostId() {
        return postId;
    }

    public void addCommentListener(ActionListener listener) {
        if (listener != null) {
            commentButton.addActionListener(listener);
        }
    }

    public void addLikeListener(ActionListener listener) {
        if (listener != null) {
            likeButton.addActionListener(listener);
        }
    }

    public boolean isLiked() {
        return liked;
    }

    public void setLikeState(boolean liked, int likeCount) {
        this.liked = liked;
        this.likeCount = Math.max(0, likeCount);
        updateLikeButton();
    }

    public void setLikeCount(int likeCount) {
        this.likeCount = Math.max(0, likeCount);
        updateLikeButton();
    }

    public void setLikeLoading(boolean loading) {
        likeButton.setEnabled(!loading);
        if (loading) {
            likeButton.setText("Đang cập nhật...");
        } else {
            updateLikeButton();
        }
    }

    private void updateLikeButton() {
        Color color = liked ? new Color(24, 119, 242) : MainTheme.NAVY;
        likeButton.setText("Thích (" + likeCount + ")");
        likeButton.setForeground(color);
        likeButton.setIcon(
                MainTheme.svgIcon(MainTheme.IconType.LIKE, 18, color)
        );
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
        if (value == null) return "";
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;")
                .replace("\n", "<br>");
    }
}
