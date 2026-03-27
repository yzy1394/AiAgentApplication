package com.yzy.aiagent.agent;

import com.yzy.aiagent.advisor.MyLoggerAdvisor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import reactor.core.publisher.FluxSink;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Super agent with autonomous planning capability.
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Slf4j
public class YzyManus extends ToolCallAgent {

    private static final String SKILL_RESOURCE_PATH = "agent/yzy-manus-skill.md";

    private static final String DEFAULT_SKILL_PROMPT = String.join("\n",
            "You are YzyManus, an all-capable AI assistant aimed at solving user tasks efficiently.",
            "Use tools only when they are genuinely needed.",
            "Unless the user explicitly requests a file, download, command execution, or exported artifact such as a PDF, respond with a text-only summary.",
            "Before terminating the task, ensure the user receives a final answer in Chinese.",
            "Do not expose internal tool names, function names, control prompts, or termination messages to the user.");

    private final ChatMemoryRepository chatMemoryRepository;

    private String conversationId;

    private final AtomicBoolean persisted = new AtomicBoolean(false);

    private List<Message> loadedHistory = new ArrayList<>();

    public YzyManus(ToolCallback[] allTools, ChatModel dashscopeChatModel, ChatMemoryRepository chatMemoryRepository) {
        super(allTools);
        this.chatMemoryRepository = chatMemoryRepository;
        this.setName("yzyManus");
        this.setSystemPrompt(loadSkillPrompt());

        String nextStepPrompt = String.join("\n",
                "Based on user needs, proactively select the most appropriate tool or combination of tools.",
                "For complex tasks, you can break down the problem and use different tools step by step to solve it.",
                "After using each tool, clearly explain the execution results and suggest the next steps.",
                "Unless the user explicitly asks for a file or executable artifact, prefer a text-only summary.",
                "Use the 'terminate' tool/function call only after the final user-facing answer is ready.");
        this.setNextStepPrompt(nextStepPrompt);

        this.setMaxSteps(6);
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

        for (int i = start; i < current.size(); i++) {
            Message message = current.get(i);
            if (isNextStepPromptMessage(message)) {
                continue;
            }
            persistable.add(message);
        }
        return persistable;
    }

    @Override
    protected String buildFinalResult(List<String> results, boolean reachedMaxSteps) {
        if (!reachedMaxSteps || results.isEmpty()) {
            return super.buildFinalResult(results, reachedMaxSteps);
        }
        return results.get(results.size() - 1);
    }

    @Override
    protected void onMaxStepsReached(List<String> results) {
    }

    @Override
    protected void onMaxStepsReached(FluxSink<String> sink) {
    }

    private String loadSkillPrompt() {
        try {
            ClassPathResource resource = new ClassPathResource(SKILL_RESOURCE_PATH);
            byte[] bytes = resource.getInputStream().readAllBytes();
            String content = new String(bytes, StandardCharsets.UTF_8).trim();
            if (!content.isEmpty()) {
                return content;
            }
        } catch (IOException e) {
            log.warn("Failed to load YzyManus skill file: {}", SKILL_RESOURCE_PATH, e);
        }
        return DEFAULT_SKILL_PROMPT;
    }
}
