package com.yzy.aiagent.app;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest
public class ProgrammingAppTest {
    @Resource
    private ProgrammingApp programmingApp;

    @Test
    void testChat() {
        String chatId= UUID.randomUUID().toString();
        //第一轮
        String message="你好，我是后端开发工程师";
        String answer = programmingApp.doChat(message,chatId);
        //第二轮
        message="请帮我设计一个支持高并发的订单服务架构";
        answer = programmingApp.doChat(message,chatId);
        Assertions.assertNotNull(answer);
        //第三轮
        message="回忆一下我上一条问题的核心诉求";
        answer = programmingApp.doChat(message,chatId);
        Assertions.assertNotNull(answer);
    }

    @Test
    void doChatWithProgrammingReport() {
        String chatId= UUID.randomUUID().toString();
        String message="你好，我在做 Java 后端服务，需要一个可落地的性能优化方案";
        ProgrammingApp.ProgrammingReport report = programmingApp.doChatWithProgrammingReport(message,chatId);
        Assertions.assertNotNull(report);
    }

    @Test
    void doChatWithRag() {
        String chatId= UUID.randomUUID().toString();
        String message="你好，我想了解如何在微服务架构中做好服务拆分和事务一致性";
        String anser = programmingApp.doChatWithRag(message,chatId);
        Assertions.assertNotNull(anser);
    }

    @Test
    void doChatWithTool() {
        // 测试联网搜索问题的答案
//        testMessage("帮我推荐一套适合 Spring Boot 项目的代码质量工具链");
//
//        // 测试网页抓取：技术案例分析
//        testMessage("抓取一篇关于 Java 性能优化的文章并总结关键结论");

        // 测试资源下载：图片下载
        testMessage("直接下载一张适合作为 IDE 壁纸的科技风图片为文件");

//        // 测试终端操作：执行代码
//        testMessage("执行 Python3 脚本来生成数据分析报告");
//
//        // 测试文件操作：保存用户档案
//        testMessage("保存我的编程档案为文件");
//
//        // 测试 PDF 生成
//        testMessage("生成一份‘系统重构计划’PDF，包含目标、里程碑和风险清单");
    }

    private void testMessage(String message) {
        String chatId= UUID.randomUUID().toString();
        String answer = programmingApp.doChatWithTools(message,chatId);
        Assertions.assertNotNull(answer);
    }

    @Test
    void doChatWithMcp() {
        String chatId= UUID.randomUUID().toString();
        //测试地图MCP
//        String message="我的团队在成都金牛万达附近办公，请帮我找5公里内适合技术交流的共享空间";
//        String answer=programmingApp.doChatWithMcp(message,chatId);
//        Assertions.assertNotNull(answer);
        //测试图片搜索MCP
        String message="请帮我找到一张 suitable for a programming meetup in Chengdu, China";
        String answer=programmingApp.doChatWithMcp(message,chatId);
        Assertions.assertNotNull(answer);
    }
}

