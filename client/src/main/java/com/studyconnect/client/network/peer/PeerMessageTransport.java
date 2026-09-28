package com.studyconnect.client.network.peer;

import com.studyconnect.common.dto.MessageAckDTO;
import com.studyconnect.common.dto.MessageStatus;
import com.studyconnect.common.dto.PeerInfoDTO;
import com.studyconnect.common.dto.SendMessageDTO;
import com.studyconnect.common.util.JsonUtils;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class PeerMessageTransport implements AutoCloseable {
    private static final int CONNECT_TIMEOUT_MILLIS = 2500;
    private static final int ACK_TIMEOUT_MILLIS = 4000;
    private static final int MAX_CONTENT_LENGTH = 5000;

    private final long currentUserId;
    private final AtomicBoolean running = new AtomicBoolean();
    private final ExecutorService handlers = Executors.newCachedThreadPool(runnable -> {
        Thread thread = new Thread(runnable, "studyconnect-peer-handler");
        thread.setDaemon(true);
        return thread;
    });
    private final Set<String> receivedMessageIds = ConcurrentHashMap.newKeySet();

    private volatile ServerSocket serverSocket;
    private volatile Thread acceptThread;
    private volatile Consumer<SendMessageDTO> messageListener;
    private volatile String peerToken;

    public PeerMessageTransport(long currentUserId) {
        if (currentUserId <= 0) throw new IllegalArgumentException("User ID không hợp lệ");
        this.currentUserId = currentUserId;
    }

    public int start(Consumer<SendMessageDTO> listener) throws IOException {
        if (listener == null) throw new IllegalArgumentException("Message listener không được null");
        if (running.get()) return serverSocket.getLocalPort();
        ServerSocket socket = new ServerSocket(0);
        serverSocket = socket;
        messageListener = listener;
        running.set(true);
        acceptThread = new Thread(this::acceptLoop, "studyconnect-peer-accept");
        acceptThread.setDaemon(true);
        acceptThread.start();
        return socket.getLocalPort();
    }

    public void setPeerToken(String peerToken) {
        this.peerToken = peerToken;
    }

    public MessageAckDTO send(PeerInfoDTO peer, SendMessageDTO message)
            throws IOException {
        if (peer == null || !peer.isAvailable()
                || peer.getHost() == null || peer.getHost().isBlank()
                || peer.getPort() <= 0) {
            throw new IOException("Peer không khả dụng");
        }
        if (peer.getPeerToken() == null || peer.getPeerToken().isBlank()) {
            throw new IOException("Peer token không hợp lệ");
        }
        message.setPeerToken(peer.getPeerToken());
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(peer.getHost(), peer.getPort()),
                    CONNECT_TIMEOUT_MILLIS);
            socket.setSoTimeout(ACK_TIMEOUT_MILLIS);
            DataOutputStream output = new DataOutputStream(socket.getOutputStream());
            DataInputStream input = new DataInputStream(socket.getInputStream());
            output.writeUTF(JsonUtils.toJson(message));
            output.flush();
            MessageAckDTO ack = JsonUtils.fromJson(input.readUTF(), MessageAckDTO.class);
            if (ack == null || ack.getStatus() == MessageStatus.FAILED) {
                throw new IOException(ack == null ? "Peer không trả ACK"
                        : ack.getErrorMessage());
            }
            return ack;
        }
    }

    private void acceptLoop() {
        while (running.get()) {
            try {
                Socket socket = serverSocket.accept();
                handlers.execute(() -> handle(socket));
            } catch (SocketException exception) {
                if (running.get()) System.err.println("Peer socket bị đóng: " + exception.getMessage());
                break;
            } catch (IOException exception) {
                if (running.get()) System.err.println("Không thể nhận kết nối P2P: " + exception.getMessage());
            }
        }
    }

    private void handle(Socket socket) {
        try (socket;
             DataInputStream input = new DataInputStream(socket.getInputStream());
             DataOutputStream output = new DataOutputStream(socket.getOutputStream())) {
            socket.setSoTimeout(ACK_TIMEOUT_MILLIS);
            SendMessageDTO message = JsonUtils.fromJson(input.readUTF(), SendMessageDTO.class);
            MessageAckDTO ack;
            try {
                validateIncoming(message);
                if (receivedMessageIds.add(message.getClientMessageId())) {
                    messageListener.accept(message);
                }
                ack = new MessageAckDTO(
                        message.getClientMessageId(), 0L,
                        MessageStatus.DELIVERED, System.currentTimeMillis(), null);
            } catch (RuntimeException exception) {
                ack = new MessageAckDTO(
                        message == null ? null : message.getClientMessageId(), 0L,
                        MessageStatus.FAILED, System.currentTimeMillis(), exception.getMessage());
            }
            output.writeUTF(JsonUtils.toJson(ack));
            output.flush();
        } catch (IOException | RuntimeException exception) {
            System.err.println("Lỗi xử lý tin nhắn P2P: " + exception.getMessage());
        }
    }

    private void validateIncoming(SendMessageDTO message) {
        if (message == null) throw new IllegalArgumentException("Payload P2P không hợp lệ");
        if (message.getReceiverId() != currentUserId) {
            throw new IllegalArgumentException("Tin nhắn không dành cho peer này");
        }
        if (message.getSenderId() <= 0 || message.getSenderId() == currentUserId) {
            throw new IllegalArgumentException("Người gửi P2P không hợp lệ");
        }
        String expectedToken = peerToken;
        if (expectedToken == null || !expectedToken.equals(message.getPeerToken())) {
            throw new IllegalArgumentException("Peer token không hợp lệ");
        }
        if (message.getClientMessageId() == null || message.getClientMessageId().isBlank()) {
            throw new IllegalArgumentException("clientMessageId không hợp lệ");
        }
        String content = message.getContent() == null ? "" : message.getContent().trim();
        if (content.isEmpty() || content.length() > MAX_CONTENT_LENGTH) {
            throw new IllegalArgumentException("Nội dung P2P không hợp lệ");
        }
        message.setContent(content);
    }

    @Override
    public void close() {
        running.set(false);
        ServerSocket socket = serverSocket;
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException ignored) {
            }
        }
        handlers.shutdownNow();
        Thread thread = acceptThread;
        if (thread != null) thread.interrupt();
    }
}
