package com.studyconnect.server.network.session;

import java.util.UUID;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentHashMap;

public final class SessionManager {
    private static final SessionManager INSTANCE = new SessionManager();

    private final ConcurrentMap<String, Long> sessions = new ConcurrentHashMap<>();

    private SessionManager() {
    }

    public static SessionManager getInstance() {
        return INSTANCE;
    }

    public String createSession(long userId) {
        String token = UUID.randomUUID().toString();
        sessions.put(token, userId);

        return token;
    }

    public Long getUserId(String token) {
        if (token == null || token.isBlank()) return null;

        return sessions.get(token);
    }

    public boolean isValid(String token) {
        return getUserId(token) != null;
    }

    public void removeSession(String token) {
        if (token != null) {
            sessions.remove(token);
        }
    }

}
