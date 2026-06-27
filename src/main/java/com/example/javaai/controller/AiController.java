package com.example.javaai.controller;

import cn.hutool.core.util.StrUtil;
import com.example.javaai.agent.football.FootballAgentContext;
import com.example.javaai.app.FootballApp;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/ai")
public class AiController {

    @Resource
    private FootballApp footballApp;

    /**
     * 足球多智能体协作（数据 → 战术 → 推演 → 综合），SSE 流式输出。
     * 传入 matchId 时从 Redis 读取比赛数据交给数据 Agent 整理。
     */
    @GetMapping(value = "/football/multi_agent/stream")
    public SseEmitter doFootballMultiAgentStream(String message, String matchId) {
        return streamFootballMultiAgent(message, matchId);
    }

    @PostMapping(value = "/football/multi_agent/stream")
    public SseEmitter doFootballMultiAgentStreamPost(String message, String matchId) {
        return streamFootballMultiAgent(message, matchId);
    }

    private SseEmitter streamFootballMultiAgent(String message, String matchId) {
        if (StrUtil.isNotBlank(matchId)) {
            return footballApp.analyzeStreamByMatchId(matchId, message);
        }
        return footballApp.analyzeStream(message);
    }

    /**
     * 足球多智能体协作，同步返回最终报告。
     * 传入 matchId 时从 Redis 读取比赛数据交给数据 Agent 整理。
     */
    @GetMapping(value = "/football/multi_agent")
    public String doFootballMultiAgent(String message, String matchId) {
        FootballAgentContext context = StrUtil.isNotBlank(matchId)
                ? footballApp.analyzeByMatchId(matchId, message)
                : footballApp.analyze(message);
        return context.getFinalReport();
    }

    /**
     * @deprecated 请使用 /football/multi_agent/stream；保留旧 SSE 端点名称兼容
     */
    @GetMapping(value = "/love_app/chat/sse_emitter")
    public SseEmitter doChatWithLoveAppServerSseEmitter(String message, String chatId) {
        return footballApp.analyzeStream(message);
    }

}
