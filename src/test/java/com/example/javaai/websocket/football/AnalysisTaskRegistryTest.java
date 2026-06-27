package com.example.javaai.websocket.football;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnalysisTaskRegistryTest {

    private AnalysisTaskRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new AnalysisTaskRegistry();
    }

    @Test
    void cancelMarksTaskCancelled() {
        registry.register("req-1", "principal-a");
        assertFalse(registry.isCancelled("req-1"));

        assertTrue(registry.cancel("req-1", "principal-a"));
        assertTrue(registry.isCancelled("req-1"));
    }

    @Test
    void cancelRejectsWrongPrincipal() {
        registry.register("req-2", "principal-a");
        assertFalse(registry.cancel("req-2", "principal-b"));
        assertFalse(registry.isCancelled("req-2"));
    }

    @Test
    void activeCountForPrincipal() {
        registry.register("req-3", "principal-a");
        assertEquals(1, registry.activeCountForPrincipal("principal-a"));
        registry.cancel("req-3", "principal-a");
        assertEquals(0, registry.activeCountForPrincipal("principal-a"));
    }
}
