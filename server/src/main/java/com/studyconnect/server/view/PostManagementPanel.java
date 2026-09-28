package com.studyconnect.server.view;

import com.studyconnect.common.dto.PostAttachmentDTO;
import com.studyconnect.common.dto.PostDTO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionListener;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class PostManagementPanel extends JPanel {
    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final JTextField searchField = new JTextField();
    private final JComboBox<String> subjectFilter = new JComboBox<>();
    private final JButton refreshButton = ServerTheme.secondaryButton("Làm mới");
    private final JButton detailButton = ServerTheme.primaryButton("Xem chi tiết");
    private final JLabel statusLabel = new JLabel(" ");
    private final DefaultTableModel tableModel = new DefaultTableModel(
            new Object[]{"ID", "Tác giả", "Tiêu đề", "Chủ đề", "Thời gian tạo", "Bình luận", "Attachment"},
            0
    ) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(tableModel);
    private final List<PostDTO> posts = new ArrayList<>();
    private final List<PostDTO> visiblePosts = new ArrayList<>();

    public PostManagementPanel() {
        setLayout(new BorderLayout(0, 12));
        setBackground(ServerTheme.BACKGROUND);
        setBorder(new EmptyBorder(4, 4, 16, 10));
        add(createToolbar(), BorderLayout.NORTH);
        ServerTheme.configureTable(table);
        table.setAutoCreateRowSorter(true);
        add(new JScrollPane(table), BorderLayout.CENTER);
        statusLabel.setForeground(ServerTheme.MUTED);
        statusLabel.setFont(ServerTheme.font(Font.PLAIN, 12));
        add(statusLabel, BorderLayout.SOUTH);
        installFilters();
    }

    private JComponent createToolbar() {
        JPanel container = ServerTheme.card(new BorderLayout(10, 0));
        searchField.putClientProperty("JTextField.placeholderText", "Tìm tiêu đề, nội dung hoặc tác giả...");
        searchField.setPreferredSize(new Dimension(360, 38));
        subjectFilter.addItem("Tất cả chủ đề");

        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        filters.setOpaque(false);
        filters.add(searchField);
        filters.add(subjectFilter);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        actions.add(refreshButton);
        actions.add(detailButton);
        container.add(filters, BorderLayout.CENTER);
        container.add(actions, BorderLayout.EAST);
        return container;
    }

    private void installFilters() {
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent event) { applyFilters(); }
            public void removeUpdate(DocumentEvent event) { applyFilters(); }
            public void changedUpdate(DocumentEvent event) { applyFilters(); }
        });
        subjectFilter.addActionListener(event -> applyFilters());
    }

    public void setPosts(List<PostDTO> newPosts) {
        posts.clear();
        if (newPosts != null) posts.addAll(newPosts);
        posts.sort(Comparator.comparingLong(PostDTO::getCreatedAt).reversed());
        rebuildSubjects();
        applyFilters();
    }

    public void addOrUpdatePost(PostDTO post) {
        if (post == null || post.getId() <= 0) return;
        posts.removeIf(current -> current.getId() == post.getId());
        posts.add(post);
        posts.sort(Comparator.comparingLong(PostDTO::getCreatedAt).reversed());
        rebuildSubjects();
        applyFilters();
    }

    private void rebuildSubjects() {
        Object selected = subjectFilter.getSelectedItem();
        Set<String> subjects = new LinkedHashSet<>();
        for (PostDTO post : posts) {
            if (post.getSubject() != null && !post.getSubject().isBlank()) {
                subjects.add(post.getSubject());
            }
        }
        subjectFilter.removeAllItems();
        subjectFilter.addItem("Tất cả chủ đề");
        subjects.stream().sorted(String.CASE_INSENSITIVE_ORDER).forEach(subjectFilter::addItem);
        if (selected != null) subjectFilter.setSelectedItem(selected);
    }

    private void applyFilters() {
        String query = searchField.getText().trim().toLowerCase(Locale.ROOT);
        String subject = String.valueOf(subjectFilter.getSelectedItem());
        visiblePosts.clear();
        for (PostDTO post : posts) {
            String searchable = (safe(post.getTitle()) + " "
                    + safe(post.getContent()) + " "
                    + safe(post.getAuthorName())).toLowerCase(Locale.ROOT);
            boolean matchesQuery = query.isEmpty() || searchable.contains(query);
            boolean matchesSubject = subjectFilter.getSelectedIndex() <= 0
                    || subject.equalsIgnoreCase(safe(post.getSubject()));
            if (matchesQuery && matchesSubject) visiblePosts.add(post);
        }

        tableModel.setRowCount(0);
        for (PostDTO post : visiblePosts) {
            tableModel.addRow(new Object[]{
                    post.getId(),
                    post.getAuthorName(),
                    post.getTitle(),
                    post.getSubject(),
                    formatTime(post.getCreatedAt()),
                    post.getCommentCount(),
                    post.getAttachments() == null ? 0 : post.getAttachments().size()
            });
        }
        statusLabel.setForeground(ServerTheme.MUTED);
        statusLabel.setText("Hiển thị " + visiblePosts.size() + " / " + posts.size() + " bài viết");
    }

    public PostDTO getSelectedPost() {
        int selected = table.getSelectedRow();
        if (selected < 0) return null;
        int modelRow = table.convertRowIndexToModel(selected);
        if (modelRow < 0 || modelRow >= visiblePosts.size()) return null;
        return visiblePosts.get(modelRow);
    }

    public void showSelectedPostDetail(Component parent) {
        PostDTO post = getSelectedPost();
        if (post == null) {
            JOptionPane.showMessageDialog(parent, "Vui lòng chọn một bài viết.", "Bài viết", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        JTextArea content = new JTextArea(post.getContent());
        content.setEditable(false);
        content.setLineWrap(true);
        content.setWrapStyleWord(true);
        content.setRows(10);
        content.setFont(ServerTheme.font(Font.PLAIN, 13));

        JPanel details = new JPanel();
        details.setLayout(new BoxLayout(details, BoxLayout.Y_AXIS));
        details.add(new JLabel("Tác giả: " + safe(post.getAuthorName())));
        details.add(new JLabel("Chủ đề: " + safe(post.getSubject())));
        details.add(new JLabel("Thời gian: " + formatTime(post.getCreatedAt())));
        details.add(new JLabel("Số bình luận: " + post.getCommentCount()));
        details.add(Box.createVerticalStrut(8));
        details.add(new JLabel("Nội dung:"));
        details.add(new JScrollPane(content));
        details.add(Box.createVerticalStrut(8));
        details.add(new JLabel("Tệp đính kèm:"));

        List<PostAttachmentDTO> attachments = post.getAttachments();
        if (attachments == null || attachments.isEmpty()) {
            details.add(new JLabel("Không có tệp đính kèm"));
        } else {
            for (PostAttachmentDTO attachment : attachments) {
                details.add(new JLabel("• " + safe(attachment.getOriginalName())
                        + " — " + safe(attachment.getMimeType())
                        + " — " + formatSize(attachment.getSizeBytes())));
            }
        }

        JScrollPane scroll = new JScrollPane(details);
        scroll.setPreferredSize(new Dimension(620, 470));
        JOptionPane.showMessageDialog(
                parent,
                scroll,
                "Chi tiết bài viết #" + post.getId() + " — " + safe(post.getTitle()),
                JOptionPane.INFORMATION_MESSAGE
        );
    }

    public void setLoading(boolean loading) {
        refreshButton.setEnabled(!loading);
        if (loading) statusLabel.setText("Đang tải dữ liệu bài viết...");
    }

    public void showError(String message) {
        statusLabel.setForeground(ServerTheme.DANGER);
        statusLabel.setText(message == null ? "Không thể tải bài viết." : message);
    }

    public void addRefreshListener(ActionListener listener) { refreshButton.addActionListener(listener); }
    public void addDetailListener(ActionListener listener) { detailButton.addActionListener(listener); }

    private String formatTime(long value) {
        if (value <= 0) return "--";
        return Instant.ofEpochMilli(value).atZone(ZoneId.systemDefault()).format(DATE_TIME);
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) return Math.max(0, bytes) + " B";
        if (bytes < 1024L * 1024) return String.format(Locale.ROOT, "%.1f KB", bytes / 1024.0);
        return String.format(Locale.ROOT, "%.1f MB", bytes / (1024.0 * 1024.0));
    }

    private String safe(String value) { return value == null ? "" : value; }
}
