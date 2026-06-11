package com.example.javaai.controller;

import cn.hutool.core.util.StrUtil;
import com.example.javaai.app.GeneralQaApp;
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
 * 通用足球问答独立入口：规则、术语、背景资料等，不走比赛分析流水线。
 */
@RestController
@RequestMapping("/ai/general")
@RequiredArgsConstructor
public class GeneralQaController {

    private final GeneralQaApp generalQaApp;

    /**
     * SSE 流式问答。
     * GET/POST /api/ai/general/stream?message=什么是越位
     */
    @GetMapping("/stream")
    public SseEmitter askStream(@RequestParam String message) {
        return stream(message);
    }

    @PostMapping("/stream")
    public SseEmitter askStreamPost(@RequestParam String message) {
        return stream(message);
    }

    /**
     * 同步问答，返回最终回答文本。
     * GET/POST /api/ai/general?message=亚盘让半球是什么意思
     */
    @GetMapping
    public String ask(@RequestParam String message) {
        return sync(message);
    }

    @PostMapping
    public String askPost(@RequestParam String message) {
        return sync(message);
    }

    private SseEmitter stream(String message) {
        validateMessage(message);
        return generalQaApp.askStream(message.trim());
    }

    private String sync(String message) {
        validateMessage(message);
        return generalQaApp.ask(message.trim());
    }

    private void validateMessage(String message) {
        if (StrUtil.isBlank(message)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "message 不能为空");
        }
    }
}
