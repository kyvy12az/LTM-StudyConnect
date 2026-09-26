package com.studyconnect.client.controller;

import com.studyconnect.client.service.CommentService;
import com.studyconnect.client.service.PostService;
import com.studyconnect.client.view.main.MainFrame;
import com.studyconnect.common.dto.CommentDTO;
import com.studyconnect.common.dto.CreateCommentDTO;
import com.studyconnect.common.dto.CreatePostDTO;
import com.studyconnect.common.dto.PostDTO;
import com.studyconnect.common.protocol.Response;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.function.Function;

public class MainController {
    private final MainFrame mainFrame;
    private final PostService postService;
    private final CommentService commentService;
    private final Object serviceLock = new Object();

    public MainController(
            MainFrame mainFrame,
            PostService postService,
            CommentService commentService
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
        if (!validatePostId(postId)
                || !validateCallback(onSuccess)) {
            return;
        }

        executeListRequest(
                () -> commentService.getComments(postId),
                onSuccess,
                "tải bình luận"
        );
    }

    public void createComment(
            CreateCommentDTO createCommentDTO,
            Consumer<CommentDTO> onSuccess
    ) {
        if (!validateCreateComment(createCommentDTO)
                || !validateCallback(onSuccess)) {
            return;
        }

        executeEntityRequest(
                () -> commentService.createComment(createCommentDTO),
                onSuccess,
                "tạo bình luận"
        );
    }

    private <T> void executeListRequest(
            Callable<Response<List<T>>> request,
            Consumer<List<T>> onSuccess,
            String actionDescription
    ) {
        executeRequest(
                request,
                data -> data == null
                        ? Collections.emptyList()
                        : new ArrayList<>(data),
                onSuccess,
                actionDescription
        );
    }

    private <T> void executeEntityRequest(
            Callable<Response<T>> request,
            Consumer<T> onSuccess,
            String actionDescription
    ) {
        executeRequest(
                request,
                Function.identity(),
                onSuccess,
                actionDescription
        );
    }

    private <T> void executeRequest(
            Callable<Response<T>> request,
            Function<T, T> dataNormalizer,
            Consumer<T> onSuccess,
            String actionDescription
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
                                actionDescription
                        );
                    }
                };

        worker.execute();
    }

    private <T> void handleCompletedRequest(
            SwingWorker<Response<T>, Void> worker,
            Function<T, T> dataNormalizer,
            Consumer<T> onSuccess,
            String actionDescription
    ) {
        try {
            Response<T> response = worker.get();

            if (response == null) {
                showError(
                        "Server không trả về phản hồi khi "
                                + actionDescription
                                + "."
                );
                return;
            }

            if (!response.isSuccess()) {
                showError(responseMessage(response, actionDescription));
                return;
            }

            T data = dataNormalizer.apply(response.getData());
            if (data == null) {
                showError(
                        "Server không trả về dữ liệu khi "
                                + actionDescription
                                + "."
                );
                return;
            }

            onSuccess.accept(data);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            showError(
                    "Yêu cầu đã bị gián đoạn khi "
                            + actionDescription
                            + "."
            );
        } catch (ExecutionException exception) {
            showRequestException(exception, actionDescription);
        } catch (RuntimeException exception) {
            showRequestException(exception, actionDescription);
        }
    }

    private boolean validatePostId(long postId) {
        if (postId > 0) {
            return true;
        }

        showError("Mã bài viết phải lớn hơn 0.");
        return false;
    }

    private boolean validateCreateComment(
            CreateCommentDTO createCommentDTO
    ) {
        if (createCommentDTO == null) {
            showError("Thông tin bình luận không được null.");
            return false;
        }
        if (!validatePostId(createCommentDTO.getPostId())) {
            return false;
        }
        if (createCommentDTO.getContent() == null
                || createCommentDTO.getContent().isBlank()) {
            showError("Nội dung bình luận không được để trống.");
            return false;
        }

        return true;
    }

    private boolean validateCallback(Consumer<?> callback) {
        if (callback != null) {
            return true;
        }

        showError("Callback nhận kết quả không được null.");
        return false;
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

    private void showRequestException(
            Throwable throwable,
            String actionDescription
    ) {
        Throwable cause = rootCause(throwable);
        String detail = cause.getMessage();
        if (detail == null || detail.isBlank()) {
            detail = cause.getClass().getSimpleName();
        }

        showError(
                "Không thể "
                        + actionDescription
                        + ": "
                        + detail
        );
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
