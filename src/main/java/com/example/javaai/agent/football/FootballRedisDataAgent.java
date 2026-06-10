package com.example.javaai.agent.football;

import com.example.javaai.advisor.MyLoggerAdvisor;
import com.example.javaai.agent.ChatOnlyAgent;
import com.example.javaai.prompt.DocumentPromptService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;

/**
 * Redis 数据整理 Agent：单轮 LLM 调用，直接整理 Redis 原始数据，不调用搜索工具。
 */
public class FootballRedisDataAgent extends ChatOnlyAgent {

    private static final String PROMPT_KEY = "数据Agent提示词";
    private static final String REDIS_SYSTEM_SUPPLEMENT = """

            # Redis 数据模式（当前任务）
            - 用户消息中包含从 Redis 读取并已系统清洗的比赛 JSON（无 @class、无 BigDecimal 包装，中文已还原）。
            - 请直接解析该数据并整理为结构化报告，**禁止**要求用户提供数据或声称无法访问 Redis。
            - 查看 `_meta.missingSections`：若含赔率相关项，表示 Redis 无赔率，不得编造，缺失项标注「待核实」或说明需搜索补充。
            - 必须输出完整报告正文，不要只回复「已收到」或空内容。
            """;

    public FootballRedisDataAgent(ChatModel dashscopeChatModel,
                                  DocumentPromptService documentPromptService) {
        setName("数据Agent(Redis)");
        setSystemPrompt(documentPromptService.getRaw(PROMPT_KEY) + REDIS_SYSTEM_SUPPLEMENT);
        setMaxSteps(1);
        ChatClient chatClient = ChatClient.builder(dashscopeChatModel)
                .defaultAdvisors(new MyLoggerAdvisor())
                .build();
        setChatClient(chatClient);
    }
}
