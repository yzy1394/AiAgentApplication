package com.yzy.yzyimagesearchmcpserver;

import com.yzy.yzyimagesearchmcpserver.tools.ImageSearchTool;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallback;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class YzyImageSearchMcpServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(YzyImageSearchMcpServerApplication.class, args);
	}

	@Bean
	public ToolCallbackProvider imageSearchTools(ImageSearchTool imageSearchTool) {
		return MethodToolCallbackProvider.builder()
				.toolObjects(imageSearchTool)
				.build();
	}
}
