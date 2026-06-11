package com.example.javaai.app;

import com.example.javaai.agent.football.GeneralQaOrchestrator;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 通用足球问答应用入口（独立于比赛分析流水线）。
 */
@Component
@Slf4j
public class GeneralQaApp {

    @Resource
    private GeneralQaOrchestrator orchestrator;

    public String ask(String message) {
        return orchestrator.run(message);
    }

    public SseEmitter askStream(String message) {
        return orchestrator.runStream(message);
    }
}
