package com.studyconnect.server.network.tcp;

import com.studyconnect.common.dto.*;
import com.studyconnect.common.protocol.Request;
import com.studyconnect.common.protocol.Response;
import com.studyconnect.common.protocol.ServerEvent;
import com.studyconnect.common.protocol.ServerEventType;
import com.studyconnect.common.protocol.StatusCode;
import com.studyconnect.common.util.JsonUtils;
import com.studyconnect.server.model.dao.UserDAO;
import com.studyconnect.server.network.peer.PeerRegistry;
import com.studyconnect.server.network.session.SessionManager;
import com.studyconnect.server.service.AuthService;
import com.studyconnect.server.service.CommentService;
import com.studyconnect.server.service.MessageService;
import com.studyconnect.server.service.PostService;

import java.sql.SQLException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Consumer;

public class RequestRouter {
    private final AuthService authService;
    private final PostService postService;
    private final CommentService commentService;
    private final MessageService messageService;
    private final SessionManager sessionManager;
    private final ClientConnectionManager connectionManager;
    private final UserDAO userDAO;
    private final PeerRegistry peerRegistry;
    private final Consumer<PostDTO> postCreatedListener;

    public RequestRouter(
            ClientConnectionManager connectionManager
    ) {
        this(connectionManager, null);
    }

    public RequestRouter(
            ClientConnectionManager connectionManager,
            Consumer<PostDTO> postCreatedListener
    ) {
        this.authService = new AuthService();
        this.postService = new PostService();
        this.commentService = new CommentService();
        this.messageService = new MessageService();
        this.userDAO = new UserDAO();
        this.peerRegistry = new PeerRegistry();
        this.sessionManager = SessionManager.getInstance();
        this.connectionManager = Objects.requireNonNull(
                connectionManager,
                "connectionManager"
        );
        this.postCreatedListener = postCreatedListener;
        this.connectionManager.setPresenceChangedListener(this::pushOnlineUsers);
        this.connectionManager.setUserOfflineListener(this::unregisterOfflinePeer);
    }

    private void pushOnlineUsers() {
        try {
            List<UserDTO> onlineUsers = userDAO.findByIds(connectionManager.getOnlineUserIds());
            long timestamp = System.currentTimeMillis();
            OnlineUsersEventDTO eventData = new OnlineUsersEventDTO(onlineUsers, timestamp);
            ServerEvent<String> event = new ServerEvent<>(ServerEventType.ONLINE_USERS_UPDATED, JsonUtils.toJson(eventData));
            event.setTimestamp(timestamp);

            connectionManager.broadcastAuthenticated(event, null);
            System.out.println(
                    "[PRESENCE] Đã push "
                            + onlineUsers.size()
                            + " người dùng online"
            );
        } catch (SQLException e) {
            System.err.println("Không thể lấy danh sách online: " + e.getMessage());
        }
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
                case LIKE_POST -> handlePostLike(
                        request,
                        sourceConnection,
                        true
                );
                case UNLIKE_POST -> handlePostLike(
                        request,
                        sourceConnection,
                        false
                );
                case CREATE_COMMENT -> handleCreateComment(
                        request,
                        sourceConnection
                );
                case GET_COMMENTS -> handleGetComments(
                        request,
                        sourceConnection
                );
                case REGISTER_PEER -> handleRegisterPeer(request, sourceConnection);
                case UNREGISTER_PEER -> handleUnregisterPeer(request, sourceConnection);
                case REQUEST_PEER_INFO -> handleRequestPeerInfo(request, sourceConnection);
                case SYNC_MESSAGE -> handleSyncMessage(request, sourceConnection);
                case SEND_MESSAGE -> handleSendMessage(request, sourceConnection);
                case GET_MESSAGES, GET_MESSAGE_HISTORY -> handleGetMessages(request, sourceConnection);
                case GET_CONVERSATIONS -> handleGetConversations(request, sourceConnection);
                case MARK_MESSAGES_READ, MARK_MESSAGE_READ -> handleMarkMessagesRead(request, sourceConnection);
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
        long userId = requireUserId(request, sourceConnection);
        pushPeerStatus(peerRegistry.unregister(userId));
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

        long timestamp = System.currentTimeMillis();

        PostCreatedEventDTO eventData = new PostCreatedEventDTO(createdPost, timestamp);
        ServerEvent<String> event = new ServerEvent<>(ServerEventType.POST_CREATED, JsonUtils.toJson(eventData));
        event.setTimestamp(timestamp);

        System.out.println(
                "[SERVER] Chuẩn bị push POST_CREATED, postId="
                        + createdPost.getId()
        );

        connectionManager.broadcastAuthenticated(event, sourceConnection);

        if (postCreatedListener != null) {
            postCreatedListener.accept(createdPost);
        }

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
        long viewerId = requireUserId(request, sourceConnection);
        List<PostDTO> posts;

        if (request.getData() == null
                || request.getData().isBlank()
                || "null".equals(request.getData())) {
            posts = postService.getAllPosts(viewerId);
        } else {
            String subject = JsonUtils.fromJson(
                    request.getData(),
                    String.class
            );
            posts = subject == null || subject.isBlank()
                    ? postService.getAllPosts(viewerId)
                    : postService.getPostsBySubject(subject, viewerId);
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
        long viewerId = requireUserId(request, sourceConnection);
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
        PostDTO post = postService.getPostById(postId, viewerId);
        return Response.success(
                request.getRequestId(),
                "Lấy chi tiết bài viết thành công",
                JsonUtils.toJson(post)
        );
    }

    private Response<String> handlePostLike(
            Request<String> request,
            ClientHandler sourceConnection,
            boolean liked
    ) throws SQLException {
        long userId = requireUserId(request, sourceConnection);
        requireData(request);
        Long postId = JsonUtils.fromJson(request.getData(), Long.class);
        if (postId == null || postId <= 0) {
            throw new IllegalArgumentException("Mã bài viết không hợp lệ");
        }

        PostLikeDTO result = liked
                ? postService.likePost(userId, postId)
                : postService.unlikePost(userId, postId);
        ServerEvent<String> event = new ServerEvent<>(
                ServerEventType.POST_LIKE_UPDATED,
                JsonUtils.toJson(result)
        );
        event.setTimestamp(result.getUpdatedAt());
        connectionManager.broadcastAuthenticated(event, sourceConnection);

        return Response.success(
                request.getRequestId(),
                liked ? "Đã thích bài viết" : "Đã bỏ thích bài viết",
                JsonUtils.toJson(result)
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

    private Response<String> handleSendMessage(
            Request<String> request,
            ClientHandler sourceConnection
    ) throws SQLException {
        long senderId = requireUserId(request, sourceConnection);
        requireData(request);
        SendMessageDTO command = JsonUtils.fromJson(
                request.getData(), SendMessageDTO.class);
        MessageDTO message = messageService.relayMessage(senderId, command);

        ServerEvent<String> event = new ServerEvent<>(
                ServerEventType.MESSAGE_RECEIVED,
                JsonUtils.toJson(message)
        );
        event.setTimestamp(message.getCreatedAt());
        boolean delivered = connectionManager.sendToUser(message.getReceiverId(), event);
        if (delivered && message.getStatus() != MessageStatus.READ) {
            message = messageService.markDelivered(message.getClientMessageId());
            pushMessageStatus(senderId, message);
        }

        return Response.success(
                request.getRequestId(),
                "Gửi tin nhắn thành công",
                JsonUtils.toJson(message)
        );
    }

    private Response<String> handleSyncMessage(
            Request<String> request,
            ClientHandler sourceConnection
    ) throws SQLException {
        long senderId = requireUserId(request, sourceConnection);
        requireData(request);
        SendMessageDTO command = JsonUtils.fromJson(
                request.getData(), SendMessageDTO.class);
        MessageDTO message = messageService.syncP2PMessage(senderId, command);

        ServerEvent<String> event = new ServerEvent<>(
                ServerEventType.MESSAGE_RECEIVED,
                JsonUtils.toJson(message)
        );
        event.setTimestamp(message.getCreatedAt());
        connectionManager.sendToUser(message.getReceiverId(), event);
        pushMessageStatus(senderId, message);

        return Response.success(
                request.getRequestId(),
                "Đồng bộ tin nhắn P2P thành công",
                JsonUtils.toJson(message)
        );
    }

    private Response<String> handleRegisterPeer(
            Request<String> request,
            ClientHandler sourceConnection
    ) {
        long userId = requireUserId(request, sourceConnection);
        requireData(request);
        RegisterPeerDTO command = JsonUtils.fromJson(
                request.getData(), RegisterPeerDTO.class);
        if (command == null) {
            throw new IllegalArgumentException("Thông tin peer không hợp lệ");
        }
        PeerInfoDTO peer = peerRegistry.register(
                userId, sourceConnection.getRemoteHost(), command.getPort());
        pushPeerStatus(peerRegistry.publicStatus(userId));
        return Response.success(
                request.getRequestId(),
                "Đăng ký peer thành công",
                JsonUtils.toJson(peer)
        );
    }

    private Response<String> handleUnregisterPeer(
            Request<String> request,
            ClientHandler sourceConnection
    ) {
        long userId = requireUserId(request, sourceConnection);
        PeerInfoDTO offline = peerRegistry.unregister(userId);
        pushPeerStatus(offline);
        return Response.success(
                request.getRequestId(),
                "Hủy đăng ký peer thành công",
                JsonUtils.toJson(offline)
        );
    }

    private Response<String> handleRequestPeerInfo(
            Request<String> request,
            ClientHandler sourceConnection
    ) {
        long requesterId = requireUserId(request, sourceConnection);
        requireData(request);
        Long targetUserId = JsonUtils.fromJson(request.getData(), Long.class);
        if (targetUserId == null || targetUserId <= 0 || targetUserId == requesterId) {
            throw new IllegalArgumentException("Người nhận peer không hợp lệ");
        }
        PeerInfoDTO peer = peerRegistry.find(targetUserId)
                .filter(value -> connectionManager.getOnlineUserIds().contains(targetUserId))
                .orElse(new PeerInfoDTO(
                        targetUserId, null, 0, false,
                        System.currentTimeMillis(), null));
        return Response.success(
                request.getRequestId(),
                peer.isAvailable() ? "Đã tìm thấy peer" : "Peer hiện không khả dụng",
                JsonUtils.toJson(peer)
        );
    }

    private Response<String> handleGetMessages(
            Request<String> request,
            ClientHandler sourceConnection
    ) throws SQLException {
        long currentUserId = requireUserId(request, sourceConnection);
        requireData(request);
        GetMessagesDTO query = JsonUtils.fromJson(
                request.getData(), GetMessagesDTO.class);
        List<MessageDTO> messages = messageService.getMessages(currentUserId, query);
        for (int index = 0; index < messages.size(); index++) {
            MessageDTO message = messages.get(index);
            if (message.getReceiverId() == currentUserId
                    && message.getStatus() == MessageStatus.SENT) {
                MessageDTO delivered = messageService.markDelivered(
                        message.getClientMessageId());
                messages.set(index, delivered);
                pushMessageStatus(delivered.getSenderId(), delivered);
            }
        }
        return Response.success(
                request.getRequestId(),
                "Lấy lịch sử tin nhắn thành công",
                JsonUtils.toJson(messages)
        );
    }

    private Response<String> handleGetConversations(
            Request<String> request,
            ClientHandler sourceConnection
    ) throws SQLException {
        long currentUserId = requireUserId(request, sourceConnection);
        List<ConversationDTO> conversations = messageService.getConversations(
                currentUserId, connectionManager.getOnlineUserIds());
        return Response.success(
                request.getRequestId(),
                "Lấy danh sách hội thoại thành công",
                JsonUtils.toJson(conversations)
        );
    }

    private Response<String> handleMarkMessagesRead(
            Request<String> request,
            ClientHandler sourceConnection
    ) throws SQLException {
        long currentUserId = requireUserId(request, sourceConnection);
        requireData(request);
        MarkMessagesReadDTO command = JsonUtils.fromJson(
                request.getData(), MarkMessagesReadDTO.class);
        if (command == null) {
            throw new IllegalArgumentException("Dữ liệu đánh dấu đã đọc không hợp lệ");
        }
        MessageReadEventDTO receipt = messageService.markMessagesRead(
                currentUserId, command.getOtherUserId());
        if (!receipt.getMessageIds().isEmpty()) {
            ServerEvent<String> event = new ServerEvent<>(
                    ServerEventType.MESSAGE_READ,
                    JsonUtils.toJson(receipt)
            );
            event.setTimestamp(receipt.getReadAt());
            connectionManager.sendToUser(command.getOtherUserId(), event);
        }
        return Response.success(
                request.getRequestId(),
                "Đã đánh dấu tin nhắn là đã đọc",
                JsonUtils.toJson(receipt)
        );
    }

    private void pushMessageStatus(long senderId, MessageDTO message) {
        ServerEvent<String> event = new ServerEvent<>(
                ServerEventType.MESSAGE_STATUS_UPDATED,
                JsonUtils.toJson(message)
        );
        event.setTimestamp(System.currentTimeMillis());
        connectionManager.sendToUser(senderId, event);
    }

    private void pushPeerStatus(PeerInfoDTO peer) {
        ServerEvent<String> event = new ServerEvent<>(
                ServerEventType.PEER_STATUS_CHANGED,
                JsonUtils.toJson(peer)
        );
        event.setTimestamp(System.currentTimeMillis());
        connectionManager.broadcastAuthenticated(event, null);
    }

    private void unregisterOfflinePeer(long userId) {
        pushPeerStatus(peerRegistry.unregister(userId));
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
