package com.example.javaai.agent.football.graph.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.example.javaai.agent.football.graph.FootballGraphKeys;
import com.example.javaai.agent.football.graph.FootballGraphProgress;
import com.example.javaai.agent.football.graph.FootballGraphStateHelper;
import com.example.javaai.agent.football.graph.FootballIntentClassifier;
import com.example.javaai.agent.football.graph.FootballIntentType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 意图分类：决定走比赛分析流水线或通用问答 Agent。
 */
@Component
@RequiredArgsConstructor
public class ClassifyIntentNode implements NodeAction {

    private final FootballIntentClassifier intentClassifier;

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        return FootballGraphProgress.runWithSession(state, () -> {
            String userQuery = FootballGraphStateHelper.stringValue(state, FootballGraphKeys.USER_QUERY);
            String matchId = FootballGraphStateHelper.stringValue(state, FootballGraphKeys.MATCH_ID);
            FootballIntentType intent = intentClassifier.classify(userQuery, matchId);

            FootballGraphProgress.emit("【编排器】意图识别结果：" + intent.name());
            if (intent == FootballIntentType.MATCH_ANALYSIS) {
                FootballGraphProgress.emit("【编排器】进入比赛分析流水线：数据 → 战术 → 推演 → 综合");
            } else {
                FootballGraphProgress.emit("【编排器】进入通用问答 Agent");
            }
            return Map.of(FootballGraphKeys.INTENT, intent.name());
        });
    }
}
