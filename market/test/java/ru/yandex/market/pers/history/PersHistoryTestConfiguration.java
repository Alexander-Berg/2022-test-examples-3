package ru.yandex.market.pers.history;

import java.util.concurrent.TimeUnit;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.common.cache.memcached.MemCachedAgent;
import ru.yandex.market.pers.history.config.CoreConfig;
import ru.yandex.market.pers.history.dj.DjClient;
import ru.yandex.market.pers.test.common.MemCachedMockUtils;
import ru.yandex.market.pers.test.common.PersTestMocksHolder;
import ru.yandex.market.pers.views.ViewsController;
import ru.yandex.market.pers.yt.YtClient;
import ru.yandex.market.pers.yt.YtClientMocks;
import ru.yandex.market.pers.yt.YtClusterType;
import ru.yandex.market.pers.yt.tm.TransferManagerClient;
import ru.yandex.market.pers.yt.yqlgen.YqlProcessor;
import ru.yandex.market.saas.search.SaasKvSearchService;

/**
 * @author Ilya Kislitsyn / ilyakis@ / 26.11.2021
 */
@Configuration
// all history configs (non-test) + all services
@ComponentScan(basePackageClasses = CoreConfig.class)
@ComponentScan(
    basePackageClasses = {PersHistoryMain.class, ViewsController.class},
    excludeFilters = @ComponentScan.Filter(Configuration.class)
)
public class PersHistoryTestConfiguration {

    @Bean
    @Qualifier("memCacheMock")
    public Cache<String, Object> localMemCachedCache() {
        return CacheBuilder.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .build();
    }

    @Bean
    public MemCachedAgent memCachedAgent(@Qualifier("memCacheMock") Cache<String, Object> cache) {
        return MemCachedMockUtils.buildMemCachedAgentMock(cache);
    }

    @Bean
    public SaasKvSearchService authorKvSearchService() {
        return PersTestMocksHolder.registerMock(SaasKvSearchService.class);
    }

    @Bean
    public DjClient djClient() {
        return PersTestMocksHolder.registerMock(DjClient.class);
    }

    @Bean
    @Primary
    public YtClient mainYtClient() {
        return PersTestMocksHolder.registerMock(YtClient.class, ytClient ->
            YtClientMocks.baseMock(YtClusterType.HAHN, ytClient));
    }

    @Bean
    public YtClient arnoldYtClient() {
        return PersTestMocksHolder.registerMock(YtClient.class, ytClient ->
            YtClientMocks.baseMock(YtClusterType.ARNOLD, ytClient));
    }

    @Bean
    public TransferManagerClient transferManagerClient() {
        return PersTestMocksHolder.registerMock(TransferManagerClient.class);
    }

    @Bean
    public JdbcTemplate ytJdbcTemplate() {
        return PersTestMocksHolder.registerMock(JdbcTemplate.class);
    }

    @Bean
    public YqlProcessor yqlProcessor() {
        return PersTestMocksHolder.registerMock(YqlProcessor.class);
    }

}
