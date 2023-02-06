package ru.yandex.market.pers.qa;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.util.concurrent.MoreExecutors;
import org.apache.http.client.HttpClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.web.client.RestTemplate;

import ru.yandex.common.cache.memcached.MemCachedAgent;
import ru.yandex.market.cleanweb.CleanWebClient;
import ru.yandex.market.grade.statica.client.PersStaticClient;
import ru.yandex.market.pers.author.client.PersAuthorClient;
import ru.yandex.market.pers.grade.client.GradeClient;
import ru.yandex.market.pers.notify.PersNotifyClient;
import ru.yandex.market.pers.qa.client.avatarnica.AvatarnicaClient;
import ru.yandex.market.pers.qa.mock.AutoFilterServiceTestUtils;
import ru.yandex.market.pers.service.common.startrek.StartrekService;
import ru.yandex.market.pers.test.common.MemCachedMockUtils;
import ru.yandex.market.pers.test.common.PersTestMocksHolder;
import ru.yandex.market.report.ReportService;
import ru.yandex.passport.tvmauth.TvmClient;

@Import({DatabaseMockConfiguration.class})
@Configuration
public class CoreMockConfiguration {
    @Bean
    public static PropertySourcesPlaceholderConfigurer propertyPlaceholderConfigurer() {
        return new PropertySourcesPlaceholderConfigurer();
    }

    @Bean
    public ReportService reportService() {
        return PersQaServiceMockFactory.reportServiceMock();
    }

    @Bean
    public PersNotifyClient persNotifyClient() {
        return PersQaServiceMockFactory.persNotifyClientMock();
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
    public GradeClient gradeClient() {
        return PersQaServiceMockFactory.gradeClientMock();
    }

    @Bean
    public AvatarnicaClient avatarnicaClient() {
        return PersTestMocksHolder.registerMock(AvatarnicaClient.class);
    }

    @Bean
    public RestTemplate persNotifyRestTemplate() {
        return PersTestMocksHolder.registerMock(RestTemplate.class);
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

    @Bean("saasHttpClient")
    public HttpClient saasHttpClient() {
        return PersQaServiceMockFactory.saasHttpClient();
    }

    @Bean
    public CleanWebClient cleanWebClient() {
        return PersTestMocksHolder.registerMock(CleanWebClient.class, AutoFilterServiceTestUtils::mockCleanWebClient);
    }

    @Bean
    public TvmClient tvmClient() {
        return PersTestMocksHolder.registerMock(TvmClient.class);
    }

    @Bean
    @Qualifier("threadPoolSupplier")
    public Supplier<ExecutorService> threadPoolSupplier() {
        return MoreExecutors::newDirectExecutorService;
    }

    @Bean
    @Qualifier("fixedThreadPoolSupplier")
    public Function<Integer, ExecutorService> fixedThreadPoolSupplier() {
        return val -> MoreExecutors.newDirectExecutorService();
    }

    @Bean
    public StartrekService startrekService() {
        return PersTestMocksHolder.registerMock(StartrekService.class);
    }
}
