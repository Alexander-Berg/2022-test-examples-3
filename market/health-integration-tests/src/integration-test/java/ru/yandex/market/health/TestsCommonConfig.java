package ru.yandex.market.health;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import ru.yandex.market.health.configs.clickphite.spring.ClickhouseConfig;
import ru.yandex.market.logshatter.LogShatterMonitoring;
import ru.yandex.market.monitoring.ComplicatedMonitoring;

@Configuration
@PropertySource("classpath:integration-tests.properties")
@Import(ClickhouseConfig.class)
public class TestsCommonConfig {
    @Bean
    public ComplicatedMonitoring complicatedMonitoring() {
        return new ComplicatedMonitoring();
    }

    @Bean
    public LogShatterMonitoring logShatterMonitoring() {
        return new LogShatterMonitoring();
    }
}
