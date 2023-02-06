package ru.yandex.market.clab.db.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import ru.yandex.market.clab.ControlledClock;

/**
 * @author Alexander Kramarev (pochemuto@yandex-team.ru)
 * @date 23.10.2018
 */
@Primary
@Configuration
@Profile("test")
public class ControlledClockConfiguration extends ClockConfiguration {

    @Bean
    @Override
    public ControlledClock clock() {
        return new ControlledClock(super.clock());
    }

}
