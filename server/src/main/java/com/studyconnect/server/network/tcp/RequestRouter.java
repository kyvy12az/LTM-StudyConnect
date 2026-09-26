package com.studyconnect.server.network.tcp;

import com.studyconnect.common.dto.CommentDTO;
import com.studyconnect.common.dto.CreateCommentDTO;
import com.studyconnect.common.dto.CreatePostDTO;
import com.studyconnect.common.dto.LoginDTO;
import com.studyconnect.common.dto.PostDTO;
import com.studyconnect.common.dto.RegisterDTO;
import com.studyconnect.common.protocol.Request;
import com.studyconnect.common.protocol.Response;
import com.studyconnect.common.protocol.StatusCode;
import com.studyconnect.common.util.JsonUtils;
import com.studyconnect.server.network.session.SessionManager;
import com.studyconnect.server.service.AuthService;
import com.studyconnect.server.service.CommentService;
import com.studyconnect.server.service.PostService;

import java.sql.SQLException;
import java.util.List;
import java.util.NoSuchElementException;

public class RequestRouter {

    private final AuthService authService;
    private final PostService postService;
    private final CommentService commentService;
    private final SessionManager sessionManager;

    public RequestRouter() {
        this.authService = new AuthService();
        this.postService = new PostService();
        this.commentService = new CommentService();
        this.sessionManager = SessionManager.getInstance();
    }

    public Response<String> route(Request<String> request) {
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
                case LOGOUT -> handleLogout(request);

                case CREATE_POST -> handleCreatePost(request);
                case GET_POSTS -> handleGetPosts(request);
                case GET_POST_DETAIL -> handleGetPostDetail(request);

                case CREATE_COMMENT -> handleCreateComment(request);
                case GET_COMMENTS -> handleGetComments(request);

                default -> Response.error(
                        request.getRequestId(),
                        StatusCode.BAD_REQUEST,
                        "Server chưa hỗ trợ hành động: "
                                + request.getAction()
                );
            };

        } catch (SecurityException e) {
            return Response.error(
                    request.getRequestId(),
                    StatusCode.UNAUTHORIZED,
                    e.getMessage()
            );

        } catch (NoSuchElementException e) {
            return Response.error(
                    request.getRequestId(),
                    StatusCode.NOT_FOUND,
                    e.getMessage()
            );

        } catch (IllegalArgumentException e) {
            return Response.error(
                    request.getRequestId(),
                    StatusCode.BAD_REQUEST,
                    e.getMessage()
            );

        } catch (SQLException e) {
            e.printStackTrace();

            return Response.error(
                    request.getRequestId(),
                    StatusCode.SERVER_ERROR,
                    "Lỗi truy cập cơ sở dữ liệu"
            );

        } catch (RuntimeException e) {
            e.printStackTrace();

            return Response.error(
                    request.getRequestId(),
                    StatusCode.BAD_REQUEST,
                    "Dữ liệu gửi lên không hợp lệ"
            );
        }
    }

    private Response<String> handlePing(Request<String> request) {
        System.out.println("Nội dung PING: " + request.getData());

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
            Request<String> request
    ) {
        requireUserId(request);

        sessionManager.removeSession(request.getToken());

        return Response.success(
                request.getRequestId(),
                "Đăng xuất thành công",
                null
        );
    }

    private Response<String> handleCreatePost(
            Request<String> request
    ) throws SQLException {
        long authorId = requireUserId(request);
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
            Request<String> request
    ) throws SQLException {
        requireUserId(request);

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

            if (subject == null || subject.isBlank()) {
                posts = postService.getAllPosts();
            } else {
                posts = postService.getPostsBySubject(subject);
            }
        }

        return Response.success(
                request.getRequestId(),
                "Lấy danh sách bài viết thành công",
                JsonUtils.toJson(posts)
        );
    }

    private Response<String> handleGetPostDetail(
            Request<String> request
    ) throws SQLException {
        requireUserId(request);
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
            Request<String> request
    ) throws SQLException {
        long authorId = requireUserId(request);
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

        return Response.success(
                request.getRequestId(),
                "Bình luận thành công",
                JsonUtils.toJson(createdComment)
        );
    }

    private Response<String> handleGetComments(
            Request<String> request
    ) throws SQLException {
        requireUserId(request);
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


    private long requireUserId(Request<String> request) {
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