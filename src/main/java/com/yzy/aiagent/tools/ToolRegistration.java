package com.yzy.aiagent.tools;

import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * 统一工具注册。
 */
@Configuration
public class ToolRegistration {

    @Value("${search-api.api-key}")
    private String searchApiKey;

    /**
     * 高风险工具默认不注册，测试时通过配置显式开启。
     */
    @Value("${app.tools.terminal-enabled:false}")
    private boolean terminalToolEnabled;

    /**
     * 文件工具默认不注册，测试时通过配置显式开启。
     */
    @Value("${app.tools.file-enabled:false}")
    private boolean fileToolEnabled;

    @Bean
    public ToolCallback[] allTool() {
        List<Object> toolList = new ArrayList<>();
        toolList.add(new WebSearchTool(searchApiKey));
        toolList.add(new WebScrapingTool());
        toolList.add(new ResourceDownloadTool());
        toolList.add(new PDFGenerationTool());
        toolList.add(new TerminateTool());

        if (fileToolEnabled) {
            toolList.add(new FileOperationTool());
        }
        if (terminalToolEnabled) {
            toolList.add(new TerminalOperationTool());
        }
        return ToolCallbacks.from(toolList.toArray());
    }
}
