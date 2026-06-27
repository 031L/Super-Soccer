package com.example.javaai.agent.football;

import com.example.javaai.advisor.MyLoggerAdvisor;
import com.example.javaai.agent.ChatOnlyAgent;
import com.example.javaai.prompt.DocumentPromptService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
/**
 * 推演 Agent：基于数据 Agent 与战术 Agent 产出进行赛果推演与情景分析。
 */
public class FootballSimulationAgent extends ChatOnlyAgent {

    private static final String PROMPT_KEY = "推演Agent提示词";

    public FootballSimulationAgent(ChatModel dashscopeChatModel,
                                   DocumentPromptService documentPromptService) {
        setName("推演Agent");
        setSystemPrompt(documentPromptService.getRaw(PROMPT_KEY));
        setMaxSteps(1);
        ChatClient chatClient = ChatClient.builder(dashscopeChatModel)
                .defaultAdvisors(new MyLoggerAdvisor())
                .build();
        setChatClient(chatClient);
    }
}
