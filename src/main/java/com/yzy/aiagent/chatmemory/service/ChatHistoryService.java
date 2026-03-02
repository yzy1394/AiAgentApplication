package com.yzy.aiagent.chatmemory.service;

import com.yzy.aiagent.chatmemory.dto.ChatHistoryMessageResponse;
import com.yzy.aiagent.chatmemory.dto.ChatSessionSummaryResponse;
import com.yzy.aiagent.chatmemory.mapper.ChatMessageMapper;
import com.yzy.aiagent.chatmemory.model.ChatMessageDO;
import com.yzy.aiagent.chatmemory.model.ChatSessionSummaryDO;
import com.yzy.aiagent.exception.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Service
public class ChatHistoryService {

    private static final String MANUS_PREFIX = "manus:";

    private final ChatMessageMapper chatMessageMapper;

    public ChatHistoryService(ChatMessageMapper chatMessageMapper) {
        this.chatMessageMapper = chatMessageMapper;
    }

    public List<ChatSessionSummaryResponse> listSessionsByUser(Long userId) {
        String conversationPrefix = userId + ":";
        List<ChatSessionSummaryDO> records = chatMessageMapper.selectSessionSummariesByConversationPrefix(conversationPrefix);
        List<ChatSessionSummaryResponse> result = new ArrayList<>(records.size());
        for (ChatSessionSummaryDO record : records) {
            ChatSessionSummaryResponse item = new ChatSessionSummaryResponse();
            item.setChatId(extractChatIdByPrefix(record.getConversationId(), conversationPrefix));
            item.setLastMessageAt(record.getLastMessageAt());
            item.setLastMessageText(record.getLastMessageText());
            item.setMessageCount(record.getMessageCount());
            result.add(item);
        }
        return result;
    }

    public List<ChatHistoryMessageResponse> listMessagesByUserAndChatId(Long userId, String chatId) {
        if (!StringUtils.hasText(chatId)) {
            throw new BusinessException(400, "chatId is blank");
        }
        String scopedConversationId = buildScopedConversationId(userId, chatId);
        return queryMessagesByConversationId(scopedConversationId);
    }

    public void deleteSessionByUserAndChatId(Long userId, String chatId) {
        if (!StringUtils.hasText(chatId)) {
            throw new BusinessException(400, "chatId is blank");
        }
        String scopedConversationId = buildScopedConversationId(userId, chatId);
        chatMessageMapper.deleteByConversationId(scopedConversationId);
    }

    public List<ChatSessionSummaryResponse> listManusSessionsByUser(Long userId) {
        String conversationPrefix = buildManusConversationPrefix(userId);
        List<ChatSessionSummaryDO> records = chatMessageMapper.selectSessionSummariesByConversationPrefix(conversationPrefix);
        List<ChatSessionSummaryResponse> result = new ArrayList<>(records.size());
        for (ChatSessionSummaryDO record : records) {
            ChatSessionSummaryResponse item = new ChatSessionSummaryResponse();
            item.setChatId(extractChatIdByPrefix(record.getConversationId(), conversationPrefix));
            item.setLastMessageAt(record.getLastMessageAt());
            item.setLastMessageText(record.getLastMessageText());
            item.setMessageCount(record.getMessageCount());
            result.add(item);
        }
        return result;
    }

    public List<ChatHistoryMessageResponse> listManusMessagesByUserAndChatId(Long userId, String chatId) {
        if (!StringUtils.hasText(chatId)) {
            throw new BusinessException(400, "chatId is blank");
        }
        String scopedConversationId = buildScopedManusConversationId(userId, chatId);
        return queryMessagesByConversationId(scopedConversationId);
    }

    public void deleteManusSessionByUserAndChatId(Long userId, String chatId) {
        if (!StringUtils.hasText(chatId)) {
            throw new BusinessException(400, "chatId is blank");
        }
        String scopedConversationId = buildScopedManusConversationId(userId, chatId);
        chatMessageMapper.deleteByConversationId(scopedConversationId);
    }

    private String buildScopedConversationId(Long userId, String chatId) {
        return userId + ":" + chatId.trim();
    }

    private String buildManusConversationPrefix(Long userId) {
        return MANUS_PREFIX + userId + ":";
    }

    private String buildScopedManusConversationId(Long userId, String chatId) {
        return buildManusConversationPrefix(userId) + chatId.trim();
    }

    private List<ChatHistoryMessageResponse> queryMessagesByConversationId(String conversationId) {
        List<ChatMessageDO> records = chatMessageMapper.selectByConversationId(conversationId);
        List<ChatHistoryMessageResponse> result = new ArrayList<>(records.size());
        for (ChatMessageDO record : records) {
            ChatHistoryMessageResponse item = new ChatHistoryMessageResponse();
            item.setMessageType(record.getMessageType());
            item.setMessageText(record.getMessageText());
            item.setCreatedAt(record.getCreatedAt());
            result.add(item);
        }
        return result;
    }

    private String extractChatIdByPrefix(String conversationId, String prefix) {
        if (!StringUtils.hasText(conversationId)) {
            return "";
        }
        if (StringUtils.hasText(prefix) && conversationId.startsWith(prefix)) {
            return conversationId.substring(prefix.length());
        }
        return conversationId;
    }
}