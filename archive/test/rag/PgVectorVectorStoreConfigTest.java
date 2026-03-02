package com.yzy.aiagent.rag;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;

@SpringBootTest
class PgVectorVectorStoreConfigTest {

    @Resource
    private VectorStore pgVectorVectorStore;

    @Test
    void pgVectorVectorStore() {
        List<Document> documents = List.of(
                new Document("耄耋是一只很好的猫，从来不对人哈气", Map.of("meta1", "meta1")),
                new Document("耄耋是一只流浪猫"),
                new Document("耄耋又可以做称作哈基米", Map.of("meta2", "meta2")));

// Add the documents to PGVector
        pgVectorVectorStore.add(documents);

// Retrieve documents similar to a query
        List<Document> results = pgVectorVectorStore.similaritySearch(SearchRequest.builder().query("哈基米是什么").topK(10).build());
        Assertions.assertNotNull(results);
    }
}