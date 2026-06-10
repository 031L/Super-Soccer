package com.example.javaai.agent.football.graph;

import cn.hutool.core.util.StrUtil;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

/**
 * 根据用户问题与上下文判断应走比赛分析流水线还是通用问答 Agent。
 */
@Component
public class FootballIntentClassifier {

    private static final List<String> MATCH_ANALYSIS_KEYWORDS = List.of(
            "预测", "赛果", "胜负", "比分", "谁会赢", "赢球", "爆冷", "平局",
            "大小球", "让球", "赔率", "投注", "推荐", "分析这场", "这场比赛",
            "这场球", "赛前", "对阵", "结果", "怎么看", "怎么看这场", "怎么看这场比赛");

    public FootballIntentType classify(String userQuery, String matchId) {
        if (StrUtil.isNotBlank(matchId)) {
            return FootballIntentType.MATCH_ANALYSIS;
        }
        if (StrUtil.isBlank(userQuery)) {
            return FootballIntentType.GENERAL_QA;
        }
        String normalized = userQuery.toLowerCase(Locale.ROOT);
        for (String keyword : MATCH_ANALYSIS_KEYWORDS) {
            if (normalized.contains(keyword.toLowerCase(Locale.ROOT))) {
                return FootballIntentType.MATCH_ANALYSIS;
            }
        }
        return FootballIntentType.GENERAL_QA;
    }
}
