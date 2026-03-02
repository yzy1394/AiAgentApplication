package com.yzy.aiagent.controller;

import com.yzy.aiagent.auth.dto.LoginRequest;
import com.yzy.aiagent.auth.dto.LoginResponse;
import com.yzy.aiagent.auth.dto.RegisterRequest;
import com.yzy.aiagent.auth.dto.UserProfileResponse;
import com.yzy.aiagent.auth.service.AuthService;
import com.yzy.aiagent.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private static final String AUTHORIZATION = "Authorization";

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ApiResponse<UserProfileResponse> register(@RequestBody RegisterRequest request) {
        return ApiResponse.success(authService.register(request));
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@RequestBody LoginRequest request) {
        return ApiResponse.success(authService.login(request));
    }

    @GetMapping("/me")
    public ApiResponse<UserProfileResponse> currentUser(
            @RequestHeader(value = AUTHORIZATION, required = false) String authorization) {
        return ApiResponse.success(authService.getCurrentUserByToken(extractToken(authorization)));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(
            @RequestHeader(value = AUTHORIZATION, required = false) String authorization) {
        authService.logout(extractToken(authorization));
        return ApiResponse.success();
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
