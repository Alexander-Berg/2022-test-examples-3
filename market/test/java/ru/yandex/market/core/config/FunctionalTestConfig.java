package ru.yandex.market.core.config;

import java.net.MalformedURLException;
import java.time.Clock;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

import javax.annotation.Nonnull;
import javax.sql.DataSource;

import com.amazonaws.services.s3.AmazonS3;
import com.google.common.cache.Cache;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.support.AbstractBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import ru.market.partner.notification.client.PartnerNotificationClient;

import ru.yandex.common.cache.memcached.MemCachedTransactionTemplate;
import ru.yandex.common.cache.memcached.impl.TransactionalMemCachedAgent;
import ru.yandex.common.geocoder.client.GeoClient;
import ru.yandex.common.transaction.ListenableTransactionTemplate;
import ru.yandex.common.transaction.LocalTransactionListener;
import ru.yandex.common.util.xmlrpc.XmlRPCServiceFactory;
import ru.yandex.inside.passport.tvm2.Tvm2;
import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.market.api.billing.PartnerContractOptionsConfig;
import ru.yandex.market.checkout.checkouter.client.CheckouterAPI;
import ru.yandex.market.checkout.checkouter.client.CheckouterShopApi;
import ru.yandex.market.checkout.pushapi.client.PushApi;
import ru.yandex.market.common.balance.xmlrpc.Balance2XmlRPCServiceFactory;
import ru.yandex.market.common.bunker.model.route.RouteContent;
import ru.yandex.market.common.mds.s3.client.service.api.MdsS3Client;
import ru.yandex.market.common.mds.s3.client.service.api.impl.MdsS3ClientImpl;
import ru.yandex.market.common.mds.s3.client.service.factory.ResourceLocationFactory;
import ru.yandex.market.common.mds.s3.spring.configuration.MdsS3BasicConfiguration;
import ru.yandex.market.common.mds.s3.spring.db.ResourceConfigurationDao;
import ru.yandex.market.common.report.AsyncMarketReportService;
import ru.yandex.market.common.report.DefaultAsyncMarketReportService;
import ru.yandex.market.core.MbiCoreConfig;
import ru.yandex.market.core.abo._public.config.AboPublicServiceConfig;
import ru.yandex.market.core.agency.AgencyService;
import ru.yandex.market.core.asyncreport.ReportsConfig;
import ru.yandex.market.core.asyncreport.yt.AsyncReportYtConfig;
import ru.yandex.market.core.balance.BalanceContactService;
import ru.yandex.market.core.balance.BalanceServiceExecutor;
import ru.yandex.market.core.balance.ExternalBalanceService;
import ru.yandex.market.core.billing.BillingService;
import ru.yandex.market.core.billing.DbBillingService;
import ru.yandex.market.core.billing.config.OrderBillingConfig;
import ru.yandex.market.core.billing.dao.BillingDao;
import ru.yandex.market.core.bunker.dao.BunkerDao;
import ru.yandex.market.core.bunker.dao.CachedBunkerDao;
import ru.yandex.market.core.bunker.dao.RoutesBunkerDao;
import ru.yandex.market.core.business.BusinessMetrikaTestConfig;
import ru.yandex.market.core.business.BusinessService;
import ru.yandex.market.core.business.migration.DatacampBusinessMigrationConfig;
import ru.yandex.market.core.campaign.cache.MemCachedCampaignService;
import ru.yandex.market.core.campaign.model.PartnerId;
import ru.yandex.market.core.client.ClientConfig;
import ru.yandex.market.core.client.remove.RemoveClientEnvironmentService;
import ru.yandex.market.core.contact.db.LinkDao;
import ru.yandex.market.core.datacamp.DataCampCoreServicesConfig;
import ru.yandex.market.core.datacamp.DataCampService;
import ru.yandex.market.core.datacamp.feed.FeedProcessorUpdateRequestEvent;
import ru.yandex.market.core.delivery.AsyncTarifficatorService;
import ru.yandex.market.core.delivery.LogisticPointInfoYtDao;
import ru.yandex.market.core.delivery.config.ShopSelfDeliveryConfig;
import ru.yandex.market.core.direct.feed.DirectFeedConfig;
import ru.yandex.market.core.environment.DBEnvironmentService;
import ru.yandex.market.core.environment.UnitedCatalogEnvironmentService;
import ru.yandex.market.core.express.ExpressDeliveryBillingDao;
import ru.yandex.market.core.feature.FeatureConfig;
import ru.yandex.market.core.feed.FeedRefreshConfig;
import ru.yandex.market.core.feed.assortment.db.AssortmentValidationDao;
import ru.yandex.market.core.feed.mds.FeedFileStorage;
import ru.yandex.market.core.feed.supplier.SupplierXlsHelper;
import ru.yandex.market.core.feed.supplier.config.SupplierExposedActConfig;
import ru.yandex.market.core.feed.supplier.config.SupplierReportServiceConfig;
import ru.yandex.market.core.feed.supplier.db.FeedSupplierDao;
import ru.yandex.market.core.fulfillment.FulfillmentWorkflowService;
import ru.yandex.market.core.fulfillment.MbiCoreFulfillmentConfig;
import ru.yandex.market.core.fulfillment.OebsSendingServiceConfig;
import ru.yandex.market.core.fulfillment.StatisticsReportServiceConfig;
import ru.yandex.market.core.fulfillment.calculator.TarifficatorConfig;
import ru.yandex.market.core.fulfillment.report.OebsReportTestConfig;
import ru.yandex.market.core.fulfillment.report.async.stocks.by.supply.SupplyStocksReportConfig;
import ru.yandex.market.core.fulfillment.supply.FFSupplyGenerator;
import ru.yandex.market.core.fulfillment.tariff.TariffsService;
import ru.yandex.market.core.history.HistoryService;
import ru.yandex.market.core.history.RowSerializer;
import ru.yandex.market.core.logbroker.config.MarketQuickLogbrokerConfig;
import ru.yandex.market.core.logbroker.event.datacamp.SyncChangeOfferLogbrokerEvent;
import ru.yandex.market.core.logo.LogoServiceConfig;
import ru.yandex.market.core.marketmanager.MarketManagerService;
import ru.yandex.market.core.mbo.PartnerChangeLogbrokerConfig;
import ru.yandex.market.core.mboc.MbocMappingsListenerConfig;
import ru.yandex.market.core.model.ModelService;
import ru.yandex.market.core.notification.MbiCoreNotificationConfig;
import ru.yandex.market.core.notification.history.PartnerLastNotificationConfig;
import ru.yandex.market.core.notification.resolver.impl.YaManagerOnlyResolver;
import ru.yandex.market.core.npd.client.IntegrationNpdRetrofitService;
import ru.yandex.market.core.offer.PapiMarketSkuOfferMetaDataService;
import ru.yandex.market.core.offer.mapping.MboApprovedMappingService;
import ru.yandex.market.core.offer.mapping.MboApprovedMappingServiceImpl;
import ru.yandex.market.core.offer.warehouse.MboDeliveryParamsClient;
import ru.yandex.market.core.order.OrderConfig;
import ru.yandex.market.core.order.ServiceFeePartitionDao;
import ru.yandex.market.core.orginfo.OrgInfoConfig;
import ru.yandex.market.core.outlet.OutletServicesConfig;
import ru.yandex.market.core.outlet.warehouse.WarehouseInletConfig;
import ru.yandex.market.core.param.ParamService;
import ru.yandex.market.core.partner.PartnerDao;
import ru.yandex.market.core.partner.PartnerService;
import ru.yandex.market.core.partner.PartnerTypeAwareService;
import ru.yandex.market.core.partner.fulfillment.yt.SalesDynamicsYtConfig;
import ru.yandex.market.core.partner.fulfillment.yt.SalesDynamicsYtService;
import ru.yandex.market.core.partner.fulfillment.yt.SalesDynamicsYtStorage;
import ru.yandex.market.core.partner.onboarding.state.PartnerOnboardingStateConfig;
import ru.yandex.market.core.passport.PassportService;
import ru.yandex.market.core.periodic_survey.PeriodicSurveyTestConfig;
import ru.yandex.market.core.periodic_survey.config.PeriodicSurveyConfig;
import ru.yandex.market.core.post.RusPostAuthClient;
import ru.yandex.market.core.post.RusPostConfig;
import ru.yandex.market.core.post.RusPostContractClient;
import ru.yandex.market.core.program.ProgramConfig;
import ru.yandex.market.core.program.partner.ProgramParamService;
import ru.yandex.market.core.program.partner.calculator.marketplace.FulfillmentSupplyProgramStatusResolver;
import ru.yandex.market.core.program.partner.calculator.marketplace.NoLoadedOffersResolver;
import ru.yandex.market.core.replenishment.supplier.PilotSupplierYtDao;
import ru.yandex.market.core.replenishment.supplier.PilotSupplierYtDaoConfig;
import ru.yandex.market.core.replication.ReplicationServiceConfig;
import ru.yandex.market.core.salesnotes.SalesNotesServiceConfig;
import ru.yandex.market.core.sorting.SortingDailyTariffDao;
import ru.yandex.market.core.sorting.SortingOrdersTariffDao;
import ru.yandex.market.core.stocks.FF4ShopsClient;
import ru.yandex.market.core.stocks.RetryableFF4ShopsClient;
import ru.yandex.market.core.supplier.SupplierDao;
import ru.yandex.market.core.supplier.category.SupplierCategoryDao;
import ru.yandex.market.core.supplier.certification.CertificationDocumentServiceConfig;
import ru.yandex.market.core.supplier.certification.MarketProtoSupplierDocumentServiceConfig;
import ru.yandex.market.core.supplier.promo.config.SupplierPromoOfferConfig;
import ru.yandex.market.core.supplier.state.FF4ShopsPartnerStateListener;
import ru.yandex.market.core.supplier.state.PartnerFulfillmentLinkListenerConfig;
import ru.yandex.market.core.supplier.state.PartnerServiceLinkLogbrokerEvent;
import ru.yandex.market.core.supplier.state.SupplierStateStockStorageConfig;
import ru.yandex.market.core.supplier.state.dao.FF4ShopsPartnerStateDao;
import ru.yandex.market.core.supplier.state.service.FF4ShopsPartnerStateService;
import ru.yandex.market.core.supplier.summary.SupplierSummaryInfoServiceConfig;
import ru.yandex.market.core.supplier.summary.SupplierSummaryService;
import ru.yandex.market.core.supplier.summary.SupplierSummaryServiceConfig;
import ru.yandex.market.core.tanker.dao.TankerDao;
import ru.yandex.market.core.tanker.dao.TankerDaoImpl;
import ru.yandex.market.core.testing.TestingStatusDaoConfig;
import ru.yandex.market.core.tmessage.TargetMessageTestConfig;
import ru.yandex.market.core.transfermanager.TransferClient;
import ru.yandex.market.core.tvm.MbiTvmCrossContextConfig;
import ru.yandex.market.core.ultracontroller.UltraControllerServiceConfig;
import ru.yandex.market.core.util.http.UnitTestMarketHttpClient;
import ru.yandex.market.core.wizard.WizardConfig;
import ru.yandex.market.core.yt.dynamic.samovar.config.SamovarFeedImportConfig;
import ru.yandex.market.core.yt.dynamic.samovar.config.SamovarFeedServiceConfig;
import ru.yandex.market.deliverycalculator.indexerclient.HttpDeliveryCalculatorIndexerClient;
import ru.yandex.market.ff.client.FulfillmentWorkflowClientApi;
import ru.yandex.market.fulfillment.stockstorage.client.StockStorageFFIntervalClient;
import ru.yandex.market.id.MarketIdServiceGrpc;
import ru.yandex.market.integration.npd.client.api.ApplicationApi;
import ru.yandex.market.ir.http.PartnerContentService;
import ru.yandex.market.ir.http.SupplierContentService;
import ru.yandex.market.logbroker.LogbrokerEventPublisher;
import ru.yandex.market.logbroker.LogbrokerService;
import ru.yandex.market.logbroker.config.LogbrokerListenerConfig;
import ru.yandex.market.logbroker.model.LogbrokerCluster;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.nesu.client.NesuClient;
import ru.yandex.market.logistics.util.client.ExternalServiceProperties;
import ru.yandex.market.logistics.util.client.HttpTemplate;
import ru.yandex.market.mbi.api.billing.client.MbiBillingClient;
import ru.yandex.market.mbi.bpmn.client.MbiBpmnClient;
import ru.yandex.market.mbi.core.IndexerApiClient;
import ru.yandex.market.mbi.data.PartnerChangesDataSender;
import ru.yandex.market.mbi.datacamp.saas.SaasService;
import ru.yandex.market.mbi.datacamp.saas.impl.SaasDatacampService;
import ru.yandex.market.mbi.datacamp.stroller.DataCampClient;
import ru.yandex.market.mbi.environment.ApplicationContextEnvironmentService;
import ru.yandex.market.mbi.environment.EnvironmentService;
import ru.yandex.market.mbi.environment.TestEnvironmentService;
import ru.yandex.market.mbi.http.MarketHttpClient;
import ru.yandex.market.mbi.lock.LockService;
import ru.yandex.market.mbi.util.templates.TemplateContext;
import ru.yandex.market.mbi.yt.YtCluster;
import ru.yandex.market.mbi.yt.YtTemplate;
import ru.yandex.market.mboc.http.MboMappingsService;
import ru.yandex.market.mdm.http.SupplierDocumentService;
import ru.yandex.market.notification.telegram.bot.client.PartnerBotRestClient;
import ru.yandex.yadoc.YaDocClient;

import static org.mockito.AdditionalAnswers.delegatesTo;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

/**
 * Конфигурация для функциональных тестов.
 * Для поднятия базы надо заимпортить {@link ru.yandex.market.core.database.EmbeddedPostgresConfig}
 */
@Configuration
@ImportResource({
        "classpath:common/common-audit-services.xml",
        "classpath:common/common-config.xml",
        "classpath:common/common-services-base.xml",
})
@PropertySource("classpath:test-context.properties")
@Import({
        MemCachedTestConfig.class, // используем почти честный кеш
        YtTestConfig.class, // выключаем yt
        MbiCoreConfig.class,
        OrgInfoConfig.class,
        FeatureConfig.class,
        OrderConfig.class,
        UltraControllerServiceConfig.class,
        MbiCoreNotificationConfig.class,
        MbiCoreFulfillmentConfig.class,
        TarifficatorConfig.class,
        OebsSendingServiceConfig.class,
        StatisticsReportServiceConfig.class,
        ClientConfig.class,
        AboPublicServiceConfig.class,
        OrderBillingConfig.class,
        OutletServicesConfig.class,
        WarehouseInletConfig.class,
        ProgramConfig.class,
        TestingStatusDaoConfig.class,
        SalesNotesServiceConfig.class,
        TestSupplierFeedConfig.class,
        LogoServiceConfig.class,
        MarketProtoSupplierDocumentServiceConfig.class,
        CertificationDocumentServiceConfig.class,
        LogbrokerListenerConfig.class,
        MarketQuickLogbrokerConfig.class,
        SupplierExposedActConfig.class,
        SamovarFeedImportConfig.class,
        SamovarFeedServiceConfig.class,
        LogbrokerChangesEventConfig.class,
        RusPostConfig.class,
        MbocMappingsListenerConfig.class,
        SupplierSummaryInfoServiceConfig.class,
        SalesDynamicsYtConfig.class,
        SupplierStateStockStorageConfig.class,
        PartnerLastNotificationConfig.class,
        PartnerOnboardingStateConfig.class,
        FeedRefreshConfig.class,
        DatacampBusinessMigrationConfig.class,
        WizardConfig.class,
        TargetMessageTestConfig.class,
        ShopsDataTestConfig.class,
        PartnerChangeLogbrokerConfig.class,
        DirectFeedConfig.class,
        SupplyStocksReportConfig.class,
        SupplierReportServiceConfig.class,
        SupplierPromoOfferConfig.class,
        DataCampCoreServicesConfig.class,
        ReplicationServiceConfig.class,
        BusinessMetrikaTestConfig.class,
        MdsS3BasicConfiguration.class,
        OebsReportTestConfig.class,
        PilotSupplierYtDaoConfig.class,
        OebsReportTestConfig.class,
        PeriodicSurveyConfig.class,
        ExpressDeliveryBillingDao.class,
        PeriodicSurveyTestConfig.class,
        PartnerFulfillmentLinkListenerConfig.class,
        StatisticsReportConfigTest.class,
        PartnerContractOptionsConfig.class,
        ReportsConfig.class,
        AsyncReportYtConfig.class,
        TarifficatorClientFunctionalTestConfig.class,
        ShopSelfDeliveryConfig.class,
        SupplierSummaryServiceConfig.class,
        PromoOfferCoreFunctionalTestConfig.class,
})
public class FunctionalTestConfig {
    FunctionalTestConfig() {
        // сделать класс final мы не можем, поэтому хотя бы "запретим" наследование в других модулях
    }

    @Bean
    public static PropertySourcesPlaceholderConfigurer propertyConfigurer() {
        var configurer = new PropertySourcesPlaceholderConfigurer();
        configurer.setIgnoreUnresolvablePlaceholders(true);
        return configurer;
    }

    @Bean
    public LinkDao linkDao(JdbcTemplate jdbcTemplate) {
        var linkDao = new LinkDao();
        linkDao.setShopJdbcTemplate(jdbcTemplate);
        linkDao.setTablesContext(mock(TemplateContext.class));
        return linkDao;
    }

    @Bean
    public BillingDao billingDao(JdbcTemplate jdbcTemplate) {
        return new BillingDao(jdbcTemplate);
    }

    @Bean
    public BillingService billingService(
            JdbcTemplate jdbcTemplate,
            TransactionTemplate transactionTemplate,
            HistoryService historyService,
            RowSerializer rowSerializer,
            RemoveClientEnvironmentService removeClientEnvironmentService,
            AgencyService agencyService
    ) {
        return new DbBillingService(
                jdbcTemplate,
                transactionTemplate,
                historyService,
                rowSerializer,
                removeClientEnvironmentService,
                agencyService
        );
    }

    @Bean
    public ExternalBalanceService balanceService() {
        return mock(ExternalBalanceService.class);
    }

    @Bean
    public ExternalBalanceService impatientBalanceService() {
        return balanceService();
    }

    @Bean
    public ExternalBalanceService patientBalanceService() {
        return balanceService();
    }

    @Bean
    public BalanceContactService balanceContactService() {
        return patientBalanceService();
    }

    @Bean
    public BalanceServiceExecutor balanceServiceExecutor() {
        return new BalanceServiceExecutor() {
            @Override
            @Autowired
            public void setServiceFactory(XmlRPCServiceFactory serviceFactory) {
                super.setServiceFactory(serviceFactory);
            }

            @Override
            @Autowired
            public void setRepeatMap(Map<String, Integer> repeatMap) {
                super.setRepeatMap(repeatMap);
            }
        };
    }

    @Bean
    public Balance2XmlRPCServiceFactory balance2XmlRPCServiceFactory() {
        return new Balance2XmlRPCServiceFactory() {
            @Override
            @Value("${balance.xmlrpc.url}")
            public void setServerUrl(String serverUrl) throws MalformedURLException {
                super.setServerUrl(serverUrl);
            }
        };
    }

    @Bean
    public EnvironmentService dbEnvironmentService(
            JdbcTemplate jdbcTemplate,
            TransactionTemplate transactionTemplate,
            LockService lockService
    ) {
        return new DBEnvironmentService(jdbcTemplate, transactionTemplate, lockService);
    }

    @Bean
    @Primary
    public TestEnvironmentService environmentService(AbstractBeanFactory abstractBeanFactory,
                                                     EnvironmentService environmentService) {
        return new TestEnvironmentService(Arrays.asList(
                environmentService,
                new ApplicationContextEnvironmentService(abstractBeanFactory)));
    }

    @Bean
    public JdbcTemplate shopJdbcTemplate(JdbcTemplate jdbcTemplate) {
        return jdbcTemplate;
    }

    @Bean({"mockIndexerApiClient", "indexerApiClient"})
    public IndexerApiClient indexerApiClient() {
        return mock(IndexerApiClient.class);
    }

    @Bean
    public PassportService passportService() {
        return mock(PassportService.class);
    }

    @Bean
    public MemCachedCampaignService memCachedCampaignService() {
        return mock(MemCachedCampaignService.class);
    }

    @Bean
    public PushApi pushApiClient() {
        return mock(PushApi.class);
    }

    @Bean
    public CheckouterShopApi checkouterShopApi() {
        return mock(CheckouterShopApi.class);
    }

    @Bean
    public CheckouterAPI checkouterClient(
            CheckouterShopApi checkouterShopApi
    ) {
        var result = mock(CheckouterAPI.class);
        when(result.shops()).thenReturn(checkouterShopApi);
        return result;
    }

    @Bean
    public HttpDeliveryCalculatorIndexerClient deliveryCalculatorIndexerClient() {
        return mock(HttpDeliveryCalculatorIndexerClient.class);
    }

    @Bean
    public SupplierDao supplierDao(NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        return new SupplierDao(namedParameterJdbcTemplate);
    }

    @Bean
    public FeedSupplierDao feedSupplierDao(
            NamedParameterJdbcTemplate namedParameterJdbcTemplate,
            Supplier<Integer> samovarInactivePeriodSupplier
    ) {
        return new FeedSupplierDao(namedParameterJdbcTemplate, samovarInactivePeriodSupplier);
    }

    @Bean
    public AssortmentValidationDao supplierFeedValidationDao(NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        return new AssortmentValidationDao(namedParameterJdbcTemplate);
    }

    @Bean
    public ModelService modelService() {
        return mock(ModelService.class);
    }

    @Bean
    public YtTemplate ultraControllerYtTemplate() {
        return spy(new YtTemplate(new YtCluster("hahn.yt.yandex.net", mock(Yt.class))));
    }

    @Bean
    public YtTemplate weeklyReportDataAggregatorYtTemplate() {
        return mock(YtTemplate.class);
    }

    @Bean
    public Yt ultracontrollerYt() {
        return mock(Yt.class);
    }

    @Bean
    public TransferClient transferClient() {
        return mock(TransferClient.class);
    }

    @Bean
    public ru.yandex.market.ir.http.UltraControllerService ultraControllerClient() {
        return mock(ru.yandex.market.ir.http.UltraControllerService.class);
    }

    @Bean
    public SupplierContentService supplierContentService() {
        return mock(SupplierContentService.class);
    }

    @Bean
    public FulfillmentWorkflowClientApi fulfillmentWorkflowClientApi() {
        return mock(FulfillmentWorkflowClientApi.class);
    }

    @Bean
    public HttpTemplate fulfillmentHttpTemplate() {
        return mock(HttpTemplate.class);
    }

    @Bean
    public ExternalServiceProperties fulfillmentWorkflowProperties() {
        return mock(ExternalServiceProperties.class);
    }

    @Bean
    public MboApprovedMappingService mboApprovedMappingService(MboMappingsService mboMappingsService) {
        return new MboApprovedMappingServiceImpl(mboMappingsService, Executors.newSingleThreadExecutor());
    }

    @Bean
    public PapiMarketSkuOfferMetaDataService papiMarketSkuOfferMetaDataService(
            MboApprovedMappingService mboApprovedMappingService,
            NamedParameterJdbcTemplate namedParameterJdbcTemplate
    ) {
        return new PapiMarketSkuOfferMetaDataService(namedParameterJdbcTemplate, mboApprovedMappingService);
    }

    @Bean
    public FeedFileStorage feedFileStorage() {
        return mock(FeedFileStorage.class);
    }

    @Bean
    public MdsS3Client mdsS3Client(AmazonS3 amazonS3) {
        return spy(new MdsS3ClientImpl(amazonS3));
    }

    @Bean
    public AmazonS3 amazonS3() {
        return mock(AmazonS3.class);
    }

    @Bean
    public ResourceLocationFactory resourceLocationFactory() {
        return mock(ResourceLocationFactory.class);
    }

    @Bean
    @Primary
    public MboMappingsService patientMboMappingsService() {
        return mock(MboMappingsService.class);
    }

    @Bean
    public SalesDynamicsYtStorage salesDynamicsYtStorage(final SalesDynamicsYtStorage salesDynamicsYtStorage) {
        return spy(salesDynamicsYtStorage);
    }

    @Bean(autowire = Autowire.BY_NAME)
    public MboMappingsService impatientMboMappingsService() {
        return mock(MboMappingsService.class);
    }

    @Bean
    public PartnerContentService marketProtoPartnerContentService() {
        return mock(PartnerContentService.class);
    }

    @Bean
    public MarketHttpClient unitTestHttpClient() {
        return spy(new UnitTestMarketHttpClient());
    }

    @Nonnull
    @Bean
    public Integer papiOfferPriceDiffServiceBatchSize() {
        return 3;
    }

    @Bean
    //todo: Убрать отсюда в рамках MBI-32754
    public YaManagerOnlyResolver testYaManagerOnlyResolver(
            MarketManagerService marketManagerService,
            PartnerService partnerService,
            @Qualifier("dbEnvironmentService") EnvironmentService environmentService
    ) {
        return new YaManagerOnlyResolver(marketManagerService, partnerService, environmentService);
    }

    @Bean
    public ResourceConfigurationDao resourceConfigurationDao() {
        return mock(ResourceConfigurationDao.class);
    }

    @Bean
    public SupplierDocumentService marketProtoSupplierDocumentService() {
        return mock(SupplierDocumentService.class);
    }

    @Bean
    public SupplierXlsHelper supplierXlsHelper() {
        return spy(new SupplierXlsHelper(
                new ClassPathResource("supplier/feed/xls_template.xlsm"),
                ".xlsm"
        ));
    }

    @Bean
    public SupplierXlsHelper unitedSupplierXlsHelper() {
        return spy(new SupplierXlsHelper(
                new ClassPathResource("supplier/feed/marketplace-catalog-united.xlsx"),
                ".xlsx"
        ));
    }

    @Bean
    public SupplierXlsHelper dataCampStocksSupplierXlsHelper() {
        return spy(new SupplierXlsHelper(
                new ClassPathResource("supplier/feed/marketplace-stock.xlsx"),
                ".xlsx"
        ));
    }

    @Bean
    public Tvm2 mbiTvm() {
        var tvm2 = mock(Tvm2.class);
        MbiTvmCrossContextConfig.setMbiTvm(tvm2);
        return tvm2;
    }

    @Bean
    public Tvm2 logbrokerTvm2() {
        return mock(Tvm2.class);
    }

    @Bean
    public LogbrokerCluster logbrokerCluster() {
        return mock(LogbrokerCluster.class);
    }

    @Bean
    public LogbrokerCluster lbkxCluster() {
        return mock(LogbrokerCluster.class);
    }

    @Bean
    public LogbrokerEventPublisher<SyncChangeOfferLogbrokerEvent> marketQuickLogbrokerService() {
        return mock(LogbrokerEventPublisher.class);
    }

    @Bean
    public LogbrokerService samovarLogbrokerService() {
        return mock(LogbrokerService.class);
    }

    @Bean
    public String getMarketIdGrpcServerName() {
        return InProcessServerBuilder.generateName();
    }

    @Bean
    public ManagedChannel managedChannel() {
        return InProcessChannelBuilder.forName(getMarketIdGrpcServerName()).directExecutor().build();
    }

    @Bean(initMethod = "start", destroyMethod = "shutdown")
    public Server marketIdServer() {
        return InProcessServerBuilder
                .forName(getMarketIdGrpcServerName()).directExecutor().addService(marketIdServiceImplBase()).build();
    }

    @Bean
    public MarketIdServiceGrpc.MarketIdServiceImplBase marketIdServiceImplBase() {
        return mock(MarketIdServiceGrpc.MarketIdServiceImplBase.class,
                delegatesTo(new MarketIdServiceGrpc.MarketIdServiceImplBase() {
                }));
    }

    @Bean
    public LMSClient lmsClient() {
        return mock(LMSClient.class);
    }

    @Bean
    public StockStorageFFIntervalClient stockStorageFFIntervalClient() {
        return mock(StockStorageFFIntervalClient.class);
    }

    @Bean
    public NesuClient nesuClient() {
        return mock(NesuClient.class);
    }

    @Bean
    public YaDocClient yaDocClient() {
        return mock(YaDocClient.class);
    }

    @Bean
    public PartnerService partnerService(
            PartnerDao partnerDao, ParamService paramService, HistoryService historyService,
            PartnerChangesDataSender partnerChangesDataSender
    ) {
        @SuppressWarnings("unchecked") Cache<Long, PartnerId> noCache = mock(Cache.class);
        return new PartnerService(partnerDao, paramService, historyService, noCache, partnerChangesDataSender);
    }

    @Bean
    public FF4ShopsClient ff4ShopsClient() {
        return mock(FF4ShopsClient.class);
    }

    @Bean
    public Clock clock() {
        return mock(Clock.class);
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
    public SortingOrdersTariffDao sortingOrdersTariffDao(
            NamedParameterJdbcTemplate namedParameterJdbcTemplate,
            EnvironmentService environmentService
    ) {
        return new SortingOrdersTariffDao(namedParameterJdbcTemplate, environmentService, clientTariffsService());
    }

    @Bean
    public TariffsService clientTariffsService() {
        return mock(TariffsService.class);
    }

    @Bean
    public SortingDailyTariffDao sortingDailyTariffDao(
            NamedParameterJdbcTemplate namedParameterJdbcTemplate,
            EnvironmentService environmentService
    ) {
        return new SortingDailyTariffDao(namedParameterJdbcTemplate, environmentService, clientTariffsService());
    }

    @Bean
    public GeoClient geoClient() {
        return mock(GeoClient.class);
    }

    @Bean
    public SupplierCategoryDao supplierCategoryDao(JdbcTemplate jdbcTemplate) {
        return new SupplierCategoryDao(jdbcTemplate);
    }

    @Bean
    public MboDeliveryParamsClient mboDeliveryParamsClient() {
        return mock(MboDeliveryParamsClient.class);
    }

    @Bean
    public SaasService saasService() {
        return mock(SaasDatacampService.class);
    }

    @Bean
    public SaasService saasDataCampShopService() {
        return mock(SaasDatacampService.class);
    }

    @Bean
    @SuppressWarnings("checkstyle:parameterNumber")
    public FFSupplyGenerator ffSupplyGenerator(
            SalesDynamicsYtService salesDynamicsYtService,
            FulfillmentWorkflowService fulfillmentWorkflowService,
            MboDeliveryParamsClient mboDeliveryParamsClient,
            DataCampService dataCampService,
            BusinessService businessService,
            EnvironmentService environmentService,
            @Value("${market.ff-supply-generator.limits.cache-sec}") int cacheSec
    ) {
        return new FFSupplyGenerator(
                salesDynamicsYtService,
                fulfillmentWorkflowService,
                mboDeliveryParamsClient,
                dataCampService,
                businessService,
                environmentService,
                cacheSec
        );
    }

    @Bean
    public FulfillmentSupplyProgramStatusResolver fulfillmentSupplyProgramStatusResolver(
            ProgramParamService programParamService) {
        return new FulfillmentSupplyProgramStatusResolver(programParamService);
    }

    @Bean
    public NoLoadedOffersResolver noLoadedOffersResolver(SupplierSummaryService supplierSummaryService) {
        return new NoLoadedOffersResolver(supplierSummaryService, () -> true);
    }

    @Bean
    public ProgramParamService programParamService(ParamService paramService, EnvironmentService environmentService) {
        return new ProgramParamService(paramService, environmentService, 0);
    }

    /**
     * @param txManager из {@link ru.yandex.market.common.test.spring.DbUnitTestConfig#txManager(DataSource)}
     * @param transactionalMemCachedAgent из common-memcached.xml
     * @return почти дублирует common-transactions.xml
     */
    @Bean(name = {
            "memCachedTransactionTemplate",
            "transactionTemplate",
    })
    public ListenableTransactionTemplate memCachedTransactionTemplate(
            PlatformTransactionManager txManager,
            TransactionalMemCachedAgent transactionalMemCachedAgent
    ) {
        var template = new MemCachedTransactionTemplate();
        template.setTransactionManager(txManager);
        template.setTransactionalMemCachedAgent(transactionalMemCachedAgent);
        return template;
    }

    @Bean
    public LocalTransactionListener localTransactionListener(
            ListenableTransactionTemplate transactionTemplate
    ) {
        var localTransactionListener = new LocalTransactionListener();
        transactionTemplate.addListener(localTransactionListener);
        return localTransactionListener;
    }

    @Bean
    public BunkerDao<RouteContent> bunkerRoutesDao(JdbcTemplate jdbcTemplate) {
        return spy(new RoutesBunkerDao(jdbcTemplate));
    }

    @Primary
    @Bean
    public BunkerDao<RouteContent> cachedBunkerDao(@Qualifier("bunkerRoutesDao") BunkerDao<RouteContent> bunkerDao) {
        return spy(new CachedBunkerDao<>(bunkerDao));
    }

    @Bean
    @Primary
    public DataCampClient dataCampShopClient() {
        return mock(DataCampClient.class);
    }

    @Bean
    public YtTemplate webmasterYtTemplate(
            @Value("#{'${yt.mbi.webmaster.hosts}'.split(',')}") List<String> webmasterYtHosts
    ) {
        return new YtTemplate(
                new YtCluster[]{
                        new YtCluster(webmasterYtHosts.get(0), mock(Yt.class)),
                        new YtCluster(webmasterYtHosts.get(1), mock(Yt.class))
                }
        );
    }

    @Bean("dbsFeedXlsTemplateResource")
    public Resource dbsFeedXlsTemplateResource() {
        return new ClassPathResource("shop/feed/market-pricelist-standard-dbs.xlsx");
    }

    @Bean("advFeedXlsTemplateResource")
    public Resource advFeedXlsTemplateResource() {
        return new ClassPathResource("shop/feed/market-pricelist-standard-win7-10.xlsx");
    }

    @Bean("supplierFeedXlsTemplateResource")
    public Resource supplierFeedXlsTemplateResource() {
        return new ClassPathResource("supplier/feed/marketplace-catalog.xlsm");
    }

    @Bean("unitedSupplierFeedXlsTemplateResource")
    public Resource unitedSupplierFeedXlsTemplateResource() {
        return new ClassPathResource("supplier/feed/marketplace-catalog-standard-warnings.xlsx");
    }

    @Bean("businessFeedXlsTemplateResource")
    public Resource businessFeedXlsTemplateResource() {
        return new ClassPathResource("supplier/feed/marketplace-catalog-business.xlsx");
    }

    @Bean("stockFeedXlsTemplateResource")
    public Resource stockFeedXlsTemplateResource() {
        return new ClassPathResource("united/feed/marketplace-stock-warnings.xlsx");
    }

    @Bean("priceFeedXlsTemplateResource")
    public Resource priceFeedXlsTemplateResource() {
        return new ClassPathResource("united/feed/assortment-price.xlsm");
    }

    @Bean("priceSupplierXlsTemplate")
    public Resource priceSupplierXlsTemplate() {
        return new ClassPathResource("united/feed/marketplace-prices.xlsm");
    }

    @Bean("feedHistoryXlsTemplateResource")
    public Resource feedHistoryXlsTemplateResource() {
        return new ClassPathResource("supplier/feed/offer-history.xlsx");
    }

    @Bean
    public MbiBpmnClient bpmnClient() {
        return mock(MbiBpmnClient.class);
    }

    @Bean
    public PartnerBotRestClient partnerBotRestClient() {
        return mock(PartnerBotRestClient.class);
    }

    @Bean
    public FF4ShopsPartnerStateDao ff4ShopsPartnerStateDao(NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        return new FF4ShopsPartnerStateDao(namedParameterJdbcTemplate);
    }

    @Bean
    public FF4ShopsPartnerStateService ff4ShopsPartnerStateService(
            EnvironmentService environmentService,
            PartnerTypeAwareService partnerTypeAwareService,
            FF4ShopsPartnerStateDao ff4ShopsPartnerStateDao) {
        return new FF4ShopsPartnerStateService(environmentService, partnerTypeAwareService, ff4ShopsPartnerStateDao);
    }

    @Bean
    public RetryableFF4ShopsClient retryableFF4ShopsClient(FF4ShopsClient ff4ShopsClient) {
        return new RetryableFF4ShopsClient(ff4ShopsClient, new RetryTemplate());
    }

    @Bean
    public FF4ShopsPartnerStateListener ff4ShopsPartnerStateListener(
            FF4ShopsPartnerStateService ff4ShopsPartnerStateService,
            RetryableFF4ShopsClient ff4ShopsClient,
            LocalTransactionListener localTransactionListener
    ) {
        return new FF4ShopsPartnerStateListener(ff4ShopsPartnerStateService, ff4ShopsClient, localTransactionListener);
    }

    @Bean("marketReportService")
    public AsyncMarketReportService marketReportService() {
        return new DefaultAsyncMarketReportService();
    }

    @Bean
    @Primary
    public TankerDao basicTankerDao(JdbcTemplate jdbcTemplate) {
        return new TankerDaoImpl(jdbcTemplate);
    }

    @Bean
    public LogbrokerService internalLogbrokerService() {
        return mock(LogbrokerService.class);
    }

    @Bean(name = "enabledValidationCpaStatusSupplier")
    public Supplier<Boolean> enabledValidationCpaStatusSupplier(
            @Nonnull UnitedCatalogEnvironmentService unitedCatalogEnvironmentService
    ) {
        return unitedCatalogEnvironmentService::isValidationCpaStatusEnabled;
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
    public LogisticPointInfoYtDao logisticPointInfoYtDao() {
        return mock(LogisticPointInfoYtDao.class);
    }

    @Bean
    public MbiBillingClient mbiBillingClient() {
        return mock(MbiBillingClient.class);
    }

    @Bean
    public LogbrokerEventPublisher<FeedProcessorUpdateRequestEvent> feedProcessorUpdateLogbrokerEventPublisher() {
        return mock(LogbrokerEventPublisher.class);
    }

    @Bean
    public LogbrokerService mboPartnerExportLogbrokerService() {
        return mock(LogbrokerService.class);
    }

    @Bean
    public LogbrokerEventPublisher<PartnerServiceLinkLogbrokerEvent> partnerFfLinkLbProducer() {
        return mock(LogbrokerEventPublisher.class);
    }

    @Bean
    public PartnerNotificationClient partnerNotificationClient() {
        return mock(PartnerNotificationClient.class);
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
    public ServiceFeePartitionDao serviceFeePartitionDao(NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        return new ServiceFeePartitionDao(namedParameterJdbcTemplate);
    }
}
