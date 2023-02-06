package ru.yandex.market.tpl.billing.config;

import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.common.util.date.TestableClock;

@ContextConfiguration
public class ClockConfig {

    @Bean
    TestableClock clock() {
        return new TestableClock();
    }
}
