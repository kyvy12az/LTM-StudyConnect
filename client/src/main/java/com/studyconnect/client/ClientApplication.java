package com.studyconnect.client;

import com.formdev.flatlaf.FlatLightLaf;
import com.studyconnect.client.controller.AuthController;
import com.studyconnect.client.network.tcp.TCPClient;
import com.studyconnect.client.service.AuthService;
import com.studyconnect.client.service.CommentService;
import com.studyconnect.client.service.PostService;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;

public class ClientApplication {

    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 2006;

    public static void main(String[] args) {
        configureLookAndFeel();

        TCPClient tcpClient = new TCPClient(
                SERVER_HOST,
                SERVER_PORT
        );

        try {
            tcpClient.connect();

        } catch (IOException exception) {
            JOptionPane.showMessageDialog(
                    null,
                    "Không thể kết nối đến Server "
                            + SERVER_HOST
                            + ":"
                            + SERVER_PORT
                            + "\n"
                            + exception.getMessage(),
                    "Lỗi kết nối",
                    JOptionPane.ERROR_MESSAGE
            );

            return;
        }

        Runtime.getRuntime().addShutdownHook(
                new Thread(
                        tcpClient::close,
                        "tcp-client-shutdown"
                )
        );

        AuthService authService =
                new AuthService(tcpClient);

        PostService postService =
                new PostService(
                        tcpClient,
                        null
                );

        CommentService commentService =
                new CommentService(
                        tcpClient,
                        null
                );

        SwingUtilities.invokeLater(() -> {
            AuthController controller =
                    new AuthController(
                            authService,
                            postService,
                            commentService,
                            tcpClient
                    );

            controller.showLogin();
        });
    }

    private static void configureLookAndFeel() {
        FlatLightLaf.setup();

        UIManager.put(
                "defaultFont",
                new Font(
                        "Segoe UI",
                        Font.PLAIN,
                        14
                )
        );

        UIManager.put("Component.arc", 14);
        UIManager.put("Button.arc", 14);
        UIManager.put("TextComponent.arc", 12);

        UIManager.put("Component.focusWidth", 1);
        UIManager.put("Component.innerFocusWidth", 0);

        UIManager.put("ScrollBar.width", 10);
        UIManager.put("ScrollBar.thumbArc", 999);
        UIManager.put("ScrollBar.trackArc", 999);

        UIManager.put(
                "ScrollBar.showButtons",
                false
        );

        UIManager.put(
                "Component.arrowType",
                "triangle"
        );

        UIManager.put(
                "TextComponent.selectAllOnFocusPolicy",
                "once"
        );

        UIManager.put("ToolTip.arc", 10);
    }
}
