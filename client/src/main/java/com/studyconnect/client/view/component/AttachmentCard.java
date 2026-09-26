package com.studyconnect.client.view.component;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class AttachmentCard extends MainTheme.RoundedPanel {
    public AttachmentCard(String fileName, String details, String type) {
        super(14); setLayout(new BorderLayout(12,0)); setFill(new Color(248,250,252));
        setBorder(new EmptyBorder(8,10,8,12)); setPreferredSize(new Dimension(390,58)); setMaximumSize(new Dimension(430,58));
        JLabel fileIcon=new JLabel(type,SwingConstants.CENTER); fileIcon.setOpaque(true);
        fileIcon.setBackground(type.equalsIgnoreCase("PDF")?new Color(239,68,68):new Color(37,121,230));
        fileIcon.setForeground(Color.WHITE); fileIcon.setFont(MainTheme.font(Font.BOLD,11)); fileIcon.setPreferredSize(new Dimension(38,38));
        JPanel text=new JPanel(); text.setOpaque(false); text.setLayout(new BoxLayout(text,BoxLayout.Y_AXIS));
        JLabel name=new JLabel(fileName); name.setFont(MainTheme.font(Font.BOLD,13)); name.setForeground(MainTheme.NAVY);
        JLabel meta=new JLabel(details); meta.setFont(MainTheme.font(Font.PLAIN,11)); meta.setForeground(MainTheme.MUTED);
        text.add(name); text.add(Box.createVerticalStrut(3)); text.add(meta);
        add(fileIcon,BorderLayout.WEST); add(text,BorderLayout.CENTER);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }
}
