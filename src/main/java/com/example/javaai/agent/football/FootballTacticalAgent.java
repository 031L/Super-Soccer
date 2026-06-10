package com.example.javaai.agent.football;

import com.example.javaai.advisor.MyLoggerAdvisor;
import com.example.javaai.agent.ChatOnlyAgent;
import com.example.javaai.prompt.DocumentPromptService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
/**
 * 战术 Agent：分析阵型、打法风格与战术克制关系。
 */
public class FootballTacticalAgent extends ChatOnlyAgent {

    private static final String PROMPT_KEY = "战术Agent提示词";

    public FootballTacticalAgent(ChatModel dashscopeChatModel,
                                 DocumentPromptService documentPromptService) {
        setName("战术Agent");
        setSystemPrompt(documentPromptService.getRaw(PROMPT_KEY));
        setMaxSteps(1);
        ChatClient chatClient = ChatClient.builder(dashscopeChatModel)
                .defaultAdvisors(new MyLoggerAdvisor())
                .build();
        setChatClient(chatClient);
    }
}
