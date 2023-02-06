package ru.yandex.market.antifraud.orders.test.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.market.antifraud.orders.config.AppConfig;
import ru.yandex.market.antifraud.orders.config.DatasourceConfiguration;
import ru.yandex.market.antifraud.orders.config.LiquibaseConfiguration;
import ru.yandex.market.antifraud.orders.config.SchedulerConfiguration;
import ru.yandex.market.antifraud.orders.config.YtTablePaths;
import ru.yandex.market.antifraud.orders.logbroker.entities.CancelOrderRequest;
import ru.yandex.market.antifraud.orders.queue.PgQueue;
import ru.yandex.market.antifraud.orders.service.CheckouterDataService;
import ru.yandex.market.antifraud.orders.service.ConfigurationService;
import ru.yandex.market.antifraud.orders.service.offline.OfflineAntifraudService;
import ru.yandex.market.antifraud.orders.storage.dao.ConfigurationDao;
import ru.yandex.market.antifraud.orders.util.DateProvider;
import ru.yandex.yt.ytclient.proxy.YtClient;

import static org.mockito.Mockito.mock;

/**
 * @author dzvyagin
 */
@Configuration
@Profile("integration-test")
@Import({
        EmbeddedPgConfig.class,
        DatasourceConfiguration.class,
        LiquibaseConfiguration.class,

        AppConfig.StorageConfiguration.class,
        AppConfig.JobConfiguration.class,
        SchedulerConfiguration.class
})
public class JobsTestConfiguration {

    @Bean
    public CheckouterDataService checkouterDataService() {
        return mock(CheckouterDataService.class);
    }

    @Bean
    public PgQueue<CancelOrderRequest> cancelOrderRequestQueue() {
        return mock(PgQueue.class);
    }

    @Bean
    public OfflineAntifraudService offlineAntifraudService() {
        return mock(OfflineAntifraudService.class);
    }

    @Bean("checkouterJdbcTemplate")
    public NamedParameterJdbcTemplate checkouterJdbcTemplate() {
        return mock(NamedParameterJdbcTemplate.class);
    }

    @Bean
    public YtClient ytClient() {
        return mock(YtClient.class);
    }

    @Bean
    public YtTablePaths ytTablePaths() {
        return new YtTablePaths();
    }

    @Bean
    public ConfigurationService configurationService(NamedParameterJdbcTemplate pgaasJdbcOperations) {
        return new ConfigurationService(new ConfigurationDao(pgaasJdbcOperations));
    }

    @Bean
    public DateProvider dateProvider() {
        return new DateProvider();
    }
}

