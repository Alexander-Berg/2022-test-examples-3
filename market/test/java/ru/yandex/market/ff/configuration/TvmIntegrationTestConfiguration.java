package ru.yandex.market.ff.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.market.ff.config.DeliveryTrackerClientConfig;
import ru.yandex.market.ff.config.metrics.PrometheusConfiguration;
import ru.yandex.market.fulfillment.stockstorage.client.StockStorageClientConfiguration;
import ru.yandex.market.health.jobs.service.TmsJobExecutionFacade;

import static org.mockito.Mockito.mock;

@Configuration
@Import({
        BaseIntegrationTestConfiguration.class,
        TvmTestMockConfiguration.class,
        StockStorageClientConfiguration.class,
        DbQueueConfigWithoutConsumers.class,
        DeliveryTrackerClientConfig.class,
        PrometheusConfiguration.class,
        YtJdbcTestConfig.class,
        PechkinClientTestConfig.class,
        HistoryAgencyTestConfiguration.class
})
public class TvmIntegrationTestConfiguration {
    @Bean
    public TmsJobExecutionFacade tmsJobExecutionFacade() {
        return mock(TmsJobExecutionFacade.class);
    }


}
