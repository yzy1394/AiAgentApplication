package com.yzy.aiagent.auth.service;

import com.yzy.aiagent.auth.dto.LoginRequest;
import com.yzy.aiagent.auth.dto.LoginResponse;
import com.yzy.aiagent.auth.dto.RegisterRequest;
import com.yzy.aiagent.auth.dto.UserProfileResponse;
import com.yzy.aiagent.auth.mapper.UserMapper;
import com.yzy.aiagent.auth.model.UserDO;
import com.yzy.aiagent.exception.BusinessException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.regex.Pattern;

@Service
public class AuthService {

    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{4,32}$");

    private final UserMapper userMapper;

    private final AuthSessionService authSessionService;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AuthService(UserMapper userMapper, AuthSessionService authSessionService) {
        this.userMapper = userMapper;
        this.authSessionService = authSessionService;
    }

    public UserProfileResponse register(RegisterRequest request) {
        if (request == null) {
            throw new BusinessException(400, "请求参数不能为空");
        }
        String username = normalize(request.getUsername());
        String password = request.getPassword();
        String nickname = normalize(request.getNickname());

        validateRegister(username, password);
        UserDO existed = userMapper.selectByUsername(username);
        if (existed != null) {
            throw new BusinessException(400, "用户名已存在");
        }

        UserDO userDO = new UserDO();
        userDO.setUsername(username);
        userDO.setPasswordHash(passwordEncoder.encode(password));
        userDO.setNickname(StringUtils.hasText(nickname) ? nickname : username);
        LocalDateTime now = LocalDateTime.now();
        userDO.setCreatedAt(now);
        userDO.setUpdatedAt(now);
        int affected = userMapper.insert(userDO);
        if (affected != 1) {
            throw new BusinessException(500, "注册失败，请稍后重试");
        }
        return toUserProfile(userDO);
    }

    public LoginResponse login(LoginRequest request) {
        if (request == null) {
            throw new BusinessException(400, "请求参数不能为空");
        }
        String username = normalize(request.getUsername());
        String password = request.getPassword();

        if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
            throw new BusinessException(400, "用户名和密码不能为空");
        }

        UserDO userDO = userMapper.selectByUsername(username);
        if (userDO == null || !passwordEncoder.matches(password, userDO.getPasswordHash())) {
            throw new BusinessException(400, "用户名或密码错误");
        }

        String token = authSessionService.createSession(userDO);
        LoginResponse loginResponse = new LoginResponse();
        loginResponse.setToken(token);
        loginResponse.setUser(toUserProfile(userDO));
        return loginResponse;
    }

    public UserProfileResponse getCurrentUserByToken(String token) {
        UserProfileResponse user = authSessionService.getUserByToken(token);
        if (user == null) {
            throw new BusinessException(401, "登录已过期，请重新登录");
        }
        return user;
    }

    public void logout(String token) {
        authSessionService.removeSession(token);
    }

    private void validateRegister(String username, String password) {
        if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
            throw new BusinessException(400, "用户名和密码不能为空");
        }
        if (!USERNAME_PATTERN.matcher(username).matches()) {
            throw new BusinessException(400, "用户名必须为4-32位字母、数字或下划线");
        }
        if (password.length() < 6 || password.length() > 64) {
            throw new BusinessException(400, "密码长度必须在6-64位之间");
        }
    }

    private UserProfileResponse toUserProfile(UserDO userDO) {
        UserProfileResponse response = new UserProfileResponse();
        response.setId(userDO.getId());
        response.setUsername(userDO.getUsername());
        response.setNickname(userDO.getNickname());
        return response;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
