package com.example.javaai.agent.football.graph.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.example.javaai.agent.football.GeneralQaRunner;
import com.example.javaai.agent.football.graph.FootballGraphKeys;
import com.example.javaai.agent.football.graph.FootballGraphStateHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class GeneralQaAgentNode implements NodeAction {

    private final GeneralQaRunner generalQaRunner;

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        String userQuery = FootballGraphStateHelper.stringValue(state, FootballGraphKeys.USER_QUERY);
        String output = generalQaRunner.run(userQuery, null);
        return Map.of(FootballGraphKeys.FINAL_REPORT, output);
    }
}
