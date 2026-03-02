package com.yzy.aiagent.demo.invoke;

import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;

/**
 * HTTP方式调用AI
 */
public class HttpAiInvoke {
    public static void main(String[] args) {
        // 替换为你的API密钥
        String apiKey = TestApiKey.API_KEY;
        String url = "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions";

        // 构建请求体JSON
        JSONObject requestBody = new JSONObject();
        requestBody.set("model", "deepseek-v3.2");

        // 构建messages数组
        JSONArray messages = new JSONArray();

        // system消息
        JSONObject systemMsg = new JSONObject();
        systemMsg.set("role", "system");
        systemMsg.set("content", "You are a helpful assistant.");
        messages.add(systemMsg);

        // user消息
        JSONObject userMsg = new JSONObject();
        userMsg.set("role", "user");
        userMsg.set("content", "你是谁？");
        messages.add(userMsg);

        requestBody.set("messages", messages);

        // 发送POST请求 - 更简洁的写法
        String result = HttpUtil.createPost(url)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .body(requestBody.toString())
                .execute()
                .body();

        // 输出响应结果
        System.out.println("响应结果：" + result);
    }
}
