package com.example.javaai.agent.football;

import com.example.javaai.agent.ToolCallAgent;
import com.example.javaai.prompt.DocumentPromptService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;

/**
 * 通用足球问答 Agent：处理非单场赛果预测类问题，可使用搜索工具。
 */
public class FootballGeneralAgent extends ToolCallAgent {

    private static final String PROMPT_KEY = "通用Agent提示词";
    private static final String NEXT_STEP_PROMPT = """
            请根据用户问题作答：优先使用任务消息中的「知识库参考」；知识库不足或需要最新动态时再使用搜索工具。
            每次工具调用后简要说明获得了什么；信息足够时输出完整回答，再调用 terminate 结束。
            禁止在仅说明「将要搜索」时结束；禁止不输出回答就调用 terminate。
            """;

    public FootballGeneralAgent(ToolCallback[] footballDataTools,
                                ChatModel chatModel,
                                DocumentPromptService documentPromptService) {
        super(footballDataTools);
        setName("通用Agent");
        setSystemPrompt(documentPromptService.getRaw(PROMPT_KEY));
        setNextStepPrompt(NEXT_STEP_PROMPT);
        setMaxSteps(6);
        ChatClient chatClient = ChatClient.builder(chatModel)
                .build();
        setChatClient(chatClient);
    }
}
