package com.example.javaai.agent;

import com.example.javaai.agent.model.AgentState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;

/**
 * 单轮对话 Agent：不调用工具，一次 LLM 调用即完成本阶段任务。
 */
@Slf4j
public abstract class ChatOnlyAgent extends BaseAgent {

    @Override
    public String step() {
        try {
            Prompt prompt = new Prompt(getMessageList());
            String result = getChatClient().prompt(prompt)
                    .system(getSystemPrompt())
                    .call()
                    .content();
            getMessageList().add(new AssistantMessage(result));
            setState(AgentState.FINISHED);
            log.info("{} 完成分析，输出长度: {}", getName(), result.length());
            return result;
        } catch (Exception e) {
            setState(AgentState.ERROR);
            log.error("{} 执行失败", getName(), e);
            return "执行失败：" + e.getMessage();
        }
    }

    /**
     * 以用户问题 + 上游 Agent 产出作为输入，执行单轮分析。
     */
    public String analyze(String taskPrompt) {
        return run(taskPrompt);
    }
}
