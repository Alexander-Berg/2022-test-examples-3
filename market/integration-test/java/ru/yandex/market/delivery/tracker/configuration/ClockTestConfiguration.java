package ru.yandex.market.delivery.tracker.configuration;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ClockTestConfiguration {

    @Bean
    public Clock getClock() {
        ZoneId zoneId = ZoneId.systemDefault();
        return Clock.fixed(LocalDateTime.of(2018, 1, 1, 15, 27)
            .atZone(zoneId).toInstant(), zoneId);
    }
}
