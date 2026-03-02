package com.yzy.aiagent.auth.service;

import com.yzy.aiagent.auth.dto.UserProfileResponse;
import com.yzy.aiagent.auth.model.UserDO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AuthSessionService {

    private final Map<String, SessionValue> tokenStore = new ConcurrentHashMap<>();

    @Value("${auth.session.timeout-hours:24}")
    private long timeoutHours;

    public String createSession(UserDO userDO) {
        String token = UUID.randomUUID().toString().replace("-", "");
        LocalDateTime expireAt = LocalDateTime.now().plusHours(timeoutHours);
        tokenStore.put(token, new SessionValue(toProfile(userDO), expireAt));
        return token;
    }

    public UserProfileResponse getUserByToken(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        SessionValue sessionValue = tokenStore.get(token);
        if (sessionValue == null) {
            return null;
        }
        if (sessionValue.expireAt().isBefore(LocalDateTime.now())) {
            tokenStore.remove(token);
            return null;
        }
        return sessionValue.user();
    }

    public void removeSession(String token) {
        if (token == null || token.isBlank()) {
            return;
        }
        tokenStore.remove(token);
    }

    private UserProfileResponse toProfile(UserDO userDO) {
        UserProfileResponse response = new UserProfileResponse();
        response.setId(userDO.getId());
        response.setUsername(userDO.getUsername());
        response.setNickname(userDO.getNickname());
        return response;
    }

    private record SessionValue(UserProfileResponse user, LocalDateTime expireAt) {
    }
}
