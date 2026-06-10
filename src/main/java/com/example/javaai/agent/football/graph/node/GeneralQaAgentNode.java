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
public class GeneralQaAgentNode implements NodeAction {

    private final FootballAgentFactory agentFactory;
    private final FootballAgentTaskBuilder taskBuilder;
    private final FootballAgentNodeExecutor nodeExecutor;

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        FootballGraphProgress.emit("\n\n---------- 通用问答 Agent ----------\n");
        var generalAgent = agentFactory.createGeneralAgent();
        String output = nodeExecutor.runStage(
                null, generalAgent, taskBuilder.buildGeneralQaTask(state), "通用 Agent");
        FootballGraphProgress.emit(output);
        return Map.of(FootballGraphKeys.FINAL_REPORT, output);
    }
}
