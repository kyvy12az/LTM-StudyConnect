package com.studyconnect.client.service;

import com.studyconnect.client.network.peer.PeerMessageTransport;
import com.studyconnect.client.network.tcp.TCPClient;
import com.studyconnect.common.dto.ConversationDTO;
import com.studyconnect.common.dto.GetMessagesDTO;
import com.studyconnect.common.dto.MarkMessagesReadDTO;
import com.studyconnect.common.dto.MessageDTO;
import com.studyconnect.common.dto.MessageDeliveryMode;
import com.studyconnect.common.dto.MessageReadEventDTO;
import com.studyconnect.common.dto.MessageStatus;
import com.studyconnect.common.dto.PeerInfoDTO;
import com.studyconnect.common.dto.RegisterPeerDTO;
import com.studyconnect.common.dto.SendMessageDTO;
import com.studyconnect.common.dto.UserDTO;
import com.studyconnect.common.protocol.ActionType;
import com.studyconnect.common.protocol.Request;
import com.studyconnect.common.protocol.Response;
import com.studyconnect.common.util.JsonUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public class MessageService implements AutoCloseable {
    private final TCPClient tcpClient;
    private final String authToken;
    private final UserDTO currentUser;
    private final PeerMessageTransport peerTransport;
    private volatile String localPeerToken;

    public MessageService(TCPClient tcpClient, String authToken, UserDTO currentUser) {
        if (tcpClient == null) throw new IllegalArgumentException("TCPClient không được null");
        if (authToken == null || authToken.isBlank()) {
            throw new IllegalArgumentException("Token đăng nhập không hợp lệ");
        }
        if (currentUser == null || currentUser.getId() <= 0) {
            throw new IllegalArgumentException("Người dùng hiện tại không hợp lệ");
        }
        this.tcpClient = tcpClient;
        this.authToken = authToken;
        this.currentUser = currentUser;
        this.peerTransport = new PeerMessageTransport(currentUser.getId());
    }

    public PeerInfoDTO initializePeer(Consumer<SendMessageDTO> incomingListener)
            throws IOException {
        int port = peerTransport.start(message -> {
            if (!isAuthenticatedPeerSender(message)) {
                throw new SecurityException("Không thể xác thực peer gửi tin nhắn");
            }
            incomingListener.accept(message);
        });
        Response<PeerInfoDTO> response = convert(
                send(ActionType.REGISTER_PEER, new RegisterPeerDTO(port)),
                PeerInfoDTO.class
        );
        if (!response.isSuccess() || response.getData() == null) {
            peerTransport.close();
            throw new IOException(response.getMessage());
        }
        localPeerToken = response.getData().getPeerToken();
        peerTransport.setPeerToken(localPeerToken);
        return response.getData();
    }

    public SendMessageDTO createOutgoingMessage(long receiverId, String content) {
        SendMessageDTO message = new SendMessageDTO();
        message.setClientMessageId(UUID.randomUUID().toString());
        message.setSenderId(currentUser.getId());
        message.setSenderName(displayName(currentUser));
        message.setSenderAvatarUrl(currentUser.getAvatarUrl());
        message.setReceiverId(receiverId);
        message.setContent(content == null ? "" : content.trim());
        message.setCreatedAt(System.currentTimeMillis());
        message.setDeliveryMode(MessageDeliveryMode.P2P);
        message.setSenderPeerToken(localPeerToken);
        return message;
    }

    public Response<MessageDTO> sendMessage(SendMessageDTO message) throws IOException {
        IOException p2pFailure = null;
        try {
            Response<PeerInfoDTO> peerResponse = requestPeerInfo(message.getReceiverId());
            PeerInfoDTO peer = peerResponse.getData();
            if (peerResponse.isSuccess() && peer != null && peer.isAvailable()) {
                peerTransport.send(peer, message);
                message.setDeliveryMode(MessageDeliveryMode.P2P);
                Response<MessageDTO> synced = convert(
                        send(ActionType.SYNC_MESSAGE, message), MessageDTO.class);
                if (synced.isSuccess() && synced.getData() != null) return synced;
                p2pFailure = new IOException(synced.getMessage());
            }
        } catch (IOException | RuntimeException exception) {
            p2pFailure = exception instanceof IOException io
                    ? io : new IOException(exception.getMessage(), exception);
        }

        message.setDeliveryMode(MessageDeliveryMode.SERVER_RELAY);
        Response<MessageDTO> relayed = convert(
                send(ActionType.SEND_MESSAGE, message), MessageDTO.class);
        if (!relayed.isSuccess() && p2pFailure != null) {
            relayed.setMessage(relayed.getMessage() + " (P2P: " + p2pFailure.getMessage() + ")");
        }
        return relayed;
    }

    public Response<PeerInfoDTO> requestPeerInfo(long userId) throws IOException {
        return convert(send(ActionType.REQUEST_PEER_INFO, userId), PeerInfoDTO.class);
    }

    private boolean isAuthenticatedPeerSender(SendMessageDTO message) {
        if (message == null || message.getSenderPeerToken() == null
                || message.getSenderPeerToken().isBlank()) {
            return false;
        }
        try {
            Response<PeerInfoDTO> response = requestPeerInfo(message.getSenderId());
            PeerInfoDTO sender = response.getData();
            return response.isSuccess()
                    && sender != null
                    && sender.isAvailable()
                    && message.getSenderPeerToken().equals(sender.getPeerToken());
        } catch (IOException | RuntimeException exception) {
            return false;
        }
    }

    public Response<List<MessageDTO>> getMessages(
            long otherUserId, Long beforeMessageId, int limit
    ) throws IOException {
        Response<String> raw = send(
                ActionType.GET_MESSAGE_HISTORY,
                new GetMessagesDTO(otherUserId, beforeMessageId, limit)
        );
        List<MessageDTO> data = new ArrayList<>();
        if (raw != null && raw.isSuccess() && hasData(raw)) {
            MessageDTO[] values = JsonUtils.fromJson(raw.getData(), MessageDTO[].class);
            if (values != null) data.addAll(Arrays.asList(values));
        }
        return copy(raw, data);
    }

    public Response<List<ConversationDTO>> getConversations() throws IOException {
        Response<String> raw = send(ActionType.GET_CONVERSATIONS, null);
        List<ConversationDTO> data = new ArrayList<>();
        if (raw != null && raw.isSuccess() && hasData(raw)) {
            ConversationDTO[] values = JsonUtils.fromJson(
                    raw.getData(), ConversationDTO[].class);
            if (values != null) data.addAll(Arrays.asList(values));
        }
        return copy(raw, data);
    }

    public Response<MessageReadEventDTO> markMessagesRead(long otherUserId)
            throws IOException {
        return convert(
                send(ActionType.MARK_MESSAGE_READ,
                        new MarkMessagesReadDTO(otherUserId)),
                MessageReadEventDTO.class
        );
    }

    public void unregisterPeer() throws IOException {
        send(ActionType.UNREGISTER_PEER, null);
    }

    private Response<String> send(ActionType action, Object payload) throws IOException {
        Request<String> request = new Request<>(
                action,
                authToken,
                payload == null ? "" : JsonUtils.toJson(payload)
        );
        return tcpClient.sendRequest(request);
    }

    private <T> Response<T> convert(Response<String> raw, Class<T> type) {
        T data = null;
        if (raw != null && raw.isSuccess() && hasData(raw)) {
            data = JsonUtils.fromJson(raw.getData(), type);
        }
        return copy(raw, data);
    }

    private <T> Response<T> copy(Response<String> raw, T data) {
        if (raw == null) return new Response<>(null, null, "Server không trả về phản hồi", data);
        Response<T> response = new Response<>(
                raw.getRequestId(), raw.getStatusCode(), raw.getMessage(), data);
        response.setTimestamp(raw.getTimestamp());
        return response;
    }

    private boolean hasData(Response<String> response) {
        return response.getData() != null
                && !response.getData().isBlank()
                && !"null".equals(response.getData());
    }

    private String displayName(UserDTO user) {
        if (user.getFullName() != null && !user.getFullName().isBlank()) {
            return user.getFullName().trim();
        }
        return user.getUsername();
    }

    @Override
    public void close() {
        peerTransport.close();
    }
}
