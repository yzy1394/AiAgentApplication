package com.yzy.aiagent.tools;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WebScrapingToolTest {

    @Test
    void scrapeWebPage() throws Exception {
        WebScrapingTool webScrapingTool = new WebScrapingTool();
        String url="https://www.baidu.com";
        String result=webScrapingTool.scrapeWebPage(url);
        assertNotNull(result);
    }
}