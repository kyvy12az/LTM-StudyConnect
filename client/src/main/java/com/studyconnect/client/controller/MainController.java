package com.studyconnect.client.controller;

import com.studyconnect.client.model.CurrentUser;
import com.studyconnect.client.network.file.FileTransferClient;
import com.studyconnect.client.network.tcp.TCPClient;
import com.studyconnect.client.service.CommentService;
import com.studyconnect.client.service.PostService;
import com.studyconnect.client.view.component.CommentDialog;
import com.studyconnect.client.view.main.MainFrame;
import com.studyconnect.common.dto.*;
import com.studyconnect.common.protocol.Response;
import com.studyconnect.common.protocol.ServerEvent;
import com.studyconnect.common.protocol.ServerEventType;
import com.studyconnect.common.util.JsonUtils;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;

public class MainController {
    private final MainFrame mainFrame;
    private final PostService postService;
    private final CommentService commentService;
    private final TCPClient tcpClient;
    private final Object serviceLock = new Object();
    private final AtomicBoolean closed = new AtomicBoolean();
    private final Consumer<ServerEvent<String>> commentCreatedListener =
            this::handleCommentCreatedEvent;
    private final Consumer<ServerEvent<String>> postCreatedListener =
            this::handlePostCreatedEvent;
    private final Consumer<ServerEvent<String>> onlineUsersListener =
            this::handleOnlineUsersEvent;
    private volatile CommentDialog activeCommentDialog;
    private volatile long activeCommentPostId = -1L;
    private final FileTransferClient fileTransferClient;

    public MainController(
            MainFrame mainFrame,
            PostService postService,
            CommentService commentService,
            TCPClient tcpClient,
            FileTransferClient fileTransferClient
    ) {
        this.mainFrame = Objects.requireNonNull(
                mainFrame,
                "mainFrame"
        );
        this.postService = Objects.requireNonNull(
                postService,
                "postService"
        );
        this.commentService = Objects.requireNonNull(
                commentService,
                "commentService"
        );
        this.tcpClient = Objects.requireNonNull(tcpClient, "tcpClient");
        this.fileTransferClient = Objects.requireNonNull(fileTransferClient, "fileTransferClient");

        tcpClient.addServerEventListener(
                ServerEventType.COMMENT_CREATED,
                commentCreatedListener
        );

        tcpClient.addServerEventListener(
                ServerEventType.POST_CREATED,
                postCreatedListener
        );

        tcpClient.addServerEventListener(
                ServerEventType.ONLINE_USERS_UPDATED,
                onlineUsersListener
        );

        mainFrame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                close();
            }

            @Override
            public void windowClosed(WindowEvent event) {
                close();
            }
        });
    }

    public void createPostWithAttachments(
            CreatePostDTO post,
            List<File> files,
            Consumer<PostDTO> onSuccess,
            Consumer<String> onError,
            Runnable onFinished
    ) {
        new SwingWorker<Response<PostDTO>, Void>() {
            @Override
            protected Response<PostDTO> doInBackground()
                    throws Exception {

                List<Long> attachmentIds =
                        new ArrayList<>();

                if (files != null) {
                    for (File file : files) {
                        PostAttachmentDTO uploaded =
                                fileTransferClient.upload(
                                        file,
                                        CurrentUser.getToken()
                                );

                        attachmentIds.add(
                                uploaded.getId()
                        );
                    }
                }

                post.setAttachmentIds(attachmentIds);

                synchronized (serviceLock) {
                    return postService.createPost(post);
                }
            }

            @Override
            protected void done() {
                try {
                    Response<PostDTO> response = get();

                    if (response == null
                            || !response.isSuccess()
                            || response.getData() == null) {

                        onError.accept(
                                response == null
                                        ? "Server không phản hồi."
                                        : response.getMessage()
                        );
                        return;
                    }

                    onSuccess.accept(response.getData());

                } catch (Exception exception) {
                    Throwable cause = exception.getCause();

                    onError.accept(
                            cause != null
                                    && cause.getMessage() != null
                                    ? cause.getMessage()
                                    : exception.getMessage()
                    );
                } finally {
                    onFinished.run();
                }
            }
        }.execute();
    }

    public void setActiveCommentDialog(long postId, CommentDialog dialog) {
        if (postId <= 0 || dialog == null) {
            return;
        }
        activeCommentPostId = postId;
        activeCommentDialog = dialog;
    }

    public void clearActiveCommentDialog(CommentDialog dialog) {
        if (dialog != null && activeCommentDialog == dialog) {
            activeCommentDialog = null;
            activeCommentPostId = -1L;
        }
    }

    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }

        tcpClient.removeServerEventListener(ServerEventType.COMMENT_CREATED, commentCreatedListener);
        tcpClient.removeServerEventListener(ServerEventType.POST_CREATED, postCreatedListener);
        tcpClient.removeServerEventListener(ServerEventType.ONLINE_USERS_UPDATED, onlineUsersListener);

        activeCommentDialog = null;
        activeCommentPostId = -1L;
    }

    private void handleCommentCreatedEvent(ServerEvent<String> event) {
        if (closed.get() || event == null || event.getData() == null) {
            return;
        }

        final CommentCreatedEventDTO eventData;
        try {
            eventData = JsonUtils.fromJson(
                    event.getData(),
                    CommentCreatedEventDTO.class
            );
        } catch (RuntimeException exception) {
            System.err.println(
                    "Không thể đọc sự kiện bình luận: "
                            + exception.getMessage()
            );
            return;
        }

        if (eventData == null || eventData.getPostId() <= 0) {
            return;
        }

        SwingUtilities.invokeLater(() -> applyCommentCreatedEvent(eventData));
    }

    private void applyCommentCreatedEvent(CommentCreatedEventDTO eventData) {
        if (closed.get() || !mainFrame.isDisplayable()) {
            return;
        }

        mainFrame.updatePostCommentCount(
                eventData.getPostId(),
                Math.max(0, eventData.getCommentCount())
        );

        CommentDialog dialog = activeCommentDialog;
        if (dialog != null
                && dialog.isDisplayable()
                && activeCommentPostId == eventData.getPostId()
                && eventData.getComment() != null) {
            dialog.addOrUpdateComment(eventData.getComment());
        }
    }

    private void handlePostCreatedEvent(ServerEvent<String> event) {
        if (closed.get() || event == null || event.getData().isBlank()) {
            return;
        }

        final PostCreatedEventDTO eventData;

        try {
            eventData = JsonUtils.fromJson(event.getData(), PostCreatedEventDTO.class);
        } catch (RuntimeException e) {
            System.err.println("Không thể đọc sự kiện bài viết: " + e.getMessage());
            return;
        }

        if (eventData == null || eventData.getPost() == null || eventData.getPost().getId() <= 0) {
            return;
        }

        SwingUtilities.invokeLater(() -> {
            if (closed.get() || !mainFrame.isDisplayable()) {
                return;
            }

            mainFrame.addOrUpdatePost(eventData.getPost());
        });
    }

    public void loadAllPosts(
            Consumer<List<PostDTO>> onSuccess
    ) {
        if (!validateCallback(onSuccess)) {
            return;
        }

        executeListRequest(
                postService::getPosts,
                onSuccess,
                "tải danh sách bài viết"
        );
    }

    public void loadPostsBySubject(
            String subject,
            Consumer<List<PostDTO>> onSuccess
    ) {
        if (!validateCallback(onSuccess)) {
            return;
        }

        String normalizedSubject = subject == null
                ? null
                : subject.trim();

        executeListRequest(
                () -> postService.getPosts(normalizedSubject),
                onSuccess,
                "tải bài viết theo chủ đề"
        );
    }

    public void createPost(
            CreatePostDTO createPostDTO,
            Consumer<PostDTO> onSuccess
    ) {
        if (createPostDTO == null) {
            showError("Thông tin bài viết không được null.");
            return;
        }
        if (!validateCallback(onSuccess)) {
            return;
        }

        executeEntityRequest(
                () -> postService.createPost(createPostDTO),
                onSuccess,
                "tạo bài viết"
        );
    }

    public void loadPostDetail(
            long postId,
            Consumer<PostDTO> onSuccess
    ) {
        if (!validatePostId(postId)
                || !validateCallback(onSuccess)) {
            return;
        }

        executeEntityRequest(
                () -> postService.getPostDetail(postId),
                onSuccess,
                "tải chi tiết bài viết"
        );
    }

    public void loadComments(
            long postId,
            Consumer<List<CommentDTO>> onSuccess
    ) {
        loadComments(postId, onSuccess, this::showError);
    }

    public void loadComments(
            long postId,
            Consumer<List<CommentDTO>> onSuccess,
            Consumer<String> onError
    ) {
        Consumer<String> errorHandler = safeErrorHandler(onError);
        if (!validatePostId(postId, errorHandler)
                || !validateCallback(onSuccess, errorHandler)) {
            return;
        }

        executeListRequest(
                () -> commentService.getComments(postId),
                onSuccess,
                "tải bình luận",
                errorHandler
        );
    }

    public void createComment(
            CreateCommentDTO createCommentDTO,
            Consumer<CommentDTO> onSuccess
    ) {
        createComment(createCommentDTO, onSuccess, this::showError);
    }

    public void createComment(
            CreateCommentDTO createCommentDTO,
            Consumer<CommentDTO> onSuccess,
            Consumer<String> onError
    ) {
        Consumer<String> errorHandler = safeErrorHandler(onError);
        if (!validateCreateComment(createCommentDTO, errorHandler)
                || !validateCallback(onSuccess, errorHandler)) {
            return;
        }

        executeEntityRequest(
                () -> commentService.createComment(createCommentDTO),
                onSuccess,
                "tạo bình luận",
                errorHandler
        );
    }

    private void handleOnlineUsersEvent(ServerEvent<String> event) {
        if (closed.get() || event == null || event.getData().isBlank()) return;

        try {
            OnlineUsersEventDTO eventData = JsonUtils.fromJson(event.getData(), OnlineUsersEventDTO.class);
            if (eventData == null) return;

            SwingUtilities.invokeLater(() -> {
                if (!closed.get() && mainFrame.isDisplayable()) {
                    mainFrame.displayOnlineUsers(eventData.getUsers());
                }
            });
        } catch (RuntimeException e) {
            System.err.println("Không thể đọc sự kiện người dùng online: " + e.getMessage());
        }
    }

    private <T> void executeListRequest(
            Callable<Response<List<T>>> request,
            Consumer<List<T>> onSuccess,
            String actionDescription
    ) {
        executeListRequest(
                request,
                onSuccess,
                actionDescription,
                this::showError
        );
    }

    private <T> void executeListRequest(
            Callable<Response<List<T>>> request,
            Consumer<List<T>> onSuccess,
            String actionDescription,
            Consumer<String> onError
    ) {
        executeRequest(
                request,
                data -> data == null
                        ? Collections.emptyList()
                        : new ArrayList<>(data),
                onSuccess,
                actionDescription,
                onError
        );
    }

    private <T> void executeEntityRequest(
            Callable<Response<T>> request,
            Consumer<T> onSuccess,
            String actionDescription
    ) {
        executeEntityRequest(
                request,
                onSuccess,
                actionDescription,
                this::showError
        );
    }

    private <T> void executeEntityRequest(
            Callable<Response<T>> request,
            Consumer<T> onSuccess,
            String actionDescription,
            Consumer<String> onError
    ) {
        executeRequest(
                request,
                Function.identity(),
                onSuccess,
                actionDescription,
                onError
        );
    }

    private <T> void executeRequest(
            Callable<Response<T>> request,
            Function<T, T> dataNormalizer,
            Consumer<T> onSuccess,
            String actionDescription,
            Consumer<String> onError
    ) {
        SwingWorker<Response<T>, Void> worker =
                new SwingWorker<>() {
                    @Override
                    protected Response<T> doInBackground()
                            throws Exception {
                        synchronized (serviceLock) {
                            return request.call();
                        }
                    }

                    @Override
                    protected void done() {
                        handleCompletedRequest(
                                this,
                                dataNormalizer,
                                onSuccess,
                                actionDescription,
                                onError
                        );
                    }
                };

        worker.execute();
    }

    private <T> void handleCompletedRequest(
            SwingWorker<Response<T>, Void> worker,
            Function<T, T> dataNormalizer,
            Consumer<T> onSuccess,
            String actionDescription,
            Consumer<String> onError
    ) {
        try {
            Response<T> response = worker.get();

            if (response == null) {
                onError.accept(
                        "Server không trả về phản hồi khi "
                                + actionDescription
                                + "."
                );
                return;
            }

            if (!response.isSuccess()) {
                onError.accept(
                        responseMessage(response, actionDescription)
                );
                return;
            }

            T data = dataNormalizer.apply(response.getData());
            if (data == null) {
                onError.accept(
                        "Server không trả về dữ liệu khi "
                                + actionDescription
                                + "."
                );
                return;
            }

            onSuccess.accept(data);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            onError.accept(
                    "Yêu cầu đã bị gián đoạn khi "
                            + actionDescription
                            + "."
            );
        } catch (ExecutionException exception) {
            onError.accept(
                    requestExceptionMessage(
                            exception,
                            actionDescription
                    )
            );
        } catch (RuntimeException exception) {
            onError.accept(
                    requestExceptionMessage(
                            exception,
                            actionDescription
                    )
            );
        }
    }

    private boolean validatePostId(long postId) {
        return validatePostId(postId, this::showError);
    }

    private boolean validatePostId(
            long postId,
            Consumer<String> onError
    ) {
        if (postId > 0) {
            return true;
        }

        onError.accept("Mã bài viết phải lớn hơn 0.");
        return false;
    }

    private boolean validateCreateComment(
            CreateCommentDTO createCommentDTO,
            Consumer<String> onError
    ) {
        if (createCommentDTO == null) {
            onError.accept("Thông tin bình luận không được null.");
            return false;
        }
        if (!validatePostId(createCommentDTO.getPostId(), onError)) {
            return false;
        }
        if (createCommentDTO.getContent() == null
                || createCommentDTO.getContent().isBlank()) {
            onError.accept(
                    "Nội dung bình luận không được để trống."
            );
            return false;
        }

        return true;
    }

    private boolean validateCallback(Consumer<?> callback) {
        return validateCallback(callback, this::showError);
    }

    private boolean validateCallback(
            Consumer<?> callback,
            Consumer<String> onError
    ) {
        if (callback != null) {
            return true;
        }

        onError.accept("Callback nhận kết quả không được null.");
        return false;
    }

    private Consumer<String> safeErrorHandler(
            Consumer<String> onError
    ) {
        return onError == null ? this::showError : onError;
    }

    private String responseMessage(
            Response<?> response,
            String actionDescription
    ) {
        String message = response.getMessage();
        if (message != null && !message.isBlank()) {
            return message;
        }
        return "Không thể " + actionDescription + ".";
    }

    private String requestExceptionMessage(
            Throwable throwable,
            String actionDescription
    ) {
        Throwable cause = rootCause(throwable);
        String detail = cause.getMessage();
        if (detail == null || detail.isBlank()) {
            detail = cause.getClass().getSimpleName();
        }

        return "Không thể "
                + actionDescription
                + ": "
                + detail;
    }

    private Throwable rootCause(Throwable throwable) {
        Throwable cause = throwable;
        while (cause.getCause() != null
                && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        return cause;
    }

    private void showError(String message) {
        Runnable showDialog = () -> JOptionPane.showMessageDialog(
                mainFrame,
                message,
                "Lỗi",
                JOptionPane.ERROR_MESSAGE
        );

        if (SwingUtilities.isEventDispatchThread()) {
            showDialog.run();
        } else {
            SwingUtilities.invokeLater(showDialog);
        }
    }
}
