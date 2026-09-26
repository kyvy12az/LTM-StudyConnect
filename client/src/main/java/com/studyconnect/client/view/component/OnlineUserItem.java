package com.studyconnect.client.view.component;

import javax.swing.*;
import java.awt.*;

public class OnlineUserItem extends JPanel {
    public OnlineUserItem(String name,String status){
        setOpaque(false);setLayout(new BorderLayout(10,0));setMaximumSize(new Dimension(Integer.MAX_VALUE,52));
        AvatarView avatar=new AvatarView(name,42);avatar.setOnline(true);add(avatar,BorderLayout.WEST);
        JPanel text=new JPanel();text.setOpaque(false);text.setLayout(new BoxLayout(text,BoxLayout.Y_AXIS));
        JLabel title=new JLabel(name);title.setFont(MainTheme.font(Font.BOLD,13));title.setForeground(MainTheme.NAVY);
        JLabel detail=new JLabel(status);detail.setFont(MainTheme.font(Font.PLAIN,11));detail.setForeground(MainTheme.MUTED);
        text.add(title);text.add(Box.createVerticalStrut(2));text.add(detail);add(text,BorderLayout.CENTER);
    }
}
