package com.example.javaai.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 足球各 Agent 模型配置，与 application.yml 中 app.ai.football.agents 一一对应。
 * 修改模型只需改 application.yml，无需改本类代码。
 */
@Data
@ConfigurationProperties(prefix = "app.ai.football.agents")
public class FootballAgentsModelProperties {

    private AgentModelSettings data = new AgentModelSettings();
    private AgentModelSettings simulation = new AgentModelSettings();
    private AgentModelSettings tactical = new AgentModelSettings();
    private AgentModelSettings synthesis = new AgentModelSettings();
    private AgentModelSettings general = new AgentModelSettings();

    @Data
    public static class AgentModelSettings {

        /** DashScope 模型名，如 qwen-plus、qwen-max */
        private String model;

        /** 是否启用思考模式 */
        private Boolean enableThinking;
    }
}
