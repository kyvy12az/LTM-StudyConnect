package com.studyconnect.client.view.profile;

import com.studyconnect.client.model.CurrentUser;
import com.studyconnect.client.network.file.FileTransferClient;
import com.studyconnect.client.view.component.AvatarView;
import com.studyconnect.client.view.component.MainTheme;
import com.studyconnect.client.view.component.PostCard;
import com.studyconnect.common.dto.PostAttachmentDTO;
import com.studyconnect.common.dto.PostDTO;
import com.studyconnect.common.dto.UserDTO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionListener;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Trang hồ sơ của người dùng đang đăng nhập.
 * Dữ liệu bài viết được MainFrame chuyển vào từ luồng GET_POSTS/Server Push hiện có.
 */
public class ProfilePanel extends JPanel {
    private static final Color SOFT_BACKGROUND = new Color(245, 251, 250);
    private static final Color CHIP_BACKGROUND = new Color(232, 247, 244);

    private final FileTransferClient fileTransferClient;
    private final UserDTO currentUser;
    private final MainTheme.VerticalPanel pageContent = new MainTheme.VerticalPanel();
    private final JPanel contentHost = new JPanel(new BorderLayout());
    private final MainTheme.VerticalPanel postsColumn = new MainTheme.VerticalPanel();
    private final JLabel postCountValue = createStatValue("0");
    private final Map<Long, PostDTO> profilePosts = new LinkedHashMap<>();
    private final Map<Long, PostCard> postCards = new LinkedHashMap<>();
    private final List<JToggleButton> tabButtons = new ArrayList<>();

    private Consumer<PostDTO> commentListener;
    private BiConsumer<Long, Boolean> likeListener;
    private Runnable openFeedAction;

    public ProfilePanel(FileTransferClient fileTransferClient) {
        super(new BorderLayout());
        this.fileTransferClient = fileTransferClient;
        this.currentUser = CurrentUser.getUser();

        setOpaque(false);
        initializeUI();
    }

    private void initializeUI() {
        pageContent.setOpaque(false);
        pageContent.setBorder(new EmptyBorder(16, 22, 28, 22));

        addFullWidth(pageContent, createProfileHeader());
        pageContent.add(Box.createVerticalStrut(12));
        addFullWidth(pageContent, createTabs());
        pageContent.add(Box.createVerticalStrut(14));

        contentHost.setOpaque(false);
        contentHost.add(createPostsPage(), BorderLayout.CENTER);
        contentHost.setAlignmentX(Component.LEFT_ALIGNMENT);
        contentHost.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        pageContent.add(contentHost);

        JScrollPane scrollPane = MainTheme.scrollPane(pageContent);
        scrollPane.getViewport().setBackground(SOFT_BACKGROUND);
        scrollPane.setBorder(null);
        add(scrollPane, BorderLayout.CENTER);
    }

    private JComponent createProfileHeader() {
        return new ProfileHeader();
    }

    private JComponent createTabs() {
        JPanel tabs = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        tabs.setOpaque(false);
        tabs.setBorder(new EmptyBorder(0, 4, 0, 4));
        tabs.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));

        addTab(tabs, "Bài viết", MainTheme.IconType.DOCUMENT, true,
                this::createPostsPage);
        addTab(tabs, "Giới thiệu", MainTheme.IconType.USER, false,
                this::createAboutPage);
        addTab(tabs, "Tài liệu", MainTheme.IconType.DOCUMENT, false,
                () -> createEmptySection(
                        "Tài liệu đã chia sẻ",
                        "Các tài liệu bạn chia sẻ sẽ xuất hiện tại đây.",
                        MainTheme.IconType.DOCUMENT
                ));
        addTab(tabs, "Hoạt động", MainTheme.IconType.CALENDAR, false,
                () -> createEmptySection(
                        "Hoạt động gần đây",
                        "Lịch sử hoạt động học tập sẽ được cập nhật tại đây.",
                        MainTheme.IconType.CALENDAR
                ));
        return tabs;
    }

    private void addTab(
            JPanel parent,
            String text,
            MainTheme.IconType icon,
            boolean selected,
            java.util.function.Supplier<JComponent> pageSupplier
    ) {
        JToggleButton button = new JToggleButton(
                text,
                MainTheme.svgIcon(
                        icon,
                        17,
                        selected ? MainTheme.TEAL_DARK : MainTheme.MUTED
                )
        );
        button.setFont(MainTheme.font(Font.BOLD, 13));
        button.setForeground(selected ? MainTheme.TEAL_DARK : MainTheme.MUTED);
        button.setSelected(selected);
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setBorder(new EmptyBorder(10, 15, 10, 15));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.putClientProperty("JButton.buttonType", "borderless");
        button.putClientProperty("JButton.selectedBackground", CHIP_BACKGROUND);

        button.addActionListener(event -> {
            for (JToggleButton tab : tabButtons) {
                boolean active = tab == button;
                tab.setSelected(active);
                tab.setForeground(active ? MainTheme.TEAL_DARK : MainTheme.MUTED);
            }
            showContent(pageSupplier.get());
        });

        tabButtons.add(button);
        parent.add(button);
    }

    private void showContent(JComponent component) {
        contentHost.removeAll();
        contentHost.add(component, BorderLayout.CENTER);
        contentHost.revalidate();
        contentHost.repaint();
    }

    private JComponent createPostsPage() {
        JPanel page = new JPanel(new BorderLayout(16, 0));
        page.setOpaque(false);
        page.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setPreferredSize(new Dimension(310, 0));
        left.add(createAboutCard());
        left.add(Box.createVerticalStrut(14));
        left.add(createSkillsCard());

        JPanel right = new JPanel();
        right.setOpaque(false);
        right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));
        right.add(createQuickComposer());
        right.add(Box.createVerticalStrut(14));

        postsColumn.setOpaque(false);
        postsColumn.setAlignmentX(Component.LEFT_ALIGNMENT);
        renderPosts();
        right.add(postsColumn);

        page.add(left, BorderLayout.WEST);
        page.add(right, BorderLayout.CENTER);
        return page;
    }

    private JComponent createAboutPage() {
        JPanel page = new JPanel(new GridLayout(1, 2, 16, 0));
        page.setOpaque(false);
        page.add(createAboutCard());
        page.add(createSkillsCard());
        return page;
    }

    private JComponent createAboutCard() {
        MainTheme.RoundedPanel card = createCard();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 230));

        card.add(sectionTitle("Giới thiệu", MainTheme.IconType.USER));
        card.add(Box.createVerticalStrut(14));
        card.add(infoRow(
                MainTheme.IconType.GRADUATION_CAP,
                "Sinh viên Công nghệ thông tin"
        ));
        card.add(Box.createVerticalStrut(11));
        card.add(infoRow(
                MainTheme.IconType.USER,
                valueOrDefault(currentUser == null ? null : currentUser.getEmail(),
                        "Chưa cập nhật email")
        ));
        card.add(Box.createVerticalStrut(11));
        card.add(infoRow(
                MainTheme.IconType.CALENDAR,
                "Tham gia StudyConnect"
        ));
        return card;
    }

    private JComponent createSkillsCard() {
        MainTheme.RoundedPanel card = createCard();
        card.setLayout(new BorderLayout(0, 13));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 190));
        card.add(sectionTitle("Kỹ năng", MainTheme.IconType.SUN), BorderLayout.NORTH);

        JPanel chips = new JPanel(new GridLayout(2, 2, 9, 9));
        chips.setOpaque(false);
        chips.add(skillChip("Java"));
        chips.add(skillChip("Java Swing"));
        chips.add(skillChip("AI"));
        chips.add(skillChip("Mạng máy tính"));
        card.add(chips, BorderLayout.CENTER);
        return card;
    }

    private JComponent createQuickComposer() {
        MainTheme.RoundedPanel card = createCard();
        card.setLayout(new BorderLayout(10, 0));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 78));

        AvatarView avatar = createCurrentAvatar(40);
        card.add(avatar, BorderLayout.WEST);

        JButton prompt = new JButton("Bạn đang nghĩ gì?");
        prompt.setHorizontalAlignment(SwingConstants.LEFT);
        prompt.setFont(MainTheme.font(Font.PLAIN, 13));
        prompt.setForeground(MainTheme.MUTED);
        prompt.setBackground(new Color(246, 249, 251));
        prompt.setFocusPainted(false);
        prompt.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        prompt.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(MainTheme.BORDER, 1, true),
                new EmptyBorder(10, 14, 10, 14)
        ));
        prompt.addActionListener(event -> {
            if (openFeedAction != null) {
                openFeedAction.run();
            }
        });
        card.add(prompt, BorderLayout.CENTER);

        JButton publish = MainTheme.textButton("Đăng bài", true);
        publish.setIcon(MainTheme.svgIcon(MainTheme.IconType.SEND, 16, Color.WHITE));
        publish.setIconTextGap(7);
        publish.addActionListener(event -> {
            if (openFeedAction != null) {
                openFeedAction.run();
            }
        });
        card.add(publish, BorderLayout.EAST);
        return card;
    }

    private JComponent createEmptySection(
            String title,
            String description,
            MainTheme.IconType iconType
    ) {
        JPanel wrapper = new JPanel(new GridBagLayout());
        wrapper.setOpaque(false);
        wrapper.setBorder(new EmptyBorder(50, 0, 80, 0));

        MainTheme.RoundedPanel card = createCard();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(32, 70, 32, 70));

        JLabel icon = new JLabel(MainTheme.svgIcon(iconType, 38, MainTheme.TEAL));
        icon.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel heading = new JLabel(title);
        heading.setFont(MainTheme.font(Font.BOLD, 18));
        heading.setForeground(MainTheme.NAVY);
        heading.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel detail = new JLabel(description);
        detail.setFont(MainTheme.font(Font.PLAIN, 12));
        detail.setForeground(MainTheme.MUTED);
        detail.setAlignmentX(Component.CENTER_ALIGNMENT);

        card.add(icon);
        card.add(Box.createVerticalStrut(12));
        card.add(heading);
        card.add(Box.createVerticalStrut(6));
        card.add(detail);
        wrapper.add(card);
        return wrapper;
    }

    private MainTheme.RoundedPanel createCard() {
        MainTheme.RoundedPanel card = new MainTheme.RoundedPanel(18);
        card.setBorder(new EmptyBorder(16, 18, 16, 18));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        return card;
    }

    private JComponent sectionTitle(String text, MainTheme.IconType iconType) {
        JPanel title = new JPanel(new FlowLayout(FlowLayout.LEFT, 9, 0));
        title.setOpaque(false);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        title.add(new JLabel(MainTheme.svgIcon(iconType, 19, MainTheme.NAVY)));
        JLabel label = new JLabel(text);
        label.setFont(MainTheme.font(Font.BOLD, 16));
        label.setForeground(MainTheme.NAVY);
        title.add(label);
        return title;
    }

    private JComponent infoRow(MainTheme.IconType iconType, String text) {
        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel icon = new JLabel(MainTheme.svgIcon(iconType, 17, MainTheme.MUTED));
        JLabel label = new JLabel("<html><div style='width:210px'>"
                + escapeHtml(text) + "</div></html>");
        label.setFont(MainTheme.font(Font.PLAIN, 12));
        label.setForeground(MainTheme.TEXT);
        row.add(icon, BorderLayout.WEST);
        row.add(label, BorderLayout.CENTER);
        return row;
    }

    private JComponent skillChip(String text) {
        JLabel chip = new JLabel(text, SwingConstants.CENTER);
        chip.setOpaque(true);
        chip.setBackground(CHIP_BACKGROUND);
        chip.setForeground(MainTheme.TEAL_DARK);
        chip.setFont(MainTheme.font(Font.BOLD, 11));
        chip.setBorder(new EmptyBorder(8, 10, 8, 10));
        return chip;
    }

    public void setPosts(List<PostDTO> posts) {
        Runnable update = () -> {
            profilePosts.clear();
            if (posts != null) {
                posts.stream()
                        .filter(this::belongsToCurrentUser)
                        .sorted(Comparator.comparingLong(PostDTO::getCreatedAt).reversed())
                        .forEach(post -> profilePosts.put(post.getId(), post));
            }
            renderPosts();
            updatePostCount();
        };
        runOnEdt(update);
    }

    public void addOrUpdatePost(PostDTO post) {
        if (post == null || post.getId() <= 0 || !belongsToCurrentUser(post)) {
            return;
        }
        runOnEdt(() -> {
            profilePosts.put(post.getId(), post);
            renderPosts();
            updatePostCount();
        });
    }

    private void renderPosts() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::renderPosts);
            return;
        }

        postsColumn.removeAll();
        postCards.clear();

        if (profilePosts.isEmpty()) {
            JLabel empty = new JLabel(
                    "Chưa có bài viết nào trên hồ sơ.",
                    SwingConstants.CENTER
            );
            empty.setFont(MainTheme.font(Font.PLAIN, 13));
            empty.setForeground(MainTheme.MUTED);
            empty.setBorder(new EmptyBorder(36, 18, 36, 18));
            empty.setAlignmentX(Component.LEFT_ALIGNMENT);
            empty.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));
            postsColumn.add(empty);
        } else {
            List<PostDTO> sorted = new ArrayList<>(profilePosts.values());
            sorted.sort(Comparator.comparingLong(PostDTO::getCreatedAt).reversed());

            for (int index = 0; index < sorted.size(); index++) {
                PostDTO post = sorted.get(index);
                PostCard card = createPostCard(post);
                postCards.put(post.getId(), card);
                card.setAlignmentX(Component.LEFT_ALIGNMENT);
                card.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
                postsColumn.add(card);
                if (index < sorted.size() - 1) {
                    postsColumn.add(Box.createVerticalStrut(14));
                }
            }
        }
        postsColumn.revalidate();
        postsColumn.repaint();
    }

    private PostCard createPostCard(PostDTO post) {
        String subject = valueOrDefault(post.getSubject(), "Chung");
        List<PostAttachmentDTO> attachments = post.getAttachments();

        PostCard card = new PostCard(
                post.getId(),
                valueOrDefault(post.getAuthorName(), displayName()),
                valueOrDefault(post.getAuthorAvatarUrl(), "/images/default-avatar.png"),
                formatCreatedAt(post.getCreatedAt()),
                subjectIcon(subject) + "  " + subject,
                valueOrDefault(post.getTitle(), "Bài viết"),
                valueOrDefault(post.getContent(), ""),
                attachments,
                fileTransferClient,
                CurrentUser.getToken(),
                Math.max(0, post.getLikeCount()),
                post.isLikedByCurrentUser(),
                Math.max(0, post.getCommentCount())
        );

        card.addCommentListener(event -> {
            if (commentListener != null) {
                commentListener.accept(post);
            }
        });
        card.addLikeListener(event -> {
            if (likeListener != null) {
                card.setLikeLoading(true);
                likeListener.accept(post.getId(), !card.isLiked());
            }
        });
        return card;
    }

    public void setCommentListener(Consumer<PostDTO> listener) {
        this.commentListener = listener;
    }

    public void setLikeListener(BiConsumer<Long, Boolean> listener) {
        this.likeListener = listener;
    }

    public void setOpenFeedAction(Runnable action) {
        this.openFeedAction = action;
    }

    public void updatePostLikeState(long postId, int count, Boolean liked) {
        runOnEdt(() -> {
            PostCard card = postCards.get(postId);
            if (card == null) return;
            if (liked == null) {
                card.setLikeCount(count);
            } else {
                card.setLikeState(liked, count);
            }
            card.setLikeLoading(false);
        });
    }

    public void setPostLikeLoading(long postId, boolean loading) {
        runOnEdt(() -> {
            PostCard card = postCards.get(postId);
            if (card != null) card.setLikeLoading(loading);
        });
    }

    public void updatePostCommentCount(long postId, int count) {
        runOnEdt(() -> {
            PostCard card = postCards.get(postId);
            if (card != null) card.setCommentCount(count);
        });
    }

    private boolean belongsToCurrentUser(PostDTO post) {
        if (post == null) return false;
        if (currentUser != null && currentUser.getId() > 0) {
            return post.getAuthorId() == currentUser.getId();
        }
        return valueOrDefault(post.getAuthorName(), "")
                .equalsIgnoreCase(displayName());
    }

    private void updatePostCount() {
        postCountValue.setText(String.valueOf(profilePosts.size()));
    }

    private AvatarView createCurrentAvatar(int size) {
        AvatarView avatar = new AvatarView(displayName(), size);
        avatar.setOnline(currentUser != null && currentUser.isOnline());
        configureAvatar(avatar, currentUser == null ? null : currentUser.getAvatarUrl());
        return avatar;
    }

    private void configureAvatar(AvatarView avatar, String avatarUrl) {
        if (avatarUrl == null || avatarUrl.isBlank()) {
            avatar.setAvatarResource("/images/default-avatar.png");
            return;
        }
        String normalized = avatarUrl.trim();
        if (normalized.startsWith("http://") || normalized.startsWith("https://")) {
            avatar.setAvatarUrl(normalized);
        } else if (normalized.startsWith("/")) {
            avatar.setAvatarResource(normalized);
        } else {
            avatar.setAvatarResource("/images/" + normalized);
        }
    }

    private String displayName() {
        if (currentUser == null) return "Người dùng StudyConnect";
        return valueOrDefault(
                currentUser.getFullName(),
                valueOrDefault(currentUser.getUsername(), "Người dùng StudyConnect")
        );
    }

    private String username() {
        if (currentUser == null) return "@studyconnect";
        return "@" + valueOrDefault(currentUser.getUsername(), "studyconnect");
    }

    private String subjectIcon(String subject) {
        String normalized = subject.toLowerCase();
        if (normalized.contains("lập trình")) return "</>";
        if (normalized.equals("ai") || normalized.contains("trí tuệ")) return "AI";
        if (normalized.contains("toán")) return "∑";
        if (normalized.contains("nhật")) return "あ";
        return "#";
    }

    private String formatCreatedAt(long createdAt) {
        if (createdAt <= 0) return "Vừa xong";
        Instant created = Instant.ofEpochMilli(createdAt);
        Instant now = Instant.now();
        if (created.isAfter(now)) return "Vừa xong";

        Duration duration = Duration.between(created, now);
        long minutes = duration.toMinutes();
        if (minutes < 1) return "Vừa xong";
        if (minutes < 60) return minutes + " phút trước";
        long hours = duration.toHours();
        if (hours < 24) return hours + " giờ trước";
        long days = duration.toDays();
        if (days < 7) return days + " ngày trước";
        return created.atZone(ZoneId.systemDefault()).toLocalDate().toString();
    }

    private void addFullWidth(JPanel panel, JComponent component) {
        component.setAlignmentX(Component.LEFT_ALIGNMENT);
        Dimension maximum = component.getMaximumSize();
        component.setMaximumSize(new Dimension(
                Integer.MAX_VALUE,
                maximum == null ? Integer.MAX_VALUE : maximum.height
        ));
        panel.add(component);
    }

    private String valueOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    private void runOnEdt(Runnable action) {
        if (SwingUtilities.isEventDispatchThread()) action.run();
        else SwingUtilities.invokeLater(action);
    }

    private static JLabel createStatValue(String value) {
        JLabel label = new JLabel(value, SwingConstants.CENTER);
        label.setFont(MainTheme.font(Font.BOLD, 21));
        label.setForeground(MainTheme.NAVY);
        return label;
    }

    private final class ProfileHeader extends MainTheme.RoundedPanel {
        private final CoverBanner cover = new CoverBanner();
        private final AvatarView avatar = createCurrentAvatar(98);
        private final JPanel identity = createIdentity();
        private final JPanel actions = createActions();
        private final JPanel stats = createStats();

        private ProfileHeader() {
            super(22);
            setLayout(null);
            setPreferredSize(new Dimension(920, 255));
            setMinimumSize(new Dimension(760, 255));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 255));
            add(cover);
            add(avatar);
            add(identity);
            add(actions);
            add(stats);
        }

        @Override
        public void doLayout() {
            int width = getWidth();
            cover.setBounds(0, 0, Math.max(0, width - 4), 128);
            avatar.setBounds(28, 86, 108, 108);
            identity.setBounds(150, 137, 330, 84);
            actions.setBounds(Math.max(490, width - 344), 140, 315, 44);
            stats.setBounds(Math.max(470, width - 590), 194, 560, 48);
        }

        private JPanel createIdentity() {
            JPanel panel = new JPanel();
            panel.setOpaque(false);
            panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

            JLabel name = new JLabel(displayName());
            name.setFont(MainTheme.font(Font.BOLD, 23));
            name.setForeground(MainTheme.NAVY);

            JLabel account = new JLabel(username());
            account.setFont(MainTheme.font(Font.PLAIN, 12));
            account.setForeground(MainTheme.MUTED);

            JLabel role = new JLabel("Sinh viên Công nghệ thông tin");
            role.setFont(MainTheme.font(Font.PLAIN, 12));
            role.setForeground(MainTheme.TEXT);

            JLabel online = new JLabel("●  "
                    + (currentUser != null && currentUser.isOnline()
                    ? "Đang trực tuyến" : "Ngoại tuyến"));
            online.setFont(MainTheme.font(Font.BOLD, 11));
            online.setForeground(currentUser != null && currentUser.isOnline()
                    ? MainTheme.SUCCESS : MainTheme.MUTED);

            panel.add(name);
            panel.add(Box.createVerticalStrut(2));
            panel.add(account);
            panel.add(Box.createVerticalStrut(3));
            panel.add(role);
            panel.add(Box.createVerticalStrut(3));
            panel.add(online);
            return panel;
        }

        private JPanel createActions() {
            JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
            panel.setOpaque(false);

            JButton edit = MainTheme.textButton("Chỉnh sửa hồ sơ", true);
            edit.setIcon(MainTheme.svgIcon(MainTheme.IconType.EDIT, 16, Color.WHITE));
            edit.setIconTextGap(7);
            edit.addActionListener(event -> JOptionPane.showMessageDialog(
                    ProfilePanel.this,
                    "Chức năng cập nhật hồ sơ sẽ được kết nối với server ở bước tiếp theo.",
                    "Chỉnh sửa hồ sơ",
                    JOptionPane.INFORMATION_MESSAGE
            ));

            JButton share = MainTheme.textButton("Chia sẻ", false);
            share.setIcon(MainTheme.svgIcon(MainTheme.IconType.SHARE, 16, MainTheme.NAVY));
            share.setForeground(MainTheme.NAVY);
            share.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(MainTheme.BORDER, 1, true),
                    new EmptyBorder(8, 13, 8, 13)
            ));
            share.addActionListener(event -> shareProfile());

            panel.add(edit);
            panel.add(share);
            return panel;
        }

        private JPanel createStats() {
            JPanel panel = new JPanel(new GridLayout(1, 4, 0, 0));
            panel.setOpaque(false);
            panel.add(statItem(postCountValue, "Bài viết"));
            panel.add(statItem(createStatValue("0"), "Bạn bè"));
            panel.add(statItem(createStatValue("0"), "Nhóm học tập"));
            panel.add(statItem(createStatValue("0"), "Tài liệu"));
            return panel;
        }

        private JComponent statItem(JLabel value, String labelText) {
            JPanel panel = new JPanel();
            panel.setOpaque(false);
            panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
            value.setAlignmentX(Component.CENTER_ALIGNMENT);
            JLabel label = new JLabel(labelText);
            label.setFont(MainTheme.font(Font.PLAIN, 11));
            label.setForeground(MainTheme.MUTED);
            label.setAlignmentX(Component.CENTER_ALIGNMENT);
            panel.add(value);
            panel.add(Box.createVerticalStrut(2));
            panel.add(label);
            return panel;
        }
    }

    private void shareProfile() {
        String text = "StudyConnect - " + displayName() + " (" + username() + ")";
        try {
            Toolkit.getDefaultToolkit().getSystemClipboard()
                    .setContents(new StringSelection(text), null);
            JOptionPane.showMessageDialog(
                    this,
                    "Đã sao chép thông tin hồ sơ.",
                    "Chia sẻ hồ sơ",
                    JOptionPane.INFORMATION_MESSAGE
            );
        } catch (IllegalStateException exception) {
            JOptionPane.showMessageDialog(
                    this,
                    "Không thể truy cập clipboard lúc này.",
                    "Chia sẻ hồ sơ",
                    JOptionPane.WARNING_MESSAGE
            );
        }
    }

    private static final class CoverBanner extends JComponent {
        private CoverBanner() {
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);

            Shape clip = new RoundRectangle2D.Double(
                    0, 0, getWidth(), getHeight() + 22, 22, 22
            );
            g.setClip(clip);
            g.setPaint(new GradientPaint(
                    0, 0, new Color(211, 251, 239),
                    getWidth(), getHeight(), new Color(189, 230, 249)
            ));
            g.fillRect(0, 0, getWidth(), getHeight());

            paintMountain(g, new Color(132, 218, 203, 115), 0.60, 0.40);
            paintMountain(g, new Color(91, 185, 184, 95), 0.76, 0.22);

            g.setColor(new Color(255, 255, 255, 145));
            g.fillOval(getWidth() - 190, 18, 82, 38);
            g.fillOval(getWidth() - 145, 8, 100, 48);
            g.dispose();
        }

        private void paintMountain(
                Graphics2D g,
                Color color,
                double baseline,
                double peak
        ) {
            int width = getWidth();
            int height = getHeight();
            Path2D path = new Path2D.Double();
            path.moveTo(0, height);
            path.lineTo(0, height * baseline);
            path.curveTo(
                    width * .18, height * peak,
                    width * .28, height * .86,
                    width * .46, height * .50
            );
            path.curveTo(
                    width * .62, height * .20,
                    width * .76, height * .82,
                    width, height * .42
            );
            path.lineTo(width, height);
            path.closePath();
            g.setColor(color);
            g.fill(path);
        }
    }
}
