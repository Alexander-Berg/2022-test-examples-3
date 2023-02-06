package ru.yandex.market.partner.contract.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import ru.yandex.common.util.date.TestableClock;

/**
 * Основной конфиг для тестов
 */
@TestConfiguration
public class FunctionalTestConfig {

    @Bean
    public TestableClock clock() {
        return new TestableClock();
    }
}
