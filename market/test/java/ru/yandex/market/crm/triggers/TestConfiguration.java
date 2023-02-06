package ru.yandex.market.crm.triggers;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.market.crm.environment.Environment;
import ru.yandex.market.crm.environment.EnvironmentResolver;
import ru.yandex.market.crm.platform.ConfigRepository;
import ru.yandex.market.crm.platform.config.PlatformConfiguration;
import ru.yandex.market.crm.triggers.services.bpm.platform.FactStrategyRegistry;
import ru.yandex.market.crm.triggers.services.platform.PlatformUtils;

@Configuration
@Import(PlatformConfiguration.class)
public class TestConfiguration {

    @Bean
    public FactStrategyRegistry factStrategyRegistry() {
        return new FactStrategyRegistry();
    }

    @Bean
    public EnvironmentResolver environmentResolver() {
        return () -> Environment.PRODUCTION;
    }

    @Bean
    public PlatformUtils platformUtils(ConfigRepository configRepository) {
        return new PlatformUtils(configRepository);
    }
}
