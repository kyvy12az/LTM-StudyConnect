package com.studyconnect.server.model.dto;

public record AdminUserDTO(
        long id,
        String username,
        String fullName,
        String email,
        String avatarUrl,
        String status,
        boolean online,
        long lastSeen
) {
    public AdminUserDTO withOnline(boolean currentOnline) {
        return new AdminUserDTO(
                id,
                username,
                fullName,
                email,
                avatarUrl,
                status,
                currentOnline,
                lastSeen
        );
    }
}
