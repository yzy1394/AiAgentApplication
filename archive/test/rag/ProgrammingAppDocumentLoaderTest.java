package com.yzy.aiagent.rag;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest
class ProgrammingAppDocumentLoaderTest {

    @Resource
    private ProgrammingAppDocumentLoader programmingAppDocumentLoader;
    @Test
    void loadMarkdown() {
        programmingAppDocumentLoader.loadMarkdown();
    }
}
