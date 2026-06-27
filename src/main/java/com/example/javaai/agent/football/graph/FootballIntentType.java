package com.example.javaai.agent.football.graph;

/**
 * 用户问题意图：决定走比赛分析流水线还是通用问答 Agent。
 */
public enum FootballIntentType {

    /** 比赛结果预测 / 赛前分析 → 数据 → 战术 → 推演 → 综合 */
    MATCH_ANALYSIS,

    /** 其他足球相关问题 → 通用 Agent */
    GENERAL_QA;

    public static FootballIntentType fromValue(String value) {
        if (value == null || value.isBlank()) {
            return GENERAL_QA;
        }
        try {
            return FootballIntentType.valueOf(value.trim());
        } catch (IllegalArgumentException ex) {
            return GENERAL_QA;
        }
    }
}
