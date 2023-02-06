package ru.yandex.market.partner.notification.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import ru.yandex.common.util.date.TestableClock;

@TestConfiguration
public class TestableClockConfig {

    @Bean
    public TestableClock clock() {
        return new TestableClock();
    }

}
