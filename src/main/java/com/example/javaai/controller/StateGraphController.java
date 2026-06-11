package com.example.javaai.controller;

import cn.hutool.core.util.StrUtil;
import com.example.javaai.agent.football.graph.api.FootballGraphResponse;
import com.example.javaai.agent.football.graph.api.GraphWorkflowInfo;
import com.example.javaai.app.StateGraphApp;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * StateGraph 工作流 API：完整暴露图编排能力，供前端展示节点进度与各阶段状态。
 */
@RestController
@RequestMapping("/ai/graph")
@RequiredArgsConstructor
public class StateGraphController {

    private final StateGraphApp stateGraphApp;

    /**
     * 获取工作流元信息（节点列表、路由规则），供前端渲染流程图。
     * GET /api/ai/graph/workflow
     */
    @GetMapping("/workflow")
    public GraphWorkflowInfo workflow() {
        return GraphWorkflowInfo.football();
    }

    /**
     * StateGraph 结构化 SSE 流式执行。
     * 事件类型：PROGRESS | NODE_COMPLETE | DONE | ERROR
     * GET/POST /api/ai/graph/stream?message=...&matchId=...
     */
    @GetMapping("/stream")
    public SseEmitter stream(
            @RequestParam(required = false) String message,
            @RequestParam(required = false) String matchId) {
        validateInput(message, matchId);
        return stateGraphApp.stream(message, matchId);
    }

    @PostMapping("/stream")
    public SseEmitter streamPost(
            @RequestParam(required = false) String message,
            @RequestParam(required = false) String matchId) {
        return stream(message, matchId);
    }

    /**
     * StateGraph 同步执行，返回完整图状态（含各 Agent 产出与 intent）。
     * GET/POST /api/ai/graph/invoke?message=...&matchId=...
     */
    @GetMapping("/invoke")
    public FootballGraphResponse invoke(
            @RequestParam(required = false) String message,
            @RequestParam(required = false) String matchId) {
        validateInput(message, matchId);
        return stateGraphApp.invoke(message, matchId);
    }

    @PostMapping("/invoke")
    public FootballGraphResponse invokePost(
            @RequestParam(required = false) String message,
            @RequestParam(required = false) String matchId) {
        return invoke(message, matchId);
    }

    private void validateInput(String message, String matchId) {
        if (StrUtil.isBlank(message) && StrUtil.isBlank(matchId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "message 与 matchId 不能同时为空");
        }
    }
}
