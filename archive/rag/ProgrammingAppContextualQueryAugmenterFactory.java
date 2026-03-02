package com.yzy.aiagent.rag;

import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter;

/**
 * 创建上下文查询增强器的工厂
 */
public class ProgrammingAppContextualQueryAugmenterFactory {
    public static ContextualQueryAugmenter createInstance(){
        PromptTemplate emptyContextpromptTemplate=new PromptTemplate("""
                你应该输出下面的内容：
                抱歉，我只能回答编程相关问题，其他问题请切换为智能体模式进行询问。
                """);
        return ContextualQueryAugmenter.builder()
                .allowEmptyContext(false)
                .emptyContextPromptTemplate(emptyContextpromptTemplate)
                .build();
    }
}

