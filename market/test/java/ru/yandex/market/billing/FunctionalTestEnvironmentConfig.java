package ru.yandex.market.billing;

import java.time.Clock;
import java.time.ZoneId;
import java.util.TimeZone;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.sql.DataSource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.cache.Cache;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.bolts.collection.Option;
import ru.yandex.common.util.date.TestableClock;
import ru.yandex.inside.passport.tvm2.Tvm2;
import ru.yandex.market.abo.api.client.AboPublicRestClient;
import ru.yandex.market.billing.agency_commission.AgencyCommissionTestConfig;
import ru.yandex.market.billing.calendar.YandexCalendarApiTestConfig;
import ru.yandex.market.billing.checkout.logbroker.LogbrokerCheckouterConsumerTestConfig;
import ru.yandex.market.billing.checkout.logbroker.LogbrokerGetOrdersTestConfig;
import ru.yandex.market.billing.tasks.distribution.TestDistributionConfig;
import ru.yandex.market.billing.tasks.shopdata.ClassPathReaderProvider;
import ru.yandex.market.billing.tasks.shopdata.ConfigurationReaderProvider;
import ru.yandex.market.checkout.checkouter.jackson.ObjectMapperTimeZoneSetter;
import ru.yandex.market.common.mds.s3.client.service.api.DirectHistoryMdsS3Client;
import ru.yandex.market.common.mds.s3.client.service.api.DirectMdsS3Client;
import ru.yandex.market.common.mds.s3.client.service.api.MdsS3Client;
import ru.yandex.market.common.mds.s3.client.service.api.NamedHistoryMdsS3Client;
import ru.yandex.market.common.mds.s3.spring.db.ResourceConfigurationDao;
import ru.yandex.market.core.TestClock;
import ru.yandex.market.core.axapta.AxaptaRealSupplierDao;
import ru.yandex.market.core.balance.BalanceService;
import ru.yandex.market.core.campaign.model.PartnerId;
import ru.yandex.market.core.config.ShopsDataTestConfig;
import ru.yandex.market.core.database.EmbeddedPostgresConfig;
import ru.yandex.market.core.datacamp.DataCampCoreServicesConfig;
import ru.yandex.market.core.datacamp.feed.FeedProcessorUpdateRequestEvent;
import ru.yandex.market.core.delivery.AsyncTarifficatorService;
import ru.yandex.market.core.environment.CompareAndUpdateEnvironmentService;
import ru.yandex.market.core.environment.EnvironmentAwareDatesProcessingService;
import ru.yandex.market.core.feed.supplier.SupplierXlsHelper;
import ru.yandex.market.core.fulfillment.tariff.TariffsService;
import ru.yandex.market.core.history.HistoryService;
import ru.yandex.market.core.npd.client.IntegrationNpdRetrofitService;
import ru.yandex.market.core.param.ParamService;
import ru.yandex.market.core.partner.PartnerDao;
import ru.yandex.market.core.partner.PartnerService;
import ru.yandex.market.core.post.RusPostAuthClient;
import ru.yandex.market.core.post.RusPostContractClient;
import ru.yandex.market.core.replenishment.supplier.PilotSupplierYtDao;
import ru.yandex.market.core.solomon.SolomonTestJvmConfig;
import ru.yandex.market.core.supplier.state.PartnerServiceLinkLogbrokerEvent;
import ru.yandex.market.core.util.DateTimes;
import ru.yandex.market.core.yt.indexer.YtFactory;
import ru.yandex.market.id.MarketIdServiceGrpc;
import ru.yandex.market.indexer.problem.strategy.impl.CommonFeedsStrategy;
import ru.yandex.market.indexer.yt.generation.MockYtErrorsProviderFactory;
import ru.yandex.market.indexer.yt.generation.YtErrorsProviderFactory;
import ru.yandex.market.indexer.yt.generation.YtErrorsProviderOptions;
import ru.yandex.market.integration.npd.client.api.ApplicationApi;
import ru.yandex.market.logbroker.LogbrokerEventPublisher;
import ru.yandex.market.logbroker.LogbrokerService;
import ru.yandex.market.mbi.bpmn.client.MbiBpmnClient;
import ru.yandex.market.mbi.data.PartnerChangesDataSender;
import ru.yandex.market.mbi.datacamp.saas.SaasService;
import ru.yandex.market.mbi.environment.EnvironmentService;
import ru.yandex.market.mbi.open.api.client.MbiOpenApiClient;
import ru.yandex.market.notification.telegram.bot.client.PartnerBotRestClient;
import ru.yandex.yt.ytclient.rpc.RpcOptions;

import static org.mockito.AdditionalAnswers.delegatesTo;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static ru.yandex.market.billing.config.MbiOpenApiClientConfig.MBI_OPEN_API_CLIENT_BEAN_NAME;

@ParametersAreNonnullByDefault
@Configuration
@Import({
        SolomonTestJvmConfig.class,
        LogbrokerGetOrdersTestConfig.class,
        ShopsDataTestConfig.class,
        LogbrokerCheckouterConsumerTestConfig.class,
        DataCampCoreServicesConfig.class,
        YandexCalendarApiTestConfig.class,
        TestDistributionConfig.class,
        EmbeddedPostgresConfig.class,
        AgencyCommissionTestConfig.class
})
public class FunctionalTestEnvironmentConfig {
    public FunctionalTestEnvironmentConfig() {
        System.setProperty("simple.host.name", "localhost");
    }

    @Bean({
            "aliasDataSource",
            "readOnlyDataSource",
            "shopDataSource",
            "mbiStatsDataSource",
            "mdbMbiDataSourceRO"
    })
    public DataSource aliasDataSource(DataSource dataSource) {
        return dataSource;
    }

    @Bean({
            "aliasJdbcTemplate",
            "mbiStatsJdbcTemplate",
            "readOnlyJdbcTemplate",
            "shopJdbcTemplate",
    })
    public JdbcTemplate aliasJdbcTemplate(JdbcTemplate jdbcTemplate) {
        return jdbcTemplate;
    }

    @Bean({
            "mockJdbcTemplate",
            "yqlJdbcTemplate",
            "yqlReplicaJdbcTemplate",
    })
    public JdbcTemplate mockJdbcTemplate() {
        return mock(JdbcTemplate.class);
    }

    @Bean({
            "aliasNamedParameterJdbcTemplate",
            "namedMbiStatsJdbcTemplate",
            "yqlNamedParameterJdbcTemplate",
            "billingPgJdbcTemplate"
    })
    public NamedParameterJdbcTemplate aliasNamedParameterJdbcTemplate(
            NamedParameterJdbcTemplate namedParameterJdbcTemplate
    ) {
        return namedParameterJdbcTemplate;
    }

    @Bean({
            "mockPlatformTransactionManager",
            "mstApiClientTransactionManager"
    })
    public PlatformTransactionManager mockPlatformTransactionManager() {
        return mock(PlatformTransactionManager.class);
    }

    @Bean({
            "aliasTransactionTemplate",
            "locksTransactionTemplate",
            "billingPgTransactionTemplate",
    })
    public TransactionTemplate aliasTransactionTemplate(TransactionTemplate transactionTemplate) {
        return transactionTemplate;
    }

    @Bean({
            "mockTransactionTemplate",
            "mbiStatsTransactionTemplate",
    })
    public TransactionTemplate mockTransactionTemplate() {
        return mock(TransactionTemplate.class);
    }

    @Bean
    public AxaptaRealSupplierDao axaptaRealSupplierDao() {
        return mock(AxaptaRealSupplierDao.class);
    }

    @Bean
    public ResourceConfigurationDao resourceConfigurationDao() {
        return mock(ResourceConfigurationDao.class);
    }

    @Bean
    public DirectMdsS3Client directMdsS3Client() {
        return mock(DirectMdsS3Client.class);
    }

    @Bean
    public NamedHistoryMdsS3Client namedHistoryMdsS3Client() {
        return mock(NamedHistoryMdsS3Client.class);
    }

    @Bean
    public DirectHistoryMdsS3Client aboDirectHistoryMdsS3Client() {
        return mock(DirectHistoryMdsS3Client.class);
    }

    @Bean
    public MdsS3Client mdsS3Client() {
        return mock(MdsS3Client.class);
    }

    @Bean
    public ObjectMapperTimeZoneSetter checkouterAnnotationObjectMapperTimeZoneSetter(
            ObjectMapper checkouterAnnotationObjectMapper
    ) {
        var timeZoneSetter = new ObjectMapperTimeZoneSetter();
        timeZoneSetter.setObjectMapper(checkouterAnnotationObjectMapper);
        timeZoneSetter.setTimeZone(TimeZone.getDefault());
        timeZoneSetter.afterPropertiesSet();
        return timeZoneSetter;
    }

    @Bean
    public Resource supplierXlsTemplate() {
        return new ClassPathResource("supplier/feed/marketplace-catalog.xlsm");
    }

    @Bean(name = {
            "mockTvm2",
            "mbiTvm",
            "mbiReportGeneratorTvm"
    })
    public Tvm2 mockTvm2() {
        var tvm2 = mock(Tvm2.class);
        when(tvm2.getServiceTicket(anyInt())).thenReturn(Option.of("test-tvm-ticket"));
        return tvm2;
    }

    @Bean
    public BalanceService impatientBalanceService() {
        return mock(BalanceService.class);
    }

    @Bean
    public String getGrpcServerName() {
        return InProcessServerBuilder.generateName();
    }

    @Bean
    public ManagedChannel managedChannel() {
        return InProcessChannelBuilder.forName(getGrpcServerName()).directExecutor().build();
    }

    @Bean(initMethod = "start", destroyMethod = "shutdown")
    public Server marketIdServer() {
        return InProcessServerBuilder
                .forName(getGrpcServerName()).directExecutor().addService(marketIdServiceImplBase()).build();
    }

    @Bean
    public MarketIdServiceGrpc.MarketIdServiceImplBase marketIdServiceImplBase() {
        return mock(MarketIdServiceGrpc.MarketIdServiceImplBase.class,
                delegatesTo(new MarketIdServiceGrpc.MarketIdServiceImplBase() {
                }));
    }

    @Bean
    public Clock clock() {
        return spy(new TestClock());
    }

    @Bean
    public Clock logbrokerOrderEventsClock() {
        return Clock.fixed(DateTimes.toInstantAtDefaultTz(2020, 11, 17), ZoneId.systemDefault());
    }

    @Bean
    public Clock deleteStrategyCheckClock() {
        return Clock.fixed(DateTimes.toInstantAtDefaultTz(2019, 10, 28, 12, 0, 0), ZoneId.systemDefault());
    }

    @Bean
    public PartnerService partnerService(
            PartnerDao partnerDao, ParamService paramService, HistoryService historyService,
            PartnerChangesDataSender partnerChangesDataSender
    ) {
        @SuppressWarnings("unchecked") final Cache<Long, PartnerId> noCache = Mockito.mock(Cache.class);
        return new PartnerService(partnerDao, paramService, historyService, noCache,
                partnerChangesDataSender);
    }

    @Bean
    @Primary
    public CommonFeedsStrategy commonStrategySpy(CommonFeedsStrategy commonStrategy) {
        return spy(commonStrategy);
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
    @Primary
    public YtErrorsProviderFactory ytErrorsProviderFactory(RpcOptions ytErrorsProviderRpcOptions,
                                                           YtErrorsProviderOptions ytErrorsProviderOptions,
                                                           YtFactory ytFactory,
                                                           NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        var factory = new MockYtErrorsProviderFactory(
                ytErrorsProviderRpcOptions,
                ytErrorsProviderOptions,
                ytFactory,
                namedParameterJdbcTemplate);
        return spy(factory);
    }

    @Bean
    @Primary
    public YtFactory ytFactorySpy(YtFactory ytFactory) {
        return spy(ytFactory);
    }

    @Bean
    @Primary
    public TariffsService clientTariffsService() {
        return mock(TariffsService.class);
    }

    @Bean
    public PartnerBotRestClient partnerBotRestClient() {
        return mock(PartnerBotRestClient.class);
    }

    @Bean
    @Primary
    public AboPublicRestClient aboPublicRestClient() {
        return mock(AboPublicRestClient.class);
    }

    @Bean
    public PilotSupplierYtDao pilotSupplierYtDao() {
        return mock(PilotSupplierYtDao.class);
    }

    @Bean
    public AsyncTarifficatorService asyncTarifficatorService() {
        return mock(AsyncTarifficatorService.class);
    }

    @Bean
    public SupplierXlsHelper unitedSupplierXlsHelper() {
        return spy(new SupplierXlsHelper(
                new ClassPathResource("supplier/feed/marketplace-catalog-united.xlsx"),
                ".xlsx"
        ));
    }

    @Bean
    public LogbrokerEventPublisher<FeedProcessorUpdateRequestEvent> feedProcessorUpdateLogbrokerEventPublisher() {
        return mock(LogbrokerEventPublisher.class);
    }

    @Bean(name = "configurationReaderProvider")
    public ConfigurationReaderProvider testConfigurationReaderProvider() {
        return new ClassPathReaderProvider();
    }

    @Bean
    public LogbrokerService mboPartnerExportLogbrokerService() {
        return mock(LogbrokerService.class);
    }

    @Bean(MBI_OPEN_API_CLIENT_BEAN_NAME)
    public MbiOpenApiClient mbiOpenApiClient() {
        return mock(MbiOpenApiClient.class);
    }

    @Bean(name = {
            "datesProcessingService",
            "environmentAwareDatesProcessingService",
            "billingPgEnvironmentAwareDaysProcessorService"
    })
    @Primary
    public EnvironmentAwareDatesProcessingService environmentAwareDatesProcessingService(
            TestableClock clock,
            EnvironmentService environmentService,
            CompareAndUpdateEnvironmentService compareAndUpdateEnvironmentService
    ) {
        return new EnvironmentAwareDatesProcessingService(clock, environmentService,
                compareAndUpdateEnvironmentService);
    }

    @Bean
    public LogbrokerEventPublisher<PartnerServiceLinkLogbrokerEvent> partnerFfLinkLbProducer() {
        return mock(LogbrokerEventPublisher.class);
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

    @Bean
    public SaasService saasService() {
        return mock(SaasService.class);
    }

    @Bean
    @Primary
    public MbiBpmnClient mbiBpmnClient() {
        return Mockito.mock(MbiBpmnClient.class);
    }
}
