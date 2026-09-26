package com.studyconnect.common.protocol;

public enum ActionType {
    PING,

    REGISTER,
    LOGIN,
    LOGOUT,

    CREATE_POST,
    GET_POSTS,
    GET_POST_DETAIL,

    CREATE_COMMENT,
    GET_COMMENTS,

    GET_ONLINE_USERS,
    SEND_MESSAGE,
    GET_MESSAGES,

    REQUEST_PEER_INFO,
    REQUEST_CALL,
    ACCEPT_CALL,
    REJECT_CALL,
    END_CALL
}
