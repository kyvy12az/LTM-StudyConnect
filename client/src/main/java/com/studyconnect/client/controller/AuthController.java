package com.studyconnect.client.controller;

import com.studyconnect.client.model.CurrentUser;
import com.studyconnect.client.service.AuthService;
import com.studyconnect.client.service.CommentService;
import com.studyconnect.client.service.PostService;
import com.studyconnect.client.view.auth.LoginFrame;
import com.studyconnect.client.view.auth.RegisterFrame;
import com.studyconnect.client.view.main.MainFrame;
import com.studyconnect.common.dto.*;
import com.studyconnect.common.protocol.Response;

import javax.swing.SwingWorker;

public class AuthController {

    private final AuthService authService;
    private final PostService postService;
    private final CommentService commentService;

    private LoginFrame loginFrame;
    private RegisterFrame registerFrame;

    private MainFrame mainFrame;
    private MainController mainController;

    public AuthController(
            AuthService authService,
            PostService postService,
            CommentService commentService
    ) {
        if (authService == null) {
            throw new IllegalArgumentException(
                    "AuthService không được null"
            );
        }

        if (postService == null) {
            throw new IllegalArgumentException(
                    "PostService không được null"
            );
        }

        if (commentService == null) {
            throw new IllegalArgumentException(
                    "CommentService không được null"
            );
        }

        this.authService = authService;
        this.postService = postService;
        this.commentService = commentService;
    }

    public void showLogin() {
        if (registerFrame != null) {
            registerFrame.dispose();
            registerFrame = null;
        }

        loginFrame = new LoginFrame();

        loginFrame.addLoginListener(
                event -> handleLogin()
        );

        loginFrame.addRegisterListener(
                event -> showRegister()
        );

        loginFrame.setVisible(true);
    }

    private void showRegister() {
        if (loginFrame != null) {
            loginFrame.dispose();
            loginFrame = null;
        }

        registerFrame = new RegisterFrame();

        registerFrame.addRegisterListener(
                event -> handleRegister()
        );

        registerFrame.addLoginListener(
                event -> showLogin()
        );

        registerFrame.setVisible(true);
    }

    private void handleLogin() {
        String account = loginFrame.getAccount();
        String password = loginFrame.getPasswordText();

        if (account.isBlank() || password.isBlank()) {
            loginFrame.showError(
                    "Vui lòng nhập đầy đủ tài khoản và mật khẩu."
            );
            return;
        }

        loginFrame.setLoading(true);

        SwingWorker<Response<AuthResponseDTO>, Void> worker =
                new SwingWorker<>() {

                    @Override
                    protected Response<AuthResponseDTO> doInBackground()
                            throws Exception {
                        LoginDTO loginDTO =
                                new LoginDTO(account, password);

                        return authService.login(loginDTO);
                    }

                    @Override
                    protected void done() {
                        loginFrame.setLoading(false);

                        try {
                            Response<AuthResponseDTO> response =
                                    get();

                            if (response == null) {
                                loginFrame.showError(
                                        "Server không trả về phản hồi."
                                );
                                return;
                            }

                            if (!response.isSuccess()) {
                                loginFrame.showError(
                                        response.getMessage()
                                );
                                return;
                            }

                            AuthResponseDTO authResponseDTO =
                                    response.getData();

                            if (authResponseDTO == null) {
                                loginFrame.showError(
                                        "Server không trả về "
                                                + "thông tin đăng nhập."
                                );
                                return;
                            }

                            String token =
                                    authResponseDTO.getToken();

                            if (token == null
                                    || token.isBlank()) {
                                loginFrame.showError(
                                        "Server không trả về "
                                                + "token đăng nhập."
                                );
                                return;
                            }

                            /*
                             * Lưu thông tin người dùng và token
                             * vào CurrentUser.
                             */
                            CurrentUser.setSession(
                                    authResponseDTO
                            );

                            /*
                             * Cập nhật token cho các service
                             * cần xác thực.
                             */
                            postService.setAuthToken(token);
                            commentService.setAuthToken(token);

                            openMainFrame();

                        } catch (Exception exception) {
                            loginFrame.showError(
                                    "Không thể kết nối đến Server: "
                                            + getCauseMessage(exception)
                            );
                        }
                    }
                };

        worker.execute();
    }

    private void handleRegister() {
        String fullName = registerFrame.getFullName();
        String username = registerFrame.getUsername();
        String email = registerFrame.getEmail();
        String password = registerFrame.getPasswordText();
        String confirmPassword =
                registerFrame.getConfirmPassword();

        if (fullName.isBlank()
                || username.isBlank()
                || email.isBlank()
                || password.isBlank()
                || confirmPassword.isBlank()) {

            registerFrame.showError(
                    "Vui lòng nhập đầy đủ thông tin."
            );
            return;
        }

        if (!email.matches(
                "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
        )) {
            registerFrame.showError(
                    "Email không đúng định dạng."
            );
            return;
        }

        if (!password.equals(confirmPassword)) {
            registerFrame.showError(
                    "Mật khẩu xác nhận không khớp."
            );
            return;
        }

        if (password.length() < 6) {
            registerFrame.showError(
                    "Mật khẩu phải có ít nhất 6 ký tự."
            );
            return;
        }

        if (!registerFrame.isTermsAccepted()) {
            registerFrame.showError(
                    "Vui lòng chấp nhận điều khoản sử dụng."
            );
            return;
        }

        registerFrame.setLoading(true);

        SwingWorker<Response<AuthResponseDTO>, Void> worker =
                new SwingWorker<>() {

                    @Override
                    protected Response<AuthResponseDTO> doInBackground()
                            throws Exception {
                        RegisterDTO registerDTO =
                                new RegisterDTO(
                                        username,
                                        password,
                                        fullName,
                                        email
                                );

                        return authService.register(
                                registerDTO
                        );
                    }

                    @Override
                    protected void done() {
                        registerFrame.setLoading(false);

                        try {
                            Response<AuthResponseDTO> response =
                                    get();

                            if (response == null) {
                                registerFrame.showError(
                                        "Server không trả về phản hồi."
                                );
                                return;
                            }

                            if (!response.isSuccess()) {
                                registerFrame.showError(
                                        response.getMessage()
                                );
                                return;
                            }

                            registerFrame.showSuccess(
                                    "Đăng ký thành công. "
                                            + "Bạn có thể đăng nhập!"
                            );

                            showLogin();

                        } catch (Exception exception) {
                            registerFrame.showError(
                                    "Không thể kết nối đến Server: "
                                            + getCauseMessage(exception)
                            );
                        }
                    }
                };

        worker.execute();
    }

    private void openMainFrame() {
        if (loginFrame != null) {
            loginFrame.dispose();
            loginFrame = null;
        }

        mainFrame = new MainFrame();

        mainController = new MainController(
                mainFrame,
                postService,
                commentService
        );

        mainFrame.setVisible(true);

        /*
         * Tạm thời tải bài viết và in ra console.
         * Sau này callback này sẽ gọi hàm hiển thị
         * bài viết trong MainFrame.
         */
//        mainController.loadAllPosts(posts -> {
//            System.out.println(
//                    "Số bài viết nhận được: "
//                            + posts.size()
//            );
//
//            for (PostDTO post : posts) {
//                System.out.println(post);
//            }
//        });

        mainFrame.setFeedLoading(true);

        mainController.loadAllPosts(posts -> {
            mainFrame.displayPosts(posts);
        });

        mainFrame.addCategoryListener(subject -> {
            mainFrame.setFeedLoading(true);

            mainController.loadPostsBySubject(
                    subject,
                    mainFrame::displayPosts
            );
        });

        mainFrame.addCreatePostListener(event -> {
            CreatePostDTO createPostDTO =
                    mainFrame.getCreatePostData();

            if (createPostDTO == null) {
                return;
            }

            mainFrame.setCreatePostLoading(true);

            mainController.createPost(
                    createPostDTO,
                    createdPost -> {
                        mainFrame.setCreatePostLoading(false);
                        mainFrame.clearPostInput();
                        mainFrame.showSuccess(
                                "Đăng bài thành công"
                        );

                        mainFrame.setFeedLoading(true);

                        mainController.loadPostsBySubject(
                                mainFrame.getSelectedSubject(),
                                mainFrame::displayPosts
                        );
                    }
            );
        });
    }

    private String getCauseMessage(Exception exception) {
        Throwable cause = exception;

        while (cause.getCause() != null
                && cause.getCause() != cause) {
            cause = cause.getCause();
        }

        String message = cause.getMessage();

        if (message == null || message.isBlank()) {
            return cause.getClass().getSimpleName();
        }

        return message;
    }
}