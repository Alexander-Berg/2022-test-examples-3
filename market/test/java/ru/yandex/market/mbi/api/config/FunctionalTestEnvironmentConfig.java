package ru.yandex.market.mbi.api.config;

import java.time.Clock;
import java.time.ZoneId;

import javax.annotation.ParametersAreNonnullByDefault;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.common.mds.s3.spring.db.ResourceConfigurationDao;
import ru.yandex.market.core.asyncreport.AsyncReports;
import ru.yandex.market.core.asyncreport.DisabledAsyncReportService;
import ru.yandex.market.core.asyncreport.ReportsDao;
import ru.yandex.market.core.asyncreport.ReportsService;
import ru.yandex.market.core.asyncreport.ReportsServiceSettings;
import ru.yandex.market.core.asyncreport.model.ReportsType;
import ru.yandex.market.core.datacamp.feed.FeedProcessorUpdateRequestEvent;
import ru.yandex.market.core.npd.client.IntegrationNpdRetrofitService;
import ru.yandex.market.core.solomon.SolomonTestJvmConfig;
import ru.yandex.market.core.util.DateTimes;
import ru.yandex.market.integration.npd.client.api.ApplicationApi;
import ru.yandex.market.logbroker.LogbrokerEventPublisher;
import ru.yandex.market.logbroker.LogbrokerService;
import ru.yandex.market.logbroker.LogbrokerServiceImpl;
import ru.yandex.market.mbi.environment.EnvironmentService;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ParametersAreNonnullByDefault
@Configuration
@Import({
        ApiMdsS3Config.class,
        SolomonTestJvmConfig.class,
})
public class FunctionalTestEnvironmentConfig {
    private static final int REPORTS_QUEUE_LIMIT = 10;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EnvironmentService environmentService;

    @Autowired
    private ReportsDao<ReportsType> reportsDao;

    /**
     * @param server сервер
     * @return порт, определяется сервером автоматически из свободных при запуске.
     */
    @Bean
    @DependsOn("server")
    public int port(Server server) {
        return ((ServerConnector) server.getConnectors()[0]).getLocalPort();
    }

    @Bean
    public ResourceConfigurationDao resourceConfigurationDao() {
        return mock(ResourceConfigurationDao.class);
    }

    @Bean
    public AsyncReports<ReportsType> asyncReportsService() {
        return new ReportsService<>(
                new ReportsServiceSettings.Builder<ReportsType>().setReportsQueueLimit(REPORTS_QUEUE_LIMIT).build(),
                reportsDao,
                transactionTemplate,
                () -> "777",
                Clock.fixed(
                        DateTimes.toInstantAtDefaultTz(2019, 6, 28, 10, 0, 0),
                        ZoneId.systemDefault()
                ),
                new DisabledAsyncReportService(jdbcTemplate),
                environmentService
        );
    }

    @Bean
    public LogbrokerEventPublisher<FeedProcessorUpdateRequestEvent> feedProcessorUpdateLogbrokerEventPublisher() {
        return mock(LogbrokerEventPublisher.class);
    }

    @Bean
    public LogbrokerService promoOfferLogbrokerService() {
        return mock(LogbrokerServiceImpl.class);
    }

    @Bean
    public ApplicationApi applicationApi() {
        return mock(ApplicationApi.class);
    }

    @Bean
    public IntegrationNpdRetrofitService integrationNpdRetrofitService(ApplicationApi applicationApi) {
        var retrofitMock = mock(IntegrationNpdRetrofitService.class);
        when(retrofitMock.create(eq(ApplicationApi.class))).thenReturn(applicationApi);
        return retrofitMock;
    }
}
