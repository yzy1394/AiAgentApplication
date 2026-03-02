package com.yzy.aiagent.agent;

import cn.hutool.core.util.StrUtil;
import com.yzy.aiagent.agent.model.AgentState;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 代理抽象基类，封装通用状态管理与执行循环。
 */
@Data
@Slf4j
public abstract class BaseAgent {

    private String name;

    private String systemPrompt;
    private String nextStepPrompt;

    private AgentState state = AgentState.IDLE;

    private int currentStep = 0;
    private int maxSteps = 10;

    private ChatClient chatClient;

    private List<Message> messageList = new ArrayList<>();

    /**
     * 同步运行代理。
     */
    public String run(String userPrompt) {
        if (this.state != AgentState.IDLE) {
            throw new RuntimeException("Cannot run agent from state: " + this.state);
        }
        if (StrUtil.isBlank(userPrompt)) {
            throw new RuntimeException("Cannot run agent with empty user prompt");
        }
        this.state = AgentState.RUNNING;
        messageList.add(new UserMessage(userPrompt));
        List<String> results = new ArrayList<>();
        try {
            for (int i = 0; i < maxSteps && state != AgentState.FINISHED; i++) {
                int stepNumber = i + 1;
                currentStep = stepNumber;
                log.info("Executing step {}/{}", stepNumber, maxSteps);
                String stepResult = step();
                results.add("Step " + stepNumber + ": " + stepResult);
            }
            if (currentStep >= maxSteps) {
                state = AgentState.FINISHED;
                results.add("Terminated: Reached max steps (" + maxSteps + ")");
            }
            return String.join("\n", results);
        } catch (Exception e) {
            state = AgentState.ERROR;
            log.error("error executing agent", e);
            return "执行错误：" + e.getMessage();
        } finally {
            this.cleanup();
        }
    }

    /**
     * 流式运行代理，输出每一步结果。
     */
    public Flux<String> runStream(String userPrompt) {
        return Flux.create(sink -> {
            CompletableFuture.runAsync(() -> {
                if (this.state != AgentState.IDLE) {
                    sink.next("错误：无法从状态运行代理：" + this.state);
                    sink.complete();
                    return;
                }
                if (StrUtil.isBlank(userPrompt)) {
                    sink.next("错误：提示词不能为空");
                    sink.complete();
                    return;
                }

                this.state = AgentState.RUNNING;
                messageList.add(new UserMessage(userPrompt));
                try {
                    for (int i = 0; i < maxSteps && state != AgentState.FINISHED; i++) {
                        int stepNumber = i + 1;
                        currentStep = stepNumber;
                        log.info("Executing step {}/{}", stepNumber, maxSteps);
                        String stepResult = step();
                        sink.next("Step " + stepNumber + ": " + stepResult);
                    }
                    if (currentStep >= maxSteps) {
                        state = AgentState.FINISHED;
                        sink.next("Terminated: Reached max steps (" + maxSteps + ")");
                    }
                    sink.complete();
                } catch (Exception e) {
                    state = AgentState.ERROR;
                    log.error("error executing agent", e);
                    sink.next("执行错误：" + e.getMessage());
                    sink.complete();
                } finally {
                    this.cleanup();
                }
            });

            sink.onCancel(() -> {
                if (this.state == AgentState.RUNNING) {
                    this.state = AgentState.ERROR;
                }
                log.info("Agent stream canceled");
            });
        });
    }

    public abstract String step();

    protected void cleanup() {
        // 子类可按需覆盖资源清理逻辑
    }
}
