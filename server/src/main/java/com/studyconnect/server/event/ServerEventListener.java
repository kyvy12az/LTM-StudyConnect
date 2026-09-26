package com.studyconnect.server.event;

public interface ServerEventListener {
    void onLog(String message);
    void onStatusChanged(boolean running);
    void onClientCountChanged(int clientCount);
}
