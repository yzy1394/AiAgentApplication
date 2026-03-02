package com.yzy.aiagent.chatmemory;

import com.yzy.aiagent.chatmemory.mapper.ChatMessageMapper;
import com.yzy.aiagent.chatmemory.model.ChatMessageDO;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
public class MybatisChatMemoryRepository implements ChatMemoryRepository {

    private final ChatMessageMapper chatMessageMapper;

    public MybatisChatMemoryRepository(ChatMessageMapper chatMessageMapper) {
        this.chatMessageMapper = chatMessageMapper;
    }

    @Override
    public List<String> findConversationIds() {
        return chatMessageMapper.selectConversationIds();
    }

    @Override
    public List<Message> findByConversationId(String conversationId) {
        List<ChatMessageDO> records = chatMessageMapper.selectByConversationId(conversationId);
        if (records == null || records.isEmpty()) {
            return Collections.emptyList();
        }
        List<Message> messages = new ArrayList<>(records.size());
        for (ChatMessageDO record : records) {
            messages.add(toMessage(record.getMessageType(), record.getMessageText()));
        }
        return messages;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveAll(String conversationId, List<Message> messages) {
        if (conversationId == null || conversationId.isBlank() || messages == null || messages.isEmpty()) {
            return;
        }
        List<Message> persistableMessages = filterPersistableMessages(messages);
        if (persistableMessages.isEmpty()) {
            return;
        }
        List<ChatMessageDO> existingRecords = chatMessageMapper.selectByConversationId(conversationId);
        int overlap = calculateOverlap(existingRecords, persistableMessages);

        for (int i = overlap; i < persistableMessages.size(); i++) {
            Message message = persistableMessages.get(i);
            ChatMessageDO record = new ChatMessageDO();
            record.setConversationId(conversationId);
            record.setMessageType(message.getMessageType() == null ? MessageType.ASSISTANT.name() : message.getMessageType().name());
            record.setMessageText(message.getText() == null ? "" : message.getText());
            record.setCreatedAt(LocalDateTime.now());
            chatMessageMapper.insert(record);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteByConversationId(String conversationId) {
        if (conversationId == null || conversationId.isBlank()) {
            return;
        }
        chatMessageMapper.deleteByConversationId(conversationId);
    }

    private int calculateOverlap(List<ChatMessageDO> existingRecords, List<Message> incoming) {
        if (existingRecords == null || existingRecords.isEmpty()) {
            return 0;
        }
        int existingSize = existingRecords.size();
        int incomingSize = incoming.size();
        int max = Math.min(existingSize, incomingSize);

        for (int overlap = max; overlap > 0; overlap--) {
            if (isOverlap(existingRecords, incoming, overlap, existingSize)) {
                return overlap;
            }
        }
        return 0;
    }

    private boolean isOverlap(List<ChatMessageDO> existingRecords, List<Message> incoming, int overlap, int existingSize) {
        int existingStart = existingSize - overlap;
        for (int i = 0; i < overlap; i++) {
            ChatMessageDO existing = existingRecords.get(existingStart + i);
            Message message = incoming.get(i);
            String leftType = normalize(existing.getMessageType());
            String rightType = message.getMessageType() == null ? "" : normalize(message.getMessageType().name());
            String leftText = normalize(existing.getMessageText());
            String rightText = normalize(message.getText());
            if (!leftType.equals(rightType) || !leftText.equals(rightText)) {
                return false;
            }
        }
        return true;
    }

    private Message toMessage(String messageType, String messageText) {
        String type = normalize(messageType);
        String text = messageText == null ? "" : messageText;
        switch (type) {
            case "USER":
                return new UserMessage(text);
            case "SYSTEM":
                return new SystemMessage(text);
            case "ASSISTANT":
                return new AssistantMessage(text);
            default:
                return new AssistantMessage(text);
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private List<Message> filterPersistableMessages(List<Message> messages) {
        List<Message> result = new ArrayList<>(messages.size());
        for (Message message : messages) {
            if (message == null || isToolMessage(message)) {
                continue;
            }
            result.add(message);
        }
        return result;
    }

    private boolean isToolMessage(Message message) {
        if (message instanceof ToolResponseMessage) {
            return true;
        }
        MessageType type = message.getMessageType();
        return type != null && "TOOL".equalsIgnoreCase(type.name());
    }
}
