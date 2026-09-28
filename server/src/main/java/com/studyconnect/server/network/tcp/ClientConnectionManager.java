package com.studyconnect.server.network.tcp;

import com.studyconnect.common.protocol.ServerEvent;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.LongConsumer;

public class ClientConnectionManager {
    private final Set<ClientHandler> connections =
            ConcurrentHashMap.newKeySet();
    private final ConcurrentMap<Long, Set<ClientHandler>>
            connectionsByUser = new ConcurrentHashMap<>();
    private final ConcurrentMap<ClientHandler, Long>
            usersByConnection = new ConcurrentHashMap<>();

    private volatile Runnable presenceChangedListener;
    private volatile LongConsumer userOfflineListener;

    public void register(ClientHandler handler) {
        if (handler != null) {
            connections.add(handler);
        }
    }

    public void authenticate(
            ClientHandler handler,
            long userId
    ) {
        if (handler == null || userId <= 0) {
            return;
        }

        Long previousUser = usersByConnection.put(
                handler,
                userId
        );
        if (previousUser != null && previousUser.longValue() == userId) {
            return;
        }
        if (previousUser != null) {
            removeFromUser(previousUser, handler);
        }

        connectionsByUser.computeIfAbsent(
                userId,
                ignored -> ConcurrentHashMap.newKeySet()
        ).add(handler);

        notifyPresenceChanged();
    }

    public void unauthenticate(ClientHandler handler) {
        if (handler == null) {
            return;
        }
        Long userId = usersByConnection.remove(handler);
        if (userId == null) return;

        boolean userBecameOffline = removeFromUser(userId, handler);

        if (userBecameOffline) {
            LongConsumer offlineListener = userOfflineListener;
            if (offlineListener != null) {
                try {
                    offlineListener.accept(userId);
                } catch (RuntimeException exception) {
                    System.err.println("Không thể thông báo peer offline: " + exception.getMessage());
                }
            }
            notifyPresenceChanged();
        }
    }

    public void unregister(ClientHandler handler) {
        if (handler == null) {
            return;
        }
        connections.remove(handler);
        unauthenticate(handler);
    }

    public void broadcastAuthenticated(
            ServerEvent<String> event,
            ClientHandler excludedConnection
    ) {
        if (event == null) {
            return;
        }

        for (ClientHandler handler : usersByConnection.keySet()) {
            if (handler == excludedConnection
                    || !handler.isOpen()) {
                continue;
            }

            try {
                handler.sendEvent(event);
            } catch (IOException exception) {
                System.err.println(
                        "Không thể gửi event đến "
                                + handler.getRemoteAddress()
                                + ": "
                                + exception.getMessage()
                );
                handler.close();
                unregister(handler);
            }
        }
    }

    public boolean sendToUser(long userId, ServerEvent<String> event) {
        if (userId <= 0 || event == null) {
            return false;
        }
        Set<ClientHandler> userConnections = connectionsByUser.get(userId);
        if (userConnections == null || userConnections.isEmpty()) {
            return false;
        }
        boolean delivered = false;
        for (ClientHandler handler : new ArrayList<>(userConnections)) {
            if (!handler.isOpen()) {
                unregister(handler);
                continue;
            }
            try {
                handler.sendEvent(event);
                delivered = true;
            } catch (IOException exception) {
                System.err.println(
                        "Không thể gửi event riêng tới user " + userId
                                + ": " + exception.getMessage()
                );
                handler.close();
                unregister(handler);
            }
        }
        return delivered;
    }

    public int getConnectionCount() {
        return connections.size();
    }

    public void closeAll() {
        for (ClientHandler handler : connections) {
            handler.close();
        }
        connections.clear();
        connectionsByUser.clear();
        usersByConnection.clear();
    }

    private boolean removeFromUser(
            long userId,
            ClientHandler handler
    ) {
        Set<ClientHandler> userConnections =
                connectionsByUser.get(userId);
        if (userConnections == null) {
            return false;
        }

        userConnections.remove(handler);

        if (!userConnections.isEmpty()) return false;

        boolean removed = connectionsByUser.remove(userId, userConnections);

        return removed;
    }

    private void notifyPresenceChanged() {
        Runnable listener = presenceChangedListener;

        if (listener == null) return;

        try {
            listener.run();
        } catch (RuntimeException exception) {
            System.err.println("Không thể thông báo thay đổi online: " + exception.getMessage());
        }
    }

    public Set<Long> getOnlineUserIds() {
        return Set.copyOf(connectionsByUser.keySet());
    }

    public List<ConnectionSnapshot> getConnectionSnapshots() {
        List<ConnectionSnapshot> snapshots = new ArrayList<>();
        for (ClientHandler handler : connections) {
            if (!handler.isOpen()) continue;
            Long userId = usersByConnection.get(handler);
            snapshots.add(new ConnectionSnapshot(
                    userId,
                    handler.getRemoteHost(),
                    handler.getRemotePort(),
                    handler.getConnectedAt(),
                    userId != null
            ));
        }
        snapshots.sort(Comparator.comparingLong(ConnectionSnapshot::connectedAt));
        return snapshots;
    }

    public void setPresenceChangedListener(Runnable presenceChangedListener) {
        this.presenceChangedListener = presenceChangedListener;
    }

    public void setUserOfflineListener(LongConsumer userOfflineListener) {
        this.userOfflineListener = userOfflineListener;
    }

    public record ConnectionSnapshot(
            Long userId,
            String ipAddress,
            int port,
            long connectedAt,
            boolean authenticated
    ) {
    }
}
