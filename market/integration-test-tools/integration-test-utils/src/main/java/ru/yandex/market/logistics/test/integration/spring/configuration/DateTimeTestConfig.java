package ru.yandex.market.logistics.test.integration.spring.configuration;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class DateTimeTestConfig {

    public static final String DEFAULT_DATE_TIME = "2018-01-01T00:00:00";
    @Value("${tests.fixedDatetime:" + DEFAULT_DATE_TIME + "}")
    private String expectedTestDate;

    @Bean
    public Clock getClock() {
        ZoneId zoneId = ZoneId.systemDefault();
        Instant instant = LocalDateTime
            .from(DateTimeFormatter.ISO_LOCAL_DATE_TIME.parse(expectedTestDate))
            .atZone(zoneId)
            .toInstant();
        return Clock.fixed(instant, zoneId);
    }
}
