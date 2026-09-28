package com.studyconnect.common.dto;

import java.io.Serializable;

public class RegisterPeerDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private int port;

    public RegisterPeerDTO() {
    }

    public RegisterPeerDTO(int port) {
        this.port = port;
    }

    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }
}
