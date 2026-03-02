package com.yzy.aiagent.app;

import com.yzy.aiagent.advisor.MyLoggerAdvisor;
import com.yzy.aiagent.rag.QueryRewriter;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
// import org.springframework.ai.vectorstore.VectorStore;
// 说明：当前项目仅使用云端 RAG Advisor，暂不启用本地/pgvector 向量库。
// 开启场景：后续需要离线检索或私有知识库时再恢复相关 import 与字段。
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.List;

@Component
@Slf4j
public class ProgrammingApp {

    private final ChatClient chatClient;

    private static final String SYSTEM_PROMPT = "You are a programming master proficient in Java, C++, Python, and other mainstream languages. " +
            "You are especially skilled at frontend development that pairs with Java backend systems, including Vue 2, Vue 3, and other common frontend frameworks. " +
            "Provide practical, production-ready solutions with clear technical reasoning and maintainable code.";

    public ProgrammingApp(ChatModel dashscopeChatModel, ChatMemoryRepository chatMemoryRepository) {
        MessageWindowChatMemory chatMemory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(chatMemoryRepository)
                .maxMessages(20)
                .build();
        chatClient = ChatClient.builder(dashscopeChatModel)
                .defaultSystem(SYSTEM_PROMPT)
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory).build(),
                        new MyLoggerAdvisor()
                )
                .build();
    }

    public String doChat(String message, String chatId) {
        ChatResponse chatResponse = chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                .call()
                .chatResponse();
        String content = chatResponse.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }

    public Flux<String> doChatByStream(String message, String chatId) {
        return chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                .stream()
                .content();
    }

    public Flux<ChatResponse> doChatResponseByStream(String message, String chatId) {
        return chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                .stream()
                .chatResponse();
    }

    record ProgrammingReport(String title, List<String> suggestions) {
    }

    public ProgrammingReport doChatWithProgrammingReport(String message, String chatId) {
        ProgrammingReport programmingReport = chatClient
                .prompt()
                .system(SYSTEM_PROMPT + "After each conversation, generate a programming result report. The title should be \"{User's Programming Report}\", and the content should be a list of actionable engineering suggestions.")
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                .call()
                .entity(ProgrammingReport.class);
        log.info("programmingReport: {}", programmingReport);
        return programmingReport;
    }

    // 当前只使用云端 RAG Advisor。
    @Resource
    private Advisor programmingAppRagCloudAdvisor;

    @Resource
    private QueryRewriter queryRewriter;

    // 说明：以下字段是本地向量库/pgvector预留，当前练手项目不启用，先注释保留。
    // 开启场景：需要本地文档检索、私有化部署、或调研不同向量存储方案时再恢复。
    // @Resource
    // private VectorStore programmingAppVectorStore;

    // @Autowired(required = false)
    // @Qualifier("pgVectorVectorStore")
    // @Nullable
    // private VectorStore pgVectorVectorStore;

    /**
     * 单次请求降级：每次请求都先尝试 RAG，失败后仅当前请求回退到普通对话。
     */
    public String doChatWithRag(String message, String chatId) {
        try {
            String rewrittenMessage = queryRewriter.doQueryRewrite(message);
            ChatResponse chatResponse = chatClient
                    .prompt()
                    .user(rewrittenMessage)
                    .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                    .advisors(new MyLoggerAdvisor())
                    .advisors(programmingAppRagCloudAdvisor)
                    .call()
                    .chatResponse();
            String content = chatResponse.getResult().getOutput().getText();
            log.info("content: {}", content);
            return content;
        } catch (Exception e) {
            log.warn("RAG call failed for current request, fallback to normal chat. cause={}", e.getMessage());
            return doChat(message, chatId);
        }
    }

    public Flux<String> doChatWithRagByStream(String message, String chatId) {
        try {
            String rewrittenMessage = queryRewriter.doQueryRewrite(message);
            return chatClient
                    .prompt()
                    .user(rewrittenMessage)
                    .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                    .advisors(new MyLoggerAdvisor())
                    .advisors(programmingAppRagCloudAdvisor)
                    .stream()
                    .content()
                    .onErrorResume(e -> {
                        log.warn("RAG stream failed for current request, fallback to normal stream. cause={}", e.getMessage());
                        return doChatByStream(message, chatId);
                    });
        } catch (Exception e) {
            log.warn("RAG stream prepare failed for current request, fallback to normal stream. cause={}", e.getMessage());
            return doChatByStream(message, chatId);
        }
    }

    public Flux<ChatResponse> doChatWithRagResponseByStream(String message, String chatId) {
        try {
            String rewrittenMessage = queryRewriter.doQueryRewrite(message);
            return chatClient
                    .prompt()
                    .user(rewrittenMessage)
                    .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                    .advisors(new MyLoggerAdvisor())
                    .advisors(programmingAppRagCloudAdvisor)
                    .stream()
                    .chatResponse()
                    .onErrorResume(e -> {
                        log.warn("RAG response stream failed for current request, fallback to normal stream. cause={}", e.getMessage());
                        return doChatResponseByStream(message, chatId);
                    });
        } catch (Exception e) {
            log.warn("RAG response stream prepare failed for current request, fallback to normal stream. cause={}", e.getMessage());
            return doChatResponseByStream(message, chatId);
        }
    }

    @Resource
    private ToolCallback[] allTools;

    public String doChatWithTools(String message, String chatId) {
        ChatResponse chatResponse = chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                .advisors(new MyLoggerAdvisor())
                .toolCallbacks(allTools)
                .call()
                .chatResponse();
        String content = chatResponse.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }

    @Resource
    private ToolCallbackProvider toolCallbackProvider;

    public String doChatWithMcp(String message, String chatId) {
        ChatResponse chatResponse = chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                .advisors(new MyLoggerAdvisor())
                .toolCallbacks(toolCallbackProvider)
                .call()
                .chatResponse();
        String content = chatResponse.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }
}
