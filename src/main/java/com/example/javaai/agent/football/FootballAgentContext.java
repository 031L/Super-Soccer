package com.example.javaai.agent.football;

import lombok.Data;

/**
 * 多 Agent 协作时的共享上下文，各阶段产出依次写入。
 */
@Data
public class FootballAgentContext {

    private final String userQuery;
    private final String matchId;
    private final String redisRawData;

    private String dataAnalysis;
    private String simulationAnalysis;
    private String tacticalAnalysis;
    private String finalReport;
    /** StateGraph 意图分类结果：MATCH_ANALYSIS / GENERAL_QA */
    private String intent;

    public FootballAgentContext(String userQuery) {
        this(userQuery, null, null);
    }

    public FootballAgentContext(String userQuery, String matchId, String redisRawData) {
        this.userQuery = userQuery;
        this.matchId = matchId;
        this.redisRawData = redisRawData;
    }

    public boolean hasRedisMatchData() {
        return redisRawData != null && !redisRawData.isBlank();
    }

    public String buildTacticalInput() {
        return """
                【用户原始问题】
                %s

                【数据 Agent 产出】
                %s
                """.formatted(userQuery, nullToEmpty(dataAnalysis));
    }

    public String buildSimulationInput() {
        return """
                【用户原始问题】
                %s

                【数据 Agent 产出】
                %s

                【战术 Agent 产出】
                %s
                """.formatted(userQuery, nullToEmpty(dataAnalysis), nullToEmpty(tacticalAnalysis));
    }

    public String buildSynthesisInput() {
        return """
                【用户原始问题】
                %s

                【数据 Agent 产出】
                %s

                【战术 Agent 产出】
                %s

                【推演 Agent 产出】
                %s
                """.formatted(
                userQuery,
                nullToEmpty(dataAnalysis),
                nullToEmpty(tacticalAnalysis),
                nullToEmpty(simulationAnalysis));
    }

    private static String nullToEmpty(String value) {
        return value == null ? "（暂无）" : value;
    }
}
