package com.studyconnect.client.view.component;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class CategoryChip extends JButton {
    private boolean active;
    private boolean hover;
    public CategoryChip(String text){
        super(text); setFont(MainTheme.font(Font.PLAIN,13)); setForeground(MainTheme.NAVY);
        setBorder(null); setContentAreaFilled(false); setFocusPainted(false); setOpaque(false);
        setPreferredSize(new Dimension(Math.max(90,getFontMetrics(getFont()).stringWidth(text)+36),38));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addMouseListener(new MouseAdapter(){public void mouseEntered(MouseEvent e){hover=true;repaint();}public void mouseExited(MouseEvent e){hover=false;repaint();}});
    }
    public void setActive(boolean active){this.active=active;setForeground(active?Color.WHITE:MainTheme.NAVY);setFont(MainTheme.font(active?Font.BOLD:Font.PLAIN,13));repaint();}
    protected void paintComponent(Graphics graphics){
        Graphics2D g=(Graphics2D)graphics.create();g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(active?MainTheme.TEAL:hover?new Color(225,243,239):new Color(238,244,246));g.fillRoundRect(0,0,getWidth(),getHeight(),getHeight(),getHeight());g.dispose();super.paintComponent(graphics);
    }
}
