package com.example.javaai.agent.football;

import com.example.javaai.advisor.MyLoggerAdvisor;
import com.example.javaai.agent.ChatOnlyAgent;
import com.example.javaai.prompt.DocumentPromptService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
/**
 * 综合 Agent：整合数据、战术、推演三方结论，输出最终报告。
 */
public class FootballSynthesisAgent extends ChatOnlyAgent {

    private static final String PROMPT_KEY = "综合Agent提示词";

    public FootballSynthesisAgent(ChatModel dashscopeChatModel,
                                  DocumentPromptService documentPromptService) {
        setName("综合Agent");
        setSystemPrompt(documentPromptService.getRaw(PROMPT_KEY));
        setMaxSteps(1);
        ChatClient chatClient = ChatClient.builder(dashscopeChatModel)
                .defaultAdvisors(new MyLoggerAdvisor())
                .build();
        setChatClient(chatClient);
    }
}
