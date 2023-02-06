package ru.yandex.market.mcadapter.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ru.yandex.common.util.date.TestableClock;

/**
 * @author zagidullinri
 * @date 12.07.2022
 */
@Configuration
public class TestsInternalConfiguration {
    @Bean
    public Clock clock() {
        return new ClockForTests();
    }
}
