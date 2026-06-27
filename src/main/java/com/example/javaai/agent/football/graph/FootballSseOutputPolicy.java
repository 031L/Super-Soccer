package com.example.javaai.agent.football.graph;

/**
 * SSE 流式输出策略：前序 Agent 仅推送摘要，保证推演 / 综合 Agent 完整输出。
 */
public final class FootballSseOutputPolicy {

    /** 数据 / 战术 Agent 推送到前端的最大字符数 */
    private static final int SUMMARY_MAX_CHARS = 800;

    private FootballSseOutputPolicy() {
    }

    /**
     * @param stage     阶段标识：data / tactical / simulation / synthesis
     * @param fullOutput Agent 完整输出（图状态中仍保存全文）
     */
    public static String forSse(String stage, String fullOutput) {
        if (fullOutput == null) {
            return null;
        }
        if ("data".equals(stage) || "tactical".equals(stage)) {
            return summarize(fullOutput, SUMMARY_MAX_CHARS);
        }
        return fullOutput;
    }

    static String summarize(String text, int maxChars) {
        if (text.length() <= maxChars) {
            return text;
        }
        int omitted = text.length() - maxChars;
        return text.substring(0, maxChars)
                + "\n\n... [已省略 " + omitted + " 字，完整内容已传递给下游 Agent]\n";
    }
}
