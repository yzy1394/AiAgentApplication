package com.yzy.aiagent.auth.interceptor;

import com.yzy.aiagent.auth.context.AuthContext;
import com.yzy.aiagent.auth.dto.UserProfileResponse;
import com.yzy.aiagent.auth.service.AuthSessionService;
import com.yzy.aiagent.exception.UnauthorizedException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    private static final String AUTHORIZATION = "Authorization";

    private final AuthSessionService authSessionService;

    public AuthInterceptor(AuthSessionService authSessionService) {
        this.authSessionService = authSessionService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String token = extractToken(request.getHeader(AUTHORIZATION));
        UserProfileResponse currentUser = authSessionService.getUserByToken(token);
        if (currentUser == null) {
            throw new UnauthorizedException("未登录或登录已过期");
        }
        AuthContext.setCurrentUser(currentUser);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        AuthContext.clear();
    }

    private String extractToken(String authorization) {
        if (authorization == null || authorization.isBlank()) {
            return null;
        }
        if (authorization.startsWith("Bearer ")) {
            return authorization.substring(7).trim();
        }
        return authorization.trim();
    }
}
