package com.yzy.aiagent.tools;

import org.springframework.ai.tool.annotation.Tool;

/**
 * Tool used to explicitly terminate autonomous execution.
 */
public class TerminateTool {

    @Tool(description = "Terminate the interaction when the request is met or when the assistant cannot proceed further. Call this tool after all tasks are finished.")
    public String doTerminate() {
        return "Task finished";
    }
}
