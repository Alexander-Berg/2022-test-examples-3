package ru.yandex.market.pers.pay;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.util.concurrent.MoreExecutors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.context.WebApplicationContext;

import ru.yandex.common.cache.memcached.MemCachedAgent;
import ru.yandex.kikimr.persqueue.LogbrokerClientFactory;
import ru.yandex.market.grade.statica.client.PersStaticClient;
import ru.yandex.market.loyalty.client.MarketLoyaltyClient;
import ru.yandex.market.pers.author.client.PersAuthorClient;
import ru.yandex.market.pers.pay.config.CoreConfig;
import ru.yandex.market.pers.pay.config.JdbcConfig;
import ru.yandex.market.pers.pay.config.SwaggerConfig;
import ru.yandex.market.pers.test.common.MemCachedMockUtils;
import ru.yandex.market.pers.test.common.PersTestMocksHolder;
import ru.yandex.market.pers.tvm.TvmChecker;
import ru.yandex.market.pers.yt.YtClient;
import ru.yandex.market.pers.yt.YtClusterType;
import ru.yandex.passport.tvmauth.TvmClient;

import static org.mockito.Mockito.when;

@Configuration
@Import({
    JdbcConfig.class,
    CoreConfig.class,
    TestDbConfig.class,
    SwaggerConfig.class
})
@ComponentScan(
    basePackageClasses = PersPay.class,
    excludeFilters = @ComponentScan.Filter(Configuration.class)
)
class PersPayTestConfiguration {

    @Autowired
    private WebApplicationContext wac;

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
    public TvmChecker tvmChecker() {
        return PersTestMocksHolder.registerMock(TvmChecker.class);
    }

    @Bean
    public TvmClient tvmClient() {
        return PersTestMocksHolder.registerMock(TvmClient.class);
    }

    @Bean
    public LogbrokerClientFactory lbkxClientFactory() {
        return PersTestMocksHolder.registerMock(LogbrokerClientFactory.class);
    }

    @Bean
    public MarketLoyaltyClient loyaltyClient() {
        return PersTestMocksHolder.registerMock(MarketLoyaltyClient.class);
    }

    @Bean
    public JdbcTemplate yqlJdbcTemplate() {
        return PersTestMocksHolder.registerMock(JdbcTemplate.class);
    }

    @Bean
    public YtClient ytClientHahn() {
        return PersTestMocksHolder.registerMock(YtClient.class, ytClient -> {
            when(ytClient.getClusterType()).thenReturn(YtClusterType.HAHN);
        });
    }

    @Bean
    public YtClient ytClientArnold() {
        return PersTestMocksHolder.registerMock(YtClient.class, ytClient -> {
            when(ytClient.getClusterType()).thenReturn(YtClusterType.ARNOLD);
        });
    }

    @Bean
    public PersStaticClient persStaticClient() {
        return PersTestMocksHolder.registerMock(PersStaticClient.class);
    }

    @Bean
    public PersAuthorClient persAuthorClient() {
        return PersTestMocksHolder.registerMock(PersAuthorClient.class);
    }

    @Bean
    public BiFunction<Integer, BlockingQueue<Runnable>, ExecutorService> threadPoolBuilder() {
        return (n, q) -> MoreExecutors.newDirectExecutorService();
    }

}
