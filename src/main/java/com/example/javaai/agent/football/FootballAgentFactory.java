package com.example.javaai.agent.football;

import com.example.javaai.prompt.DocumentPromptService;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * 每次协作流水线创建独立的 Agent 实例，避免会话状态串扰。
 */
@Component
public class FootballAgentFactory {

    private final ToolCallback[] footballDataTools;
    private final ChatModel footballDataChatModel;
    private final ChatModel footballSimulationChatModel;
    private final ChatModel footballTacticalChatModel;
    private final ChatModel footballSynthesisChatModel;
    private final ChatModel footballGeneralChatModel;
    private final DocumentPromptService documentPromptService;

    public FootballAgentFactory(
            ToolCallback[] footballDataTools,
            @Qualifier("footballDataChatModel") ChatModel footballDataChatModel,
            @Qualifier("footballSimulationChatModel") ChatModel footballSimulationChatModel,
            @Qualifier("footballTacticalChatModel") ChatModel footballTacticalChatModel,
            @Qualifier("footballSynthesisChatModel") ChatModel footballSynthesisChatModel,
            @Qualifier("footballGeneralChatModel") ChatModel footballGeneralChatModel,
            DocumentPromptService documentPromptService) {
        this.footballDataTools = footballDataTools;
        this.footballDataChatModel = footballDataChatModel;
        this.footballSimulationChatModel = footballSimulationChatModel;
        this.footballTacticalChatModel = footballTacticalChatModel;
        this.footballSynthesisChatModel = footballSynthesisChatModel;
        this.footballGeneralChatModel = footballGeneralChatModel;
        this.documentPromptService = documentPromptService;
    }

    public FootballDataAgent createDataAgent() {
        return new FootballDataAgent(footballDataTools, footballDataChatModel, documentPromptService);
    }

    public FootballDataAgent createDataAgentForRedisMatch() {
        FootballDataAgent agent = createDataAgent();
        agent.configureForRedisMatchData();
        return agent;
    }

    public FootballRedisDataAgent createRedisDataAgent() {
        return new FootballRedisDataAgent(footballDataChatModel, documentPromptService);
    }

    public FootballSimulationAgent createSimulationAgent() {
        return new FootballSimulationAgent(footballSimulationChatModel, documentPromptService);
    }

    public FootballTacticalAgent createTacticalAgent() {
        return new FootballTacticalAgent(footballTacticalChatModel, documentPromptService);
    }

    public FootballSynthesisAgent createSynthesisAgent() {
        return new FootballSynthesisAgent(footballSynthesisChatModel, documentPromptService);
    }

    public FootballGeneralAgent createGeneralAgent() {
        return new FootballGeneralAgent(footballDataTools, footballGeneralChatModel, documentPromptService);
    }
}
