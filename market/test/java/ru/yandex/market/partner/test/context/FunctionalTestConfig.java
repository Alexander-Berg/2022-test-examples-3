package ru.yandex.market.partner.test.context;

import java.time.Clock;
import java.time.ZoneId;
import java.util.Collections;
import java.util.UUID;
import java.util.function.Supplier;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.servlet.ServletContext;
import javax.sql.DataSource;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.grpc.ManagedChannel;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.swagger.annotations.Api;
import org.apache.http.client.HttpClient;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.NetworkConnector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHandler;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.transaction.support.TransactionTemplate;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;

import ru.yandex.common.geocoder.client.GeoClient;
import ru.yandex.common.util.date.TestableClock;
import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.tables.YTableEntryType;
import ru.yandex.inside.yt.kosher.tables.YtTables;
import ru.yandex.market.adv.promo.client.AdvPromoClient;
import ru.yandex.market.core.asyncreport.DisabledAsyncReportService;
import ru.yandex.market.core.asyncreport.ReportsDao;
import ru.yandex.market.core.asyncreport.ReportsService;
import ru.yandex.market.core.asyncreport.ReportsServiceSettings;
import ru.yandex.market.core.asyncreport.model.ReportsType;
import ru.yandex.market.core.balance.ExternalBalanceService;
import ru.yandex.market.core.business.BusinessMetrikaTestConfig;
import ru.yandex.market.core.config.IntegrationNpdRetrofitClientTestConfig;
import ru.yandex.market.core.config.LogbrokerChangesEventConfig;
import ru.yandex.market.core.config.PromoOfferCoreFunctionalTestConfig;
import ru.yandex.market.core.config.ShopsDataTestConfig;
import ru.yandex.market.core.config.TarifficatorClientFunctionalTestConfig;
import ru.yandex.market.core.database.EmbeddedPostgresConfig;
import ru.yandex.market.core.datacamp.DataCampCoreServicesConfig;
import ru.yandex.market.core.datacamp.feed.FeedProcessorUpdateRequestEvent;
import ru.yandex.market.core.delivery.AsyncTarifficatorService;
import ru.yandex.market.core.delivery.LogisticPointInfoYtDao;
import ru.yandex.market.core.delivery.label.metrics.LabelGenerationProtoLBEvent;
import ru.yandex.market.core.feed.supplier.SupplierXlsHelper;
import ru.yandex.market.core.fulfillment.tariff.TariffsService;
import ru.yandex.market.core.fulfillment.tariff.YtTariffsService;
import ru.yandex.market.core.order.ServiceFeePartitionDao;
import ru.yandex.market.core.order.returns.os.OrderServiceReturnDao;
import ru.yandex.market.core.partner.fulfillment.yt.SalesDynamicsYtStorage;
import ru.yandex.market.core.post.RusPostAuthClient;
import ru.yandex.market.core.post.RusPostContractClient;
import ru.yandex.market.core.security.MockSecManager;
import ru.yandex.market.core.solomon.SolomonTestJvmConfig;
import ru.yandex.market.core.stockstorage.StockStorageSearchClientConfig;
import ru.yandex.market.core.supplier.SupplierExposedActService;
import ru.yandex.market.core.supplier.SupplierExposedActServiceImpl;
import ru.yandex.market.core.supplier.dao.SupplierExposedActDao;
import ru.yandex.market.core.supplier.state.PartnerServiceLinkLogbrokerEvent;
import ru.yandex.market.core.util.DateTimes;
import ru.yandex.market.fulfillment.stockstorage.client.StockStorageSearchClient;
import ru.yandex.market.id.MarketIdServiceGrpc;
import ru.yandex.market.logbroker.LogbrokerEventPublisher;
import ru.yandex.market.logbroker.LogbrokerService;
import ru.yandex.market.mbi.api.billing.client.MbiBillingClient;
import ru.yandex.market.mbi.core.HttpIndexerApiClient;
import ru.yandex.market.mbi.core.IndexerApiClient;
import ru.yandex.market.mbi.core.ff4shops.FF4ShopsOpenApiClient;
import ru.yandex.market.mbi.environment.EnvironmentService;
import ru.yandex.market.mbi.tariffs.client.TariffClientConfiguration;
import ru.yandex.market.mbi.tariffs.client.TariffClientMetaConverter;
import ru.yandex.market.mbi.tariffs.client.TariffTvm2ClientProperties;
import ru.yandex.market.mbi.tariffs.client.api.TariffsApi;
import ru.yandex.market.mbi.yt.YtCluster;
import ru.yandex.market.mbi.yt.YtTemplate;
import ru.yandex.market.partner.mvc.controller.periodic_survey.PeriodicSurveyTestConfig;
import ru.yandex.market.partner.security.checker.SimpleStaticDomainAuthorityChecker;
import ru.yandex.market.partner.servant.PartnerDefaultRequestHandler;
import ru.yandex.market.personal_market.PersonalMarketService;
import ru.yandex.market.security.AuthorityChecker;
import ru.yandex.market.security.BatchAuthoritiesLoader;
import ru.yandex.market.security.CheckerResolver;
import ru.yandex.market.security.SecManager;
import ru.yandex.market.security.core.CachedKampferFactory;
import ru.yandex.market.security.core.MainSecManager;
import ru.yandex.market.security.core.SimpleAuthoritiesLoader;
import ru.yandex.yadoc.YaDocClient;

import static org.mockito.AdditionalAnswers.delegatesTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

@ParametersAreNonnullByDefault
@Import({
        EmbeddedPostgresConfig.class,
        SolomonTestJvmConfig.class,
        ShopsDataTestConfig.class,
        DataCampCoreServicesConfig.class,
        PeriodicSurveyTestConfig.class,
        BusinessMetrikaTestConfig.class,
        TarifficatorClientFunctionalTestConfig.class,
        StockStorageSearchClientConfig.class,
        PromoOfferCoreFunctionalTestConfig.class,
        TariffClientConfiguration.class,
        IntegrationNpdRetrofitClientTestConfig.class,
        LogbrokerChangesEventConfig.class
})
@Configuration
public class FunctionalTestConfig {
    private static final String TEST_INDEXER_API_URL = "http://active.idxapi.tst.vs.market.yandex.net:29334/";
    private static final int REPORTS_QUEUE_LIMIT = 10;

    @Autowired
    private PartnerDefaultRequestHandler defaultHttpRequestHandler;

    @Autowired
    private Handler mvcHandler;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private ReportsDao<ReportsType> reportsDao;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EnvironmentService environmentService;

    @Bean
    public RetryTemplate retryTemplate() {
        return new RetryTemplate();
    }

    @Bean({"mockIndexerApiClient", "indexerApiClient"})
    public IndexerApiClient indexerApiClient() {
        return new HttpIndexerApiClient(
                indexerApiClientHttpClient(),
                TEST_INDEXER_API_URL,
                TEST_INDEXER_API_URL,
                retryTemplate()
        );
    }

    @Bean({"mockIndexerApiClientHttpClient", "indexerApiClientHttpClient"})
    public HttpClient indexerApiClientHttpClient() {
        return mock(HttpClient.class);
    }

    @Bean
    ServletContext servletContext() {
        return ((ServletHandler) mvcHandler).getServletContext();
    }

    @Bean
    @DependsOn("httpServerInitializer")
    public String baseUrl() {
        Server jettyServer = defaultHttpRequestHandler.getServer();
        Connector[] connectors = jettyServer.getConnectors();
        Connector firstConnector = connectors[0];

        NetworkConnector networkConnector = (NetworkConnector) firstConnector;
        int actualPort = networkConnector.getLocalPort();

        return "http://localhost:" + actualPort;
    }

    @Bean
    public Docket docket() {
        return new Docket(DocumentationType.SWAGGER_2)
                .select()
                .apis(RequestHandlerSelectors.basePackage("ru.yandex.market.partner.mvc.controller.offer.mapping"))
                .apis(RequestHandlerSelectors.withClassAnnotation(Api.class))
                .build()
                .apiInfo(new ApiInfoBuilder()
                        .title("MBI-Partner backend application")
                        .description("")
                        .contact(new Contact("MBI", "https://wiki.yandex-team.ru/MBI/", "mbi@yandex-team.ru"))
                        .build());
    }

    @Bean
    public ReportsService<ReportsType> asyncReportsService() {
        return new ReportsService<>(
                new ReportsServiceSettings.Builder<ReportsType>().setReportsQueueLimit(REPORTS_QUEUE_LIMIT).build(),
                reportsDao,
                transactionTemplate,
                () -> "777",
                Clock.fixed(
                        DateTimes.toInstantAtDefaultTz(2019, 6, 28, 10, 0, 0),
                        ZoneId.systemDefault()
                ),
                new DisabledAsyncReportService<>(jdbcTemplate),
                environmentService
        );
    }

    @Bean
    public String getGrpcServerName() {
        return InProcessServerBuilder.generateName();
    }

    @Bean
    public MarketIdServiceGrpc.MarketIdServiceImplBase marketIdServiceImplBase() {
        return mock(MarketIdServiceGrpc.MarketIdServiceImplBase.class,
                delegatesTo(new MarketIdServiceGrpc.MarketIdServiceImplBase() {
                }));
    }

    @Bean
    public ManagedChannel managedChannel() {
        return InProcessChannelBuilder
                .forName(getGrpcServerName())
                .directExecutor()
                .build();
    }

    @Bean(initMethod = "start", destroyMethod = "shutdown")
    public io.grpc.Server marketIdServer() {
        return InProcessServerBuilder
                .forName(getGrpcServerName())
                .directExecutor()
                .addService(marketIdServiceImplBase())
                .build();
    }

    @Bean
    public Clock clock() {
        return spy(new TestableClock());
    }

    @Bean
    public RusPostAuthClient rusPostOauthClient() {
        return mock(RusPostAuthClient.class);
    }

    @Bean
    public RusPostContractClient rusPostContractClient() {
        return mock(RusPostContractClient.class);
    }

    @Bean
    public SupplierExposedActService supplierExposedActService(SupplierExposedActDao supplierExposedActDao) {
        return new SupplierExposedActServiceImpl(
                supplierExposedActDao,
                Clock.fixed(DateTimes.toInstantAtDefaultTz(2020, 2, 15), ZoneId.systemDefault())
        );
    }

    @Bean
    public GeoClient geoClient() {
        return mock(GeoClient.class);
    }

    @Bean
    public SalesDynamicsYtStorage salesDynamicsYtStorage() {
        return mock(SalesDynamicsYtStorage.class);
    }

    @Bean
    public YaDocClient yaDocClient() {
        return mock(YaDocClient.class);
    }

    @Bean
    public LogbrokerService mboPartnerExportLogbrokerService() {
        return mock(LogbrokerService.class);
    }

    @Bean
    public TariffTvm2ClientProperties tariffTvm2ClientProperties() {
        return mock(TariffTvm2ClientProperties.class);
    }

    @Bean
    public Yt tariffsServiceYt(@Autowired ObjectMapper tariffClientObjectMapper) {
        var tariffsServiceYt = mock(Yt.class);
        YtTables ytTables = Mockito.mock(YtTables.class);
        when(ytTables.read(any(YPath.class), any(YTableEntryType.class))).thenReturn(
                new YtResponseReaderFromJson("json/TarifficatorController.testGetFBSYtContent.json",
                        tariffClientObjectMapper));
        when(tariffsServiceYt.tables()).thenReturn(ytTables);
        return tariffsServiceYt;
    }

    @Bean
    public YtTariffsService ytTariffsService(
            RetryTemplate retryTemplate,
            @Qualifier("tariffsServiceYt") Yt yt,
            TariffClientMetaConverter tariffClientMetaConverter,
            EnvironmentService environmentService) {
        return new YtTariffsService("//dummy", retryTemplate,
                new YtTemplate(new YtCluster("dummy", yt)), tariffClientMetaConverter, environmentService);
    }

    @Bean
    public TariffsService tariffsService(@Autowired TariffClientMetaConverter metaConverter,
                                         @Autowired RetryTemplate retryTemplate,
                                         @Autowired YtTariffsService ytTariffsService) {
        return new TariffsService(mock(TariffsApi.class), mock(ObjectMapper.class), metaConverter,
                retryTemplate, ytTariffsService);
    }

    @Bean
    public Supplier<UUID> uuidSupplier(Supplier<UUID> uuidSupplier) {
        return spy(uuidSupplier);
    }

    @Bean({"testSecManager", "secManager"})
    public SecManager secManager() {
        return spy(MockSecManager.class);
    }

    @Bean({"testSecurityServiceSecManager", "securityServiceSecManager"})
    public SecManager securityServiceSecManager(
            CheckerResolver checkerResolver,
            @Value("${javasec.security.domain}") String domain,
            @Value("${javasec.cache.ttl}") int cacheTtl,
            DataSource readOnlyBillingDataSource
    ) {
        MainSecManager secManager = new MainSecManager();
        secManager.setDomain(domain);
        secManager.setCheckerResolver(checkerResolver);
        SimpleAuthoritiesLoader simpleAuthoritiesLoader = new SimpleAuthoritiesLoader();
        simpleAuthoritiesLoader.setKampferFactory(new CachedKampferFactory(readOnlyBillingDataSource, cacheTtl));
        secManager.setAuthoritiesLoaders(Collections.singletonList(simpleAuthoritiesLoader));
        return secManager;
    }

    @Bean
    public StockStorageSearchClient stockStorageSearchClient() {
        return mock(StockStorageSearchClient.class);
    }

    @Bean
    public AsyncTarifficatorService asyncTarifficatorService() {
        return mock(AsyncTarifficatorService.class);
    }

    @Bean
    public LogisticPointInfoYtDao logisticPointInfoYtDao() {
        return mock(LogisticPointInfoYtDao.class);
    }

    @Bean
    public SupplierXlsHelper unitedSupplierXlsHelper() {
        return spy(new SupplierXlsHelper(
                new ClassPathResource("supplier/feed/marketplace-catalog-united.xlsx"),
                ".xlsx"
        ));
    }

    @Bean
    public MbiBillingClient mbiBillingClient() {
        return mock(MbiBillingClient.class);
    }

    @Bean
    @Primary
    public BatchAuthoritiesLoader operationAuthoritiesCachedService(DataSource dataSource) {
        SimpleAuthoritiesLoader simpleAuthoritiesLoader = new SimpleAuthoritiesLoader();
        simpleAuthoritiesLoader.setKampferFactory(new CachedKampferFactory(dataSource, 10));
        return simpleAuthoritiesLoader;
    }

    @Bean
    @Primary
    public AuthorityChecker staticDomainAuthorityChecker() {
        return new SimpleStaticDomainAuthorityChecker(jdbcTemplate);
    }

    @Bean(name = {
            "externalBalanceService",
            "balanceContactService",
            "balanceService",
            "impatientBalanceService",
            "patientBalanceService",
    })
    public ExternalBalanceService externalBalanceService() {
        return mock(ExternalBalanceService.class);
    }

    @Bean
    public LogbrokerEventPublisher<FeedProcessorUpdateRequestEvent> feedProcessorUpdateLogbrokerEventPublisher() {
        return mock(LogbrokerEventPublisher.class);
    }

    @Bean
    public LogbrokerEventPublisher<LabelGenerationProtoLBEvent> logbrokerLabelGenerateEventPublisher() {
        return mock(LogbrokerEventPublisher.class);
    }

    @Bean
    public Supplier<Boolean> useNewInIndexCalculationLogic() {
        return () -> true;
    }

    @Bean
    public OrderServiceReturnDao orderServiceReturnDao() {
        return mock(OrderServiceReturnDao.class);
    }

    @Bean
    public LogbrokerEventPublisher<PartnerServiceLinkLogbrokerEvent> partnerFfLinkLbProducer() {
        return mock(LogbrokerEventPublisher.class);
    }

    @Bean
    public FF4ShopsOpenApiClient ff4ShopsOpenApiClient() {
        return mock(FF4ShopsOpenApiClient.class);
    }

    @Bean
    public PersonalMarketService personalMarketService() {
        return mock(PersonalMarketService.class);
    }

    @Bean
    public ServiceFeePartitionDao serviceFeePartitionDao(NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        return new ServiceFeePartitionDao(namedParameterJdbcTemplate);
    }

    @Bean
    public AdvPromoClient advPromoClient() {
        return mock(AdvPromoClient.class);
    }
}
