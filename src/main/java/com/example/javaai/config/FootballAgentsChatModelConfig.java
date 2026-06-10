package com.example.javaai.config;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.example.javaai.config.properties.FootballAgentsModelProperties;
import com.example.javaai.config.properties.FootballAgentsModelProperties.AgentModelSettings;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Primary;

/**
 * 为足球流水线各 Agent 提供独立 ChatModel。
 */
@Configuration
@EnableConfigurationProperties(FootballAgentsModelProperties.class)
public class FootballAgentsChatModelConfig {

    @Bean
    @Primary
    @DependsOn("dashScopeChatModel")
    public ChatModel primaryChatModel(@Qualifier("dashScopeChatModel") ChatModel dashScopeChatModel) {
        return dashScopeChatModel;
    }

    @Bean("footballDataChatModel")
    @DependsOn("dashScopeChatModel")
    public ChatModel footballDataChatModel(
            DashScopeChatModel dashScopeChatModel,
            FootballAgentsModelProperties properties) {
        return buildAgentChatModel(dashScopeChatModel, properties.getData());
    }

    @Bean("footballSimulationChatModel")
    @DependsOn("dashScopeChatModel")
    public ChatModel footballSimulationChatModel(
            DashScopeChatModel dashScopeChatModel,
            FootballAgentsModelProperties properties) {
        return buildAgentChatModel(dashScopeChatModel, properties.getSimulation());
    }

    @Bean("footballTacticalChatModel")
    @DependsOn("dashScopeChatModel")
    public ChatModel footballTacticalChatModel(
            DashScopeChatModel dashScopeChatModel,
            FootballAgentsModelProperties properties) {
        return buildAgentChatModel(dashScopeChatModel, properties.getTactical());
    }

    @Bean("footballSynthesisChatModel")
    @DependsOn("dashScopeChatModel")
    public ChatModel footballSynthesisChatModel(
            DashScopeChatModel dashScopeChatModel,
            FootballAgentsModelProperties properties) {
        return buildAgentChatModel(dashScopeChatModel, properties.getSynthesis());
    }

    @Bean("footballGeneralChatModel")
    @DependsOn("dashScopeChatModel")
    public ChatModel footballGeneralChatModel(
            DashScopeChatModel dashScopeChatModel,
            FootballAgentsModelProperties properties) {
        return buildAgentChatModel(dashScopeChatModel, properties.getGeneral());
    }

    private ChatModel buildAgentChatModel(
            DashScopeChatModel dashScopeChatModel,
            AgentModelSettings settings) {
        DashScopeChatOptions options = DashScopeChatOptions.builder()
                .withModel(settings.getModel())
                .withEnableThinking(settings.getEnableThinking())
                .build();
        return dashScopeChatModel.mutate()
                .defaultOptions(options)
                .build();
    }
}
