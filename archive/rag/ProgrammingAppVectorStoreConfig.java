package com.yzy.aiagent.rag;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * 编程助手向量数据库配置（初始化基于内存的向量数据库bean）
 */
@Configuration
@Slf4j
public class ProgrammingAppVectorStoreConfig {

    @Resource
    private ProgrammingAppDocumentLoader programmingAppDocumentLoader;

    @Resource
    private MyTokenTextSplitter myTokenTextSplitter;

    @Resource
    private MyKeywordEnricher myKeywordEnricher;

    @Bean
    VectorStore programmingAppVectorStore(EmbeddingModel dashScopeEmbeddingModel) {
        SimpleVectorStore simpleVectorStore = SimpleVectorStore.builder(dashScopeEmbeddingModel).build();
        try {
            //加载文档
            List<Document> documentList = programmingAppDocumentLoader.loadMarkdown();
            //自主切分文档
            //List<Document> splitDocuments=myTokenTextSplitter.splitCustomized(documentList);
            //自动补充关键词元信息
            List<Document> enrichedDocuments = myKeywordEnricher.enrichDocuments(documentList);
            simpleVectorStore.add(enrichedDocuments);
        } catch (Exception e) {
            log.warn("Initialize programmingAppVectorStore failed, fallback to empty vector store. cause={}", e.getMessage());
        }
        return simpleVectorStore;
    }
}

