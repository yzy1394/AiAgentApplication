package com.yzy.aiagent.chatmemory.mapper;

import com.yzy.aiagent.chatmemory.model.ChatMessageDO;
import com.yzy.aiagent.chatmemory.model.ChatSessionSummaryDO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ChatMessageMapper {

    @Select({
            "SELECT DISTINCT conversation_id",
            "FROM ai_chat_message",
            "ORDER BY conversation_id"
    })
    List<String> selectConversationIds();

    @Select({
            "SELECT id, conversation_id, message_type, message_text, created_at",
            "FROM ai_chat_message",
            "WHERE conversation_id = #{conversationId}",
            "  AND (message_type IS NULL OR UPPER(message_type) <> 'TOOL')",
            "ORDER BY id ASC"
    })
    List<ChatMessageDO> selectByConversationId(String conversationId);

    @Select({
            "SELECT t.conversation_id, t.last_message_at, t.message_count, m.message_text AS last_message_text",
            "FROM (",
            "    SELECT conversation_id, MAX(created_at) AS last_message_at, COUNT(*) AS message_count, MAX(id) AS last_id",
            "    FROM ai_chat_message",
            "    WHERE conversation_id LIKE CONCAT(#{conversationPrefix}, '%')",
            "      AND (message_type IS NULL OR UPPER(message_type) <> 'TOOL')",
            "    GROUP BY conversation_id",
            ") t",
            "LEFT JOIN ai_chat_message m ON m.id = t.last_id",
            "ORDER BY t.last_message_at DESC"
    })
    List<ChatSessionSummaryDO> selectSessionSummariesByConversationPrefix(String conversationPrefix);

    @Insert({
            "INSERT INTO ai_chat_message(conversation_id, message_type, message_text, created_at)",
            "VALUES(#{conversationId}, #{messageType}, #{messageText}, #{createdAt})"
    })
    int insert(ChatMessageDO chatMessage);

    @Delete({
            "DELETE FROM ai_chat_message",
            "WHERE conversation_id = #{conversationId}"
    })
    int deleteByConversationId(String conversationId);
}
