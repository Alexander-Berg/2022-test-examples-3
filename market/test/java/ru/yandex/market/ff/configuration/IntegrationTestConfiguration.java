package ru.yandex.market.ff.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.market.ff.config.AppInfoConfig;
import ru.yandex.market.ff.config.CacheConfig;
import ru.yandex.market.ff.config.HistoryAgencyContextBeanConfig;
import ru.yandex.market.ff.config.HistoryAgencyPropsConfig;
import ru.yandex.market.ff.config.HistoryAgencyThreadScopeConfig;
import ru.yandex.market.ff.config.metrics.PrometheusConfiguration;
import ru.yandex.market.logistics.util.client.TvmTicketProvider;

@Configuration
@Import({
        BaseIntegrationTestConfiguration.class,
        DatabaseDatasourceConfig.class,
        MockConfiguration.class,
        DbQueueConfigWithoutConsumers.class,
        AppInfoConfig.class,
        CacheConfig.class,
        PrometheusConfiguration.class,
        FeatureToggleTestConfiguration.class,
        YtJdbcTestConfig.class,
        PechkinClientTestConfig.class,
        HistoryAgencyTestConfiguration.class,
        HistoryAgencyPropsConfig.class,
        HistoryAgencyContextBeanConfig.class,
        HistoryAgencyThreadScopeConfig.class
})
@ComponentScan({"ru.yandex.market.ff.mvc"})
public class IntegrationTestConfiguration {
    @Bean
    public TvmTicketProvider logbrokerTvmTicketProvider() {
        return () -> "NOTHING";
    }
}
