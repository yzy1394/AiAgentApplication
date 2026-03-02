package com.yzy.aiagent.agent;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.yzy.aiagent.agent.model.AgentState;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.tool.ToolCallback;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Base ReAct agent with explicit tool-calling control.
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Slf4j
public class ToolCallAgent extends ReActAgent {

    private final ToolCallback[] availableTools;
    private ChatResponse toolCallChatResponse;
    private final ToolCallingManager toolCallingManager;
    private final ChatOptions chatOptions;
    private Integer promptTokens = 0;
    private Integer completionTokens = 0;
    private Integer totalTokens = 0;
    private boolean usageAvailable = false;

    public ToolCallAgent(ToolCallback[] availableTools) {
        this.availableTools = availableTools;
        this.toolCallingManager = ToolCallingManager.builder().build();
        this.chatOptions = DashScopeChatOptions.builder()
                .withInternalToolExecutionEnabled(false)
                .build();
    }

    protected void resetUsageSnapshot() {
        promptTokens = 0;
        completionTokens = 0;
        totalTokens = 0;
        usageAvailable = false;
    }

    public UsageSnapshot getUsageSnapshot() {
        if (!usageAvailable) {
            return null;
        }
        return new UsageSnapshot(promptTokens, completionTokens, totalTokens);
    }

    private void accumulateUsage(ChatResponse chatResponse) {
        if (chatResponse == null || chatResponse.getMetadata() == null) {
            return;
        }
        Usage usage = chatResponse.getMetadata().getUsage();
        if (usage == null) {
            return;
        }
        Integer prompt = usage.getPromptTokens();
        Integer completion = usage.getCompletionTokens();
        Integer total = usage.getTotalTokens();
        if (prompt == null && completion == null && total == null) {
            return;
        }
        usageAvailable = true;
        if (prompt != null) {
            promptTokens += prompt;
        }
        if (completion != null) {
            completionTokens += completion;
        }
        if (total != null) {
            totalTokens += total;
        }
    }

    @Override
    public boolean think() {
        if (StrUtil.isNotBlank(getNextStepPrompt())) {
            getMessageList().add(new UserMessage(getNextStepPrompt()));
        }

        Prompt prompt = new Prompt(getMessageList(), this.chatOptions);
        try {
            ChatResponse chatResponse = getChatClient().prompt(prompt)
                    .system(getSystemPrompt())
                    .toolCallbacks(availableTools)
                    .call()
                    .chatResponse();

            this.toolCallChatResponse = chatResponse;
            accumulateUsage(chatResponse);
            AssistantMessage assistantMessage = chatResponse.getResult().getOutput();
            List<AssistantMessage.ToolCall> toolCallList = assistantMessage.getToolCalls();

            log.info("{} think: {}", getName(), assistantMessage.getText());
            log.info("{} selected {} tools", getName(), toolCallList.size());
            if (!toolCallList.isEmpty()) {
                String toolCallInfo = toolCallList.stream()
                        .map(toolCall -> String.format("tool=%s, args=%s", toolCall.name(), toolCall.arguments()))
                        .collect(Collectors.joining("\n"));
                log.info(toolCallInfo);
                return true;
            }

            // No tool call: keep model answer in memory and let step() finish this run.
            getMessageList().add(assistantMessage);
            return false;
        } catch (Exception e) {
            log.error(getName() + " think failed", e);
            getMessageList().add(new AssistantMessage("Processing failed: " + e.getMessage()));
            return false;
        }
    }

    @Override
    public String act() {
        if (toolCallChatResponse == null || !toolCallChatResponse.hasToolCalls()) {
            return "No tools need to be called.";
        }

        Prompt prompt = new Prompt(getMessageList(), this.chatOptions);
        ToolExecutionResult toolExecutionResult = toolCallingManager.executeToolCalls(prompt, toolCallChatResponse);

        setMessageList(toolExecutionResult.conversationHistory());
        ToolResponseMessage toolResponseMessage =
                (ToolResponseMessage) CollUtil.getLast(toolExecutionResult.conversationHistory());

        boolean terminateToolCalled = toolResponseMessage.getResponses().stream()
                .anyMatch(response -> "doTerminate".equals(response.name()));
        if (terminateToolCalled) {
            setState(AgentState.FINISHED);
        }

        String results = toolResponseMessage.getResponses().stream()
                .map(response -> "Tool " + response.name() + " result: " + response.responseData())
                .collect(Collectors.joining("\n"));
        log.info(results);
        return results;
    }

    @Override
    public String step() {
        try {
            boolean shouldAct = think();
            if (!shouldAct) {
                if (getState() != AgentState.FINISHED) {
                    setState(AgentState.FINISHED);
                }
                String finalAnswer = getLastAssistantText();
                return StrUtil.isNotBlank(finalAnswer) ? finalAnswer : "Reasoning completed: no further action required.";
            }
            return act();
        } catch (Exception e) {
            log.error(getName() + " step failed", e);
            setState(AgentState.ERROR);
            return "Step execution failed: " + e.getMessage();
        }
    }

    private String getLastAssistantText() {
        List<Message> messageList = getMessageList();
        for (int i = messageList.size() - 1; i >= 0; i--) {
            Message message = messageList.get(i);
            if (message instanceof AssistantMessage) {
                AssistantMessage assistantMessage = (AssistantMessage) message;
                String text = assistantMessage.getText();
                if (StrUtil.isNotBlank(text)) {
                    return text;
                }
            }
        }
        return "";
    }

    public record UsageSnapshot(Integer promptTokens, Integer completionTokens, Integer totalTokens) {
    }
}
