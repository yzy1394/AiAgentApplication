package com.yzy.aiagent.rag;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.rag.DashScopeDocumentRetriever;
import com.alibaba.cloud.ai.dashscope.rag.DashScopeDocumentRetrieverOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cloud RAG advisor based on DashScope knowledge base.
 */
@Configuration
@Slf4j
public class ProgrammingAppRagCloudAdvisorConfig {

    @Value("${spring.ai.dashscope.api-key}")
    private String dashScopeApiKey;

    @Value("${app.rag.cloud-index-name:vue3_frontend_knowledge}")
    private String knowledgeIndex;

    @Bean
    public Advisor programmingAppRagCloudAdvisor() {
        DashScopeApi dashScopeApi = DashScopeApi.builder()
                .apiKey(dashScopeApiKey)
                .build();

        DocumentRetriever documentRetriever = new DashScopeDocumentRetriever(
                dashScopeApi,
                DashScopeDocumentRetrieverOptions.builder()
                        .withIndexName(knowledgeIndex)
                        .build());

        PromptTemplate ragPromptTemplate = new PromptTemplate(String.join("\n",
                "You are a senior Spring Boot + Vue3 engineering assistant.",
                "Use the reference context silently as internal hints and answer the user query directly.",
                "",
                "Follow these rules:",
                "1. Never mention or imply retrieval, context, knowledge base, documents, snippets, or provided materials.",
                "2. Do not output raw copied fragments from references; synthesize into a direct, practical answer.",
                "3. If references are missing or insufficient, continue with your own knowledge and still provide the best possible answer.",
                "4. If the user query is ambiguous or incomplete, ask a concise clarification question directly.",
                "5. Do not use phrases like \"Based on the context...\" or \"The provided information...\".",
                "",
                "Reference context (internal only, do not disclose):",
                "{context}",
                "",
                "User query:",
                "{query}",
                "",
                "Answer:"));

        PromptTemplate emptyContextPromptTemplate = new PromptTemplate(String.join("\n",
                "You are a senior Spring Boot + Vue3 engineering assistant.",
                "Answer the user query directly.",
                "",
                "Follow these rules:",
                "1. Never mention retrieval, context, knowledge base, or provided materials.",
                "2. Give a practical and complete answer using your own knowledge.",
                "3. If the query is ambiguous or incomplete, ask one concise clarification question.",
                "",
                "User query:",
                "{query}",
                "",
                "Answer:"));

        return RetrievalAugmentationAdvisor.builder()
                .documentRetriever(documentRetriever)
                .queryAugmenter(ContextualQueryAugmenter.builder()
                        .allowEmptyContext(true)
                        .promptTemplate(ragPromptTemplate)
                        .emptyContextPromptTemplate(emptyContextPromptTemplate)
                        .build())
                .build();
    }
}
