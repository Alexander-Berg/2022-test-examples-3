package ru.yandex.market.pers.basket;

import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Function;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.common.cache.memcached.MemCachedAgent;
import ru.yandex.market.pers.basket.config.CoreConfiguration;
import ru.yandex.market.pers.basket.config.SwaggerConfig;
import ru.yandex.market.pers.basket.config.WebConfig;
import ru.yandex.market.pers.list.mock.AliceMvcMocks;
import ru.yandex.market.pers.test.common.MemCachedMockUtils;
import ru.yandex.market.pers.test.common.PersTestMocksHolder;
import ru.yandex.market.pers.yt.YtClient;
import ru.yandex.market.pers.yt.YtClientMocks;
import ru.yandex.market.pers.yt.YtClusterType;
import ru.yandex.market.report.ReportService;
import ru.yandex.passport.tvmauth.TvmClient;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.when;

/**
 * @author Ilya Kislitsyn / ilyakis@ / 25.11.2021
 */
@Configuration
@Import({
    EmbeddedPostgresDbConfig.class,
    CoreConfiguration.class,
    SwaggerConfig.class,
    WebConfig.class
})
// all configs from config package (filter with profile)
// and all basket service/components
@ComponentScan(basePackageClasses = CoreConfiguration.class)
@ComponentScan(
    basePackageClasses = {PersBasketMain.class, AliceMvcMocks.class},
    excludeFilters = @ComponentScan.Filter(Configuration.class)
)
public class PersBasketTestConfiguration {

    @Bean
    public Cache<String, Object> mockedCacheMap() {
        return CacheBuilder.newBuilder().build();
    }

    @Bean
    public MemCachedAgent getMemCachedAgentMock() {
        return MemCachedMockUtils.buildMemCachedAgentMock(mockedCacheMap());
    }

    @Bean
    public TvmClient tvmClient() {
        return PersTestMocksHolder.registerMock(TvmClient.class);
    }

    @Bean
    public ReportService reportService() {
        return PersTestMocksHolder.registerMock(ReportService.class, reportService ->
            Mockito.when(reportService.getOffersByIds(Matchers.anyList())).thenReturn(Optional.empty()));
    }

    @Bean
    public CollectionsClient collectionsClient() {
        return PersTestMocksHolder.registerMock(CollectionsClient.class, collectionsClient -> {
            when(collectionsClient.deleteCardFromCollections(any(), any(), any(), any()))
                .thenReturn(new BasketCollectionsDtoResponse[]{});

            when(collectionsClient.createCollectionsCard(any(),
                argThat((request) -> request == null || request.getDescription() == null), any()))
                .thenReturn(new BasketCollectionsDtoResponse());
        });
    }

    @Bean
    public Function<Integer, ScheduledExecutorService> buildScheduledPoolFactory() {
        return Executors::newScheduledThreadPool;
    }

    @Bean
    public Function<Runnable, Runnable> identityWorkerWrapper() {
        return x -> (Runnable) () -> {
            // wrapper, that does not calls original runnable
        };
    }

    @Bean
    @Qualifier("hahnYtClient")
    public YtClient ytClientHahn() {
        return PersTestMocksHolder.registerMock(YtClient.class, result ->
            YtClientMocks.baseMock(YtClusterType.HAHN, result)
        );
    }

    @Bean
    @Qualifier("yqlJdbcTemplate")
    public JdbcTemplate yqlJdbcTemplate() {
        return PersTestMocksHolder.registerMock(JdbcTemplate.class);
    }

}
