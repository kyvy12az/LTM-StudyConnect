package com.studyconnect.client.view.component;

import com.studyconnect.common.dto.UserDTO;

import javax.swing.*;
import java.awt.*;

public class OnlineUserItem extends JPanel {

    public OnlineUserItem(UserDTO user) {
        setOpaque(false);
        setLayout(new BorderLayout(10, 0));
        setMaximumSize(
                new Dimension(
                        Integer.MAX_VALUE,
                        52
                )
        );

        String name = resolveName(user);

        AvatarView avatar =
                new AvatarView(name, 42);

        avatar.setOnline(true);

        configureAvatar(
                avatar,
                user == null
                        ? null
                        : user.getAvatarUrl()
        );

        add(avatar, BorderLayout.WEST);

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(
                new BoxLayout(
                        text,
                        BoxLayout.Y_AXIS
                )
        );

        JLabel title = new JLabel(name);
        title.setFont(
                MainTheme.font(
                        Font.BOLD,
                        13
                )
        );
        title.setForeground(MainTheme.NAVY);

        JLabel detail =
                new JLabel("Đang trực tuyến");

        detail.setFont(
                MainTheme.font(
                        Font.PLAIN,
                        11
                )
        );
        detail.setForeground(MainTheme.SUCCESS);

        text.add(title);
        text.add(Box.createVerticalStrut(2));
        text.add(detail);

        add(text, BorderLayout.CENTER);
    }

    private String resolveName(UserDTO user) {
        if (user == null) {
            return "Người dùng StudyConnect";
        }

        if (user.getFullName() != null
                && !user.getFullName().isBlank()) {
            return user.getFullName().trim();
        }

        if (user.getUsername() != null
                && !user.getUsername().isBlank()) {
            return user.getUsername().trim();
        }

        return "Người dùng StudyConnect";
    }

    private void configureAvatar(
            AvatarView avatar,
            String avatarUrl
    ) {
        if (avatarUrl == null
                || avatarUrl.isBlank()
                || avatarUrl.trim().endsWith(
                "default_avatar.png"
        )) {
            avatar.setAvatarResource(
                    "/images/default-avatar.png"
            );
            return;
        }

        String normalized =
                avatarUrl.trim();

        if (normalized.startsWith("http://")
                || normalized.startsWith("https://")) {
            avatar.setAvatarUrl(normalized);

        } else if (normalized.startsWith("/")) {
            avatar.setAvatarResource(normalized);

        } else {
            avatar.setAvatarResource(
                    "/images/" + normalized
            );
        }
    }
}