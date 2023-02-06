package ru.yandex.market.pers.author;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import javax.sql.DataSource;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.util.concurrent.MoreExecutors;
import liquibase.integration.spring.SpringLiquibase;
import org.apache.http.client.HttpClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.common.cache.memcached.MemCachedAgent;
import ru.yandex.market.cataloger.CatalogerClient;
import ru.yandex.market.common.mds.s3.client.service.api.MdsS3Client;
import ru.yandex.market.grade.statica.client.PersStaticClient;
import ru.yandex.market.live.LiveStreamingTarantinoClient;
import ru.yandex.market.mbo.MboCmsApiClient;
import ru.yandex.market.pers.author.mock.TakeoutHelperBuffer;
import ru.yandex.market.pers.author.takeout.TakeoutHelper;
import ru.yandex.market.pers.author.tms.live.client.FApiLiveClient;
import ru.yandex.market.pers.author.tms.live.client.ZenClient;
import ru.yandex.market.pers.test.common.MemCachedMockUtils;
import ru.yandex.market.pers.test.common.PersTestMocksHolder;
import ru.yandex.market.pers.test.db.EmbeddedPostgreFactory;
import ru.yandex.market.pers.tvm.TvmChecker;
import ru.yandex.market.pers.yt.YtClient;
import ru.yandex.market.pers.yt.YtClientMocks;
import ru.yandex.market.pers.yt.YtClusterType;
import ru.yandex.market.report.ReportService;
import ru.yandex.market.saas.search.SaasKvSearchService;
import ru.yandex.market.telegram.TelegramBotClient;

/**
 * @author varvara
 * 28.01.2020
 */
@Configuration
class CoreMockConfiguration {

    @Bean(destroyMethod = "close")
    public Object embeddedPostgres() {
        return EmbeddedPostgreFactory.embeddedPostgres(x -> x);
    }

    @Bean
    public DataSource embeddedDatasource() {
        return EmbeddedPostgreFactory.embeddedDatasource(embeddedPostgres(), Map.of());
    }

    @Bean
    public SpringLiquibase pgLiquibase() {
        SpringLiquibase result = new SpringLiquibase();
        result.setDataSource(pgDataSource());
        result.setChangeLog("classpath:liquibase/changelog.xml");
        result.setChangeLogParameters(Collections.singletonMap("is-unit-testing", "true"));
        return result;
    }

    @Bean
    public DataSource pgDataSource() {
        return embeddedDatasource();
    }

    @Bean
    public DataSource tmsDataSource() {
        return pgDataSource();
    }

    @Bean
    @Qualifier("memCacheMock")
    public Cache<String, Object> localMemCachedCache() {
        return CacheBuilder.newBuilder()
            .maximumSize(100)
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .build();
    }

    @Bean
    public MemCachedAgent memCachedAgent(@Qualifier("memCacheMock") Cache<String, Object> cache) {
        return MemCachedMockUtils.buildMemCachedAgentMock(cache);
    }

    @Bean
    public TelegramBotClient telegramBotClient() {
        return PersAuthorMockFactory.telegramBotClientMock();
    }

    @Bean
    public ReportService reportService() {
        return PersAuthorMockFactory.reportServiceMock();
    }

    @Bean
    public LiveStreamingTarantinoClient liveStreamingTarantinoClient() {
        return PersAuthorMockFactory.liveStreamingTarantinoClientMock();
    }

    @Bean
    public ZenClient zenClient() {
        return PersAuthorMockFactory.zenClientMock();
    }

    @Bean
    public MboCmsApiClient mboCmsApiClient() {
        return PersAuthorMockFactory.mboCmsApiClientMock();
    }

    @Bean
    public FApiLiveClient fApiLiveClient() {
        return PersAuthorMockFactory.fApiLiveClientMock();
    }

    @Bean
    public PersStaticClient persStaticClient() {
        return PersTestMocksHolder.registerMock(PersStaticClient.class);
    }

    @Bean
    @Qualifier("threadPoolSupplier")
    public Supplier<ExecutorService> threadPoolSupplier() {
        return MoreExecutors::newDirectExecutorService;
    }

    @Bean
    @Qualifier("threadPoolBuilderWithQueue")
    public BiFunction<Integer, BlockingQueue<Runnable>, ExecutorService> threadPoolBuilderWithQueue() {
        return (nThreads, queue) -> MoreExecutors.newDirectExecutorService();
    }

    @Bean("saasHttpClient")
    public HttpClient saasHttpClient() {
        return PersTestMocksHolder.registerMock(HttpClient.class);
    }

    @Bean
    public SaasKvSearchService saasKvSearchService() {
        return PersTestMocksHolder.registerMock(SaasKvSearchService.class);
    }

    @Bean
    public CatalogerClient getCatalogerClient() {
        return PersAuthorMockFactory.catalogerClientMock();
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

    @Bean
    public TvmChecker tvmChecker() {
        return PersTestMocksHolder.registerMock(TvmChecker.class);
    }

    @Bean
    public MdsS3Client mdsTakeoutClient() {
        return PersTestMocksHolder.registerMock(MdsS3Client.class);
    }

    @Bean
    @Qualifier("takeoutHelperFactory")
    public Supplier<TakeoutHelper> takeoutHelperFactory() {
        return TakeoutHelperBuffer::new;
    }

}
