package com.studyconnect.server.model.dto;

import java.util.List;

public record DashboardSnapshot(
        String serverIp,
        int serverPort,
        boolean serverRunning,
        int onlineClients,
        long totalUsers,
        long postsToday,
        int threadCount,
        int cpuPercent,
        long usedMemoryBytes,
        long totalMemoryBytes,
        List<ConnectedClientDTO> clients
) {
}
