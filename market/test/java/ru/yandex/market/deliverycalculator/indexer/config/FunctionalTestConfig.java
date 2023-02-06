package ru.yandex.market.deliverycalculator.indexer.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.market.deliverycalculator.storage.configs.EmbeddedPostgresConfig;

@Configuration
@Import({
        // database setup
        EmbeddedPostgresConfig.class,
        // production application configs
        TestAppConfig.class,
        // test utils config
        BaseUrlConfig.class,
        // overrides
        CommonTestConfiguration.class,
        // mocks
        MocksConfig.class,
        //solomon jvm metrics
        SolomonTestJvmConfig.class
})
public class FunctionalTestConfig {
}
