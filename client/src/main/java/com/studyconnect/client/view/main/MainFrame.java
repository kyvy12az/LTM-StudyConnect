package com.studyconnect.client.view.main;

import com.studyconnect.client.model.CurrentUser;
import com.studyconnect.client.network.file.FileTransferClient;
import com.studyconnect.client.view.component.*;
import com.studyconnect.client.view.message.MessagesPanel;
import com.studyconnect.client.view.profile.ProfilePanel;
import com.studyconnect.common.dto.CreatePostDTO;
import com.studyconnect.common.dto.PostAttachmentDTO;
import com.studyconnect.common.dto.PostDTO;
import com.studyconnect.common.dto.UserDTO;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.BiConsumer;
import java.awt.event.ActionListener;

public class MainFrame extends JFrame {
    private final CardLayout contentLayout = new CardLayout();
    private final JPanel contentDeck = new JPanel(contentLayout);
    private final List<SidebarMenuButton> menuButtons = new ArrayList<>();
    private final FileTransferClient fileTransferClient;
    private final ProfilePanel profilePanel;
    private final String displayName = resolveDisplayName();

    private final JTextField postTitleField = new JTextField();
    private final JTextArea postContentArea = new JTextArea(3, 20);
    private final JButton createPostButton = MainTheme.textButton(
            "Tạo bài viết",
            true
    );
    private final MainTheme.VerticalPanel postsContainer =
            new MainTheme.VerticalPanel();
    private final JLabel postsStatusLabel = new JLabel(
            "Đang tải bài viết...",
            SwingConstants.CENTER
    );
    private final List<Consumer<String>> categoryListeners =
            new ArrayList<>();
    private final Map<Long, PostCard> postCardsById =
            new HashMap<>();

    private final JPanel onlineUsersPanel = new JPanel();
    private final JPanel messagesHost = new JPanel(new BorderLayout());
    private SidebarMenuButton messagesMenuButton;
    private Runnable messagesPageListener;

    private String selectedSubject;

    private Consumer<PostDTO> commentListener;
    private BiConsumer<Long, Boolean> likeListener;

    // file
    private static final int MAX_ATTACHMENTS = 5;
    private final List<File> selectedAttachments = new ArrayList<>();
    private final JPanel attachmentPreviewPanel  = new JPanel();
    private final JButton selectImageButton =
            composerAction(
                    MainTheme.IconType.IMAGE,
                    "Ảnh",
                    MainTheme.TEAL
            );

    private final JButton selectDocumentButton =
            composerAction(
                    MainTheme.IconType.DOCUMENT,
                    "Tài liệu",
                    new Color(30, 126, 229)
            );

    public MainFrame(FileTransferClient fileTransferClient) {
        if (fileTransferClient == null) {
            throw new IllegalArgumentException(
                    "FileTransferClient không được null"
            );
        }
        this.fileTransferClient = fileTransferClient;
        this.profilePanel = new ProfilePanel(fileTransferClient);
        setTitle("StudyConnect - Kết nối tri thức");
        setSize(1440, 900);
        setMinimumSize(new Dimension(1180, 720));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        AuthTheme.installFrameIcon(this);
        initializeUI();
    }

    private void initializeUI() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(MainTheme.BACKGROUND);
        root.add(createSidebar(), BorderLayout.WEST);
        JPanel application = new JPanel(new BorderLayout());
        application.setOpaque(false);
        application.add(createTopBar(), BorderLayout.NORTH);
        contentDeck.setOpaque(false);
        contentDeck.add(createFeedPage(), "feed");
        messagesHost.setOpaque(false);
        messagesHost.add(createPlaceholder(
                "Tin nhắn",
                "Đang khởi tạo chức năng nhắn tin...",
                MainTheme.IconType.CHAT
        ), BorderLayout.CENTER);
        contentDeck.add(messagesHost, "messages");
        contentDeck.add(createPlaceholder("Nhóm học tập", "Cùng tạo và tham gia các nhóm học tập.", MainTheme.IconType.GROUP), "groups");
        contentDeck.add(createPlaceholder("Tài liệu", "Kho tài liệu học tập của bạn.", MainTheme.IconType.DOCUMENT), "documents");
        contentDeck.add(profilePanel, "profile");
        profilePanel.setOpenFeedAction(() -> {
            if (!menuButtons.isEmpty()) {
                SidebarMenuButton feedButton = menuButtons.get(0);
                for (SidebarMenuButton item : menuButtons) {
                    item.setActive(item == feedButton);
                }
            }
            contentLayout.show(contentDeck, "feed");
        });
        application.add(contentDeck, BorderLayout.CENTER);
        root.add(application, BorderLayout.CENTER);
        setContentPane(root);
    }

    private JComponent createSidebar() {
        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setBackground(Color.WHITE);
        sidebar.setPreferredSize(new Dimension(272, 0));
        sidebar.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, MainTheme.BORDER));
        JPanel upper = new JPanel();
        upper.setOpaque(false);
        upper.setLayout(new BoxLayout(upper, BoxLayout.Y_AXIS));
        upper.setBorder(new EmptyBorder(25, 15, 0, 15));
        upper.add(createBrand());
        upper.add(Box.createVerticalStrut(38));
        addMenu(upper, "Bảng tin", MainTheme.IconType.HOME, null, "feed", true);
        messagesMenuButton = addMenu(
                upper, "Tin nhắn", MainTheme.IconType.CHAT,
                null, "messages", false
        );
        addMenu(upper, "Nhóm học tập", MainTheme.IconType.GROUP, null, "groups", false);
        addMenu(upper, "Tài liệu", MainTheme.IconType.DOCUMENT, null, "documents", false);
        addMenu(upper, "Hồ sơ", MainTheme.IconType.USER, null, "profile", false);
        sidebar.add(upper, BorderLayout.NORTH);
        sidebar.add(new SidebarDecoration(), BorderLayout.SOUTH);
        return sidebar;
    }

    private JComponent createBrand() {
        JPanel brand = new JPanel(new BorderLayout(10, 0));
        brand.setOpaque(false);
        brand.setMaximumSize(
                new Dimension(Integer.MAX_VALUE, 70)
        );

        JLabel logo = new JLabel();
        logo.setHorizontalAlignment(SwingConstants.CENTER);
        logo.setVerticalAlignment(SwingConstants.CENTER);
        logo.setPreferredSize(new Dimension(62, 58));

        URL logoUrl = MainFrame.class.getResource(
                "/images/logo-final-56.png"
        );

        if (logoUrl != null) {
            logo.setIcon(
                    loadSharpPng(logoUrl, 56)
            );
        } else {
            logo.setIcon(
                    MainTheme.svgIcon(
                            MainTheme.IconType.HOME,
                            44,
                            MainTheme.TEAL
                    )
            );
        }

        JPanel words = new JPanel();
        words.setOpaque(false);
        words.setLayout(new BoxLayout(words, BoxLayout.Y_AXIS));

        JLabel name = new JLabel(
                "<html>"
                        + "<span style='color:#073b70'>Study</span>"
                        + "<span style='color:#009b83'>Connect</span>"
                        + "</html>"
        );
        name.setFont(MainTheme.font(Font.BOLD, 22));

        JLabel slogan = new JLabel(
                "Kết nối tri thức - Cùng nhau tiến bộ"
        );
        slogan.setFont(MainTheme.font(Font.PLAIN, 10));
        slogan.setForeground(MainTheme.MUTED);

        words.add(Box.createVerticalStrut(8));
        words.add(name);
        words.add(Box.createVerticalStrut(3));
        words.add(slogan);

        brand.add(logo, BorderLayout.WEST);
        brand.add(words, BorderLayout.CENTER);

        return brand;
    }

    private ImageIcon loadHighQualityIcon(
            URL imageUrl,
            int targetWidth,
            int targetHeight
    ) {
        try {
            BufferedImage source = ImageIO.read(imageUrl);

            BufferedImage scaled = new BufferedImage(
                    targetWidth,
                    targetHeight,
                    BufferedImage.TYPE_INT_ARGB
            );

            Graphics2D g2 = scaled.createGraphics();

            g2.setRenderingHint(
                    RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BICUBIC
            );

            g2.setRenderingHint(
                    RenderingHints.KEY_RENDERING,
                    RenderingHints.VALUE_RENDER_QUALITY
            );

            g2.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON
            );

            g2.setRenderingHint(
                    RenderingHints.KEY_ALPHA_INTERPOLATION,
                    RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY
            );

            double scale = Math.min(
                    (double) targetWidth / source.getWidth(),
                    (double) targetHeight / source.getHeight()
            );

            int drawWidth = (int) Math.round(
                    source.getWidth() * scale
            );

            int drawHeight = (int) Math.round(
                    source.getHeight() * scale
            );

            int x = (targetWidth - drawWidth) / 2;
            int y = (targetHeight - drawHeight) / 2;

            g2.drawImage(
                    source,
                    x,
                    y,
                    drawWidth,
                    drawHeight,
                    null
            );

            g2.dispose();

            return new ImageIcon(scaled);

        } catch (IOException exception) {
            System.err.println(
                    "Không thể đọc logo: " + exception.getMessage()
            );

            return null;
        }
    }

    private ImageIcon loadSharpPng(
            URL imageUrl,
            int targetSize
    ) {
        try {
            BufferedImage source = ImageIO.read(imageUrl);

            // Cắt khoảng trống trong suốt
            source = cropTransparentArea(source);

            // Thu nhỏ dần để tránh ảnh bị nhòe
            while (source.getWidth() / 2 >= targetSize
                    && source.getHeight() / 2 >= targetSize) {

                source = resizeImage(
                        source,
                        source.getWidth() / 2,
                        source.getHeight() / 2
                );
            }

            // Tạo canvas vuông trong suốt
            BufferedImage result = new BufferedImage(
                    targetSize,
                    targetSize,
                    BufferedImage.TYPE_INT_ARGB
            );

            Graphics2D g2 = result.createGraphics();

            g2.setRenderingHint(
                    RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BICUBIC
            );

            g2.setRenderingHint(
                    RenderingHints.KEY_RENDERING,
                    RenderingHints.VALUE_RENDER_QUALITY
            );

            g2.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON
            );

            double scale = Math.min(
                    (double) targetSize / source.getWidth(),
                    (double) targetSize / source.getHeight()
            );

            int drawWidth = (int) Math.round(
                    source.getWidth() * scale
            );

            int drawHeight = (int) Math.round(
                    source.getHeight() * scale
            );

            int x = (targetSize - drawWidth) / 2;
            int y = (targetSize - drawHeight) / 2;

            g2.drawImage(
                    source,
                    x,
                    y,
                    drawWidth,
                    drawHeight,
                    null
            );

            g2.dispose();

            return new ImageIcon(result);

        } catch (IOException exception) {
            System.err.println(
                    "Không thể tải logo: " + exception.getMessage()
            );
            return null;
        }
    }

    private BufferedImage resizeImage(
            BufferedImage source,
            int width,
            int height
    ) {
        BufferedImage result = new BufferedImage(
                width,
                height,
                BufferedImage.TYPE_INT_ARGB
        );

        Graphics2D g2 = result.createGraphics();

        g2.setRenderingHint(
                RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BICUBIC
        );

        g2.setRenderingHint(
                RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_QUALITY
        );

        g2.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON
        );

        g2.drawImage(
                source,
                0,
                0,
                width,
                height,
                null
        );

        g2.dispose();

        return result;
    }

    private BufferedImage cropTransparentArea(
            BufferedImage source
    ) {
        int minX = source.getWidth();
        int minY = source.getHeight();
        int maxX = -1;
        int maxY = -1;

        for (int y = 0; y < source.getHeight(); y++) {
            for (int x = 0; x < source.getWidth(); x++) {
                int alpha = (source.getRGB(x, y) >>> 24) & 0xFF;

                if (alpha > 10) {
                    minX = Math.min(minX, x);
                    minY = Math.min(minY, y);
                    maxX = Math.max(maxX, x);
                    maxY = Math.max(maxY, y);
                }
            }
        }

        if (maxX < minX || maxY < minY) {
            return source;
        }

        return source.getSubimage(
                minX,
                minY,
                maxX - minX + 1,
                maxY - minY + 1
        );
    }

    private SidebarMenuButton addMenu(JPanel parent, String label, MainTheme.IconType icon, String badge, String card, boolean active) {
        SidebarMenuButton button = new SidebarMenuButton(label, icon, badge);
        button.setActive(active);
        // Keep the same center alignment as the brand so BoxLayout grants the
        // button the full sidebar width instead of collapsing it by half.
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        button.addActionListener(event -> {
            for (SidebarMenuButton item : menuButtons) item.setActive(item == button);
            contentLayout.show(contentDeck, card);
            if ("messages".equals(card) && messagesPageListener != null) {
                messagesPageListener.run();
            }
        });
        menuButtons.add(button); parent.add(button); parent.add(Box.createVerticalStrut(5));
        return button;
    }

    private JComponent createTopBar() {
        JPanel top = new JPanel(new BorderLayout(20, 0));
        top.setBackground(new Color(249, 253, 252));
        top.setBorder(new EmptyBorder(12, 24, 11, 22));
        top.setPreferredSize(new Dimension(0, 66));
        top.add(new MainTheme.SearchField("Tìm kiếm bài viết, tài liệu, nhóm học tập, bạn bè..."), BorderLayout.CENTER);
        NotificationButton notification = new NotificationButton();
        notification.addActionListener(event -> JOptionPane.showMessageDialog(this, "Bạn có 2 thông báo mới.", "Thông báo", JOptionPane.INFORMATION_MESSAGE));
        top.add(notification, BorderLayout.EAST);
        return top;
    }

    private JComponent createFeedPage() {
        JPanel page = new JPanel(new BorderLayout(18, 0));
        page.setOpaque(false);
        page.setBorder(new EmptyBorder(7, 22, 14, 12));
        JScrollPane feedScroll = MainTheme.scrollPane(createFeedContent());
        feedScroll.getViewport().setBackground(MainTheme.BACKGROUND);
        page.add(feedScroll, BorderLayout.CENTER);
        JScrollPane sidebarScroll = MainTheme.scrollPane(createRightColumn());
        sidebarScroll.setPreferredSize(new Dimension(350, 0));
        sidebarScroll.getViewport().setBackground(MainTheme.BACKGROUND);
        page.add(sidebarScroll, BorderLayout.EAST);
        return page;
    }

    private JComponent createFeedContent() {
        MainTheme.VerticalPanel feed =
                new MainTheme.VerticalPanel();

        feed.setOpaque(false);
        feed.setBorder(new EmptyBorder(12, 8, 24, 10));

        // Lời chào
        addFeedComponent(feed, createGreeting());
        addFeedSpacing(feed, 16);

        // Khung tạo bài viết
        addFeedComponent(feed, createComposer());
        addFeedSpacing(feed, 16);

        // Danh mục
        addFeedComponent(feed, createCategories());
        addFeedSpacing(feed, 14);

        postsContainer.setOpaque(false);
        postsContainer.setAlignmentX(Component.LEFT_ALIGNMENT);
        postsContainer.setMaximumSize(
                new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE)
        );

        configurePostsStatusLabel();
        postsContainer.add(postsStatusLabel);

        addFeedComponent(feed, postsContainer);
        addFeedSpacing(feed, 20);

        return feed;
    }

    private void addFeedComponent(
            MainTheme.VerticalPanel feed,
            JComponent component
    ) {
        component.setAlignmentX(Component.LEFT_ALIGNMENT);

        Dimension maximumSize = component.getMaximumSize();

        int maximumHeight = maximumSize != null
                ? maximumSize.height
                : Integer.MAX_VALUE;

        component.setMaximumSize(
                new Dimension(
                        Integer.MAX_VALUE,
                        maximumHeight
                )
        );

        feed.add(component);
    }

    private void addFeedSpacing(
            MainTheme.VerticalPanel feed,
            int height
    ) {
        feed.add(
                Box.createRigidArea(
                        new Dimension(0, height)
                )
        );
    }

    private JComponent createGreeting() {
        JPanel greeting = new JPanel(new BorderLayout(16, 0));
        greeting.setOpaque(false);
        greeting.setMaximumSize(new Dimension(Integer.MAX_VALUE, 82));
        greeting.setAlignmentX(Component.LEFT_ALIGNMENT);
        greeting.add(new JLabel(new SunIcon(58)), BorderLayout.WEST);
        JPanel text = new JPanel();
        text.setOpaque(false); text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        JLabel title = new JLabel("Chào " + greetingPeriod() + ", " + displayName + "!");
        title.setFont(MainTheme.font(Font.BOLD, 25)); title.setForeground(MainTheme.NAVY);
        JLabel subtitle = new JLabel("Hôm nay cũng là một ngày tuyệt vời để học hỏi và kết nối!");
        subtitle.setFont(MainTheme.font(Font.PLAIN, 14)); subtitle.setForeground(MainTheme.MUTED);
        text.add(Box.createVerticalStrut(7)); text.add(title); text.add(Box.createVerticalStrut(5)); text.add(subtitle);
        greeting.add(text, BorderLayout.CENTER);
        return greeting;
    }

    private JComponent createComposer() {
        MainTheme.RoundedPanel composer =
                new MainTheme.RoundedPanel(18);

        composer.setLayout(new BorderLayout(13, 10));
        composer.setBorder(new EmptyBorder(14, 16, 14, 18));
        composer.setMaximumSize(
                new Dimension(Integer.MAX_VALUE, 340)
        );
        composer.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Avatar
        AvatarView avatar = createCurrentUserAvatar(48);
//        avatar.setOnline(true);

        JPanel avatarWrapper = new JPanel(
                new FlowLayout(
                        FlowLayout.CENTER,
                        0,
                        0
                )
        );

        avatarWrapper.setOpaque(false);

        Dimension avatarWrapperSize =
                new Dimension(58, 58);

        avatarWrapper.setPreferredSize(
                avatarWrapperSize
        );

        avatarWrapper.setMinimumSize(
                avatarWrapperSize
        );

        avatarWrapper.setMaximumSize(
                avatarWrapperSize
        );

        avatarWrapper.add(avatar);

        // Ô nhập tiêu đề
        postTitleField.setFont(MainTheme.font(Font.BOLD, 14));
        postTitleField.setForeground(MainTheme.TEXT);
        postTitleField.setBackground(Color.WHITE);
        postTitleField.setCaretColor(MainTheme.TEAL);
        postTitleField.setPreferredSize(new Dimension(100, 42));
        postTitleField.setMaximumSize(
                new Dimension(Integer.MAX_VALUE, 42)
        );
        postTitleField.putClientProperty(
                "JTextField.placeholderText",
                "Tiêu đề bài viết (tối đa 200 ký tự)"
        );
        postTitleField.putClientProperty(
                "FlatLaf.style",
                """
                arc: 14;
                borderWidth: 1;
                focusWidth: 1;
                innerFocusWidth: 0;
                margin: 0,12,0,12;
                """
        );

        // Ô nhập nội dung nhiều dòng
        postContentArea.setFont(MainTheme.font(Font.PLAIN, 14));
        postContentArea.setForeground(MainTheme.TEXT);
        postContentArea.setBackground(Color.WHITE);
        postContentArea.setCaretColor(MainTheme.TEAL);
        postContentArea.setLineWrap(true);
        postContentArea.setWrapStyleWord(true);
        postContentArea.setRows(3);
        postContentArea.setMargin(new Insets(9, 11, 9, 11));
        postContentArea.putClientProperty(
                "JTextArea.placeholderText",
                "Nội dung bạn muốn chia sẻ..."
        );

        JScrollPane contentScroll = new JScrollPane(postContentArea);
        contentScroll.setPreferredSize(new Dimension(100, 82));
        contentScroll.setMinimumSize(new Dimension(100, 72));
        contentScroll.setBorder(
                BorderFactory.createLineBorder(MainTheme.BORDER, 1, true)
        );
        contentScroll.getViewport().setBackground(Color.WHITE);
        contentScroll.setHorizontalScrollBarPolicy(
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
        );
        contentScroll.putClientProperty(
                "JScrollPane.smoothScrolling",
                true
        );

        JPanel fields = new JPanel();
        fields.setOpaque(false);
        fields.setLayout(new BoxLayout(fields, BoxLayout.Y_AXIS));
        fields.add(postTitleField);
        fields.add(Box.createVerticalStrut(8));
        fields.add(contentScroll);

        attachmentPreviewPanel.setOpaque(false);
        attachmentPreviewPanel.setLayout(
                new BoxLayout(
                        attachmentPreviewPanel,
                        BoxLayout.Y_AXIS
                )
        );
        attachmentPreviewPanel.setVisible(false);

        fields.add(Box.createVerticalStrut(8));
        fields.add(attachmentPreviewPanel);

        postTitleField.addActionListener(
                event -> postContentArea.requestFocusInWindow()
        );

        JPanel inputRow = new JPanel(
                new BorderLayout(12, 0)
        );

        inputRow.setOpaque(false);

        inputRow.add(
                avatarWrapper,
                BorderLayout.WEST
        );

        inputRow.add(
                fields,
                BorderLayout.CENTER
        );

        composer.add(inputRow, BorderLayout.CENTER);

        // Khu vực hành động
        JPanel actions = new JPanel(new BorderLayout());
        actions.setOpaque(false);

        JPanel types = new JPanel(
                new FlowLayout(FlowLayout.LEFT, 12, 0)
        );
        types.setOpaque(false);

//        types.add(
//                composerAction(
//                        MainTheme.IconType.IMAGE,
//                        "Ảnh",
//                        MainTheme.TEAL
//                )
//        );
//
//        types.add(
//                composerAction(
//                        MainTheme.IconType.DOCUMENT,
//                        "Tài liệu",
//                        new Color(30, 126, 229)
//                )
//        );

        selectImageButton.addActionListener(
                event -> chooseAttachments(true)
        );

        selectDocumentButton.addActionListener(
                event -> chooseAttachments(false)
        );

        types.add(selectImageButton);
        types.add(selectDocumentButton);

        types.add(
                composerAction(
                        MainTheme.IconType.POLL,
                        "Thăm dò ý kiến",
                        MainTheme.TEAL
                )
        );

        types.add(
                composerAction(
                        MainTheme.IconType.SMILE,
                        "Cảm xúc",
                        new Color(245, 166, 35)
                )
        );

        actions.add(types, BorderLayout.CENTER);

        // Nút tạo bài viết
        createPostButton.setIcon(
                MainTheme.svgIcon(
                        MainTheme.IconType.SEND,
                        20,
                        Color.WHITE
                )
        );

        createPostButton.setIconTextGap(8);

        actions.add(createPostButton, BorderLayout.EAST);
        composer.add(actions, BorderLayout.SOUTH);

        return composer;
    }

    private void chooseAttachments(boolean imagesOnly) {
        JFileChooser chooser = new JFileChooser();

        chooser.setMultiSelectionEnabled(true);
        chooser.setAcceptAllFileFilterUsed(false);

        if (imagesOnly) {
            chooser.setDialogTitle("Chọn hình ảnh");
            chooser.setFileFilter(
                    new FileNameExtensionFilter(
                            "Hình ảnh (*.png, *.jpg, *.jpeg, *.gif, *.webp)",
                            "png",
                            "jpg",
                            "jpeg",
                            "gif",
                            "webp"
                    )
            );
        } else {
            chooser.setDialogTitle("Chọn tài liệu");
            chooser.setFileFilter(
                    new FileNameExtensionFilter(
                            "Tài liệu",
                            "pdf",
                            "doc",
                            "docx",
                            "xls",
                            "xlsx",
                            "ppt",
                            "pptx",
                            "txt"
                    )
            );
        }

        if (chooser.showOpenDialog(this)
                != JFileChooser.APPROVE_OPTION) {
            return;
        }

        for (File file : chooser.getSelectedFiles()) {
            if (selectedAttachments.size()
                    >= MAX_ATTACHMENTS) {
                showError(
                        "Mỗi bài viết chỉ được đính kèm tối đa "
                                + MAX_ATTACHMENTS
                                + " tệp."
                );
                break;
            }

            if (!selectedAttachments.contains(file)) {
                selectedAttachments.add(file);
            }
        }

        refreshAttachmentPreview();
    }

    private void refreshAttachmentPreview() {
        attachmentPreviewPanel.removeAll();

        for (File file :
                new ArrayList<>(selectedAttachments)) {

            JPanel row = new JPanel(
                    new BorderLayout(8, 0)
            );
            row.setOpaque(false);
            row.setBorder(
                    new EmptyBorder(4, 8, 4, 8)
            );

            JLabel nameLabel = new JLabel(
                    file.getName()
                            + "  •  "
                            + formatFileSize(file.length())
            );

            nameLabel.setFont(
                    MainTheme.font(Font.PLAIN, 12)
            );
            nameLabel.setForeground(MainTheme.TEXT);

            JButton removeButton = new JButton("×");
            removeButton.setToolTipText("Xóa tệp");
            removeButton.setContentAreaFilled(false);
            removeButton.setFocusPainted(false);
            removeButton.setBorder(
                    new EmptyBorder(2, 8, 2, 8)
            );

            removeButton.addActionListener(event -> {
                selectedAttachments.remove(file);
                refreshAttachmentPreview();
            });

            row.add(nameLabel, BorderLayout.CENTER);
            row.add(removeButton, BorderLayout.EAST);

            attachmentPreviewPanel.add(row);
        }

        attachmentPreviewPanel.setVisible(
                !selectedAttachments.isEmpty()
        );

        attachmentPreviewPanel.revalidate();
        attachmentPreviewPanel.repaint();
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }

        if (bytes < 1024 * 1024) {
            return String.format(
                    "%.1f KB",
                    bytes / 1024.0
            );
        }

        return String.format(
                "%.1f MB",
                bytes / (1024.0 * 1024.0)
        );
    }

    public List<File> getSelectedAttachments() {
        return new ArrayList<>(selectedAttachments);
    }

    private JButton composerAction(MainTheme.IconType icon, String text, Color color) {
        JButton button = new JButton(text, MainTheme.svgIcon(icon, 21, color));
        button.setFont(MainTheme.font(Font.PLAIN, 13)); button.setForeground(MainTheme.TEXT);
        button.setBorder(new EmptyBorder(7, 7, 7, 7)); button.setContentAreaFilled(false);
        button.setFocusPainted(false); button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

    private JComponent createCategories() {
        JPanel categories = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        categories.setOpaque(false); categories.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        categories.setAlignmentX(Component.LEFT_ALIGNMENT);
        List<CategoryChip> chips = new ArrayList<>();
        for (String name : List.of("Tất cả", "Lập trình", "AI", "Toán", "Tiếng Nhật")) {
            CategoryChip chip = new CategoryChip(name);
            chip.addActionListener(event -> {
                for (CategoryChip item : chips) {
                    item.setActive(item == chip);
                }

                selectedSubject = "Tất cả".equalsIgnoreCase(name)
                        ? null
                        : name;

                notifyCategorySelected(selectedSubject);
            });
            chips.add(chip); categories.add(chip);
        }
        chips.get(0).setActive(true);
        return categories;
    }

    private JComponent createRightColumn() {
        MainTheme.VerticalPanel right = new MainTheme.VerticalPanel();
        right.setBorder(new EmptyBorder(0, 0, 15, 4));
        right.add(createProfileCard()); right.add(Box.createVerticalStrut(12));
        right.add(createOnlineCard()); right.add(Box.createVerticalStrut(12));
        right.add(createScheduleCard());
        return right;
    }

    public void setMessagesPanel(MessagesPanel panel) {
        if (panel == null) {
            throw new IllegalArgumentException("MessagesPanel không được null");
        }
        runOnEdt(() -> {
            messagesHost.removeAll();
            messagesHost.add(panel, BorderLayout.CENTER);
            messagesHost.revalidate();
            messagesHost.repaint();
        });
    }

    public void setMessagesPageListener(Runnable listener) {
        this.messagesPageListener = listener;
    }

    public void setMessageUnreadCount(int count) {
        runOnEdt(() -> {
            if (messagesMenuButton != null) {
                messagesMenuButton.setBadge(
                        count <= 0 ? null : String.valueOf(Math.min(count, 99))
                );
            }
        });
    }

    private AvatarView createCurrentUserAvatar(int size) {
        UserDTO user = CurrentUser.getUser();

        String name = displayName;

        if (user != null
                && user.getFullName() != null
                && !user.getFullName().isBlank()) {
            name = user.getFullName().trim();
        }

        AvatarView avatar = new AvatarView(name, size);

        avatar.setOnline(
                user != null && user.isOnline()
        );

        String avatarUrl = user == null
                ? null
                : user.getAvatarUrl();

        // Không có avatar thì sử dụng ảnh mặc định.
        if (avatarUrl == null || avatarUrl.isBlank()) {
            avatar.setAvatarResource("/images/default-avatar.png");

            return avatar;
        }
        avatarUrl = avatarUrl.trim();
        if (avatarUrl.startsWith("http://") || avatarUrl.startsWith("https://")) {
            avatar.setAvatarUrl(avatarUrl);
        } else if (avatarUrl.startsWith("/")) {
            avatar.setAvatarResource(avatarUrl);
        } else {
            avatar.setAvatarResource("/images/" + avatarUrl);
        }

        return avatar;
    }

    private JComponent createProfileCard() {
        MainTheme.RoundedPanel card = new MainTheme.RoundedPanel(18);
        card.setLayout(new BorderLayout(12, 8)); card.setBorder(new EmptyBorder(16, 18, 14, 18));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 155)); card.setFill(new Color(243, 252, 249));
        AvatarView avatar = createCurrentUserAvatar(72);
        JPanel identity = new JPanel(); identity.setOpaque(false); identity.setLayout(new BoxLayout(identity, BoxLayout.Y_AXIS));
        JLabel name = new JLabel(displayName); name.setFont(MainTheme.font(Font.BOLD, 18)); name.setForeground(MainTheme.NAVY);
        JLabel role = new JLabel("Sinh viên CNTT"); role.setFont(MainTheme.font(Font.PLAIN, 13)); role.setForeground(MainTheme.MUTED);
        JLabel online = new JLabel("●  Trực tuyến"); online.setFont(MainTheme.font(Font.BOLD, 12)); online.setForeground(MainTheme.SUCCESS);
        identity.add(name); identity.add(Box.createVerticalStrut(4)); identity.add(role); identity.add(Box.createVerticalStrut(3)); identity.add(online);
        card.add(avatar, BorderLayout.WEST); card.add(identity, BorderLayout.CENTER);
        JButton edit = new JButton("Chỉnh sửa hồ sơ", MainTheme.svgIcon(MainTheme.IconType.EDIT, 16, MainTheme.NAVY));
        edit.setFont(MainTheme.font(Font.PLAIN, 12)); edit.setFocusPainted(false); edit.setBackground(Color.WHITE);
        edit.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(MainTheme.BORDER), new EmptyBorder(6, 10, 6, 10)));
        card.add(edit, BorderLayout.SOUTH);
        return card;
    }

    private JComponent createOnlineCard() {
        MainTheme.RoundedPanel card =
                sectionCard(
                        "Đang trực tuyến",
                        "Xem tất cả"
                );

        onlineUsersPanel.setOpaque(false);
        onlineUsersPanel.setLayout(
                new BoxLayout(
                        onlineUsersPanel,
                        BoxLayout.Y_AXIS
                )
        );

        JLabel loadingLabel =
                new JLabel("Đang tải...");

        loadingLabel.setFont(
                MainTheme.font(
                        Font.PLAIN,
                        12
                )
        );

        loadingLabel.setForeground(
                MainTheme.MUTED
        );

        onlineUsersPanel.add(loadingLabel);

        card.add(
                onlineUsersPanel,
                BorderLayout.CENTER
        );

        card.setMaximumSize(
                new Dimension(
                        Integer.MAX_VALUE,
                        320
                )
        );

        return card;
    }

    public void displayOnlineUsers(
            List<UserDTO> users
    ) {
        List<UserDTO> safeUsers =
                users == null
                        ? Collections.emptyList()
                        : new ArrayList<>(users);

        runOnEdt(() -> {
            onlineUsersPanel.removeAll();

            UserDTO currentUser =
                    CurrentUser.getUser();

            ///  không hiển thị chính mình trong danh sách
            safeUsers.removeIf(user ->
                    user == null
                            || (currentUser != null
                            && user.getId()
                            == currentUser.getId())
            );

            if (safeUsers.isEmpty()) {
                JLabel emptyLabel =
                        new JLabel(
                                "Chưa có người dùng khác online"
                        );

                emptyLabel.setFont(
                        MainTheme.font(
                                Font.PLAIN,
                                12
                        )
                );

                emptyLabel.setForeground(
                        MainTheme.MUTED
                );

                emptyLabel.setBorder(
                        new EmptyBorder(
                                12,
                                0,
                                12,
                                0
                        )
                );

                onlineUsersPanel.add(emptyLabel);

            } else {
                for (int index = 0;
                     index < safeUsers.size();
                     index++) {

                    onlineUsersPanel.add(
                            new OnlineUserItem(
                                    safeUsers.get(index)
                            )
                    );

                    if (index
                            < safeUsers.size() - 1) {
                        onlineUsersPanel.add(
                                Box.createVerticalStrut(7)
                        );
                    }
                }
            }

            onlineUsersPanel.revalidate();
            onlineUsersPanel.repaint();
        });
    }

    private JComponent createScheduleCard() {
        MainTheme.RoundedPanel card = sectionCard("Lịch học sắp tới", "Xem tất cả");
        JPanel list = sectionBody(card);
        list.add(new ScheduleItem("Cấu trúc dữ liệu và giải thuật", "Thứ 3, 14/01/2025", "08:00 - 09:30", new Color(22, 181, 111), MainTheme.IconType.DOCUMENT));
        list.add(Box.createVerticalStrut(6));
        list.add(new ScheduleItem("Toán xác suất thống kê", "Thứ 3, 14/01/2025", "13:00 - 14:30", new Color(34, 136, 229), MainTheme.IconType.CALENDAR));
        list.add(Box.createVerticalStrut(6));
        list.add(new ScheduleItem("Nhóm học Java Swing", "Thứ 4, 15/01/2025", "19:00 - 20:30", new Color(231, 57, 101), MainTheme.IconType.GROUP));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 238));
        return card;
    }

    private MainTheme.RoundedPanel sectionCard(String title, String link) {
        MainTheme.RoundedPanel card = new MainTheme.RoundedPanel(18);
        card.setLayout(new BorderLayout(0, 10)); card.setBorder(new EmptyBorder(13, 17, 15, 17));
        JPanel header = new JPanel(new BorderLayout()); header.setOpaque(false);
        JLabel heading = new JLabel(title); heading.setFont(MainTheme.font(Font.BOLD, 17)); heading.setForeground(MainTheme.NAVY);
        JButton more = MainTheme.textButton(link, false); more.setFont(MainTheme.font(Font.PLAIN, 12)); more.setBorder(new EmptyBorder(3, 4, 3, 4));
        header.add(heading, BorderLayout.WEST); header.add(more, BorderLayout.EAST); card.add(header, BorderLayout.NORTH);
        return card;
    }

    private JPanel sectionBody(MainTheme.RoundedPanel card) {
        JPanel body = new JPanel(); body.setOpaque(false); body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        card.add(body, BorderLayout.CENTER); return body;
    }

    private JComponent createPlaceholder(String title, String description, MainTheme.IconType icon) {
        JPanel page = new JPanel(new GridBagLayout()); page.setOpaque(false);
        MainTheme.RoundedPanel card = new MainTheme.RoundedPanel(22);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS)); card.setBorder(new EmptyBorder(38, 70, 38, 70));
        JLabel symbol = new JLabel(MainTheme.svgIcon(icon, 52, MainTheme.TEAL)); symbol.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel heading = new JLabel(title); heading.setFont(MainTheme.font(Font.BOLD, 24)); heading.setForeground(MainTheme.NAVY); heading.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel detail = new JLabel(description); detail.setFont(MainTheme.font(Font.PLAIN, 14)); detail.setForeground(MainTheme.MUTED); detail.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(symbol); card.add(Box.createVerticalStrut(18)); card.add(heading); card.add(Box.createVerticalStrut(8)); card.add(detail);
        page.add(card); return page;
    }

    private String greetingPeriod() {
        int hour = LocalTime.now().getHour();
        if (hour < 11) return "buổi sáng";
        if (hour < 14) return "buổi trưa";
        if (hour < 18) return "buổi chiều";
        return "buổi tối";
    }

    private static String resolveDisplayName() {
        UserDTO user = CurrentUser.getUser();
        if (user == null) return "Bạn";
        if (user.getFullName() != null && !user.getFullName().isBlank()) return user.getFullName().trim();
        if (user.getUsername() != null && !user.getUsername().isBlank()) return user.getUsername().trim();
        return "Bạn";
    }

    public void addOrUpdatePost(PostDTO post) {
        if (post == null || post.getId() <= 0) return;

        runOnEdt(() -> {
            profilePanel.addOrUpdatePost(post);
            if (!isPostVisibleInSelectedSubject(post)) return;
            if (postCardsById.containsKey(post.getId())) return;
            
            boolean hasExistingPosts = !postCardsById.isEmpty();
            
            postsContainer.remove(postsStatusLabel);
            postsStatusLabel.setVisible(false);
            
            PostCard card = createPostCard(post);
            
            postCardsById.put(post.getId(), card);
            
            addFeedComponentAt(postsContainer, card, 0);
            
            if (hasExistingPosts) {
                postsContainer.add(Box.createRigidArea(new Dimension(0, 14)), 1);
            }
            
            refreshPostsContainer();
        });
    }

    private boolean isPostVisibleInSelectedSubject(
            PostDTO post
    ) {
        // selectedSubject == null nghĩa là đang xem "Tất cả"
        if (selectedSubject == null || selectedSubject.isBlank()) {
            return true;
        }

        String postSubject = post.getSubject();

        return postSubject != null && selectedSubject.trim().equalsIgnoreCase(
                postSubject.trim()
        );
    }

    private void addFeedComponentAt(
            MainTheme.VerticalPanel feed,
            JComponent component,
            int index
    ) {
        component.setAlignmentX(
                Component.LEFT_ALIGNMENT
        );

        Dimension maximumSize =
                component.getMaximumSize();

        int maximumHeight =
                maximumSize == null
                        ? Integer.MAX_VALUE
                        : maximumSize.height;

        component.setMaximumSize(
                new Dimension(
                        Integer.MAX_VALUE,
                        maximumHeight
                )
        );

        feed.add(component, index);
    }

    public void addCreatePostListener(ActionListener listener) {
        if (listener != null) {
            createPostButton.addActionListener(listener);
        }
    }

    public void addCategoryListener(Consumer<String> listener) {
        if (listener != null) {
            categoryListeners.add(listener);
        }
    }

    public CreatePostDTO getCreatePostData() {
        String title = postTitleField.getText().trim();
        String content = postContentArea.getText().trim();

        if (title.isBlank()) {
            showError("Vui lòng nhập tiêu đề bài viết.");
            postTitleField.requestFocusInWindow();
            return null;
        }

        if (title.length() > 200) {
            showError("Tiêu đề không được vượt quá 200 ký tự.");
            postTitleField.requestFocusInWindow();
            return null;
        }

        if (content.isBlank()) {
            showError("Vui lòng nhập nội dung bài viết.");
            postContentArea.requestFocusInWindow();
            return null;
        }

        String subject = selectedSubject == null
                ? "Chung"
                : selectedSubject;

        CreatePostDTO dto = new CreatePostDTO();
        dto.setTitle(title);
        dto.setContent(content);
        dto.setSubject(subject);
        return dto;
    }

    public void clearPostInput() {
        runOnEdt(() -> {
            postTitleField.setText("");
            postContentArea.setText("");

            selectedAttachments.clear();
            refreshAttachmentPreview();

            postTitleField.requestFocusInWindow();
        });
    }

    public void setCreatePostLoading(boolean loading) {
        runOnEdt(() -> {
            createPostButton.setEnabled(!loading);
            postTitleField.setEnabled(!loading);
            postContentArea.setEnabled(!loading);
            selectImageButton.setEnabled(!loading);
            selectDocumentButton.setEnabled(!loading);
            createPostButton.setText(
                    loading ? "Đang đăng..." : "Tạo bài viết"
            );
        });
    }

    public void setFeedLoading(boolean loading) {
        runOnEdt(() -> {
            if (!loading) {
                return;
            }

            postsContainer.removeAll();
            postsStatusLabel.setText("Đang tải bài viết...");
            postsStatusLabel.setVisible(true);
            postsContainer.add(postsStatusLabel);
            refreshPostsContainer();
        });
    }

    public void displayPosts(List<PostDTO> posts) {
        List<PostDTO> safePosts = posts == null
                ? Collections.emptyList()
                : new ArrayList<>(posts);

        runOnEdt(() -> {
            profilePanel.setPosts(safePosts);
            postCardsById.clear();
            postsContainer.removeAll();

            if (safePosts.isEmpty()) {
                postsStatusLabel.setText(
                        selectedSubject == null
                                ? "Chưa có bài viết nào. Hãy là người đăng bài đầu tiên!"
                                : "Chưa có bài viết thuộc chủ đề này."
                );
                postsStatusLabel.setVisible(true);
                postsContainer.add(postsStatusLabel);
            } else {
                postsStatusLabel.setVisible(false);

                for (int index = 0; index < safePosts.size(); index++) {
                    PostCard card = createPostCard(safePosts.get(index));
                    postCardsById.put(card.getPostId(), card);
                    addFeedComponent(postsContainer, card);

                    if (index < safePosts.size() - 1) {
                        addFeedSpacing(postsContainer, 14);
                    }
                }
            }

            refreshPostsContainer();
        });
    }

    public String getSelectedSubject() {
        return selectedSubject;
    }

    public void showSuccess(String message) {
        runOnEdt(() -> JOptionPane.showMessageDialog(
                this,
                normalizeMessage(message, "Thao tác thành công."),
                "Thành công",
                JOptionPane.INFORMATION_MESSAGE
        ));
    }

    public void showError(String message) {
        runOnEdt(() -> JOptionPane.showMessageDialog(
                this,
                normalizeMessage(message, "Đã xảy ra lỗi."),
                "Lỗi",
                JOptionPane.ERROR_MESSAGE
        ));
    }

    private void configurePostsStatusLabel() {
        postsStatusLabel.setFont(MainTheme.font(Font.PLAIN, 14));
        postsStatusLabel.setForeground(MainTheme.MUTED);
        postsStatusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        postsStatusLabel.setBorder(new EmptyBorder(28, 18, 28, 18));
        postsStatusLabel.setMaximumSize(
                new Dimension(Integer.MAX_VALUE, 90)
        );
    }

    private PostCard createPostCard(PostDTO post) {
        if (post == null) {
            throw new IllegalArgumentException("PostDTO không được null");
        }

        String authorName = valueOrDefault(
                post.getAuthorName(),
                "Người dùng StudyConnect"
        );

        String authorAvatarUrl = valueOrDefault(
                post.getAuthorAvatarUrl(),
                "/images/default-avatar.png"
        );

        String subject = valueOrDefault(
                post.getSubject(),
                "Chung"
        );

        String title = valueOrDefault(
                post.getTitle(),
                "Bài viết"
        );

        String content = valueOrDefault(
                post.getContent(),
                ""
        );

        List<PostAttachmentDTO> attachments =
                post.getAttachments();

        PostCard card = new PostCard(
                post.getId(),
                authorName,
                authorAvatarUrl,
                formatCreatedAt(post.getCreatedAt()),
                subjectIcon(subject) + "  " + subject,
                title,
                content,
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

    private String formatAttachmentSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }

        if (bytes < 1024 * 1024) {
            return String.format(
                    "%.1f KB",
                    bytes / 1024.0
            );
        }

        return String.format(
                "%.1f MB",
                bytes / (1024.0 * 1024.0)
        );
    }

    private String subjectIcon(String subject) {
        String normalized = subject.toLowerCase();

        if (normalized.contains("lập trình")) return "</>";
        if (normalized.equals("ai")
                || normalized.contains("trí tuệ")) return "AI";
        if (normalized.contains("toán")) return "∑";
        if (normalized.contains("nhật")) return "あ";
        return "#";
    }

    private String formatCreatedAt(long createdAt) {
        if (createdAt <= 0) {
            return "Vừa xong";
        }

        Instant created = Instant.ofEpochMilli(createdAt);
        Instant now = Instant.now();

        if (created.isAfter(now)) {
            return "Vừa xong";
        }

        Duration duration = Duration.between(created, now);
        long minutes = duration.toMinutes();

        if (minutes < 1) return "Vừa xong";
        if (minutes < 60) return minutes + " phút trước";

        long hours = duration.toHours();
        if (hours < 24) return hours + " giờ trước";

        long days = duration.toDays();
        if (days < 7) return days + " ngày trước";

        return created.atZone(ZoneId.systemDefault())
                .toLocalDate()
                .toString();
    }

    private void notifyCategorySelected(String subject) {
        for (Consumer<String> listener :
                new ArrayList<>(categoryListeners)) {
            listener.accept(subject);
        }
    }

    private void refreshPostsContainer() {
        postsContainer.revalidate();
        postsContainer.repaint();
    }

    public void setCommentListener(
            Consumer<PostDTO> commentListener
    ) {
        this.commentListener = commentListener;
        profilePanel.setCommentListener(commentListener);
    }

    public void setLikeListener(
            BiConsumer<Long, Boolean> likeListener
    ) {
        this.likeListener = likeListener;
        profilePanel.setLikeListener(likeListener);
    }

    public void updatePostLikeState(
            long postId,
            int likeCount,
            Boolean likedByCurrentUser
    ) {
        runOnEdt(() -> {
            profilePanel.updatePostLikeState(
                    postId,
                    likeCount,
                    likedByCurrentUser
            );
            PostCard card = postCardsById.get(postId);
            if (card == null) {
                return;
            }
            if (likedByCurrentUser == null) {
                card.setLikeCount(likeCount);
            } else {
                card.setLikeState(likedByCurrentUser, likeCount);
            }
            card.setLikeLoading(false);
        });
    }

    public void setPostLikeLoading(long postId, boolean loading) {
        runOnEdt(() -> {
            profilePanel.setPostLikeLoading(postId, loading);
            PostCard card = postCardsById.get(postId);
            if (card != null) {
                card.setLikeLoading(loading);
            }
        });
    }

    public void updatePostCommentCount(
            long postId,
            int commentCount
    ) {
        runOnEdt(() -> {
            profilePanel.updatePostCommentCount(postId, commentCount);
            PostCard card = postCardsById.get(postId);
            if (card != null) {
                card.setCommentCount(commentCount);
            }
        });
    }

    private String valueOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String normalizeMessage(String message, String fallback) {
        return message == null || message.isBlank() ? fallback : message;
    }

    private void runOnEdt(Runnable action) {
        if (SwingUtilities.isEventDispatchThread()) {
            action.run();
        } else {
            SwingUtilities.invokeLater(action);
        }
    }

    private static class NotificationButton extends JButton {
        NotificationButton() {
            setIcon(MainTheme.svgIcon(MainTheme.IconType.BELL, 25, MainTheme.NAVY));
            setPreferredSize(new Dimension(54, 42)); setBorder(null); setContentAreaFilled(false); setFocusPainted(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); setToolTipText("Thông báo");
        }
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(MainTheme.DANGER); g.fillOval(getWidth() - 20, 1, 18, 18);
            g.setColor(Color.WHITE); g.setFont(MainTheme.font(Font.BOLD, 10)); g.drawString("2", getWidth() - 14, 14); g.dispose();
        }
    }

    private static class SunIcon implements Icon {
        private final int size;
        SunIcon(int size) { this.size = size; }
        public int getIconWidth() { return size; }
        public int getIconHeight() { return size; }
        public void paintIcon(Component component, Graphics graphics, int x, int y) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); g.translate(x, y);
            g.setColor(new Color(251, 183, 54)); g.fillOval(17, 17, 25, 25);
            g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            for (int i = 0; i < 8; i++) {
                double angle = i * Math.PI / 4;
                int x1 = 29 + (int) (19 * Math.cos(angle)); int y1 = 29 + (int) (19 * Math.sin(angle));
                int x2 = 29 + (int) (26 * Math.cos(angle)); int y2 = 29 + (int) (26 * Math.sin(angle));
                g.drawLine(x1, y1, x2, y2);
            }
            g.dispose();
        }
    }

    private static class SidebarDecoration extends JComponent {

        private final Image backgroundImage;

        SidebarDecoration() {
            setPreferredSize(new Dimension(272, 340));
            setMinimumSize(new Dimension(272, 260));
            setOpaque(false);

            var imageUrl = SidebarDecoration.class.getResource(
                    "/images/background-sidebar.png"
            );

            if (imageUrl == null) {
                backgroundImage = null;
                System.err.println(
                        "Không tìm thấy ảnh: /images/background-sidebar.png"
                );
            } else {
                backgroundImage = new ImageIcon(imageUrl).getImage();
            }
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);

            Graphics2D g2 = (Graphics2D) graphics.create();

            g2.setRenderingHint(
                    RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BICUBIC
            );
            g2.setRenderingHint(
                    RenderingHints.KEY_RENDERING,
                    RenderingHints.VALUE_RENDER_QUALITY
            );
            g2.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON
            );

            if (backgroundImage != null) {
                int componentWidth = getWidth();
                int componentHeight = getHeight();

                int imageWidth = backgroundImage.getWidth(this);
                int imageHeight = backgroundImage.getHeight(this);

                // Thu nhỏ ảnh và giữ nguyên tỉ lệ
                double scale = Math.min(
                        (double) componentWidth / imageWidth,
                        (double) componentHeight / imageHeight
                );

                int drawWidth = (int) (imageWidth * scale);
                int drawHeight = (int) (imageHeight * scale);

                // Căn giữa ảnh
                int x = (componentWidth - drawWidth) / 2;
                int y = componentHeight - drawHeight;

                g2.drawImage(
                        backgroundImage,
                        x,
                        y,
                        drawWidth,
                        drawHeight,
                        this
                );
            }

            g2.dispose();
        }
    }
}
