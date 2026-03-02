package com.yzy.yzyimagesearchmcpserver.tools;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ImageSearchTool {

    private final String apiKey;

    private final String apiUrl;

    public ImageSearchTool(
            @Value("${pexels.api-key:}") String apiKey,
            @Value("${pexels.api-url:https://api.pexels.com/v1/search}") String apiUrl) {
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.apiUrl = apiUrl;
    }

    @Tool(description = "search image from web")
    public String searchImage(@ToolParam(description = "Search query keyword") String query) {
        try {
            return String.join(",", searchMediumImages(query));
        } catch (Exception e) {
            return "Error search image: " + e.getMessage();
        }
    }

    public List<String> searchMediumImages(String query) {
        if (apiKey.isBlank()) {
            throw new IllegalStateException("PEXELS_API_KEY is not configured");
        }

        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", apiKey);

        Map<String, Object> params = new HashMap<>();
        params.put("query", query);

        String response = HttpUtil.createGet(apiUrl)
                .addHeaders(headers)
                .form(params)
                .execute()
                .body();

        return JSONUtil.parseObj(response)
                .getJSONArray("photos")
                .stream()
                .map(photoObj -> (JSONObject) photoObj)
                .map(photoObj -> photoObj.getJSONObject("src"))
                .map(photo -> photo.getStr("medium"))
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.toList());
    }
}
