package com.example.javaai.agent.football.graph.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.example.javaai.agent.football.FootballAgentFactory;
import com.example.javaai.agent.football.graph.FootballAgentNodeExecutor;
import com.example.javaai.agent.football.graph.FootballAgentTaskBuilder;
import com.example.javaai.agent.football.graph.FootballGraphKeys;
import com.example.javaai.agent.football.graph.FootballGraphProgress;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class TacticalAgentNode implements NodeAction {

    private final FootballAgentFactory agentFactory;
    private final FootballAgentTaskBuilder taskBuilder;
    private final FootballAgentNodeExecutor nodeExecutor;

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        FootballGraphProgress.emit("\n\n---------- 阶段 2/4：战术 Agent ----------\n");
        var tacticalAgent = agentFactory.createTacticalAgent();
        String output = nodeExecutor.runStage(
                null, tacticalAgent, taskBuilder.buildTacticalTask(state), "战术 Agent");
        FootballGraphProgress.emit(output);
        return Map.of(FootballGraphKeys.TACTICAL_ANALYSIS, output);
    }
}
