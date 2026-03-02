package com.yzy.aiagent.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test")
@Tag(name = "测试接口", description = "用于测试 knife4j 功能的接口")
public class TestController {

    @GetMapping("/hello")
    @Operation(summary = "测试接口", description = "返回简单的问候信息")
    public String hello() {
        return "Hello, Knife4j!";
    }
}
