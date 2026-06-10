package com.example.javaai.agent.football;

import com.example.javaai.advisor.MyLoggerAdvisor;
import com.example.javaai.agent.ToolCallAgent;
import com.example.javaai.prompt.DocumentPromptService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
/**
 * 数据 Agent：负责搜集积分榜、战绩、伤停等事实性数据，可使用搜索与爬取工具。
 */
public class FootballDataAgent extends ToolCallAgent {

    private static final String PROMPT_KEY = "数据Agent提示词";
    private static final String NEXT_STEP_PROMPT = """
            请根据用户问题，主动使用搜索工具获取最新足球数据。
            每次工具调用后简要说明获得了什么；当 Redis 与搜索结果已足够时，**必须**按系统提示输出完整结构化报告，再调用 terminate 结束。
            禁止在仅说明「将要搜索」或「缺少字段」时结束；禁止不输出报告就调用 terminate。
            搜索关键词建议使用中文，包含球队名、赛事名、日期等。
            """;

    private static final String REDIS_NEXT_STEP_PROMPT = """
            已提供 Redis 清洗后的比赛 JSON。请**先**根据 Redis 写出完整报告主体（含 `_meta.hasEuropeanOdds`/`hasAsianHandicap` 为 true 时的赔率章节）。
            `hasJczqOfficialOdds=true` 表示竞彩官方已在 `europeanOdds.companies`（companyId=1），**不要**再搜竞彩官方赔率。
            仅当 `_meta.missingSections` 列出某项时，才对该项搜索补充；不要搜索 Redis 已有字段（含非空的积分榜、交锋、欧赔、亚盘）。
            空数组（如 homeRecentRecords）视为缺失，可搜索补充。
            **工具返回后，必须输出 Redis+搜索合并的完整结构化报告，再 terminate；禁止只列搜索计划就结束。**
            """;

    private static final String REDIS_SYSTEM_SUPPLEMENT = """

            # Redis 数据模式（当前任务）
            - 用户消息中包含从 Redis 读取并已系统清洗的比赛 JSON（无 @class、无 BigDecimal 包装，中文已还原）。
            - 请优先解析并整理该数据；**禁止**要求用户提供数据或声称无法访问 Redis。
            - 仅 `_meta.missingSections` 所列或 Redis 中为空数组/空字段的项，才使用搜索补充；竞彩官方以 `hasJczqOfficialOdds` 为准（欧盘 companyId=1）。
            - 工具返回后，**必须**将搜索结果写入结构化报告，不要停在「我将使用搜索工具补充」。
            - 不要编造 Redis 与搜索结果均不存在的字段；补充数据需标注来源。
            """;

    public FootballDataAgent(ToolCallback[] footballDataTools,
                             ChatModel dashscopeChatModel,
                             DocumentPromptService documentPromptService) {
        super(footballDataTools);
        setName("数据Agent");
        setSystemPrompt(documentPromptService.getRaw(PROMPT_KEY));
        setNextStepPrompt(NEXT_STEP_PROMPT);
        setMaxSteps(8);
        ChatClient chatClient = ChatClient.builder(dashscopeChatModel)
                .defaultAdvisors(new MyLoggerAdvisor())
                .build();
        setChatClient(chatClient);
    }

    /**
     * 使用 Redis 预取数据时：优先整理 Redis，缺失字段通过搜索工具补充。
     */
    public void configureForRedisMatchData() {
        setSystemPrompt(getSystemPrompt() + REDIS_SYSTEM_SUPPLEMENT);
        setNextStepPrompt(REDIS_NEXT_STEP_PROMPT);
        setMaxSteps(8);
    }
}
