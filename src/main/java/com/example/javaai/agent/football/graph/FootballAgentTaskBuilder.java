package com.example.javaai.agent.football.graph;

import com.alibaba.cloud.ai.graph.OverAllState;
import org.springframework.stereotype.Component;

/**
 * 为各 Agent 节点构建任务 prompt。
 */
@Component
public class FootballAgentTaskBuilder {

    public String buildDataTask(OverAllState state) {
        if (FootballGraphStateHelper.hasRedisMatchData(state)) {
            return buildRedisDataAgentTask(state);
        }
        if (FootballGraphStateHelper.hasMatchId(state)) {
            return buildMatchIdSearchDataAgentTask(state);
        }
        return buildWebSearchDataAgentTask(state);
    }

    public String buildSimulationTask(OverAllState state) {
        return """
                【用户原始问题】
                %s

                【数据 Agent 产出】
                %s
                """.formatted(
                FootballGraphStateHelper.stringValue(state, FootballGraphKeys.USER_QUERY),
                nullToPlaceholder(FootballGraphStateHelper.stringValue(state, FootballGraphKeys.DATA_ANALYSIS)));
    }

    public String buildTacticalTask(OverAllState state) {
        return """
                【用户原始问题】
                %s

                【数据 Agent 产出】
                %s

                【推演 Agent 产出】
                %s
                """.formatted(
                FootballGraphStateHelper.stringValue(state, FootballGraphKeys.USER_QUERY),
                nullToPlaceholder(FootballGraphStateHelper.stringValue(state, FootballGraphKeys.DATA_ANALYSIS)),
                nullToPlaceholder(FootballGraphStateHelper.stringValue(state, FootballGraphKeys.SIMULATION_ANALYSIS)));
    }

    public String buildSynthesisTask(OverAllState state) {
        return """
                【用户原始问题】
                %s

                【数据 Agent 产出】
                %s

                【推演 Agent 产出】
                %s

                【战术 Agent 产出】
                %s
                """.formatted(
                FootballGraphStateHelper.stringValue(state, FootballGraphKeys.USER_QUERY),
                nullToPlaceholder(FootballGraphStateHelper.stringValue(state, FootballGraphKeys.DATA_ANALYSIS)),
                nullToPlaceholder(FootballGraphStateHelper.stringValue(state, FootballGraphKeys.SIMULATION_ANALYSIS)),
                nullToPlaceholder(FootballGraphStateHelper.stringValue(state, FootballGraphKeys.TACTICAL_ANALYSIS)));
    }

    public String buildGeneralQaTask(OverAllState state) {
        return FootballGraphStateHelper.stringValue(state, FootballGraphKeys.USER_QUERY);
    }

    private String buildRedisDataAgentTask(OverAllState state) {
        return """
                请解析以下 Redis 比赛原始数据，并整理为结构化数据报告（按系统提示中的输出格式）。

                【比赛 ID】
                %s

                【Redis 数据结构说明】
                - 以下 JSON 已由系统在读取 Redis 时完成清洗（已去除 @class、BigDecimal/ArrayList 包装，中文已还原）
                - detail：比赛详情（header、matchData、积分榜、交锋/战绩等）；**多数场次仅含此类数据，不含任何赔率**
                - europeanOdds：来自 jczqOuzhi，含 companyId=1 的「竞彩官」即竞彩官方欧赔（`_meta.hasJczqOfficialOdds=true` 时勿再搜竞彩赔率）
                - asianHandicap：来自 jczqYazhi，已过滤主流公司（见 `_companyFilter`）
                - 请查看 `_meta.availableSections` 与 `_meta.missingSections`；**仅 missingSections 中的项才需要搜索**

                【Redis 清洗后数据】
                %s

                【用户补充说明】
                %s

                【重要】
                - **必须先**用 Redis 已有数据写出报告主体（对阵、积分榜、交锋、欧赔、亚盘等），再对 `_meta.missingSections` 中的项搜索补充。
                - `hasJczqOfficialOdds=true` 时，竞彩官方数据在 `europeanOdds.companies`（companyId=1），**禁止**再搜索「竞彩官方 SP」。
                - `homeRecentRecords`/`awayRecentRecords` 为空数组时，近5场战绩可搜索补充；欧赔亚盘非空时不得声称 Redis 无赔率。
                - 不要根据字段名臆造数据；搜索补充须标注「来源：搜索补充」。
                - 必须输出完整结构化报告，不要只列「下一步将搜索」就结束。
                """.formatted(
                FootballGraphStateHelper.stringValue(state, FootballGraphKeys.MATCH_ID),
                FootballGraphStateHelper.stringValue(state, FootballGraphKeys.REDIS_RAW_DATA),
                FootballGraphStateHelper.stringValue(state, FootballGraphKeys.USER_QUERY));
    }

    private String buildMatchIdSearchDataAgentTask(OverAllState state) {
        String matchId = FootballGraphStateHelper.stringValue(state, FootballGraphKeys.MATCH_ID);
        return """
                Redis 中未找到比赛 ID %s 的数据。请使用搜索/爬取工具获取该场比赛及相关球队、赛事的最新数据，并整理为结构化报告。

                【比赛 ID】
                %s

                【用户补充说明】
                %s
                """.formatted(
                matchId,
                matchId,
                FootballGraphStateHelper.stringValue(state, FootballGraphKeys.USER_QUERY));
    }

    private String buildWebSearchDataAgentTask(OverAllState state) {
        return """
                请针对以下用户问题，搜集并整理足球比赛相关数据：

                %s
                """.formatted(FootballGraphStateHelper.stringValue(state, FootballGraphKeys.USER_QUERY));
    }

    private String nullToPlaceholder(String value) {
        return value == null || value.isBlank() ? "（暂无）" : value;
    }
}
