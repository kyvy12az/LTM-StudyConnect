package com.studyconnect.server.event;

import com.studyconnect.common.dto.PostDTO;

public interface ServerEventListener {
    void onLog(String message);
    void onStatusChanged(boolean running);
    void onClientCountChanged(int clientCount);
    void onPostCreated(PostDTO post);
}
