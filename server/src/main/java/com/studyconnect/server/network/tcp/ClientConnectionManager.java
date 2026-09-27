package com.studyconnect.server.network.tcp;

import com.studyconnect.common.protocol.ServerEvent;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ClientConnectionManager {
    private final Set<ClientHandler> connections =
            ConcurrentHashMap.newKeySet();
    private final ConcurrentMap<Long, Set<ClientHandler>>
            connectionsByUser = new ConcurrentHashMap<>();
    private final ConcurrentMap<ClientHandler, Long>
            usersByConnection = new ConcurrentHashMap<>();

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
        if (previousUser != null
                && previousUser.longValue() != userId) {
            removeFromUser(previousUser, handler);
        }

        connectionsByUser.computeIfAbsent(
                userId,
                ignored -> ConcurrentHashMap.newKeySet()
        ).add(handler);
    }

    public void unauthenticate(ClientHandler handler) {
        if (handler == null) {
            return;
        }
        Long userId = usersByConnection.remove(handler);
        if (userId != null) {
            removeFromUser(userId, handler);
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

    private void removeFromUser(
            long userId,
            ClientHandler handler
    ) {
        Set<ClientHandler> userConnections =
                connectionsByUser.get(userId);
        if (userConnections == null) {
            return;
        }

        userConnections.remove(handler);
        if (userConnections.isEmpty()) {
            connectionsByUser.remove(
                    userId,
                    userConnections
            );
        }
    }
}
