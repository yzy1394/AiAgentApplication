package com.yzy.aiagent.agent;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class YzyManusTest {
    @Resource
    private YzyManus yzyManus;

    @Test
    public void run(){
        String userPrompt = """
                我的团队在上海静安区办公，请帮我找到 5 公里内适合技术分享的活动场地，
                并结合一些网络图片，制定一份详细的技术沙龙计划，
                并以 PDF 格式输出""";
        String answer= yzyManus.run(userPrompt);
        Assertions.assertNotNull(answer);
    }

}
