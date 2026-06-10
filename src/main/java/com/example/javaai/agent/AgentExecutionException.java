package com.example.javaai.agent;

/**
 * Agent 串行执行或等待过程中发生的异常。
 */
public class AgentExecutionException extends RuntimeException {

    public AgentExecutionException(String message) {
        super(message);
    }

    public AgentExecutionException(String message, Throwable cause) {
        super(message, cause);
    }
}
