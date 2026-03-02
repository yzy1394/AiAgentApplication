package com.yzy.aiagent.auth.context;

import com.yzy.aiagent.auth.dto.UserProfileResponse;

public final class AuthContext {

    private static final ThreadLocal<UserProfileResponse> CURRENT_USER = new ThreadLocal<>();

    private AuthContext() {
    }

    public static void setCurrentUser(UserProfileResponse user) {
        CURRENT_USER.set(user);
    }

    public static UserProfileResponse getCurrentUser() {
        return CURRENT_USER.get();
    }

    public static void clear() {
        CURRENT_USER.remove();
    }
}
