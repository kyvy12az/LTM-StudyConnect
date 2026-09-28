package com.studyconnect.common.protocol;

public enum ActionType {
    PING,

    REGISTER,
    LOGIN,
    LOGOUT,

    CREATE_POST,
    GET_POSTS,
    GET_POST_DETAIL,
    LIKE_POST,
    UNLIKE_POST,

    CREATE_COMMENT,
    GET_COMMENTS,

    GET_ONLINE_USERS,

    REGISTER_PEER,
    UNREGISTER_PEER,
    REQUEST_PEER_INFO,

    SYNC_MESSAGE,
    SEND_MESSAGE,
    GET_CONVERSATIONS,
    GET_MESSAGE_HISTORY,
    MARK_MESSAGE_READ,

    // Backward-compatible request names used by older clients.
    GET_MESSAGES,
    MARK_MESSAGES_READ,

    REQUEST_CALL,
    ACCEPT_CALL,
    REJECT_CALL,
    END_CALL
}
