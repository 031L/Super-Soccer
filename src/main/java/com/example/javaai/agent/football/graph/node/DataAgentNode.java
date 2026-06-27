package com.example.javaai.agent.football.graph.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.example.javaai.agent.football.FootballAgentFactory;
import com.example.javaai.agent.football.FootballDataAgent;
import com.example.javaai.agent.football.graph.FootballAgentNodeExecutor;
import com.example.javaai.agent.football.graph.FootballAgentTaskBuilder;
import com.example.javaai.agent.football.graph.FootballGraphKeys;
import com.example.javaai.agent.football.graph.FootballGraphNodeNames;
import com.example.javaai.agent.football.graph.FootballGraphProgress;
import com.example.javaai.agent.football.graph.FootballGraphStateHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class DataAgentNode implements NodeAction {

    private final FootballAgentFactory agentFactory;
    private final FootballAgentTaskBuilder taskBuilder;
    private final FootballAgentNodeExecutor nodeExecutor;

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        return FootballGraphProgress.runWithSession(state, () -> {
            FootballGraphProgress.emitStageStart(FootballGraphNodeNames.DATA_AGENT, 1, 4);
            FootballGraphProgress.emit("\n\n---------- 阶段 1/4：数据 Agent ----------\n");
            FootballDataAgent dataAgent = FootballGraphStateHelper.hasRedisMatchData(state)
                    ? agentFactory.createDataAgentForRedisMatch()
                    : agentFactory.createDataAgent();
            String output = nodeExecutor.runStage(
                    null, dataAgent, taskBuilder.buildDataTask(state), "数据 Agent", state);
            FootballGraphProgress.emitStageOutput(FootballGraphNodeNames.DATA_AGENT, "data", output);
            return Map.of(FootballGraphKeys.DATA_ANALYSIS, output);
        });
    }
}
