com.example.javaai
├── controller/          # REST 接口
├── app/                 # FootballApp 应用门面
├── agent/
│   ├── BaseAgent        # Agent 基类
│   ├── ReActAgent       # 思考-行动循环
│   ├── ToolCallAgent    # 工具调用 Agent
│   ├── ChatOnlyAgent    # 纯对话 Agent
│   ├── YuManus          # 通用超级智能体
│   └── football/
│       ├── *Agent       # 5 个足球专用 Agent
│       ├── graph/       # StateGraph 节点与状态
│       └── FootballMultiAgentOrchestrator
├── config/              # Spring 配置（Graph、Model、Redis）
├── tool/                # 7 个工具（搜索、爬取、文件、PDF…）
├── prompt/              # DocumentPromptService
└── resources/document/football/  # 各 Agent 提示词 .md
