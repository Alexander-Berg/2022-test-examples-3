package ru.yandex.market.delivery.mdbapp.configuration;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import steps.utils.TestableClock;

@Configuration
public class ClockConfig {

    @Bean
    @Primary
    protected Clock mockClock() {
        return new TestableClock();
    }
}
