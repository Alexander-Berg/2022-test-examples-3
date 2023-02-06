package ru.yandex.market.logistics.nesu.configuration;

import org.springframework.context.annotation.Bean;

import ru.yandex.common.util.date.TestableClock;

public class ClockConfiguration {
    @Bean
    TestableClock clock() {
        return new TestableClock();
    }
}
