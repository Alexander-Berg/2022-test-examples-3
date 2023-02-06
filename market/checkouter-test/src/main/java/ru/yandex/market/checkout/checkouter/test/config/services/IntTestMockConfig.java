package ru.yandex.market.checkout.checkouter.test.config.services;

import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.extension.Extension;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import ru.yandex.bolts.collection.Option;
import ru.yandex.common.cache.memcached.MemCachedAgent;
import ru.yandex.common.cache.memcached.MemCachedServiceConfig;
import ru.yandex.common.cache.memcached.impl.DefaultMemCachingService;
import ru.yandex.common.geocoder.client.TvmTicketProvider;
import ru.yandex.inside.passport.tvm2.Tvm2;
import ru.yandex.kikimr.persqueue.LogbrokerClientFactory;
import ru.yandex.market.checkout.checkouter.actualization.fetchers.PushApiCartResponseFetcher;
import ru.yandex.market.checkout.checkouter.order.OrderApprove;
import ru.yandex.market.checkout.checkouter.order.delivery.track.DeliveryTrackerService;
import ru.yandex.market.checkout.checkouter.service.combinator.CombinatorGrpcClient;
import ru.yandex.market.checkout.test.MemCachedAgentMockFactory;
import ru.yandex.market.checkout.util.balance.BasketResponseTransformer;
import ru.yandex.market.checkout.util.balance.BasketStatusResponseTransformer;
import ru.yandex.market.checkout.util.datacamp.DatacampConfigurer;
import ru.yandex.market.checkout.util.report.ReportConfigurer;
import ru.yandex.market.checkout.wiremock.DynamicWiremockFactoryBean;
import ru.yandex.market.checkout.wiremock.RandomBalanceTrustIdSupplier;
import ru.yandex.market.checkout.wiremock.RandomInjectingResponseTransformer;
import ru.yandex.market.checkout.wiremock.RandomPositiveIntSupplier;
import ru.yandex.market.mbi.datacamp.stroller.DataCampClient;
import ru.yandex.passport.tvmauth.TvmClient;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

@Configuration
public class IntTestMockConfig {

    /**
     * Шпион актуализации заказа в PushApiConfigurer
     * (Для Fulfillment заказов, так как запрос не идёт в push-api)
     */
    @SpyBean
    private PushApiCartResponseFetcher cartFetcher;

    /**
     * Шпион резервирования заказа в PushApiConfigurer
     * (Для Fulfillment заказов, так как запрос не идёт в push-api)
     */
    @SpyBean
    private OrderApprove orderApprove;

    @SpyBean
    private DeliveryTrackerService deliveryTrackerService;

    private WireMockServer abstractMock() {
        return DynamicWiremockFactoryBean.create();
    }

    private WireMockServer abstractMock(Extension... extensions) {
        return DynamicWiremockFactoryBean.create(extensions);
    }

    private WireMockServer abstractMock(ObjectMapper objectMapper) {
        return DynamicWiremockFactoryBean.create(objectMapper);
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    public WireMockServer reportMock(WireMockServer reportMockWhite) {
        return reportMockWhite;
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    public WireMockServer reportMockWhite() {
        return abstractMock();
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    public WireMockServer reportMockTurbo() {
        return abstractMock();
    }

    @Bean
    public ReportConfigurer reportConfigurer() {
        return new ReportConfigurer(reportMockWhite(), fallbackReportMock());
    }

    @Bean
    public ReportConfigurer reportConfigurerWhite() {
        return new ReportConfigurer(reportMockWhite(), fallbackReportMock());
    }

    @Bean
    public ReportConfigurer reportConfigurerTurbo() {
        return new ReportConfigurer(reportMockWhite(), fallbackReportMock());
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    public WireMockServer fallbackReportMock() {
        return abstractMock();
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    public WireMockServer balanceMock() {
        RandomInjectingResponseTransformer randomInjectingResponseTransformer =
                new RandomInjectingResponseTransformer();
        randomInjectingResponseTransformer.setVariableValueSuppliers(
                Map.of("RND_POS_INT_ID", new RandomPositiveIntSupplier())
        );
        return abstractMock(randomInjectingResponseTransformer);
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    public WireMockServer trustMock() {
        RandomInjectingResponseTransformer transformer = new RandomInjectingResponseTransformer();
        transformer.setVariableValueSuppliers(Map.of(
                "RND_BALANCE_TRUST_ID", new RandomBalanceTrustIdSupplier(),
                "RND_PURCHASE_TOKEN", new RandomBalanceTrustIdSupplier()
        ));
        return abstractMock(transformer, new BasketResponseTransformer(), new BasketStatusResponseTransformer());
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    public WireMockServer trustGatewayMock() {
        return abstractMock();
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    public WireMockServer sberMock() {
        return abstractMock();
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    public WireMockServer pushApiMock() {
        return abstractMock();
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    public WireMockServer persNotifyMock() {
        return abstractMock();
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    public WireMockServer trackerMock() {
        return abstractMock();
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    public WireMockServer translateApiMock() {
        return abstractMock();
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    public WireMockServer marketLoyaltyMock() {
        ObjectMapper marketLoyaltyObjectMapper = Jackson2ObjectMapperBuilder
                .json()
                .defaultViewInclusion(true)
                .timeZone("Europe/Moscow")
                .build();
        return abstractMock(marketLoyaltyObjectMapper);
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    public WireMockServer stockStorageMock() {
        return abstractMock();
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    public WireMockServer ytHttpApiMock() {
        return abstractMock();
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    public WireMockServer shopInfoMock() {
        return abstractMock();
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    public WireMockServer geocoderMock() {
        return abstractMock();
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    public WireMockServer yqlMock() {
        return abstractMock();
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    public WireMockServer refsMock() {
        return abstractMock();
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    public WireMockServer mstatAntifraudOrdersMock() {
        return abstractMock();
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    public WireMockServer perseyPaymentsMock() {
        return abstractMock();
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    public WireMockServer pvzMock() {
        return abstractMock();
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    public WireMockServer postamatMock() {
        return abstractMock();
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    public WireMockServer yaUslugiMock() {
        return abstractMock();
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    public WireMockServer checkErxMock() {
        return abstractMock();
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    public WireMockServer communicationProxyMock() {
        return abstractMock();
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    public WireMockServer marketOmsMock() {
        return abstractMock();
    }

    @Bean
    public MemCachedAgentMockFactory mockFactory() {
        return new MemCachedAgentMockFactory();
    }

    @Bean
    public MemCachedAgent memCachedAgent() {
        return mockFactory().createMemCachedAgentMock();
    }

    @Bean
    public MemCachedServiceConfig memCachedServiceConfig() {
        MemCachedServiceConfig memCachedServiceConfig = new MemCachedServiceConfig();
        memCachedServiceConfig.setServiceName("checkouter");
        memCachedServiceConfig.setDefaultCacheTime(3600);
        return memCachedServiceConfig;
    }

    @Bean
    public DefaultMemCachingService memCachingService() {
        DefaultMemCachingService defaultMemCachingService = new DefaultMemCachingService();
        defaultMemCachingService.setMemCachedAgent(memCachedAgent());
        return defaultMemCachingService;
    }

    @Bean
    public LogbrokerClientFactory logbrokerClientFactory() {
        return Mockito.mock(LogbrokerClientFactory.class);
    }

    @Bean
    public LogbrokerClientFactory lbkxClientFactory() {
        return Mockito.mock(LogbrokerClientFactory.class);
    }

    @Bean
    public Tvm2 tvm2() {
        Tvm2 mock = Mockito.mock(Tvm2.class);
        when(mock.getServiceTicket(anyInt()))
                .thenReturn(Option.of("service_ticket"));
        return mock;
    }

    @Bean
    public TvmTicketProvider tvmTicketProvider() {
        return Mockito.mock(TvmTicketProvider.class);
    }

    @Bean
    public TvmClient tvmClient() {
        return Mockito.mock(TvmClient.class);
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    public WireMockServer geobaseMock() {
        return abstractMock();
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    public WireMockServer abcMock() {
        return abstractMock();
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    public WireMockServer yaLavkaDeliveryServiceMock() {
        return abstractMock();
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    public WireMockServer bnplMock() {
        return abstractMock();
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    public WireMockServer mediabillingMock() {
        return abstractMock();
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    public WireMockServer saturnMock() {
        return abstractMock();
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    public WireMockServer combinatorMock() {
        return abstractMock();
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    public WireMockServer b2bCustomersMock() {
        return abstractMock();
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    public WireMockServer antispamAntifraudMock() {
        return abstractMock();
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    public WireMockServer axaptaMock() {
        return abstractMock();
    }

    @Bean
    public DataCampClient dataCampShopClient() {
        return Mockito.mock(DataCampClient.class);
    }

    @Bean
    public CombinatorGrpcClient combinatorGrpcClient() {
        return Mockito.mock(CombinatorGrpcClient.class);
    }

    @Bean
    public DatacampConfigurer datacampConfigurer(DataCampClient dataCampClient) {
        return new DatacampConfigurer(dataCampClient);
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    public WireMockServer personalMock() {
        return abstractMock();
    }

}
