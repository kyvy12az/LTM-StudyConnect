package com.studyconnect.client.view.component;

import javax.swing.JButton;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class SidebarMenuButton extends JButton {
    private final MainTheme.IconType iconType;
    private final String label;
    private String badge;
    private boolean active;
    private boolean hover;

    public SidebarMenuButton(String label, MainTheme.IconType iconType, String badge) {
        this.label = label;
        this.iconType = iconType;
        this.badge = badge;
        setPreferredSize(new Dimension(244, 56));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, 56));
        setBorder(null);
        setOpaque(false);
        setContentAreaFilled(false);
        setFocusPainted(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent event) { hover = true; repaint(); }
            public void mouseExited(MouseEvent event) { hover = false; repaint(); }
        });
    }

    public void setActive(boolean active) {
        this.active = active;
        repaint();
    }

    public void setBadge(String badge) {
        this.badge = badge;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        if (active) {
            g.setPaint(new GradientPaint(0, 0, new Color(12, 174, 144), getWidth(), 0, MainTheme.TEAL));
            g.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
        } else if (hover) {
            g.setColor(MainTheme.MINT);
            g.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
        }
        Color color = active ? Color.WHITE : MainTheme.NAVY;
        MainTheme.svgIcon(iconType, 22, color).paintIcon(this, g, 18, 14);
        g.setFont(MainTheme.font(active ? Font.BOLD : Font.PLAIN, 16));
        g.setColor(color);
        g.drawString(label, 62, 35);
        if (badge != null && !badge.isBlank()) {
            int diameter = 24;
            int x = getWidth() - 35;
            g.setColor(MainTheme.DANGER);
            g.fillOval(x, 16, diameter, diameter);
            g.setFont(MainTheme.font(Font.BOLD, 12));
            g.setColor(Color.WHITE);
            FontMetrics metrics = g.getFontMetrics();
            g.drawString(badge, x + (diameter - metrics.stringWidth(badge)) / 2, 32);
        }
        g.dispose();
    }
}
