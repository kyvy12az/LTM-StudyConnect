package com.studyconnect.server.model.dto;

public record ConnectedClientDTO(
        Long userId,
        String username,
        String ipAddress,
        int port,
        long connectedAt,
        boolean authenticated
) {
}
