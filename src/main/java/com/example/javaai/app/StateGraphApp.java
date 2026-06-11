package com.example.javaai.app;

import com.example.javaai.agent.football.FootballMultiAgentOrchestrator;
import com.example.javaai.agent.football.graph.api.FootballGraphResponse;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * StateGraph 工作流应用入口。
 */
@Component
@Slf4j
public class StateGraphApp {

    @Resource
    private FootballMultiAgentOrchestrator orchestrator;

    public FootballGraphResponse invoke(String message, String matchId) {
        log.info("StateGraph 同步调用，matchId={}, message={}", matchId, message);
        return orchestrator.invokeStructured(message, matchId);
    }

    public SseEmitter stream(String message, String matchId) {
        log.info("StateGraph 流式调用，matchId={}, message={}", matchId, message);
        return orchestrator.runStructuredStream(message, matchId);
    }
}
