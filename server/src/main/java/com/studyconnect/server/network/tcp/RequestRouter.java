package com.studyconnect.server.network.tcp;

import com.studyconnect.common.dto.CommentCreatedEventDTO;
import com.studyconnect.common.dto.CommentDTO;
import com.studyconnect.common.dto.CreateCommentDTO;
import com.studyconnect.common.dto.CreatePostDTO;
import com.studyconnect.common.dto.LoginDTO;
import com.studyconnect.common.dto.PostDTO;
import com.studyconnect.common.dto.RegisterDTO;
import com.studyconnect.common.protocol.Request;
import com.studyconnect.common.protocol.Response;
import com.studyconnect.common.protocol.ServerEvent;
import com.studyconnect.common.protocol.ServerEventType;
import com.studyconnect.common.protocol.StatusCode;
import com.studyconnect.common.util.JsonUtils;
import com.studyconnect.server.network.session.SessionManager;
import com.studyconnect.server.service.AuthService;
import com.studyconnect.server.service.CommentService;
import com.studyconnect.server.service.PostService;

import java.sql.SQLException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;

public class RequestRouter {
    private final AuthService authService;
    private final PostService postService;
    private final CommentService commentService;
    private final SessionManager sessionManager;
    private final ClientConnectionManager connectionManager;

    public RequestRouter(
            ClientConnectionManager connectionManager
    ) {
        this.authService = new AuthService();
        this.postService = new PostService();
        this.commentService = new CommentService();
        this.sessionManager = SessionManager.getInstance();
        this.connectionManager = Objects.requireNonNull(
                connectionManager,
                "connectionManager"
        );
    }

    public Response<String> route(
            Request<String> request,
            ClientHandler sourceConnection
    ) {
        if (request == null) {
            return Response.error(
                    null,
                    StatusCode.BAD_REQUEST,
                    "Request không được null"
            );
        }
        if (request.getAction() == null) {
            return Response.error(
                    request.getRequestId(),
                    StatusCode.BAD_REQUEST,
                    "Action không được null"
            );
        }

        try {
            return switch (request.getAction()) {
                case PING -> handlePing(request);
                case REGISTER -> handleRegister(request);
                case LOGIN -> handleLogin(request);
                case LOGOUT -> handleLogout(
                        request,
                        sourceConnection
                );
                case CREATE_POST -> handleCreatePost(
                        request,
                        sourceConnection
                );
                case GET_POSTS -> handleGetPosts(
                        request,
                        sourceConnection
                );
                case GET_POST_DETAIL -> handleGetPostDetail(
                        request,
                        sourceConnection
                );
                case CREATE_COMMENT -> handleCreateComment(
                        request,
                        sourceConnection
                );
                case GET_COMMENTS -> handleGetComments(
                        request,
                        sourceConnection
                );
                default -> Response.error(
                        request.getRequestId(),
                        StatusCode.BAD_REQUEST,
                        "Server chưa hỗ trợ hành động: "
                                + request.getAction()
                );
            };
        } catch (SecurityException exception) {
            return Response.error(
                    request.getRequestId(),
                    StatusCode.UNAUTHORIZED,
                    exception.getMessage()
            );
        } catch (NoSuchElementException exception) {
            return Response.error(
                    request.getRequestId(),
                    StatusCode.NOT_FOUND,
                    exception.getMessage()
            );
        } catch (IllegalArgumentException exception) {
            return Response.error(
                    request.getRequestId(),
                    StatusCode.BAD_REQUEST,
                    exception.getMessage()
            );
        } catch (SQLException exception) {
            exception.printStackTrace();
            return Response.error(
                    request.getRequestId(),
                    StatusCode.SERVER_ERROR,
                    "Lỗi truy cập cơ sở dữ liệu"
            );
        } catch (RuntimeException exception) {
            exception.printStackTrace();
            return Response.error(
                    request.getRequestId(),
                    StatusCode.BAD_REQUEST,
                    "Dữ liệu gửi lên không hợp lệ"
            );
        }
    }

    private Response<String> handlePing(
            Request<String> request
    ) {
        return Response.success(
                request.getRequestId(),
                "Server đang hoạt động",
                "PONG"
        );
    }

    private Response<String> handleRegister(
            Request<String> request
    ) {
        requireData(request);
        RegisterDTO registerDTO = JsonUtils.fromJson(
                request.getData(),
                RegisterDTO.class
        );
        return authService.register(
                request.getRequestId(),
                registerDTO
        );
    }

    private Response<String> handleLogin(
            Request<String> request
    ) {
        requireData(request);
        LoginDTO loginDTO = JsonUtils.fromJson(
                request.getData(),
                LoginDTO.class
        );
        return authService.login(
                request.getRequestId(),
                loginDTO
        );
    }

    private Response<String> handleLogout(
            Request<String> request,
            ClientHandler sourceConnection
    ) {
        requireUserId(request, sourceConnection);
        sessionManager.removeSession(request.getToken());
        connectionManager.unauthenticate(sourceConnection);
        return Response.success(
                request.getRequestId(),
                "Đăng xuất thành công",
                null
        );
    }

    private Response<String> handleCreatePost(
            Request<String> request,
            ClientHandler sourceConnection
    ) throws SQLException {
        long authorId = requireUserId(
                request,
                sourceConnection
        );
        requireData(request);
        CreatePostDTO createPostDTO = JsonUtils.fromJson(
                request.getData(),
                CreatePostDTO.class
        );
        PostDTO createdPost = postService.createPost(
                authorId,
                createPostDTO
        );
        return Response.success(
                request.getRequestId(),
                "Đăng bài thành công",
                JsonUtils.toJson(createdPost)
        );
    }

    private Response<String> handleGetPosts(
            Request<String> request,
            ClientHandler sourceConnection
    ) throws SQLException {
        requireUserId(request, sourceConnection);
        List<PostDTO> posts;

        if (request.getData() == null
                || request.getData().isBlank()
                || "null".equals(request.getData())) {
            posts = postService.getAllPosts();
        } else {
            String subject = JsonUtils.fromJson(
                    request.getData(),
                    String.class
            );
            posts = subject == null || subject.isBlank()
                    ? postService.getAllPosts()
                    : postService.getPostsBySubject(subject);
        }

        return Response.success(
                request.getRequestId(),
                "Lấy danh sách bài viết thành công",
                JsonUtils.toJson(posts)
        );
    }

    private Response<String> handleGetPostDetail(
            Request<String> request,
            ClientHandler sourceConnection
    ) throws SQLException {
        requireUserId(request, sourceConnection);
        requireData(request);
        Long postId = JsonUtils.fromJson(
                request.getData(),
                Long.class
        );
        if (postId == null || postId <= 0) {
            throw new IllegalArgumentException(
                    "Mã bài viết không hợp lệ"
            );
        }
        PostDTO post = postService.getPostById(postId);
        return Response.success(
                request.getRequestId(),
                "Lấy chi tiết bài viết thành công",
                JsonUtils.toJson(post)
        );
    }

    private Response<String> handleCreateComment(
            Request<String> request,
            ClientHandler sourceConnection
    ) throws SQLException {
        long authorId = requireUserId(
                request,
                sourceConnection
        );
        requireData(request);

        CreateCommentDTO createCommentDTO = JsonUtils.fromJson(
                request.getData(),
                CreateCommentDTO.class
        );
        CommentDTO createdComment =
                commentService.createComment(
                        authorId,
                        createCommentDTO
                );

        int commentCount = commentService.countComments(
                createdComment.getPostId()
        );
        long timestamp = System.currentTimeMillis();
        CommentCreatedEventDTO eventData =
                new CommentCreatedEventDTO(
                        createdComment.getPostId(),
                        createdComment,
                        commentCount,
                        timestamp
                );
        ServerEvent<String> event = new ServerEvent<>(
                ServerEventType.COMMENT_CREATED,
                JsonUtils.toJson(eventData)
        );
        event.setTimestamp(timestamp);

        connectionManager.broadcastAuthenticated(
                event,
                sourceConnection
        );

        return Response.success(
                request.getRequestId(),
                "Bình luận thành công",
                JsonUtils.toJson(createdComment)
        );
    }

    private Response<String> handleGetComments(
            Request<String> request,
            ClientHandler sourceConnection
    ) throws SQLException {
        requireUserId(request, sourceConnection);
        requireData(request);
        Long postId = JsonUtils.fromJson(
                request.getData(),
                Long.class
        );
        if (postId == null || postId <= 0) {
            throw new IllegalArgumentException(
                    "Mã bài viết không hợp lệ"
            );
        }
        List<CommentDTO> comments =
                commentService.getCommentsByPostId(postId);
        return Response.success(
                request.getRequestId(),
                "Lấy danh sách bình luận thành công",
                JsonUtils.toJson(comments)
        );
    }

    private long requireUserId(
            Request<String> request,
            ClientHandler sourceConnection
    ) {
        String token = request.getToken();
        if (token == null || token.isBlank()) {
            throw new SecurityException(
                    "Bạn chưa đăng nhập"
            );
        }

        Long userId = sessionManager.getUserId(token);
        if (userId == null) {
            throw new SecurityException(
                    "Phiên đăng nhập không hợp lệ hoặc đã hết hạn"
            );
        }

        connectionManager.authenticate(
                sourceConnection,
                userId
        );
        return userId;
    }

    private void requireData(Request<String> request) {
        if (request.getData() == null
                || request.getData().isBlank()
                || "null".equals(request.getData())) {
            throw new IllegalArgumentException(
                    "Dữ liệu request không được để trống"
            );
        }
    }
}
