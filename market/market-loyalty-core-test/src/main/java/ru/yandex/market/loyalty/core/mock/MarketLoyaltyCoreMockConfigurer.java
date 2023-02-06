package ru.yandex.market.loyalty.core.mock;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import com.google.common.collect.ImmutableList;
import com.yandex.ydb.core.Result;
import com.yandex.ydb.core.Status;
import com.yandex.ydb.table.Session;
import com.yandex.ydb.table.TableClient;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.DefaultHttpResponseFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.mockito.MockSettings;
import org.mockito.listeners.InvocationListener;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import ru.yandex.bolts.collection.Option;
import ru.yandex.common.geocoder.client.TvmTicketProvider;
import ru.yandex.common.util.region.RegionService;
import ru.yandex.common.util.region.RegionTreePlainTextBuilder;
import ru.yandex.inside.passport.tvm2.Tvm2;
import ru.yandex.inside.passport.tvm2.TvmClientCredentials;
import ru.yandex.inside.solomon.pusher.SolomonPusher;
import ru.yandex.market.checkout.checkouter.client.CheckouterClient;
import ru.yandex.market.checkout.checkouter.client.CheckouterOrderHistoryEventsApi;
import ru.yandex.market.checkout.checkouter.client.CheckouterPaymentApi;
import ru.yandex.market.checkout.checkouter.client.CheckouterReturnApi;
import ru.yandex.market.common.report.DefaultMarketReportService;
import ru.yandex.market.common.report.GenericMarketReportService;
import ru.yandex.market.loyalty.core.config.Antifraud;
import ru.yandex.market.loyalty.core.config.Blackbox;
import ru.yandex.market.loyalty.core.config.BusinessRulesEngine;
import ru.yandex.market.loyalty.core.config.CacheForTests;
import ru.yandex.market.loyalty.core.config.CoreConfigExternal;
import ru.yandex.market.loyalty.core.config.Default;
import ru.yandex.market.loyalty.core.config.InternalGeoExport;
import ru.yandex.market.loyalty.core.config.Juggler;
import ru.yandex.market.loyalty.core.config.LaasApi;
import ru.yandex.market.loyalty.core.config.Personal;
import ru.yandex.market.loyalty.core.config.Recommendations;
import ru.yandex.market.loyalty.core.config.Smartshopping;
import ru.yandex.market.loyalty.core.config.StaffApi;
import ru.yandex.market.loyalty.core.config.TrustApi;
import ru.yandex.market.loyalty.core.config.TrustPayments;
import ru.yandex.market.loyalty.core.config.Uaas;
import ru.yandex.market.loyalty.core.config.YdbClient;
import ru.yandex.market.loyalty.core.config.caches.DefaultCache;
import ru.yandex.market.loyalty.core.config.qualifier.BankCashbackCalculatorApi;
import ru.yandex.market.loyalty.core.config.qualifier.BankCashbackCoreApi;
import ru.yandex.market.loyalty.core.config.qualifier.Tags;
import ru.yandex.market.loyalty.core.dao.ydb.AllUserOrdersDao;
import ru.yandex.market.loyalty.core.dao.ydb.CashbackOrdersDao;
import ru.yandex.market.loyalty.core.dao.ydb.NotificationDao;
import ru.yandex.market.loyalty.core.dao.ydb.PerkAcquisitionDao;
import ru.yandex.market.loyalty.core.dao.ydb.PersonalPromoPerksDao;
import ru.yandex.market.loyalty.core.dao.ydb.StaticPerkDao;
import ru.yandex.market.loyalty.core.dao.ydb.UserAccrualsCacheDao;
import ru.yandex.market.loyalty.core.dao.ydb.UserBlockPromoDao;
import ru.yandex.market.loyalty.core.dao.ydb.UserOrdersDao;
import ru.yandex.market.loyalty.core.dao.ydb.UserPromoDao;
import ru.yandex.market.loyalty.core.dao.ydb.UserReferralPromocodeDao;
import ru.yandex.market.loyalty.core.dao.ydb.caches.AntifraudOrdersCountCacheDao;
import ru.yandex.market.loyalty.core.logbroker.TskvLogBrokerClient;
import ru.yandex.market.loyalty.core.model.tags.TagsMatchResponse;
import ru.yandex.market.loyalty.core.service.ThrottlingControlService;
import ru.yandex.market.loyalty.core.service.avatar.AvatarsClient;
import ru.yandex.market.loyalty.core.service.blackbox.MonitoringAwareTvmTicketProvider;
import ru.yandex.market.loyalty.core.service.datacamp.DataCampStrollerClient;
import ru.yandex.market.loyalty.core.service.mail.YabacksMailer;
import ru.yandex.market.loyalty.core.stub.NotificationDaoStub;
import ru.yandex.market.loyalty.core.stub.StubDao;
import ru.yandex.market.loyalty.core.stub.YdbAllUsersOrdersDaoStub;
import ru.yandex.market.loyalty.core.stub.YdbAntifraudOrdersCountCacheDaoStub;
import ru.yandex.market.loyalty.core.stub.YdbPerkAcquisitionDaoStub;
import ru.yandex.market.loyalty.core.stub.YdbPersonalPromoPerksStub;
import ru.yandex.market.loyalty.core.stub.YdbStaticPerkDaoStub;
import ru.yandex.market.loyalty.core.stub.YdbUserAccrualsCacheDaoStub;
import ru.yandex.market.loyalty.core.stub.YdbUserBlockPromoDaoStub;
import ru.yandex.market.loyalty.core.stub.YdbUserReferralPromocodeDaoStub;
import ru.yandex.market.loyalty.monitoring.PushMonitor;
import ru.yandex.market.loyalty.monitoring.juggler.JugglerInternalPushMonitor;
import ru.yandex.market.loyalty.trace.HttpTrace;
import ru.yandex.market.pers.notify.PersNotifyClient;
import ru.yandex.market.request.trace.Module;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;
import static ru.yandex.market.loyalty.spring.utils.PreventAutowire.preventAutowire;

/**
 * @author <a href="mailto:maratik@yandex-team.ru">Marat Bukharov</a>
 */
@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
@Configuration
public class MarketLoyaltyCoreMockConfigurer {
    @SuppressFBWarnings(value = "MS_MUTABLE_COLLECTION_PKGPROTECT", justification = "Intentionally left mutable")
    public static final Map<Object, Runnable> MOCKS = new IdentityHashMap<>();
    @SuppressFBWarnings(value = "MS_MUTABLE_COLLECTION_PKGPROTECT", justification = "Intentionally left mutable")
    public static final List<StubDao> STUBS = new LinkedList<>();

    @Bean
    @Mocks
    public Map<Object, Runnable> createMocks() {
        return MOCKS;
    }

    @Bean
    @Stubs
    public List<StubDao> createStubs() {
        return STUBS;
    }

    @Bean
    public MockSettings mockSettings(@Autowired(required = false) List<InvocationListener> listeners) {
        return withSettings().invocationListeners(
                Optional.ofNullable(listeners)
                        .orElseGet(Collections::emptyList)
                        .toArray(InvocationListener[]::new)
        );
    }

    @Configuration
    public static class CheckouterClientConfig {
        @Bean
        public FactoryBean<CheckouterClient> getCheckouterClientMock(MockSettings mockSettings) {
            CheckouterClient result = mock(CheckouterClient.class, mockSettings);
            CheckouterOrderHistoryEventsApi api = mock(CheckouterOrderHistoryEventsApi.class);
            CheckouterReturnApi returnApi = mock(CheckouterReturnApi.class);
            CheckouterPaymentApi paymentApi = mock(CheckouterPaymentApi.class);

            MOCKS.put(api, null);
            MOCKS.put(returnApi, null);
            MOCKS.put(paymentApi, null);
            MOCKS.put(result, () -> {
                when(result.orderHistoryEvents()).thenReturn(api);
                when(result.returns()).thenReturn(returnApi);
                when(result.payments()).thenReturn(paymentApi);
            });
            return preventAutowire(result);
        }
    }

    @Configuration
    public static class GeoExportConfig {
        @Bean
        public RegionService regionService(
                @InternalGeoExport RegionTreePlainTextBuilder internalRegionTreePlainTextBuilder
        ) {
            RegionService regionService = new RegionService();
            regionService.setRegionTreeBuilder(internalRegionTreePlainTextBuilder);
            return regionService;
        }
    }

    @Configuration
    public static class MemcacheConfig {
        @Bean
        @DefaultCache
        public CacheForTests cache(Clock clock) {
            return new CacheForTests(clock);
        }
    }

    @Configuration
    public static class BlackboxClientConfig {
        @Bean
        @Blackbox
        public RestTemplate createBlackboxRestTemplate(MockSettings mockSettings) {
            RestTemplate restTemplate = mock(RestTemplate.class, mockSettings);
            MOCKS.put(restTemplate, null);
            return restTemplate;
        }

        @Bean
        public TvmTicketProvider testAuxAdminBlackboxTvmTicketProvider(Tvm2 mockTvm2, PushMonitor monitor) {
            return new MonitoringAwareTvmTicketProvider(
                    mockTvm2,
                    1,
                    monitor
            );
        }

        @Bean
        @Blackbox
        HttpClient createHttpClient(MockSettings mockSettings) {
            HttpClient httpClient = mock(HttpClient.class, mockSettings);
            MOCKS.put(httpClient, null);
            return httpClient;
        }
    }

    @Configuration
    public static class AvatarConfig {
        @Smartshopping
        @Bean
        public AvatarsClient avatarsClient(MockSettings mockSettings) {
            AvatarsClient result = mock(AvatarsClient.class, mockSettings);
            //FIXME move mocking inside AvatarsClient (use restTemplate)
            MOCKS.put(result, () -> when(result.imageLinkWithoutThumb(any())).then(
                    (Answer<String>) invocation -> invocation.getArguments()[0].toString()));
            return result;
        }
    }

    @Configuration
    public static class PersNotifyClientConfig {
        @Bean
        public FactoryBean<PersNotifyClient> persNotifyClientMock(MockSettings mockSettings) {
            PersNotifyClient result = mock(PersNotifyClient.class, mockSettings);
            MOCKS.put(result, null);
            return preventAutowire(result);
        }
    }

    @Configuration
    public static class ReportConfig {
        @Bean
        public DefaultMarketReportService marketReportServiceMock(MockSettings mockSettings) {
            DefaultMarketReportService result = mock(DefaultMarketReportService.class, mockSettings);
            MOCKS.put(result, null);
            return result;
        }

        @Bean
        public GenericMarketReportService genericMarketReportServiceMock(MockSettings mockSettings) {
            GenericMarketReportService result = mock(GenericMarketReportService.class, mockSettings);
            MOCKS.put(result, null);
            return result;
        }
    }

    @Configuration
    public static class YabacksMailerConfig {
        @Bean
        public YabacksMailer yabacksMailerMock(MockSettings mockSettings) {
            YabacksMailer result = mock(YabacksMailer.class, mockSettings);
            MOCKS.put(result, null);
            return result;
        }
    }

    @Configuration
    public static class LogBrokerConfig {
        @Bean
        public TskvLogBrokerClient logBrokerClient(MockSettings mockSettings) {
            TskvLogBrokerClient mock = mock(TskvLogBrokerClient.class, mockSettings);
            MOCKS.put(mock, null);
            return mock;
        }
    }

    @Configuration
    public static class UaasClientConfig {
        @Uaas
        @Bean
        public RestTemplate uaasRestTemplate(MockSettings mockSettings) {
            RestTemplate mock = mock(RestTemplate.class, mockSettings);
            MOCKS.put(mock, () -> when(mock.exchange(any(URI.class), eq(HttpMethod.GET), any(), eq(String.class)))
                    .thenReturn(new ResponseEntity<>(HttpStatus.OK))
            );
            return mock;
        }
    }

    @Configuration
    public static class RecommendationsConfig {
        @Bean
        @Recommendations
        public RestTemplate recommendationsRestTemplateMock(MockSettings mockSettings) {
            RestTemplate mock = mock(RestTemplate.class, mockSettings);
            MOCKS.put(mock, null);
            return mock;
        }

        @Bean
        @Recommendations
        public CoreConfigExternal.RecommendationsConfig prepareRecommendationsConfig() {
            return new CoreConfigExternal.RecommendationsConfig(100);
        }
    }

    @Configuration
    public static class AntifraudConfig {
        @Bean
        @Antifraud
        @Primary
        public RestTemplate antifraudRestTemplate(MockSettings mockSettings) {
            RestTemplate mock = mock(RestTemplate.class, mockSettings);
            MOCKS.put(mock, null);
            return mock;
        }

        @Bean
        @Antifraud
        public RestTemplate slowAntifraudRestTemplate(MockSettings mockSettings) {
            RestTemplate mock = mock(RestTemplate.class, mockSettings);
            MOCKS.put(mock, null);
            return mock;
        }
    }

    @Configuration
    public static class JugglerConfig {
        @Bean
        @Primary
        @Default
        @Profile("monitor-mock-test")
        public PushMonitor monitor(MockSettings mockSettings) {
            JugglerInternalPushMonitor monitor = mock(JugglerInternalPushMonitor.class, mockSettings.name("push_mock"));
            MOCKS.put(monitor, null);
            return monitor;
        }

        @Bean
        @Juggler
        public HttpClient jugglerHttpClient() {
            final HttpClient result = mock(HttpClient.class);
            MOCKS.put(result, () -> {
                try {
                    when(result.execute(any())).then(invocation -> {
                        HttpResponse response = DefaultHttpResponseFactory.INSTANCE.newHttpResponse(
                                HttpVersion.HTTP_1_1,
                                200,
                                new BasicHttpContext()
                        );
                        response.setEntity(new StringEntity("", StandardCharsets.UTF_8));
                        return response;
                    });
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            return result;
        }
    }

    @Configuration
    public static class BusinessRulesEngineConfig {
        @Bean
        @BusinessRulesEngine
        public RestTemplate breRestTemplate() {
            final RestTemplate result = mock(RestTemplate.class);
            MOCKS.put(result, null);
            return result;
        }
    }

    @Bean
    public ThrottlingControlService throttlingControlService() {
        return ThrottlingControlService.alwaysSuccess();
    }

    @Configuration
    public static class ClockConfig {
        @Bean
        public ClockForTests clock() {
            return new ClockForTests();
        }
    }

    @Configuration
    public static class YdbConfig {
        @Bean
        public UserPromoDao userPromoDao() {
            final UserPromoDao mock = mock(UserPromoDao.class);
            MOCKS.put(mock, null);
            return mock;
        }

        @Bean
        @Primary
        public UserOrdersDao userOrderDao(MockSettings mockSettings) {
            final UserOrdersDao mock = mock(UserOrdersDao.class, mockSettings);
            MOCKS.put(mock, () ->
                    when(mock.selectByUidWithBFCondition(any())).thenReturn(Collections.emptyList())
            );
            return mock;
        }

        @Bean
        @Primary
        public CashbackOrdersDao cashbackOrdersDao(MockSettings mockSettings) {
            CashbackOrdersDao mock = mock(CashbackOrdersDao.class, mockSettings);
            MOCKS.put(mock, null);
            return mock;
        }

        @Bean
        public PerkAcquisitionDao perkAcquisitionDao() {
            final YdbPerkAcquisitionDaoStub stub = new YdbPerkAcquisitionDaoStub();
            STUBS.add(stub);
            return stub;
        }

        @Bean
        @Primary
        public PersonalPromoPerksDao personalPromoPerksDao() {
            final YdbPersonalPromoPerksStub stub = new YdbPersonalPromoPerksStub();
            STUBS.add(stub);
            return stub;
        }

        @Bean
        @Primary
        public AllUserOrdersDao allUserOrdersDao() {
            final YdbAllUsersOrdersDaoStub stub = new YdbAllUsersOrdersDaoStub();
            STUBS.add(stub);
            return stub;
        }

        @Bean
        @Primary
        public AntifraudOrdersCountCacheDao antifraudOrdersCountCacheDao(Clock clock) {
            YdbAntifraudOrdersCountCacheDaoStub ydbAntifraudOrdersCountCacheDaoStub =
                    new YdbAntifraudOrdersCountCacheDaoStub(clock);
            STUBS.add(ydbAntifraudOrdersCountCacheDaoStub);
            return ydbAntifraudOrdersCountCacheDaoStub;
        }

        @Bean
        @Primary
        public UserAccrualsCacheDao userAccrualsCacheDao() {
            YdbUserAccrualsCacheDaoStub stub = new YdbUserAccrualsCacheDaoStub();
            STUBS.add(stub);
            return stub;
        }

        @Bean
        @Primary
        public UserBlockPromoDao userBlockPromoDao() {
            YdbUserBlockPromoDaoStub stub = new YdbUserBlockPromoDaoStub(ClockForTests.systemUTC());
            STUBS.add(stub);
            return stub;
        }

        @Bean
        @Primary
        public UserReferralPromocodeDao userReferralPromocodeDao() {
            YdbUserReferralPromocodeDaoStub stub = new YdbUserReferralPromocodeDaoStub();
            STUBS.add(stub);
            return stub;
        }

        @Bean
        @YdbClient
        public TableClient ydbClientMock() {
            final TableClient mock = mock(TableClient.class);
            Session sessionMock = mock(Session.class);
            CompletableFuture<Status> statusCompletableFuture = CompletableFuture.completedFuture(Status.SUCCESS);
            @SuppressWarnings("unchecked")
            CompletableFuture<Result<Session>> completableFutureMock = mock(CompletableFuture.class);
            when(completableFutureMock.join()).thenReturn(Result.success(sessionMock));
            when(sessionMock.createTable(anyString(), any())).thenReturn(statusCompletableFuture);
            when(mock.getOrCreateSession(any())).thenReturn(completableFutureMock);
            MOCKS.put(mock, () -> when(mock.createSession()).thenReturn(completableFutureMock));
            return mock;
        }

        @Bean
        public NotificationDao notificationDao() {
            final NotificationDaoStub stub = new NotificationDaoStub();
            STUBS.add(stub);
            return stub;
        }

        @Bean
        @Primary
        public StaticPerkDao staticPerkDao(Clock clock) {
            var stub = new YdbStaticPerkDaoStub(clock);
            STUBS.add(stub);
            return stub;
        }

        @Bean
        @Primary
        public UserBlockPromoDao userBlockPromoDao(Clock clock) {
            var stub = new YdbUserBlockPromoDaoStub(clock);
            STUBS.add(stub);
            return stub;
        }
    }

    @Configuration
    public static class TvmConfiguration {
        @Bean
        @Blackbox
        public TvmTicketProvider mockedBlackboxTvmTicketProvider() {
            TvmTicketProvider tvmTicketProvider = mock(TvmTicketProvider.class);
            MOCKS.put(tvmTicketProvider, null);
            return tvmTicketProvider;
        }

        @Bean
        @Default
        public Tvm2 mockTvm2() {
            final Tvm2 mock = mock(Tvm2.class);
            when(mock.getServiceTicket(anyInt())).thenReturn(Option.empty());
            MOCKS.put(mock, () -> when(mock.getServiceTicket(anyInt())).thenReturn(Option.empty()));
            return mock;
        }

        @Bean
        @TrustPayments
        public Tvm2 trustPaymentsTvm2() {
            final Tvm2 mock = mock(Tvm2.class);
            MOCKS.put(mock, () -> when(mock.getServiceTicket(anyInt())).thenReturn(Option.empty()));
            return mock;
        }
    }

    @Configuration
    public static class DataCampStrollerConfig {
        @Bean
        public CloseableHttpClient mockedHttpClient() {
            CloseableHttpClient httpClient = mock(CloseableHttpClient.class);
            MOCKS.put(httpClient, null);
            return httpClient;
        }

        @Bean
        @Primary
        @Qualifier("dataCampStrollerClient")
        public DataCampStrollerClient mockedDataCampStrollerClient() {
            DataCampStrollerClient dataCampStrollerClient = mock(DataCampStrollerClient.class);
            MOCKS.put(dataCampStrollerClient, null);
            return dataCampStrollerClient;
        }
    }

    @Configuration
    public static class TrustConfig {
        @Bean
        @TrustApi
        public RestTemplate createTrustRestTemplate() {
            final RestTemplate mock = mock(RestTemplate.class);
            List<HttpMessageConverter<?>> converters =
                    new RestTemplate().getMessageConverters();
            MOCKS.put(mock, () ->
                    when(mock.getMessageConverters()).thenReturn(converters)
            );
            return mock;
        }

        @Bean
        @TrustPayments
        public RestTemplate createTrustPaymentsRestTemplate() {
            final RestTemplate mock = mock(RestTemplate.class);
            MOCKS.put(mock, null);
            return mock;
        }
    }

    @Configuration
    public static class StaffConfig {
        @Bean
        @StaffApi
        public RestTemplate staffRestTemplate() {
            final RestTemplate mock = mock(RestTemplate.class);
            MOCKS.put(mock, null);
            return mock;
        }
    }

    @Configuration
    public static class LaasConfig {
        @Bean
        @LaasApi
        public RestTemplate laasRestTemplate(MockSettings mockSettings) {
            RestTemplate mock = mock(RestTemplate.class, mockSettings);
            MOCKS.put(mock, null);
            return mock;
        }

        @Bean
        @Qualifier("laasRestTemplateIntegration")
        public RestTemplate laasRestTemplateIntegration(
                @Value("${market.loyalty.laas.url}") String laasUrl,
                @Value("${market.loyalty.laas.http.max.con.total}") int httpConnTotal,
                @Value("${market.loyalty.laas.max.con.per.route}") int httpConnPerRoute,
                @Value("${market.loyalty.laas.conn.timeout.millis}") int httpConnTimeout,
                @Value("${market.loyalty.laas.read.timeout.millis}") int httpReadTimeout
        ) {
            return new RestTemplateBuilder()
                    .requestFactory(new HttpTrace(Module.LAAS)
                            .setRetryCount(1)
                            .setHttpConnTimeout(httpConnTimeout)
                            .setHttpReadTimeout(httpReadTimeout)
                            .setHttpConnPerRoute(httpConnPerRoute)
                            .setHttpConnTotal(httpConnTotal)
                            .httpConnectionFactoryWithTrace()
                    )
                    .rootUri(laasUrl)
                    .build();
        }

        @Bean
        public CoreConfigExternal.LaasConfig prepareLaasConfig() {
            return new CoreConfigExternal.LaasConfig();
        }
    }

    @Configuration
    public static class SolomonPusherConfig {
        @Bean
        public SolomonPusher mockSolomonPusher() {
            SolomonPusher mock = mock(SolomonPusher.class);
            MOCKS.put(mock, null);
            return mock;
        }
    }

    @Configuration
    public static class BankConfig {
        @Bean
        @BankCashbackCalculatorApi
        public RestTemplate bankCashbackCalculatorRestTemplate(MockSettings mockSettings) {
            RestTemplate mock = mock(RestTemplate.class, mockSettings);
            MOCKS.put(mock, null);
            return mock;
        }

        @Bean
        @BankCashbackCoreApi
        public RestTemplate bankCashbackCoreRestTemplate(MockSettings mockSettings) {
            RestTemplate mock = mock(RestTemplate.class, mockSettings);
            MOCKS.put(mock, null);
            return mock;
        }
    }

    @Configuration
    public static class TagsConfig {
        @Bean
        @Tags
        public RestTemplate tagsRestTemplate(MockSettings mockSettings) {
            RestTemplate mock = mock(RestTemplate.class, mockSettings);
            MOCKS.put(mock, () -> when(mock.exchange(any(RequestEntity.class), any(Class.class)))
                    .thenReturn(ResponseEntity.of(Optional.of(new TagsMatchResponse(List.of("TEST_TAG")))))
            );
            return mock;
        }
    }

    @Configuration
    public static class PersonalConfig {
        @Bean
        @Personal
        public RestTemplate personalRestTemplate(MockSettings mockSettings) {
            RestTemplate mock = mock(RestTemplate.class, mockSettings);
            MOCKS.put(mock, null);
            return mock;
        }
    }
}
