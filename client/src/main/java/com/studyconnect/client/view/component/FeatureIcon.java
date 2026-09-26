package com.studyconnect.client.view.component;

import javax.swing.Icon;
import java.awt.*;

/** Small dependency-free line icons used by the login feature strip. */
final class FeatureIcon implements Icon {
    enum Type { BOOK, PEOPLE, CHART, TROPHY }
    private final Type type;

    FeatureIcon(Type type) { this.type = type; }
    public int getIconWidth() { return 32; }
    public int getIconHeight() { return 30; }

    public void paintIcon(Component component, Graphics graphics, int x, int y) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.translate(x, y);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(AuthTheme.TEAL_DARK);
        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        if (type == Type.BOOK) {
            g.drawLine(3, 7, 15, 10); g.drawLine(15, 10, 29, 7);
            g.drawLine(3, 7, 3, 24); g.drawLine(29, 7, 29, 24);
            g.drawLine(3, 24, 15, 27); g.drawLine(15, 10, 15, 27); g.drawLine(15, 27, 29, 24);
        } else if (type == Type.PEOPLE) {
            g.drawOval(12, 3, 8, 8); g.drawOval(2, 8, 7, 7); g.drawOval(23, 8, 7, 7);
            g.drawArc(8, 13, 16, 14, 0, 180); g.drawArc(0, 17, 11, 10, 20, 150);
            g.drawArc(21, 17, 11, 10, 10, 150);
        } else if (type == Type.CHART) {
            g.drawRect(3, 17, 5, 10); g.drawRect(13, 11, 5, 16); g.drawRect(23, 4, 5, 23);
        } else {
            g.drawArc(8, 3, 16, 14, 180, 180); g.drawLine(8, 3, 4, 3);
            g.drawArc(1, 3, 8, 10, 180, 180); g.drawLine(24, 3, 28, 3);
            g.drawArc(23, 3, 8, 10, 180, 180); g.drawLine(16, 17, 16, 24);
            g.drawLine(11, 27, 21, 27);
        }
        g.dispose();
    }
}
