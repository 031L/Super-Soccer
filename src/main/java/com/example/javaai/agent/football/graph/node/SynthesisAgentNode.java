package com.example.javaai.agent.football.graph.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.example.javaai.agent.football.FootballAgentFactory;
import com.example.javaai.agent.football.graph.FootballAgentNodeExecutor;
import com.example.javaai.agent.football.graph.FootballAgentTaskBuilder;
import com.example.javaai.agent.football.graph.FootballGraphKeys;
import com.example.javaai.agent.football.graph.FootballGraphProgress;
import com.example.javaai.agent.football.graph.FootballSseOutputPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class SynthesisAgentNode implements NodeAction {

    private final FootballAgentFactory agentFactory;
    private final FootballAgentTaskBuilder taskBuilder;
    private final FootballAgentNodeExecutor nodeExecutor;

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        return FootballGraphProgress.runWithSession(state, () -> {
            FootballGraphProgress.emit("\n\n---------- 阶段 4/4：综合 Agent ----------\n");
            var synthesisAgent = agentFactory.createSynthesisAgent();
            String output = nodeExecutor.runStage(
                    null, synthesisAgent, taskBuilder.buildSynthesisTask(state), "综合 Agent");
            FootballGraphProgress.emit(FootballSseOutputPolicy.forSse("synthesis", output));
            return Map.of(FootballGraphKeys.FINAL_REPORT, output);
        });
    }
}
