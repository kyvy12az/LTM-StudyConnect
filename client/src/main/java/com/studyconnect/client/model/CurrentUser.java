package com.studyconnect.client.model;

import com.studyconnect.common.dto.AuthResponseDTO;
import com.studyconnect.common.dto.UserDTO;

public final class CurrentUser {
    private static String token;
    private static UserDTO user;

    private CurrentUser() {
    }

    public static void setSession(AuthResponseDTO authResponse) {
        if (authResponse == null) {
            clear();
            return;
        }

        token = authResponse.getToken();
        user = authResponse.getUser();
    }

    public static String getToken() {
        return token;
    }

    public static UserDTO getUser() {
        return user;
    }

    public static boolean isLoggedIn() {
        return token != null && !token.isBlank() && user != null;
    }

    private static void clear() {
        token = null;
        user = null;
    }
}
