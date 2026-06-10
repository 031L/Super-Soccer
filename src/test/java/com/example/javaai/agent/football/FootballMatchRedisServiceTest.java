package com.example.javaai.agent.football;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FootballMatchRedisServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private FootballRedisProperties redisProperties;
    private FootballMatchRedisService service;

    @BeforeEach
    void setUp() {
        redisProperties = new FootballRedisProperties();
        service = new FootballMatchRedisService(redisTemplate, redisProperties);
    }

    @Test
    void buildRedisKey_shouldAppendSuffix() {
        assertEquals("jczqOuzhi::1366392:false",
                service.buildRedisKey("jczqOuzhi::", "1366392"));
    }

    @Test
    void findMatchDataById_shouldFilterOuzhiCompanies() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        String ouzhiRaw = "{\"companies\":[\"java.util.ArrayList\",["
                + "{\"companyId\":\"1\",\"companyName\":\"\\xe7\\xab\\x9e*\\xe5\\xae\\x98*\","
                + "\"instantOdds\":{\"win\":[\"java.math.BigDecimal\",2.95]},\"changes\":null},"
                + "{\"companyId\":\"999\",\"companyName\":\"other\","
                + "\"instantOdds\":{\"win\":[\"java.math.BigDecimal\",9.99]},\"changes\":null},"
                + "{\"companyId\":\"293\",\"companyName\":\"william\","
                + "\"instantOdds\":{\"win\":[\"java.math.BigDecimal\",2.70]},\"changes\":null}"
                + "]]}";
        when(valueOperations.get("jczqDetail::1364015:false")).thenReturn(null);
        when(valueOperations.get("jczqDetail::1364015")).thenReturn(null);
        when(valueOperations.get("jczqOuzhi::1364015:false")).thenReturn(ouzhiRaw);
        when(valueOperations.get("jczqYazhi::1364015:false")).thenReturn(null);
        when(valueOperations.get("jczqYazhi::1364015")).thenReturn(null);

        Optional<String> merged = service.findMatchDataById("1364015");

        assertTrue(merged.isPresent());
        String json = merged.get();
        assertTrue(json.contains("\"europeanOdds\""));
        assertTrue(json.contains("_companyFilter"));
        assertTrue(json.contains("\"keptCount\": 2"));
        assertFalse(json.contains("\"companyId\": \"999\""));
    }

    @Test
    void findMatchDataById_shouldFilterYazhiCompanies() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        String yazhiRaw = "{\"companies\":[\"java.util.ArrayList\",["
                + "{\"companyId\":\"293\",\"companyName\":\"\\xe5\\xa8\\x81**\\xe5\\xb0\\x94\","
                + "\"instantHandicap\":\"\\xe5\\x8f\\x97\\xe5\\xb9\\xb3\\xe6\\x89\\x8b/\\xe5\\x8d\\x8a\\xe7\\x90\\x83\","
                + "\"instantHomeWater\":[\"java.math.BigDecimal\",0.780],\"changes\":null},"
                + "{\"companyId\":\"999\",\"companyName\":\"other\","
                + "\"instantHandicap\":\"平手\",\"instantHomeWater\":[\"java.math.BigDecimal\",0.50],\"changes\":null},"
                + "{\"companyId\":\"5\",\"companyName\":\"macau\","
                + "\"instantHandicap\":\"\\xe5\\x8f\\x97\\xe5\\xb9\\xb3\\xe6\\x89\\x8b/\\xe5\\x8d\\x8a\\xe7\\x90\\x83\","
                + "\"instantHomeWater\":[\"java.math.BigDecimal\",0.850],\"changes\":null}"
                + "]]}";
        when(valueOperations.get("jczqDetail::1364015:false")).thenReturn(null);
        when(valueOperations.get("jczqDetail::1364015")).thenReturn(null);
        when(valueOperations.get("jczqOuzhi::1364015:false")).thenReturn(null);
        when(valueOperations.get("jczqOuzhi::1364015")).thenReturn(null);
        when(valueOperations.get("jczqYazhi::1364015:false")).thenReturn(yazhiRaw);

        Optional<String> merged = service.findMatchDataById("1364015");

        assertTrue(merged.isPresent());
        String json = merged.get();
        assertTrue(json.contains("\"asianHandicap\""));
        assertTrue(json.contains("\"companyId\": \"293\""));
        assertTrue(json.contains("\"companyId\": \"5\""));
        assertTrue(json.contains("\"keptCount\": 2"));
        assertFalse(json.contains("\"companyId\": \"999\""));
    }

    @Test
    void findMatchDataById_shouldMergeDetailOuzhiAndYazhi() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("jczqDetail::1366392:false"))
                .thenReturn("{\"fixtureId\":\"1366392\",\"header\":{\"homeTeam\":\"A\"}}");
        when(valueOperations.get("jczqOuzhi::1366392:false"))
                .thenReturn("{\"companies\":[{\"companyName\":\"威廉\"}]}");
        when(valueOperations.get("jczqYazhi::1366392:false"))
                .thenReturn("{\"companies\":[{\"instantHandicap\":\"平手\"}]}");

        Optional<String> merged = service.findMatchDataById("1366392");

        assertTrue(merged.isPresent());
        String json = merged.get();
        assertTrue(json.contains("1366392"));
        assertTrue(json.contains("\"detail\""));
        assertTrue(json.contains("\"europeanOdds\""));
        assertTrue(json.contains("\"asianHandicap\""));
        assertTrue(json.contains("jczqDetail::1366392:false"));
        assertTrue(json.contains("jczqOuzhi::1366392:false"));
        assertTrue(json.contains("jczqYazhi::1366392:false"));
    }

    @Test
    void findMatchDataById_shouldFallbackToLegacyDetailKey() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("jczqDetail::1366392:false")).thenReturn(null);
        when(valueOperations.get("jczqDetail::1366392"))
                .thenReturn("{\"fixtureId\":\"1366392\"}");
        when(valueOperations.get("jczqOuzhi::1366392:false")).thenReturn(null);
        when(valueOperations.get("jczqOuzhi::1366392")).thenReturn(null);
        when(valueOperations.get("jczqYazhi::1366392:false")).thenReturn(null);
        when(valueOperations.get("jczqYazhi::1366392")).thenReturn(null);

        Optional<String> merged = service.findMatchDataById("1366392");

        assertTrue(merged.isPresent());
        assertTrue(merged.get().contains("\"detail\""));
    }

    @Test
    void findMatchDataById_shouldCleanSpringRedisJacksonFormat() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        String springRedisJson = "{\"@class\":\"com.example.reception.model.analysis.JczqMatchDetail\","
                + "\"fixtureId\":\"1364015\","
                + "\"header\":{\"homeTeam\":\"\\xe6\\x96\\xaf\\xe8\\xbe\\xbe\","
                + "\"win\":[\"java.math.BigDecimal\",2.95],\"changes\":null}}";
        when(valueOperations.get("jczqDetail::1364015:false")).thenReturn(springRedisJson);
        when(valueOperations.get("jczqOuzhi::1364015:false")).thenReturn(null);
        when(valueOperations.get("jczqOuzhi::1364015")).thenReturn(null);
        when(valueOperations.get("jczqYazhi::1364015:false")).thenReturn(null);
        when(valueOperations.get("jczqYazhi::1364015")).thenReturn(null);

        Optional<String> merged = service.findMatchDataById("1364015");

        assertTrue(merged.isPresent());
        String json = merged.get();
        assertTrue(json.contains("斯达"));
        assertFalse(json.contains("@class"));
        assertFalse(json.contains("java.math.BigDecimal"));
        assertFalse(json.contains("\\x"));
        assertTrue(json.contains("\"hasEuropeanOdds\": false"));
        assertTrue(json.contains("missingSections"));
    }
}
