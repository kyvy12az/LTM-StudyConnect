package com.studyconnect.client.view.component;

import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;

/**
 * Compact chat is intentionally inactive until it is bound to the shared
 * MessageController. MainFrame currently uses the full MessagesPanel only.
 */
public class MiniChatPanel extends MainTheme.RoundedPanel {
    public MiniChatPanel() {
        super(18);
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(330, 90));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, 90));
        JLabel label = new JLabel(
                "Mở trang Tin nhắn để trò chuyện",
                SwingConstants.CENTER
        );
        label.setFont(MainTheme.font(Font.PLAIN, 13));
        label.setForeground(MainTheme.MUTED);
        label.setBorder(new EmptyBorder(18, 10, 18, 10));
        add(label, BorderLayout.CENTER);
    }
}
