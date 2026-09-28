package com.studyconnect.client.controller;

import com.studyconnect.client.network.tcp.TCPClient;
import com.studyconnect.client.service.MessageService;
import com.studyconnect.client.view.main.MainFrame;
import com.studyconnect.client.view.message.MessagesPanel;
import com.studyconnect.common.dto.ConversationDTO;
import com.studyconnect.common.dto.MessageDTO;
import com.studyconnect.common.dto.MessageDeliveryMode;
import com.studyconnect.common.dto.MessageReadEventDTO;
import com.studyconnect.common.dto.MessageStatus;
import com.studyconnect.common.dto.OnlineUsersEventDTO;
import com.studyconnect.common.dto.SendMessageDTO;
import com.studyconnect.common.dto.UserDTO;
import com.studyconnect.common.protocol.Response;
import com.studyconnect.common.protocol.ServerEvent;
import com.studyconnect.common.protocol.ServerEventType;
import com.studyconnect.common.util.JsonUtils;

import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;
import java.util.function.Consumer;

public class MessageController implements AutoCloseable {
    private static final int HISTORY_PAGE_SIZE = 50;

    private final MessagesPanel view;
    private final MessageService messageService;
    private final TCPClient tcpClient;
    private final long currentUserId;
    private final Consumer<ServerEvent<String>> receivedListener = this::onMessageReceived;
    private final Consumer<ServerEvent<String>> readListener = this::onMessageRead;
    private final Consumer<ServerEvent<String>> statusListener = this::onMessageStatusUpdated;
    private final Consumer<ServerEvent<String>> onlineListener = this::onOnlineUsersChanged;
    private volatile boolean closed;

    public MessageController(
            MainFrame parent,
            MessagesPanel view,
            MessageService messageService,
            TCPClient tcpClient,
            long currentUserId
    ) {
        if (parent == null || view == null || messageService == null || tcpClient == null) {
            throw new IllegalArgumentException("Thành phần chat không được null");
        }
        if (currentUserId <= 0) throw new IllegalArgumentException("User ID không hợp lệ");
        this.view = view;
        this.messageService = messageService;
        this.tcpClient = tcpClient;
        this.currentUserId = currentUserId;

        view.setConversationSelectionListener(this::openConversation);
        view.setSendListener(this::sendMessage);
        tcpClient.addServerEventListener(ServerEventType.MESSAGE_RECEIVED, receivedListener);
        tcpClient.addServerEventListener(ServerEventType.MESSAGE_READ, readListener);
        tcpClient.addServerEventListener(ServerEventType.MESSAGE_STATUS_UPDATED, statusListener);
        tcpClient.addServerEventListener(ServerEventType.ONLINE_USERS_UPDATED, onlineListener);
        parent.addWindowListener(new WindowAdapter() {
            @Override public void windowClosed(WindowEvent event) { close(); }
            @Override public void windowClosing(WindowEvent event) { close(); }
        });
    }

    public void initializePeer() {
        if (closed) return;
        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() throws Exception {
                messageService.initializePeer(MessageController.this::onPeerMessage);
                return null;
            }

            @Override protected void done() {
                try {
                    get();
                    System.out.println("[P2P] Peer nhắn tin đã sẵn sàng");
                } catch (Exception exception) {
                    System.err.println(
                            "[P2P] Không thể đăng ký peer, sẽ dùng server relay: "
                                    + causeMessage(exception));
                }
            }
        }.execute();
    }

    public void loadConversations() {
        if (closed) return;
        view.setConversationLoading(true);
        new SwingWorker<Response<List<ConversationDTO>>, Void>() {
            @Override protected Response<List<ConversationDTO>> doInBackground() throws Exception {
                return messageService.getConversations();
            }
            @Override protected void done() {
                if (closed) return;
                try {
                    Response<List<ConversationDTO>> response = get();
                    if (response == null || !response.isSuccess()) {
                        view.showError(response == null ? "Server không trả về phản hồi" : response.getMessage());
                        return;
                    }
                    view.setConversations(response.getData());
                } catch (Exception exception) {
                    view.showError("Không thể tải hội thoại: " + causeMessage(exception));
                }
            }
        }.execute();
    }

    public void openConversation(ConversationDTO conversation) {
        if (closed || conversation == null || conversation.getOtherUserId() <= 0) return;
        long peerId = conversation.getOtherUserId();
        view.openConversation(conversation);
        new SwingWorker<Response<List<MessageDTO>>, Void>() {
            @Override protected Response<List<MessageDTO>> doInBackground() throws Exception {
                return messageService.getMessages(peerId, null, HISTORY_PAGE_SIZE);
            }
            @Override protected void done() {
                if (closed || view.getSelectedUserId() != peerId) return;
                try {
                    Response<List<MessageDTO>> response = get();
                    if (response == null || !response.isSuccess()) {
                        view.showError(response == null ? "Server không trả về phản hồi" : response.getMessage());
                        return;
                    }
                    view.setMessages(response.getData());
                    markRead(peerId);
                } catch (Exception exception) {
                    view.showError("Không thể tải tin nhắn: " + causeMessage(exception));
                }
            }
        }.execute();
    }

    public void sendMessage(String content) {
        if (closed) return;
        long receiverId = view.getSelectedUserId();
        String normalized = content == null ? "" : content.trim();
        if (receiverId <= 0 || normalized.isEmpty()) return;

        SendMessageDTO command = messageService.createOutgoingMessage(receiverId, normalized);
        MessageDTO pending = fromPeerPayload(command, MessageStatus.SENDING);
        view.addMessage(pending, true);
        view.updateConversationForMessage(pending, false);
        view.setSending(true);

        new SwingWorker<Response<MessageDTO>, Void>() {
            @Override protected Response<MessageDTO> doInBackground() throws Exception {
                return messageService.sendMessage(command);
            }
            @Override protected void done() {
                if (closed) return;
                view.setSending(false);
                try {
                    Response<MessageDTO> response = get();
                    if (response == null || !response.isSuccess() || response.getData() == null) {
                        pending.setStatus(MessageStatus.FAILED);
                        view.updateMessageStatus(pending);
                        view.showError(response == null ? "Server không trả về phản hồi" : response.getMessage());
                        return;
                    }
                    view.updateMessageStatus(response.getData());
                    view.clearComposerIfUnchanged(normalized);
                } catch (Exception exception) {
                    pending.setStatus(MessageStatus.FAILED);
                    view.updateMessageStatus(pending);
                    view.showError("Không thể gửi tin nhắn: " + causeMessage(exception));
                }
            }
        }.execute();
    }

    private void markRead(long peerId) {
        if (closed || peerId <= 0) return;
        new SwingWorker<Response<MessageReadEventDTO>, Void>() {
            @Override protected Response<MessageReadEventDTO> doInBackground() throws Exception {
                return messageService.markMessagesRead(peerId);
            }
            @Override protected void done() {
                if (closed) return;
                try {
                    Response<MessageReadEventDTO> response = get();
                    if (response != null && response.isSuccess()) loadConversationsSilently();
                } catch (Exception exception) {
                    System.err.println("Không thể đánh dấu đã đọc: " + causeMessage(exception));
                }
            }
        }.execute();
    }

    private void loadConversationsSilently() {
        if (closed) return;
        new SwingWorker<Response<List<ConversationDTO>>, Void>() {
            @Override protected Response<List<ConversationDTO>> doInBackground() throws Exception {
                return messageService.getConversations();
            }
            @Override protected void done() {
                if (closed) return;
                try {
                    Response<List<ConversationDTO>> response = get();
                    if (response != null && response.isSuccess()) view.setConversations(response.getData());
                } catch (Exception ignored) {
                }
            }
        }.execute();
    }

    private void onPeerMessage(SendMessageDTO payload) {
        if (closed || payload == null || payload.getReceiverId() != currentUserId) return;
        MessageDTO message = fromPeerPayload(payload, MessageStatus.DELIVERED);
        SwingUtilities.invokeLater(() -> displayIncoming(message, false));
    }

    private void onMessageReceived(ServerEvent<String> event) {
        if (closed || event == null || event.getData() == null) return;
        try {
            MessageDTO message = JsonUtils.fromJson(event.getData(), MessageDTO.class);
            if (message == null || message.getReceiverId() != currentUserId) return;
            SwingUtilities.invokeLater(() -> displayIncoming(message, true));
        } catch (RuntimeException exception) {
            System.err.println("Event MESSAGE_RECEIVED không hợp lệ: " + exception.getMessage());
        }
    }

    private void displayIncoming(MessageDTO message, boolean canonical) {
        if (closed) return;
        boolean active = view.getSelectedUserId() == message.getSenderId();
        view.updateConversationForMessage(message, !active && canonical);
        if (active) {
            view.addMessage(message, false);
            if (canonical) markRead(message.getSenderId());
        }
    }

    private void onMessageStatusUpdated(ServerEvent<String> event) {
        if (closed || event == null || event.getData() == null) return;
        try {
            MessageDTO message = JsonUtils.fromJson(event.getData(), MessageDTO.class);
            if (message == null || message.getSenderId() != currentUserId) return;
            SwingUtilities.invokeLater(() -> view.updateMessageStatus(message));
        } catch (RuntimeException exception) {
            System.err.println("Event MESSAGE_STATUS_UPDATED không hợp lệ: " + exception.getMessage());
        }
    }

    private void onMessageRead(ServerEvent<String> event) {
        if (closed || event == null || event.getData() == null) return;
        try {
            MessageReadEventDTO receipt = JsonUtils.fromJson(
                    event.getData(), MessageReadEventDTO.class);
            if (receipt == null) return;
            SwingUtilities.invokeLater(() -> view.markMessagesRead(
                    receipt.getMessageIds(), receipt.getClientMessageIds(), receipt.getReadAt()));
        } catch (RuntimeException exception) {
            System.err.println("Event MESSAGE_READ không hợp lệ: " + exception.getMessage());
        }
    }

    private void onOnlineUsersChanged(ServerEvent<String> event) {
        if (closed || event == null || event.getData() == null) return;
        try {
            OnlineUsersEventDTO online = JsonUtils.fromJson(
                    event.getData(), OnlineUsersEventDTO.class);
            List<UserDTO> users = online == null || online.getUsers() == null
                    ? List.of() : online.getUsers();
            SwingUtilities.invokeLater(() -> view.mergeOnlineUsers(users));
        } catch (RuntimeException exception) {
            System.err.println("Event ONLINE_USERS_UPDATED không hợp lệ: " + exception.getMessage());
        }
    }

    private MessageDTO fromPeerPayload(SendMessageDTO payload, MessageStatus status) {
        return new MessageDTO(
                0L,
                payload.getClientMessageId(),
                payload.getSenderId(),
                payload.getSenderName(),
                payload.getSenderAvatarUrl(),
                payload.getReceiverId(),
                payload.getContent(),
                payload.getCreatedAt(),
                status,
                payload.getDeliveryMode() == null
                        ? MessageDeliveryMode.P2P : payload.getDeliveryMode(),
                status == MessageStatus.DELIVERED ? System.currentTimeMillis() : 0L,
                0L
        );
    }

    @Override
    public void close() {
        if (closed) return;
        closed = true;
        tcpClient.removeServerEventListener(ServerEventType.MESSAGE_RECEIVED, receivedListener);
        tcpClient.removeServerEventListener(ServerEventType.MESSAGE_READ, readListener);
        tcpClient.removeServerEventListener(ServerEventType.MESSAGE_STATUS_UPDATED, statusListener);
        tcpClient.removeServerEventListener(ServerEventType.ONLINE_USERS_UPDATED, onlineListener);
        Thread cleanup = new Thread(() -> {
            try {
                messageService.unregisterPeer();
            } catch (Exception ignored) {
            } finally {
                messageService.close();
            }
        }, "studyconnect-peer-cleanup");
        cleanup.setDaemon(true);
        cleanup.start();
    }

    private String causeMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) current = current.getCause();
        String message = current.getMessage();
        return message == null || message.isBlank() ? current.getClass().getSimpleName() : message;
    }
}
