package ru.yandex.market.shop;

import java.time.Clock;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.cache.Cache;
import org.apache.http.client.HttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.TransactionManagementConfigurer;
import ru.market.partner.notification.client.PartnerNotificationClient;

import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.market.common.bunker.BunkerWritingApi;
import ru.yandex.market.common.bunker.loader.BunkerLoader;
import ru.yandex.market.common.mds.s3.spring.db.ResourceConfigurationDao;
import ru.yandex.market.core.TestClock;
import ru.yandex.market.core.business.BusinessMetrikaTestConfig;
import ru.yandex.market.core.campaign.model.PartnerId;
import ru.yandex.market.core.config.ShopsDataTestConfig;
import ru.yandex.market.core.config.TarifficatorClientFunctionalTestConfig;
import ru.yandex.market.core.dao.system.SystemDao;
import ru.yandex.market.core.dao.system.SystemDaoImpl;
import ru.yandex.market.core.database.EmbeddedPostgresConfig;
import ru.yandex.market.core.datacamp.DataCampCoreServicesConfig;
import ru.yandex.market.core.datacamp.feed.FeedProcessorUpdateRequestEvent;
import ru.yandex.market.core.environment.UnitedCatalogEnvironmentService;
import ru.yandex.market.core.feed.mds.FeedFileStorage;
import ru.yandex.market.core.feed.supplier.SupplierXlsHelper;
import ru.yandex.market.core.history.HistoryService;
import ru.yandex.market.core.npd.client.IntegrationNpdRetrofitService;
import ru.yandex.market.core.param.ParamService;
import ru.yandex.market.core.partner.PartnerDao;
import ru.yandex.market.core.partner.PartnerService;
import ru.yandex.market.core.partner.contract.PartnerContractDao;
import ru.yandex.market.core.partner.onboarding.state.PartnerOnboardingProtoLBEvent;
import ru.yandex.market.core.solomon.SolomonTestJvmConfig;
import ru.yandex.market.core.supplier.state.PartnerServiceLinkLogbrokerEvent;
import ru.yandex.market.core.supplier.summary.SupplierSummaryServiceConfig;
import ru.yandex.market.crm.client.YaCrmSpaceClient;
import ru.yandex.market.direct.feed.RefreshFeedYtDaoTestConfig;
import ru.yandex.market.id.MarketIdServiceGrpc;
import ru.yandex.market.integration.npd.client.api.ApplicationApi;
import ru.yandex.market.logbroker.LogbrokerEventPublisher;
import ru.yandex.market.logbroker.LogbrokerService;
import ru.yandex.market.mbi.api.billing.client.MbiBillingClient;
import ru.yandex.market.mbi.data.PartnerChangesDataSender;
import ru.yandex.market.mbi.datacamp.saas.SaasService;
import ru.yandex.market.mbi.yt.YtCluster;
import ru.yandex.market.mbi.yt.YtTemplate;
import ru.yandex.market.notification.telegram.bot.client.PartnerBotRestClient;
import ru.yandex.market.periodic_survey.PeriodicSurveyTestConfig;
import ru.yandex.market.supplier.act.exposed.OebsMetabaseDao;

import static org.mockito.AdditionalAnswers.delegatesTo;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

@ParametersAreNonnullByDefault
@Configuration
@Import({
        SolomonTestJvmConfig.class,
        ShopsDataTestConfig.class,
        DataCampCoreServicesConfig.class,
        BusinessMetrikaTestConfig.class,
        PeriodicSurveyTestConfig.class,
        TarifficatorClientFunctionalTestConfig.class,
        EmbeddedPostgresConfig.class,
        SupplierSummaryServiceConfig.class,
        RefreshFeedYtDaoTestConfig.class,
})
public class FunctionalTestEnvironmentConfig {
    @Bean
    public ResourceConfigurationDao resourceConfigurationDao() {
        return mock(ResourceConfigurationDao.class);
    }

    @Bean
    public Clock clock() {
        return spy(new TestClock());
    }

    @Bean
    public HttpClient validationResultHttpClient() {
        return mock(HttpClient.class);
    }

    @Bean
    public TransactionManagementConfigurer functionalTestTransactionManagementConfigurer(
            PlatformTransactionManager txManager
    ) {
        return () -> txManager;
    }

    @Bean
    public BunkerLoader bunkerLoader() {
        return mock(BunkerLoader.class);
    }

    @Bean
    public BunkerWritingApi bunkerWritingApi() {
        return mock(BunkerWritingApi.class);
    }

    @Bean
    public NamedParameterJdbcTemplate yqlNamedParameterJdbcTemplate() {
        return mock(NamedParameterJdbcTemplate.class);
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
    public YtTemplate bannerYtTemplate(
            @Value("#{'${yt.mbi.bluebanners.hosts}'.split(',')}") List<String> bannerYtHosts
    ) {
        return new YtTemplate(new YtCluster[]{
                new YtCluster(bannerYtHosts.get(0), mock(Yt.class)),
                new YtCluster(bannerYtHosts.get(1), mock(Yt.class))
        });
    }

    @Bean
    public YtTemplate webmasterYtTemplate(
            @Value("#{'${yt.mbi.webmaster.hosts}'.split(',')}") List<String> webmasterYtHosts
    ) {
        return new YtTemplate(new YtCluster[]{
                new YtCluster(webmasterYtHosts.get(0), mock(Yt.class)),
                new YtCluster(webmasterYtHosts.get(1), mock(Yt.class))
        });
    }

    @Bean
    public PartnerService partnerService(
            PartnerDao partnerDao,
            ParamService paramService,
            HistoryService historyService,
            PartnerChangesDataSender partnerChangesDataSender
    ) {
        @SuppressWarnings("unchecked") Cache<Long, PartnerId> noCache = mock(Cache.class);
        return new PartnerService(partnerDao, paramService, historyService, noCache, partnerChangesDataSender);
    }

    @Bean
    public FeedFileStorage feedFileStorage() {
        return mock(FeedFileStorage.class);
    }

    @Bean
    public MarketIdServiceGrpc.MarketIdServiceImplBase marketIdServiceImplBase() {
        return mock(MarketIdServiceGrpc.MarketIdServiceImplBase.class,
                delegatesTo(new MarketIdServiceGrpc.MarketIdServiceImplBase() {
                }));
    }

    @Bean
    public YaCrmSpaceClient yandexCrmSpaceClient() {
        return mock(YaCrmSpaceClient.class);
    }

    @Bean
    @Primary
    public Supplier<Set<Long>> feedValidationBlackListSupplierCached(
            Supplier<Set<Long>> feedValidationBlackListSupplier
    ) {
        return feedValidationBlackListSupplier;
    }

    @Bean("dbsFeedXlsTemplateResource")
    public Resource dbsFeedXlsTemplateResource() {
        return new ClassPathResource("shop/feed/shop-common.xlsx");
    }

    @Bean("supplierFeedXlsTemplateResource")
    public Resource supplierFeedXlsTemplateResource() {
        return new ClassPathResource("supplier/feed/marketplace-catalog.xlsm");
    }

    @Bean("unitedSupplierFeedXlsTemplateResource")
    public Resource unitedSupplierFeedXlsTemplateResource() {
        return new ClassPathResource("supplier/feed/marketplace-catalog-standard.xlsx");
    }

    @Bean("businessFeedXlsTemplateResource")
    public Resource businessFeedXlsTemplateResource() {
        return new ClassPathResource("business/feed/marketplace-catalog.xlsx");
    }

    @Bean("stockFeedXlsTemplateResource")
    public Resource stockFeedXlsTemplateResource() {
        return new ClassPathResource("united/feed/marketplace-stock.xlsx");
    }

    @Bean("priceFeedXlsTemplateResource")
    public Resource priceFeedXlsTemplateResource() {
        return new ClassPathResource("united/feed/marketplace-prices.xlsm");
    }

    @Bean
    public PartnerBotRestClient partnerBotRestClient() {
        return mock(PartnerBotRestClient.class);
    }

    @Bean
    public SaasService saasService() {
        return mock(SaasService.class);
    }

    @Bean
    public SystemDao systemDao(JdbcTemplate jdbcTemplate) {
        return spy(new SystemDaoImpl(jdbcTemplate));
    }

    @Bean(name = "enabledValidationCpaStatusSupplier")
    public Supplier<Boolean> enabledValidationCpaStatusSupplier(
            @Nonnull UnitedCatalogEnvironmentService unitedCatalogEnvironmentService
    ) {
        return unitedCatalogEnvironmentService::isValidationCpaStatusEnabled;
    }

    @Bean
    public LogbrokerEventPublisher<FeedProcessorUpdateRequestEvent> feedProcessorUpdateLogbrokerEventPublisher() {
        return mock(LogbrokerEventPublisher.class);
    }

    @Bean
    public OebsMetabaseDao oebsMetabaseDao(
            NamedParameterJdbcTemplate namedParameterJdbcTemplate,
            PartnerContractDao supplierContractDao
    ) {
        return new OebsMetabaseDao(namedParameterJdbcTemplate, supplierContractDao);
    }

    @Bean
    public PartnerNotificationClient partnerNotificationClient() {
        return mock(PartnerNotificationClient.class);

    }

    @Bean
    public LogbrokerEventPublisher<PartnerOnboardingProtoLBEvent> logbrokerOnboardingEventPublisher() {
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
    public MbiBillingClient mbiBillingClient() {
        return mock(MbiBillingClient.class);
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

}
