package com.studyconnect.server.view;

import com.studyconnect.common.dto.PostDTO;
import com.studyconnect.server.model.dto.AdminUserDTO;
import com.studyconnect.server.model.dto.DashboardSnapshot;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionListener;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public class ServerFrame extends JFrame {
    public static final String DASHBOARD_PAGE = "dashboard";
    public static final String USERS_PAGE = "users";
    public static final String POSTS_PAGE = "posts";

    private final CardLayout pageLayout = new CardLayout();
    private final JPanel pageDeck = new JPanel(pageLayout);
    private final DashboardPanel dashboardPanel = new DashboardPanel();
    private final UserManagementPanel userPanel = new UserManagementPanel();
    private final PostManagementPanel postPanel = new PostManagementPanel();
    private final JLabel pageTitle = new JLabel("Bảng điều khiển Server");
    private final JLabel pageSubtitle = new JLabel("Theo dõi hoạt động server và quản lý hệ thống StudyConnect");
    private final JLabel clockLabel = new JLabel();
    private final List<JButton> menuButtons = new ArrayList<>();
    private final List<Consumer<String>> pageListeners = new ArrayList<>();
    private final Timer clockTimer;
    private static final int SIDEBAR_WIDTH = 215;

    public ServerFrame() {
        setTitle("StudyConnect Server");
        setSize(1500, 900);
        setMinimumSize(new Dimension(1280, 720));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

//        ServerTheme.installFrameIcon(this);

        initializeUI();
        setServerRunning(false);

        clockTimer = new Timer(
                1_000,
                event -> updateClock()
        );
        clockTimer.setInitialDelay(0);
        clockTimer.start();
    }

    private void initializeUI() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(ServerTheme.BACKGROUND);

        root.add(
                createSidebar(),
                BorderLayout.WEST
        );

        JPanel application = new JPanel(
                new BorderLayout()
        );
        application.setBackground(
                ServerTheme.BACKGROUND
        );

        application.add(
                createHeader(),
                BorderLayout.NORTH
        );

        pageDeck.setOpaque(false);
        pageDeck.setBorder(
                new EmptyBorder(12, 14, 12, 14)
        );

        pageDeck.add(
                dashboardPanel,
                DASHBOARD_PAGE
        );
        pageDeck.add(
                userPanel,
                USERS_PAGE
        );
        pageDeck.add(
                postPanel,
                POSTS_PAGE
        );

        pageDeck.add(
                placeholder(
                        "Bình luận",
                        "Trang quản lý bình luận sẽ được hoàn thiện sau."
                ),
                "comments"
        );

        pageDeck.add(
                placeholder(
                        "Tin nhắn",
                        "Trang quản lý tin nhắn sẽ được hoàn thiện sau."
                ),
                "messages"
        );

        pageDeck.add(
                placeholder(
                        "Nhật ký",
                        "Nhật ký trực tiếp hiện có tại trang Tổng quan."
                ),
                "audit"
        );

        application.add(
                pageDeck,
                BorderLayout.CENTER
        );

        root.add(
                application,
                BorderLayout.CENTER
        );

        setContentPane(root);
    }

    private JComponent createSidebar() {
        JPanel sidebar = new JPanel(
                new BorderLayout()
        );

        /*
         * Không dùng 260 vì Windows scale 125%
         * sẽ hiển thị thành khoảng 325px.
         */
        sidebar.setPreferredSize(
                new Dimension(
                        SIDEBAR_WIDTH,
                        0
                )
        );

        sidebar.setMinimumSize(
                new Dimension(
                        SIDEBAR_WIDTH,
                        0
                )
        );

        sidebar.setBackground(
                new Color(238, 250, 246)
        );

        sidebar.setBorder(
                BorderFactory.createMatteBorder(
                        0,
                        0,
                        0,
                        1,
                        ServerTheme.BORDER
                )
        );

        JPanel upper = new JPanel();
        upper.setOpaque(false);
        upper.setLayout(
                new BoxLayout(
                        upper,
                        BoxLayout.Y_AXIS
                )
        );

        upper.setBorder(
                new EmptyBorder(
                        20,
                        12,
                        0,
                        12
                )
        );

        JLabel logo = new JLabel(
                "<html><div style='text-align:center'>"
                        + "<span style='color:#073b70'>Study</span>"
                        + "<span style='color:#009b83'>Connect</span>"
                        + " Server"
                        + "</div></html>",


                ServerTheme.imageIcon(
                        "/images/logo-final.png",
                        65,
                        55
                ),
                SwingConstants.CENTER
        );

        logo.setFont(
                ServerTheme.font(
                        Font.BOLD,
                        17
                )
        );
        logo.setForeground(ServerTheme.NAVY);

        logo.setHorizontalAlignment(
                SwingConstants.CENTER
        );
        logo.setHorizontalTextPosition(
                SwingConstants.CENTER
        );
        logo.setVerticalTextPosition(
                SwingConstants.BOTTOM
        );

        logo.setIconTextGap(5);
        logo.setAlignmentX(
                Component.LEFT_ALIGNMENT
        );

        logo.setPreferredSize(
                new Dimension(
                        SIDEBAR_WIDTH - 24,
                        112
                )
        );

        logo.setMinimumSize(
                new Dimension(
                        SIDEBAR_WIDTH - 24,
                        112
                )
        );

        logo.setMaximumSize(
                new Dimension(
                        SIDEBAR_WIDTH - 24,
                        112
                )
        );

        upper.add(logo);
        upper.add(
                Box.createVerticalStrut(18)
        );

        addMenu(
                upper,
                "Tổng quan",
                "home",
                DASHBOARD_PAGE,
                true
        );

        addMenu(
                upper,
                "Người dùng",
                "user",
                USERS_PAGE,
                false
        );

        addMenu(
                upper,
                "Bài viết",
                "file",
                POSTS_PAGE,
                false
        );

        addMenu(
                upper,
                "Bình luận",
                "message",
                "comments",
                false
        );

        addMenu(
                upper,
                "Tin nhắn",
                "message",
                "messages",
                false
        );

        addMenu(
                upper,
                "Nhật ký",
                "audit",
                "audit",
                false
        );

//        JLabel footer = new JLabel(
//                "v1.0.0  |  StudyConnect Server",
//                SwingConstants.CENTER
//        );
//
//        footer.setForeground(ServerTheme.MUTED);
//        footer.setFont(
//                ServerTheme.font(
//                        Font.PLAIN,
//                        9
//                )
//        );
//
//        footer.setBorder(
//                new EmptyBorder(
//                        0,
//                        4,
//                        12,
//                        4
//                )
//        );

        sidebar.add(
                upper,
                BorderLayout.NORTH
        );

//        sidebar.add(
//                footer,
//                BorderLayout.SOUTH
//        );

        return sidebar;
    }

    private void addMenu(
            JPanel parent,
            String text,
            String icon,
            String page,
            boolean selected
    ) {
        JButton button = new JButton(
                text,
                ServerTheme.icon(
                        icon,
                        19,
                        selected
                                ? Color.WHITE
                                : ServerTheme.NAVY
                )
        );

        button.setHorizontalAlignment(
                SwingConstants.LEFT
        );
        button.setHorizontalTextPosition(
                SwingConstants.RIGHT
        );
        button.setIconTextGap(12);

        button.setFont(
                ServerTheme.font(
                        Font.BOLD,
                        13
                )
        );

        button.setFocusPainted(false);
        button.setBorderPainted(false);

        button.setBorder(
                new EmptyBorder(
                        12,
                        15,
                        12,
                        10
                )
        );

        button.setAlignmentX(
                Component.LEFT_ALIGNMENT
        );

        int menuWidth = SIDEBAR_WIDTH - 24;

        button.setPreferredSize(
                new Dimension(menuWidth, 48)
        );
        button.setMinimumSize(
                new Dimension(menuWidth, 48)
        );
        button.setMaximumSize(
                new Dimension(menuWidth, 48)
        );

        button.setCursor(
                Cursor.getPredefinedCursor(
                        Cursor.HAND_CURSOR
                )
        );

        button.putClientProperty(
                "JButton.buttonType",
                "roundRect"
        );
        button.putClientProperty("page", page);
        button.putClientProperty(
                "iconName",
                icon
        );

        button.addActionListener(
                event -> showPage(page, button)
        );

        menuButtons.add(button);
        styleMenuButton(button, selected);

        parent.add(button);
        parent.add(
                Box.createVerticalStrut(5)
        );
    }

    private void styleMenuButton(
            JButton button,
            boolean selected
    ) {
        String iconName = String.valueOf(
                button.getClientProperty(
                        "iconName"
                )
        );

        Color foreground = selected
                ? Color.WHITE
                : ServerTheme.NAVY;

        button.setIcon(
                ServerTheme.icon(
                        iconName,
                        20,
                        foreground
                )
        );

        button.setForeground(foreground);

        if (selected) {
            button.setBackground(
                    ServerTheme.TEAL
            );
            button.setOpaque(true);
            button.setContentAreaFilled(true);

        } else {
            button.setBackground(
                    new Color(0, 0, 0, 0)
            );
            button.setOpaque(false);
            button.setContentAreaFilled(false);
        }

        button.repaint();
    }

    private void showPage(String page, JButton selectedButton) {
        pageLayout.show(pageDeck, page);
        for (JButton button : menuButtons) styleMenuButton(button, button == selectedButton);
        switch (page) {
            case USERS_PAGE -> setHeader("Quản lý người dùng", "Theo dõi tài khoản và trạng thái hoạt động");
            case POSTS_PAGE -> setHeader("Quản lý bài viết", "Tra cứu nội dung và tệp đính kèm");
            case "comments" -> setHeader("Quản lý bình luận", "Chức năng đang được hoàn thiện");
            case "messages" -> setHeader("Quản lý tin nhắn", "Chức năng đang được hoàn thiện");
            case "audit" -> setHeader("Nhật ký hệ thống", "Nhật ký trực tiếp hiện có tại trang Tổng quan");
            default -> setHeader("Bảng điều khiển Server", "Theo dõi hoạt động server và quản lý hệ thống StudyConnect");
        }
        for (Consumer<String> listener : new ArrayList<>(pageListeners)) listener.accept(page);
    }

    private JComponent createHeader() {
        JPanel header = new JPanel(
                new BorderLayout(20, 0)
        );

        header.setBackground(Color.WHITE);
        header.setPreferredSize(
                new Dimension(0, 96)
        );

        header.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(
                                0,
                                0,
                                1,
                                0,
                                ServerTheme.BORDER
                        ),
                        new EmptyBorder(
                                16,
                                28,
                                16,
                                28
                        )
                )
        );

        JPanel titles = new JPanel();
        titles.setOpaque(false);
        titles.setLayout(
                new BoxLayout(
                        titles,
                        BoxLayout.Y_AXIS
                )
        );

        pageTitle.setFont(
                ServerTheme.font(
                        Font.BOLD,
                        25
                )
        );
        pageTitle.setForeground(
                ServerTheme.NAVY
        );

        pageSubtitle.setFont(
                ServerTheme.font(
                        Font.PLAIN,
                        12
                )
        );
        pageSubtitle.setForeground(
                ServerTheme.MUTED
        );

        titles.add(pageTitle);
        titles.add(
                Box.createVerticalStrut(5)
        );
        titles.add(pageSubtitle);

        JPanel admin = new JPanel();
        admin.setOpaque(false);
        admin.setLayout(
                new BoxLayout(
                        admin,
                        BoxLayout.Y_AXIS
                )
        );

        clockLabel.setFont(
                ServerTheme.font(
                        Font.PLAIN,
                        12
                )
        );
        clockLabel.setForeground(
                ServerTheme.NAVY
        );
        clockLabel.setAlignmentX(
                Component.RIGHT_ALIGNMENT
        );

        JLabel administrator = new JLabel(
                "Quản trị viên  •  local"
        );
        administrator.setFont(
                ServerTheme.font(
                        Font.BOLD,
                        12
                )
        );
        administrator.setForeground(
                ServerTheme.NAVY
        );
        administrator.setAlignmentX(
                Component.RIGHT_ALIGNMENT
        );

        admin.add(clockLabel);
        admin.add(
                Box.createVerticalStrut(5)
        );
        admin.add(administrator);

        header.add(
                titles,
                BorderLayout.WEST
        );
        header.add(
                admin,
                BorderLayout.EAST
        );

        return header;
    }

    private JComponent placeholder(String title, String text) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(ServerTheme.BACKGROUND);
        JLabel label = new JLabel("<html><div style='text-align:center'><h2>" + title + "</h2><p>" + text + "</p></div></html>");
        label.setForeground(ServerTheme.MUTED);
        panel.add(label);
        return panel;
    }

    private void setHeader(String title, String subtitle) {
        pageTitle.setText(title);
        pageSubtitle.setText(subtitle);
    }

    private void updateClock() {
        Locale vietnamese =
                new Locale("vi", "VN");

        DateTimeFormatter formatter =
                DateTimeFormatter.ofPattern(
                        "EEEE, dd/MM/yyyy  •  HH:mm:ss",
                        vietnamese
                );

        String text = LocalDateTime.now()
                .format(formatter);

        if (!text.isBlank()) {
            text = Character.toUpperCase(
                    text.charAt(0)
            ) + text.substring(1);
        }

        clockLabel.setText(text);
    }

    @Override
    public void dispose() {
        clockTimer.stop();
        super.dispose();
    }

    public String getPortText() { return dashboardPanel.getPortText(); }
    public void setServerRunning(boolean running) { dashboardPanel.setServerRunning(running); }
    public void setClientCount(int count) { dashboardPanel.setClientCount(count); }
    public void appendLog(String message) { dashboardPanel.appendLog(message); }
    public void clearLog() { dashboardPanel.clearLog(); }
    public void addStartListener(ActionListener listener) { dashboardPanel.addStartListener(listener); }
    public void addStopListener(ActionListener listener) { dashboardPanel.addStopListener(listener); }
    public void addClearLogListener(ActionListener listener) { dashboardPanel.addClearLogListener(listener); }
    public void addPageChangeListener(Consumer<String> listener) { if (listener != null) pageListeners.add(listener); }
    public void updateDashboard(DashboardSnapshot snapshot) { dashboardPanel.setSnapshot(snapshot); }
    public void setUsers(List<AdminUserDTO> users) { userPanel.setUsers(users); }
    public void setPosts(List<PostDTO> posts) { postPanel.setPosts(posts); }
    public void addOrUpdatePost(PostDTO post) { postPanel.addOrUpdatePost(post); }
    public AdminUserDTO getSelectedUser() { return userPanel.getSelectedUser(); }
    public void setUserLoading(boolean loading) { userPanel.setLoading(loading); }
    public void setPostLoading(boolean loading) { postPanel.setLoading(loading); }
    public void showUserError(String message) { userPanel.showError(message); }
    public void showPostError(String message) { postPanel.showError(message); }
    public void addUserRefreshListener(ActionListener listener) { userPanel.addRefreshListener(listener); }
    public void addUserDetailListener(ActionListener listener) { userPanel.addDetailListener(listener); }
    public void addUserStatusListener(ActionListener listener) { userPanel.addToggleStatusListener(listener); }
    public void addPostRefreshListener(ActionListener listener) { postPanel.addRefreshListener(listener); }
    public void addPostDetailListener(ActionListener listener) { postPanel.addDetailListener(listener); }
    public void showSelectedUserDetail() { userPanel.showSelectedUserDetail(this); }
    public void showSelectedPostDetail() { postPanel.showSelectedPostDetail(this); }
    public void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Lỗi", JOptionPane.ERROR_MESSAGE);
    }
}
