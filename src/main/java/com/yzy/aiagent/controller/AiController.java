package com.yzy.aiagent.controller;

import com.yzy.aiagent.agent.ToolCallAgent;
import com.yzy.aiagent.agent.YzyManus;
import com.yzy.aiagent.app.ProgrammingApp;
import com.yzy.aiagent.auth.context.AuthContext;
import com.yzy.aiagent.auth.dto.UserProfileResponse;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicReference;

@RestController
@RequestMapping("/ai")
public class AiController {

    @Resource
    private ProgrammingApp programmingApp;

    @Resource
    private ObjectProvider<YzyManus> yzyManusProvider;

    @GetMapping("/pgapp/chat/sync")
    public String doChatWithProgrammingAppSync(
            @RequestParam("message") String message,
            @RequestParam(value = "chatId", defaultValue = "default") String chatId) {
        return programmingApp.doChat(message, buildScopedChatId(chatId));
    }

    @GetMapping("/pgapp/chat/rag/sync")
    public String doChatWithProgrammingAppRagSync(
            @RequestParam("message") String message,
            @RequestParam(value = "chatId", defaultValue = "default") String chatId) {
        return programmingApp.doChatWithRag(message, buildScopedChatId(chatId));
    }

    @GetMapping(value = "/pgapp/chat/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> doChatWithProgrammingAppSSE(
            @RequestParam("message") String message,
            @RequestParam(value = "chatId", defaultValue = "default") String chatId) {
        return buildSseStreamWithUsage(programmingApp.doChatResponseByStream(message, buildScopedChatId(chatId)));
    }

    @GetMapping(value = "/pgapp/chat/rag/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> doChatWithProgrammingAppRagSSE(
            @RequestParam("message") String message,
            @RequestParam(value = "chatId", defaultValue = "default") String chatId) {
        return buildSseStreamWithUsage(programmingApp.doChatWithRagResponseByStream(message, buildScopedChatId(chatId)));
    }

    private Flux<ServerSentEvent<String>> buildSseStreamWithUsage(Flux<ChatResponse> responseFlux) {
        AtomicReference<TokenUsageSnapshot> usageRef = new AtomicReference<>(null);
        return responseFlux
                .flatMap(chatResponse -> {
                    updateUsage(usageRef, chatResponse);
                    String chunk = extractChunk(chatResponse);
                    if (chunk == null || chunk.isEmpty()) {
                        return Mono.empty();
                    }
                    return Mono.just(ServerSentEvent.<String>builder()
                            .event("message")
                            .data(chunk)
                            .build());
                })
                .concatWith(Mono.fromSupplier(() -> buildUsageEvent(usageRef.get())))
                .onErrorResume(ex -> Flux.just(ServerSentEvent.<String>builder()
                        .data("[ERROR] " + ex.getMessage())
                        .build()));
    }

    private void updateUsage(AtomicReference<TokenUsageSnapshot> usageRef, ChatResponse chatResponse) {
        if (chatResponse == null || chatResponse.getMetadata() == null) {
            return;
        }
        Usage usage = chatResponse.getMetadata().getUsage();
        if (usage == null) {
            return;
        }
        Integer promptTokens = usage.getPromptTokens();
        Integer completionTokens = usage.getCompletionTokens();
        Integer totalTokens = usage.getTotalTokens();
        if (promptTokens == null && completionTokens == null && totalTokens == null) {
            return;
        }
        usageRef.set(new TokenUsageSnapshot(promptTokens, completionTokens, totalTokens));
    }

    private String extractChunk(ChatResponse chatResponse) {
        if (chatResponse == null || chatResponse.getResult() == null || chatResponse.getResult().getOutput() == null) {
            return "";
        }
        String text = chatResponse.getResult().getOutput().getText();
        return text == null ? "" : text;
    }

    private ServerSentEvent<String> buildUsageEvent(TokenUsageSnapshot snapshot) {
        if (snapshot == null) {
            return ServerSentEvent.<String>builder()
                    .event("usage")
                    .data("{\"available\":false}")
                    .build();
        }
        String json = String.format(
                "{\"available\":true,\"promptTokens\":%s,\"completionTokens\":%s,\"totalTokens\":%s}",
                snapshot.promptTokens() == null ? "null" : snapshot.promptTokens(),
                snapshot.completionTokens() == null ? "null" : snapshot.completionTokens(),
                snapshot.totalTokens() == null ? "null" : snapshot.totalTokens()
        );
        return ServerSentEvent.<String>builder()
                .event("usage")
                .data(json)
                .build();
    }

    private ServerSentEvent<String> buildUsageEvent(ToolCallAgent.UsageSnapshot snapshot) {
        if (snapshot == null) {
            return ServerSentEvent.<String>builder()
                    .event("usage")
                    .data("{\"available\":false}")
                    .build();
        }
        String json = String.format(
                "{\"available\":true,\"promptTokens\":%s,\"completionTokens\":%s,\"totalTokens\":%s}",
                snapshot.promptTokens() == null ? "null" : snapshot.promptTokens(),
                snapshot.completionTokens() == null ? "null" : snapshot.completionTokens(),
                snapshot.totalTokens() == null ? "null" : snapshot.totalTokens()
        );
        return ServerSentEvent.<String>builder()
                .event("usage")
                .data(json)
                .build();
    }

    private record TokenUsageSnapshot(Integer promptTokens, Integer completionTokens, Integer totalTokens) {
    }

    private String buildScopedChatId(String chatId) {
        UserProfileResponse currentUser = AuthContext.getCurrentUser();
        if (currentUser == null || currentUser.getId() == null) {
            return chatId;
        }
        return currentUser.getId() + ":" + chatId;
    }

    @GetMapping(value = "/manus/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> doChatWithManus(
            @RequestParam("message") String message,
            @RequestParam(value = "chatId", defaultValue = "default") String chatId) {
        YzyManus yzyManus = yzyManusProvider.getObject();
        yzyManus.prepareConversation(buildScopedManusChatId(chatId));
        return yzyManus.runStream(message)
                .map(chunk -> ServerSentEvent.<String>builder().data(chunk).build())
                .concatWith(Mono.fromSupplier(() -> buildUsageEvent(yzyManus.getUsageSnapshot())))
                .onErrorResume(ex -> Flux.just(ServerSentEvent.<String>builder()
                        .data("[ERROR] " + ex.getMessage())
                        .build()));
    }

    private String buildScopedManusChatId(String chatId) {
        UserProfileResponse currentUser = AuthContext.getCurrentUser();
        if (currentUser == null || currentUser.getId() == null) {
            return "manus:" + chatId;
        }
        return "manus:" + currentUser.getId() + ":" + chatId;
    }
}
