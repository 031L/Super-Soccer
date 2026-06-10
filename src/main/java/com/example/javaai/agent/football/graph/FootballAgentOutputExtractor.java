package com.example.javaai.agent.football.graph;

import cn.hutool.core.util.StrUtil;
import com.example.javaai.agent.BaseAgent;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 从 Agent 消息历史或 run 日志中提取有效分析文本。
 */
@Component
public class FootballAgentOutputExtractor {

    public String extract(BaseAgent agent, String runResult) {
        List<Message> messages = agent.getMessageList();
        for (int i = messages.size() - 1; i >= 0; i--) {
            Message message = messages.get(i);
            if (message instanceof AssistantMessage assistantMessage
                    && isFinalAssistantOutput(assistantMessage)) {
                return assistantMessage.getText().trim();
            }
        }
        for (int i = messages.size() - 1; i >= 0; i--) {
            Message message = messages.get(i);
            if (message instanceof AssistantMessage assistantMessage
                    && StrUtil.isNotBlank(assistantMessage.getText())) {
                return assistantMessage.getText().trim();
            }
        }
        if (runResult == null || runResult.isBlank()) {
            return "（无输出）";
        }
        String[] steps = runResult.split("\n");
        for (int i = steps.length - 1; i >= 0; i--) {
            String line = steps[i];
            if (line.contains("工具 ") && line.contains("返回的结果")) {
                int idx = line.indexOf("返回的结果：");
                if (idx >= 0) {
                    return line.substring(idx + "返回的结果：".length()).trim();
                }
            }
            if (line.startsWith("Step ")) {
                int colon = line.indexOf(": ");
                if (colon > 0) {
                    String content = line.substring(colon + 2).trim();
                    if (!content.equals("思考完成 - 无需行动") && !content.startsWith("没有工具需要调用")) {
                        return content;
                    }
                }
            }
        }
        return runResult;
    }

    private boolean isFinalAssistantOutput(AssistantMessage assistantMessage) {
        if (StrUtil.isBlank(assistantMessage.getText())) {
            return false;
        }
        var toolCalls = assistantMessage.getToolCalls();
        if (toolCalls == null || toolCalls.isEmpty()) {
            return true;
        }
        return toolCalls.size() == 1 && "doTerminate".equals(toolCalls.get(0).name());
    }
}
