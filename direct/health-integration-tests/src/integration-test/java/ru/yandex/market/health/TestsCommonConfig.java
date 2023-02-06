package ru.yandex.market.health;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import ru.yandex.market.clickhouse.ClickHouseSource;
import ru.yandex.market.clickhouse.ClickhouseTemplate;
import ru.yandex.market.logshatter.LogShatterMonitoring;
import ru.yandex.market.monitoring.ComplicatedMonitoring;

@Configuration
@PropertySource("classpath:integration-tests.properties")
public class TestsCommonConfig {

    @Value("${clickhouse.host}")
    private String clickhouseHost;

    @Value("${database.name}")
    private String clickhouseDatabase;

    @Bean
    public ClickHouseSource clickHouseSource() {
        ClickHouseSource clickHouseSource = new ClickHouseSource();
        clickHouseSource.setHost(clickhouseHost);
        clickHouseSource.setDb(clickhouseDatabase);
        return clickHouseSource;
    }

    @Bean
    public ClickhouseTemplate clickhouseTemplate() {
        ClickhouseTemplate clickhouseTemplate = new ClickhouseTemplate();
        clickhouseTemplate.setDb(clickHouseSource());
        return clickhouseTemplate;
    }

    @Bean
    public ComplicatedMonitoring complicatedMonitoring() {
        return new ComplicatedMonitoring();
    }

    @Bean
    public LogShatterMonitoring logShatterMonitoring() {
        return new LogShatterMonitoring();
    }
}
