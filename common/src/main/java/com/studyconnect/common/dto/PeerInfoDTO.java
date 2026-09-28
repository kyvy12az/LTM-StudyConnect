package com.studyconnect.common.dto;

import java.io.Serializable;

public class PeerInfoDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private long userId;
    private String host;
    private int port;
    private boolean available;
    private long registeredAt;
    private String peerToken;

    public PeerInfoDTO() {
    }

    public PeerInfoDTO(long userId, String host, int port, boolean available,
                       long registeredAt, String peerToken) {
        this.userId = userId;
        this.host = host;
        this.port = port;
        this.available = available;
        this.registeredAt = registeredAt;
        this.peerToken = peerToken;
    }

    public long getUserId() { return userId; }
    public void setUserId(long userId) { this.userId = userId; }
    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }
    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }
    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) { this.available = available; }
    public long getRegisteredAt() { return registeredAt; }
    public void setRegisteredAt(long registeredAt) { this.registeredAt = registeredAt; }
    public String getPeerToken() { return peerToken; }
    public void setPeerToken(String peerToken) { this.peerToken = peerToken; }
}
