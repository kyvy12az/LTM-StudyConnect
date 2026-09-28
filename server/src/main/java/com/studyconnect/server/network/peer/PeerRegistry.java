package com.studyconnect.server.network.peer;

import com.studyconnect.common.dto.PeerInfoDTO;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PeerRegistry {
    private final ConcurrentHashMap<Long, PeerInfoDTO> peers = new ConcurrentHashMap<>();

    public PeerInfoDTO register(long userId, String host, int port) {
        if (userId <= 0) throw new IllegalArgumentException("User ID không hợp lệ");
        if (host == null || host.isBlank()) throw new IllegalArgumentException("Địa chỉ peer không hợp lệ");
        if (port < 1 || port > 65535) throw new IllegalArgumentException("Cổng peer không hợp lệ");
        PeerInfoDTO peer = new PeerInfoDTO(
                userId,
                normalizeHost(host),
                port,
                true,
                System.currentTimeMillis(),
                UUID.randomUUID().toString()
        );
        peers.put(userId, peer);
        return copy(peer, true);
    }

    public Optional<PeerInfoDTO> find(long userId) {
        PeerInfoDTO peer = peers.get(userId);
        return peer == null ? Optional.empty() : Optional.of(copy(peer, true));
    }

    public PeerInfoDTO unregister(long userId) {
        PeerInfoDTO removed = peers.remove(userId);
        if (removed == null) {
            return new PeerInfoDTO(userId, null, 0, false,
                    System.currentTimeMillis(), null);
        }
        PeerInfoDTO offline = copy(removed, false);
        offline.setAvailable(false);
        offline.setHost(null);
        offline.setPort(0);
        offline.setPeerToken(null);
        return offline;
    }

    public PeerInfoDTO publicStatus(long userId) {
        PeerInfoDTO peer = peers.get(userId);
        if (peer == null) {
            return new PeerInfoDTO(userId, null, 0, false,
                    System.currentTimeMillis(), null);
        }
        PeerInfoDTO status = copy(peer, false);
        status.setPeerToken(null);
        return status;
    }

    private PeerInfoDTO copy(PeerInfoDTO value, boolean includeToken) {
        return new PeerInfoDTO(
                value.getUserId(),
                value.getHost(),
                value.getPort(),
                value.isAvailable(),
                value.getRegisteredAt(),
                includeToken ? value.getPeerToken() : null
        );
    }

    private String normalizeHost(String host) {
        String normalized = host.trim();
        if (normalized.startsWith("/")) normalized = normalized.substring(1);
        int separator = normalized.lastIndexOf(':');
        if (separator > 0 && normalized.indexOf(':') == separator) {
            normalized = normalized.substring(0, separator);
        }
        return normalized;
    }
}
