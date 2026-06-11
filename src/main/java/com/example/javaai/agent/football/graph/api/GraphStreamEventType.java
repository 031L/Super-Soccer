package com.example.javaai.agent.football.graph.api;

/**
 * StateGraph SSE 事件类型，供前端区分渲染逻辑。
 */
public enum GraphStreamEventType {

    /** 节点执行过程中的文本进度（Agent 输出、编排器日志） */
    PROGRESS,

    /** 单个 Graph 节点执行完成，携带最新状态快照 */
    NODE_COMPLETE,

    /** 整条工作流执行完成 */
    DONE,

    /** 执行失败 */
    ERROR
}
