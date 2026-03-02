package com.yzy.aiagent.demo.invoke;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Spring Ai框架调用Ai大模型
 */
@Component
@Slf4j
@ConditionalOnProperty(prefix = "app.demo", name = "spring-ai-invoke-enabled", havingValue = "true")
public class SpringAiAiInvoke implements CommandLineRunner {
    @Resource
    private ChatModel dashscopeChatModel;


    @Override
    public void run(String... args) {
        try {
            AssistantMessage assistantMessage = dashscopeChatModel.call(new Prompt("你好，我是巴巴博一"))
                    .getResult()
                    .getOutput();
            log.info("SpringAiAiInvoke output: {}", assistantMessage.getText());
        } catch (Exception e) {
            log.warn("Skip SpringAiAiInvoke startup demo call: {}", e.getMessage());
        }
    }
}
