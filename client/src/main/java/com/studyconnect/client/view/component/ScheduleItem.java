package com.studyconnect.client.view.component;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class ScheduleItem extends JPanel {
    public ScheduleItem(String title,String date,String time,Color color,MainTheme.IconType icon){
        setOpaque(false);setLayout(new BorderLayout(10,0));setMaximumSize(new Dimension(Integer.MAX_VALUE,62));
        JLabel badge=new JLabel(MainTheme.svgIcon(icon,20,color),SwingConstants.CENTER);badge.setOpaque(true);badge.setBackground(new Color(color.getRed(),color.getGreen(),color.getBlue(),28));badge.setPreferredSize(new Dimension(46,46));add(badge,BorderLayout.WEST);
        JPanel text=new JPanel();text.setOpaque(false);text.setLayout(new BoxLayout(text,BoxLayout.Y_AXIS));
        JLabel name=new JLabel(title);name.setFont(MainTheme.font(Font.BOLD,12));name.setForeground(MainTheme.NAVY);
        JLabel day=new JLabel(date);day.setFont(MainTheme.font(Font.PLAIN,11));day.setForeground(MainTheme.MUTED);text.add(name);text.add(Box.createVerticalStrut(3));text.add(day);add(text,BorderLayout.CENTER);
        JLabel hours=new JLabel(time);hours.setFont(MainTheme.font(Font.PLAIN,11));hours.setForeground(MainTheme.MUTED);hours.setBorder(new EmptyBorder(0,4,0,0));add(hours,BorderLayout.EAST);
    }
}
