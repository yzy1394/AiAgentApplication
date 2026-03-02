package com.yzy.aiagent.tools;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

/**
 * 网页抓取工具
 */
public class WebScrapingTool {
    @Tool(description = "Scrape web page content")
    public String scrapeWebPage(@ToolParam (description = "URL of a web page to scrape") String url)throws  Exception {
        try {
            Document document = Jsoup.connect(url).get();
            return document.html();
        }catch (Exception e) {
            return "Error scraping web page:"+e.getMessage();
        }
    }
}
