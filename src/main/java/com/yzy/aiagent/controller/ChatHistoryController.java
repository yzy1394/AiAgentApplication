package com.yzy.aiagent.controller;

import com.yzy.aiagent.auth.context.AuthContext;
import com.yzy.aiagent.auth.dto.UserProfileResponse;
import com.yzy.aiagent.chatmemory.dto.ChatHistoryMessageResponse;
import com.yzy.aiagent.chatmemory.dto.ChatSessionSummaryResponse;
import com.yzy.aiagent.chatmemory.service.ChatHistoryService;
import com.yzy.aiagent.common.ApiResponse;
import com.yzy.aiagent.exception.UnauthorizedException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/ai/pgapp/history")
public class ChatHistoryController {

    private final ChatHistoryService chatHistoryService;

    public ChatHistoryController(ChatHistoryService chatHistoryService) {
        this.chatHistoryService = chatHistoryService;
    }

    @GetMapping("/sessions")
    public ApiResponse<List<ChatSessionSummaryResponse>> listSessions() {
        Long userId = requireCurrentUserId();
        return ApiResponse.success(chatHistoryService.listSessionsByUser(userId));
    }

    @GetMapping("/messages")
    public ApiResponse<List<ChatHistoryMessageResponse>> listMessages(@RequestParam("chatId") String chatId) {
        Long userId = requireCurrentUserId();
        return ApiResponse.success(chatHistoryService.listMessagesByUserAndChatId(userId, chatId));
    }

    @DeleteMapping("/session")
    public ApiResponse<Void> deleteSession(@RequestParam("chatId") String chatId) {
        Long userId = requireCurrentUserId();
        chatHistoryService.deleteSessionByUserAndChatId(userId, chatId);
        return ApiResponse.success();
    }

    private Long requireCurrentUserId() {
        UserProfileResponse currentUser = AuthContext.getCurrentUser();
        if (currentUser == null || currentUser.getId() == null) {
            throw new UnauthorizedException("Not logged in");
        }
        return currentUser.getId();
    }
}