package com.studyconnect.server;

import com.formdev.flatlaf.FlatLightLaf;
import com.studyconnect.server.controller.ServerController;
import com.studyconnect.server.view.ServerFrame;

import javax.swing.*;
import java.awt.*;

public class ServerApplication {

    private static final int SERVER_PORT = 2006;

    public static void main(String[] args) {
        configureLookAndFeel();

        SwingUtilities.invokeLater(() -> {
            ServerFrame view = new ServerFrame();

            new ServerController(view);

            view.setLocationRelativeTo(null);
            view.setVisible(true);
        });
    }

    private static void configureLookAndFeel() {

        FlatLightLaf.setup();

        UIManager.put(
                "defaultFont",
                new Font("Segoe UI", Font.PLAIN, 14)
        );

        UIManager.put("Component.arc", 14);
        UIManager.put("Button.arc", 14);
        UIManager.put("TextComponent.arc", 12);

        UIManager.put("Component.focusWidth", 1);
        UIManager.put("Component.innerFocusWidth", 0);

        UIManager.put("ScrollBar.width", 10);
        UIManager.put("ScrollBar.thumbArc", 999);
        UIManager.put("ScrollBar.trackArc", 999);
        UIManager.put("ScrollBar.showButtons", false);

        UIManager.put("Component.arrowType", "triangle");
        UIManager.put("ToolTip.arc", 10);
        UIManager.put("TabbedPane.showTabSeparators", true);
        UIManager.put("Table.showHorizontalLines", true);
        UIManager.put("Table.showVerticalLines", false);
        UIManager.put("Table.rowHeight", 32);
    }
}