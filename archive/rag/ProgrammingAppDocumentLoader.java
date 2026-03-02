package com.yzy.aiagent.rag;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.markdown.MarkdownDocumentReader;
import org.springframework.ai.reader.markdown.config.MarkdownDocumentReaderConfig;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 编程助手应用文档加载器
 **/
@Component
@Slf4j
public class ProgrammingAppDocumentLoader {
    private final ResourcePatternResolver resourcePatternResolver;
    public ProgrammingAppDocumentLoader(ResourcePatternResolver resourcePatternResolver) {
        this.resourcePatternResolver = resourcePatternResolver;
    }
    public List<Document> loadMarkdown(){
        List<Document> allDocuments=new ArrayList<>();
        //加载多篇markdown
        try {
            Resource[] resources= resourcePatternResolver.getResources("classpath:document/*.md");
            for (Resource resource : resources){
                String filename=resource.getFilename();
                //根据文件名提取主题标签（例如：后端篇 / 前端篇 / 算法篇）
                String topic=filename.substring(filename.length()-6,filename.length()-3);
                MarkdownDocumentReaderConfig config= MarkdownDocumentReaderConfig.builder()
                        .withHorizontalRuleCreateDocument(true)
                        .withIncludeCodeBlock(false)
                        .withIncludeBlockquote(false)
                        .withAdditionalMetadata("filename",filename)
                        .withAdditionalMetadata("topic",topic)
                        .build();
                MarkdownDocumentReader markdownDocumentReader=new MarkdownDocumentReader(resource,config);
                allDocuments.addAll(markdownDocumentReader.read());
            }
        }catch (IOException e){
            log.error("加载markdown文档失败",e);
        }
        return allDocuments;
    }
}

