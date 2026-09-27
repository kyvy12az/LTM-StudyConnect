package com.studyconnect.common.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class OnlineUsersEventDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private List<UserDTO> users;
    private long timestamp;

    public OnlineUsersEventDTO() {
        this.users = new ArrayList<>();
    }

    public OnlineUsersEventDTO(List<UserDTO> users, long timestamp) {
        this.users = users == null ? new ArrayList<>() : new ArrayList<>(users);
    }

    public List<UserDTO> getUsers() {
        return users;
    }

    public void setUsers(List<UserDTO> users) {
        this.users = users;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
