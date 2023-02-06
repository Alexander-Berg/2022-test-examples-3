package ru.yandex.market.ff.configuration;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.market.ff.config.AppInfoConfig;
import ru.yandex.market.ff.config.CacheConfig;
import ru.yandex.market.ff.config.DbQueueConfig;
import ru.yandex.market.ff.config.metrics.PrometheusConfiguration;

@Configuration
@Import({
        BaseIntegrationTestConfiguration.class,
        DatabaseDatasourceConfig.class,
        MockConfiguration.class,
        DbQueueConfig.class,
        AppInfoConfig.class,
        CacheConfig.class,
        PrometheusConfiguration.class
})
@ComponentScan({"ru.yandex.market.ff.mvc", })
public class IntegrationTestWithDbQueueConsumersConfiguration {
}
