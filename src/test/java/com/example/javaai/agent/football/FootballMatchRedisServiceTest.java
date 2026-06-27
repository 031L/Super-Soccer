package com.example.javaai.agent.football;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
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
    void buildOddsChangesKey_shouldFormatCompanyChangesKey() {
        assertEquals("jczqOuzhi::changes:1359200:293",
                service.buildOddsChangesKey("jczqOuzhi::changes:", "1359200", "293"));
    }

    @Test
    void findMatchDataById_shouldMergeShujuMatchData() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("jczqDetail::1359200:false")).thenReturn(null);
        when(valueOperations.get("jczqDetail::1359200")).thenReturn(null);
        when(valueOperations.get("jczqShuju::1359200:false")).thenReturn(null);
        when(valueOperations.get("jczqShuju::1359200")).thenReturn("""
                {
                  "header": {"homeTeam": "主队", "awayTeam": "客队"},
                  "homeStandings": [{"played": 10}],
                  "headToHeadSummary": "近6次交锋",
                  "homeRecentRecords": [{"score": "2-1"}]
                }
                """);
        when(valueOperations.get("jczqOuzhi::1359200:false")).thenReturn(null);
        when(valueOperations.get("jczqOuzhi::1359200")).thenReturn(null);
        when(valueOperations.get("jczqYazhi::1359200:false")).thenReturn(null);
        when(valueOperations.get("jczqYazhi::1359200")).thenReturn(null);
        when(valueOperations.get("jczqTouzhu::1359200:false")).thenReturn(null);
        when(valueOperations.get("jczqTouzhu::1359200")).thenReturn(null);

        Optional<String> merged = service.findMatchDataById("1359200");

        assertTrue(merged.isPresent());
        String json = merged.get();
        assertTrue(json.contains("\"matchData\""));
        assertTrue(json.contains("主队"));
        assertTrue(json.contains("\"standings\""));
        assertTrue(json.contains("jczqShuju::1359200"));
    }

    @Test
    void findMatchDataById_shouldMergeOuzhiAndYazhiChanges() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("jczqDetail::1359200:false")).thenReturn(null);
        when(valueOperations.get("jczqDetail::1359200")).thenReturn(null);
        when(valueOperations.get("jczqShuju::1359200:false")).thenReturn(null);
        when(valueOperations.get("jczqShuju::1359200")).thenReturn(null);
        when(valueOperations.get("jczqOuzhi::1359200:false"))
                .thenReturn("{\"companies\":[{\"companyId\":\"293\",\"companyName\":\"威廉\"}]}");
        when(valueOperations.get("jczqYazhi::1359200:false"))
                .thenReturn("{\"companies\":[{\"companyId\":\"293\",\"companyName\":\"威廉\"}]}");
        when(valueOperations.get("jczqTouzhu::1359200:false")).thenReturn(null);
        when(valueOperations.get("jczqTouzhu::1359200")).thenReturn(null);
        when(valueOperations.get("jczqOuzhi::changes:1359200:293"))
                .thenReturn("{\"timeline\":[{\"time\":\"10:00\",\"win\":2.10}]}");
        when(valueOperations.get("jczqYazhi::changes:1359200:293"))
                .thenReturn("{\"timeline\":[{\"time\":\"10:00\",\"handicap\":\"平手\"}]}");

        Optional<String> merged = service.findMatchDataById("1359200");

        assertTrue(merged.isPresent());
        String json = merged.get();
        assertTrue(json.contains("\"changesByCompany\""));
        assertTrue(json.contains("jczqOuzhi::changes:1359200:293"));
        assertTrue(json.contains("jczqYazhi::changes:1359200:293"));
        assertTrue(json.contains("\"hasEuropeanOddsChanges\": true"));
        assertTrue(json.contains("\"hasAsianHandicapChanges\": true"));
        assertTrue(json.contains("\"changes\""));
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
        when(valueOperations.get("jczqShuju::1364015:false")).thenReturn(null);
        when(valueOperations.get("jczqShuju::1364015")).thenReturn(null);
        when(valueOperations.get("jczqOuzhi::1364015:false")).thenReturn(ouzhiRaw);
        when(valueOperations.get("jczqYazhi::1364015:false")).thenReturn(null);
        when(valueOperations.get("jczqYazhi::1364015")).thenReturn(null);
        when(valueOperations.get("jczqTouzhu::1364015:false")).thenReturn(null);
        when(valueOperations.get("jczqTouzhu::1364015")).thenReturn(null);

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
        when(valueOperations.get("jczqShuju::1364015:false")).thenReturn(null);
        when(valueOperations.get("jczqShuju::1364015")).thenReturn(null);
        when(valueOperations.get("jczqOuzhi::1364015:false")).thenReturn(null);
        when(valueOperations.get("jczqOuzhi::1364015")).thenReturn(null);
        when(valueOperations.get("jczqYazhi::1364015:false")).thenReturn(yazhiRaw);
        when(valueOperations.get("jczqTouzhu::1364015:false")).thenReturn(null);
        when(valueOperations.get("jczqTouzhu::1364015")).thenReturn(null);

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
    void findMatchDataById_shouldMergeDetailOuzhiYazhiAndTouzhu() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("jczqDetail::1366392:false"))
                .thenReturn("{\"fixtureId\":\"1366392\",\"header\":{\"homeTeam\":\"A\"}}");
        when(valueOperations.get("jczqOuzhi::1366392:false"))
                .thenReturn("{\"companies\":[{\"companyName\":\"威廉\"}]}");
        when(valueOperations.get("jczqYazhi::1366392:false"))
                .thenReturn("{\"companies\":[{\"instantHandicap\":\"平手\"}]}");
        when(valueOperations.get("jczqTouzhu::1366392:false"))
                .thenReturn("{\"totalVolume\":120000,\"homePercent\":45}");

        Optional<String> merged = service.findMatchDataById("1366392");

        assertTrue(merged.isPresent());
        String json = merged.get();
        assertTrue(json.contains("1366392"));
        assertTrue(json.contains("\"detail\""));
        assertTrue(json.contains("\"europeanOdds\""));
        assertTrue(json.contains("\"asianHandicap\""));
        assertTrue(json.contains("\"bettingVolume\""));
        assertTrue(json.contains("\"hasBettingVolume\": true"));
        assertTrue(json.contains("jczqDetail::1366392:false"));
        assertTrue(json.contains("jczqOuzhi::1366392:false"));
        assertTrue(json.contains("jczqYazhi::1366392:false"));
        assertTrue(json.contains("jczqTouzhu::1366392:false"));
    }

    @Test
    void findMatchDataById_shouldFallbackToLegacyTouzhuKey() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("jczqDetail::1359200:false")).thenReturn(null);
        when(valueOperations.get("jczqDetail::1359200")).thenReturn(null);
        when(valueOperations.get("jczqOuzhi::1359200:false")).thenReturn(null);
        when(valueOperations.get("jczqOuzhi::1359200")).thenReturn(null);
        when(valueOperations.get("jczqYazhi::1359200:false")).thenReturn(null);
        when(valueOperations.get("jczqYazhi::1359200")).thenReturn(null);
        when(valueOperations.get("jczqTouzhu::1359200:false")).thenReturn(null);
        when(valueOperations.get("jczqTouzhu::1359200"))
                .thenReturn("{\"totalVolume\":88000,\"drawPercent\":20}");

        Optional<String> merged = service.findMatchDataById("1359200");

        assertTrue(merged.isPresent());
        assertTrue(merged.get().contains("\"bettingVolume\""));
        assertTrue(merged.get().contains("jczqTouzhu::1359200"));
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
        when(valueOperations.get("jczqTouzhu::1366392:false")).thenReturn(null);
        when(valueOperations.get("jczqTouzhu::1366392")).thenReturn(null);

        Optional<String> merged = service.findMatchDataById("1366392");

        assertTrue(merged.isPresent());
        String json = merged.get();
        assertTrue(json.contains("1366392"));
        assertTrue(json.contains("\"detail\""));
        assertTrue(json.contains("\"europeanOdds\""));
        assertTrue(json.contains("\"asianHandicap\""));
        assertFalse(json.contains("\"hasBettingVolume\": true"));
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
        when(valueOperations.get("jczqTouzhu::1366392:false")).thenReturn(null);
        when(valueOperations.get("jczqTouzhu::1366392")).thenReturn(null);

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
        when(valueOperations.get("jczqTouzhu::1364015:false")).thenReturn(null);
        when(valueOperations.get("jczqTouzhu::1364015")).thenReturn(null);

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
