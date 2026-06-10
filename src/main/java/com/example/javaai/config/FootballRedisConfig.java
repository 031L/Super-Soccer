package com.example.javaai.config;

import com.example.javaai.agent.football.FootballRedisProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(FootballRedisProperties.class)
public class FootballRedisConfig {
}
