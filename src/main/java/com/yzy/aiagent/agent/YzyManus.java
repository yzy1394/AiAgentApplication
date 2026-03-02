package com.yzy.aiagent.agent;

import com.yzy.aiagent.advisor.MyLoggerAdvisor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Super agent with autonomous planning capability.
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class YzyManus extends ToolCallAgent {

    private final ChatMemoryRepository chatMemoryRepository;

    private String conversationId;

    private final AtomicBoolean persisted = new AtomicBoolean(false);

    private List<Message> loadedHistory = new ArrayList<>();

    public YzyManus(ToolCallback[] allTools, ChatModel dashscopeChatModel, ChatMemoryRepository chatMemoryRepository) {
        super(allTools);
        this.chatMemoryRepository = chatMemoryRepository;
        this.setName("yzyManus");

        String systemPrompt = String.join("\n",
                "You are YzyManus, an all-capable AI assistant, aimed at solving any task presented by the user.",
                "You have various tools at your disposal that you can call upon to efficiently complete complex requests.");
        this.setSystemPrompt(systemPrompt);

        String nextStepPrompt = String.join("\n",
                "Based on user needs, proactively select the most appropriate tool or combination of tools.",
                "For complex tasks, you can break down the problem and use different tools step by step to solve it.",
                "After using each tool, clearly explain the execution results and suggest the next steps.",
                "If you want to stop the interaction at any point, use the 'terminate' tool/function call.");
        this.setNextStepPrompt(nextStepPrompt);

        this.setMaxSteps(20);
        ChatClient chatClient = ChatClient.builder(dashscopeChatModel)
                .defaultAdvisors(new MyLoggerAdvisor())
                .build();
        this.setChatClient(chatClient);
    }

    public void prepareConversation(String conversationId) {
        this.conversationId = conversationId;
        this.persisted.set(false);
        resetUsageSnapshot();
        List<Message> history = chatMemoryRepository.findByConversationId(conversationId);
        this.loadedHistory = new ArrayList<>(history);
        this.setMessageList(new ArrayList<>(loadedHistory));
    }

    @Override
    protected void cleanup() {
        if (conversationId == null || conversationId.isBlank()) {
            return;
        }
        if (!persisted.compareAndSet(false, true)) {
            return;
        }
        chatMemoryRepository.saveAll(conversationId, buildPersistableMessages());
    }

    private List<Message> buildPersistableMessages() {
        List<Message> current = getMessageList();
        List<Message> persistable = new ArrayList<>(loadedHistory);
        int start = Math.min(loadedHistory.size(), current.size());
        String normalizedNextStepPrompt = normalize(getNextStepPrompt());

        for (int i = start; i < current.size(); i++) {
            Message message = current.get(i);
            if (isNextStepPromptMessage(message, normalizedNextStepPrompt)) {
                continue;
            }
            persistable.add(message);
        }
        return persistable;
    }

    private boolean isNextStepPromptMessage(Message message, String normalizedNextStepPrompt) {
        if (message == null || message.getMessageType() != MessageType.USER) {
            return false;
        }
        if (normalizedNextStepPrompt.isEmpty()) {
            return false;
        }
        return normalize(message.getText()).equals(normalizedNextStepPrompt);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().replace("\r\n", "\n");
    }
}
