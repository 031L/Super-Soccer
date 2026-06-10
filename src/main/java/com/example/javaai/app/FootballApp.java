package com.example.javaai.app;

import com.example.javaai.agent.football.FootballAgentContext;
import com.example.javaai.agent.football.FootballMultiAgentOrchestrator;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 足球多智能体应用入口。
 */
@Component
@Slf4j
public class FootballApp {

    @Resource
    private FootballMultiAgentOrchestrator orchestrator;

    /**
     * 同步执行多 Agent 协作，返回完整上下文（最终报告见 {@link FootballAgentContext#getFinalReport()}）。
     */
    public FootballAgentContext analyze(String message) {
        log.info("足球多 Agent 分析开始: {}", message);
        return orchestrator.run(message);
    }

    /**
     * 按比赛 ID 从 Redis 取数，由数据 Agent 整理后驱动完整流水线。
     */
    public FootballAgentContext analyzeByMatchId(String matchId, String message) {
        log.info("足球多 Agent 分析开始，matchId={}, message={}", matchId, message);
        return orchestrator.runByMatchId(matchId, message);
    }

    /**
     * SSE 流式输出各 Agent 阶段结果。
     */
    public SseEmitter analyzeStream(String message) {
        log.info("足球多 Agent 流式分析开始: {}", message);
        return orchestrator.runStream(message);
    }

    public SseEmitter analyzeStreamByMatchId(String matchId, String message) {
        log.info("足球多 Agent 流式分析开始，matchId={}, message={}", matchId, message);
        return orchestrator.runStreamByMatchId(matchId, message);
    }
}
