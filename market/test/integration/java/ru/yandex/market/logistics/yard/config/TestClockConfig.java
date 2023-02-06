package ru.yandex.market.logistics.yard.config;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class TestClockConfig {

    @Bean
    @Primary
    public Clock clock() {
        return Clock.fixed(Instant.parse("2020-01-01T12:00:00.00Z"), ZoneId.of("Europe/Moscow"));
    }
}
