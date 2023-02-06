package ru.yandex.market.logistics.utilizer.config;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ClockTestConfig {

    @Bean
    public Clock clock() {
        LocalDateTime now = LocalDateTime.of(2020, 12, 14, 17, 0);
        return Clock.fixed(now.toInstant(ZoneId.systemDefault().getRules().getOffset(now)), ZoneId.systemDefault());
    }
}
