package com.example.javaai.agent.football.graph;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FootballSseOutputPolicyTest {

    @Test
    void dataAndTacticalStagesAreSummarized() {
        String longText = "x".repeat(1000);
        String summarized = FootballSseOutputPolicy.forSse("data", longText);

        assertTrue(summarized.length() < longText.length());
        assertTrue(summarized.contains("已省略 200 字"));
    }

    @Test
    void simulationAndSynthesisStagesAreFull() {
        String text = "y".repeat(5000);
        assertEquals(text, FootballSseOutputPolicy.forSse("simulation", text));
        assertEquals(text, FootballSseOutputPolicy.forSse("synthesis", text));
    }
}
