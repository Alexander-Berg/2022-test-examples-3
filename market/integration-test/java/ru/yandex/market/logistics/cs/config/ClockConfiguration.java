package ru.yandex.market.logistics.cs.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ru.yandex.common.util.date.TestableClock;

@Configuration
public class ClockConfiguration {
    @Bean
    TestableClock clock() {
        return new TestableClock();
    }
}
