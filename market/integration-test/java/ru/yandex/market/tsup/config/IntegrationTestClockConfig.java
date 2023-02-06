package ru.yandex.market.tsup.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.tsup.util.Profiles;

@Configuration
public class IntegrationTestClockConfig {
    @Bean("clock")
    @Profile(Profiles.INTEGRATION_TEST)
    public TestableClock clock() {
        return new TestableClock();
    }
}
