package ru.yandex.direct.teststeps.configuration;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import javax.servlet.Filter;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.module.SimpleModule;
import one.util.streamex.StreamEx;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.context.annotation.RequestScope;
import org.springframework.web.filter.CharacterEncodingFilter;
import org.springframework.web.filter.CompositeFilter;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import springfox.documentation.swagger2.mappers.DirectModelMapperImpl;
import springfox.documentation.swagger2.mappers.ModelMapper;

import ru.yandex.direct.balance.client.BalanceClient;
import ru.yandex.direct.common.configuration.LoggingConfiguration;
import ru.yandex.direct.common.logging.LoggingConfigurerInterceptor;
import ru.yandex.direct.common.logging.LoggingSettings;
import ru.yandex.direct.common.metrics.MetricsFilter;
import ru.yandex.direct.common.metrics.MetricsInterceptor;
import ru.yandex.direct.common.tracing.TraceContextFilter;
import ru.yandex.direct.core.configuration.CoreConfiguration;
import ru.yandex.direct.core.entity.banner.repository.old.OldBannerPixelsRepository;
import ru.yandex.direct.core.entity.brandlift.repository.BrandSurveyRepository;
import ru.yandex.direct.core.entity.campaign.repository.CampaignModifyRepository;
import ru.yandex.direct.core.entity.campaign.repository.CampaignsMulticurrencySumsRepository;
import ru.yandex.direct.core.entity.campaign.service.type.add.CampaignAddOperationSupportFacade;
import ru.yandex.direct.core.entity.client.repository.ClientLimitsRepository;
import ru.yandex.direct.core.entity.client.repository.ClientOptionsRepository;
import ru.yandex.direct.core.entity.client.repository.ClientRepository;
import ru.yandex.direct.core.entity.client.service.ClientService;
import ru.yandex.direct.core.entity.crypta.repository.CryptaSegmentRepository;
import ru.yandex.direct.core.entity.feature.repository.ClientFeaturesRepository;
import ru.yandex.direct.core.entity.feature.service.DirectAuthContextService;
import ru.yandex.direct.core.entity.feature.service.FeatureManagingService;
import ru.yandex.direct.core.entity.feed.repository.FeedRepository;
import ru.yandex.direct.core.entity.internalads.service.InternalAdsProductService;
import ru.yandex.direct.core.entity.metrika.repository.LalSegmentRepository;
import ru.yandex.direct.core.entity.minuskeywordspack.repository.MinusKeywordsPackRepository;
import ru.yandex.direct.core.entity.outdoor.repository.PlacementsOutdoorDataRepository;
import ru.yandex.direct.core.entity.placements.repository.PlacementBlockRepository;
import ru.yandex.direct.core.entity.pricepackage.repository.PricePackageRepository;
import ru.yandex.direct.core.entity.sitelink.repository.SitelinkRepository;
import ru.yandex.direct.core.entity.sitelink.repository.SitelinkSetRepository;
import ru.yandex.direct.core.entity.uac.grut.GrutContext;
import ru.yandex.direct.core.entity.uac.grut.GrutTransactionProvider;
import ru.yandex.direct.core.entity.uac.grut.RequestScopeGrutContext;
import ru.yandex.direct.core.entity.uac.service.GrutUacCampaignService;
import ru.yandex.direct.core.entity.user.repository.UserRepository;
import ru.yandex.direct.core.entity.user.service.BlackboxUserService;
import ru.yandex.direct.core.entity.user.service.UserService;
import ru.yandex.direct.core.grut.replication.GrutApiService;
import ru.yandex.direct.core.testing.repository.TestAdGroupRepository;
import ru.yandex.direct.core.testing.repository.TestAgencyNdsRepository;
import ru.yandex.direct.core.testing.repository.TestAgencyRepository;
import ru.yandex.direct.core.testing.repository.TestBannerCreativeRepository;
import ru.yandex.direct.core.testing.repository.TestBannerImageFormatRepository;
import ru.yandex.direct.core.testing.repository.TestBannerImageRepository;
import ru.yandex.direct.core.testing.repository.TestBannerPixelsRepository;
import ru.yandex.direct.core.testing.repository.TestBannerRepository;
import ru.yandex.direct.core.testing.repository.TestCampaignRepository;
import ru.yandex.direct.core.testing.repository.TestClientLimitsRepository;
import ru.yandex.direct.core.testing.repository.TestClientNdsRepository;
import ru.yandex.direct.core.testing.repository.TestClientOptionsRepository;
import ru.yandex.direct.core.testing.repository.TestClientRepository;
import ru.yandex.direct.core.testing.repository.TestCpmYndxFrontpageRepository;
import ru.yandex.direct.core.testing.repository.TestCreativeRepository;
import ru.yandex.direct.core.testing.repository.TestCryptaSegmentRepository;
import ru.yandex.direct.core.testing.repository.TestFeedCategoryRepository;
import ru.yandex.direct.core.testing.repository.TestFeedHistoryRepository;
import ru.yandex.direct.core.testing.repository.TestImageRepository;
import ru.yandex.direct.core.testing.repository.TestKeywordRepository;
import ru.yandex.direct.core.testing.repository.TestLalSegmentRepository;
import ru.yandex.direct.core.testing.repository.TestMinusKeywordsPackRepository;
import ru.yandex.direct.core.testing.repository.TestModerationRepository;
import ru.yandex.direct.core.testing.repository.TestPlacementRepository;
import ru.yandex.direct.core.testing.repository.TestSitelinkSetRepository;
import ru.yandex.direct.core.testing.repository.TestSmsQueueRepository;
import ru.yandex.direct.core.testing.repository.TestUserRepository;
import ru.yandex.direct.core.testing.steps.AdGroupSteps;
import ru.yandex.direct.core.testing.steps.BannerSteps;
import ru.yandex.direct.core.testing.steps.BaseUserSteps;
import ru.yandex.direct.core.testing.steps.CalloutSteps;
import ru.yandex.direct.core.testing.steps.CampaignSteps;
import ru.yandex.direct.core.testing.steps.CampaignsMobileContentSteps;
import ru.yandex.direct.core.testing.steps.ClientOptionsSteps;
import ru.yandex.direct.core.testing.steps.ClientSteps;
import ru.yandex.direct.core.testing.steps.CreativeSteps;
import ru.yandex.direct.core.testing.steps.FeatureSteps;
import ru.yandex.direct.core.testing.steps.FeedSteps;
import ru.yandex.direct.core.testing.steps.IdmGroupSteps;
import ru.yandex.direct.core.testing.steps.InternalAdProductSteps;
import ru.yandex.direct.core.testing.steps.KeywordSteps;
import ru.yandex.direct.core.testing.steps.MinusKeywordsPackSteps;
import ru.yandex.direct.core.testing.steps.PerformanceFiltersSteps;
import ru.yandex.direct.core.testing.steps.PlacementSteps;
import ru.yandex.direct.core.testing.steps.PricePackageSteps;
import ru.yandex.direct.core.testing.steps.RetConditionSteps;
import ru.yandex.direct.core.testing.steps.RetargetingSteps;
import ru.yandex.direct.core.testing.steps.SitelinkSetSteps;
import ru.yandex.direct.core.testing.steps.TurboLandingSteps;
import ru.yandex.direct.core.testing.steps.TypedCampaignStepsUnstubbed;
import ru.yandex.direct.core.testing.steps.UserSteps;
import ru.yandex.direct.core.testing.steps.campaign.repository0.CampaignRepository;
import ru.yandex.direct.core.testing.stub.MetrikaClientStub;
import ru.yandex.direct.core.testing.stub.MetrikaHelperStub;
import ru.yandex.direct.core.testing.stub.PassportClientStub;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.dbutil.sharding.ShardSupport;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.integrations.configuration.IntegrationsConfiguration;
import ru.yandex.direct.metrika.client.MetrikaClient;
import ru.yandex.direct.metrika.client.MetrikaHelper;
import ru.yandex.direct.rbac.PpcRbac;
import ru.yandex.direct.rbac.RbacClientsRelations;
import ru.yandex.direct.rbac.RbacService;
import ru.yandex.direct.teststeps.service.InfoHelper;
import ru.yandex.direct.teststeps.service.SiteLinkStepsService;
import ru.yandex.direct.teststeps.service.TurboLandingStepsService;
import ru.yandex.direct.utils.JsonUtils;
import ru.yandex.direct.utils.json.LocalDateDeserializer;
import ru.yandex.direct.utils.json.LocalDateSerializer;
import ru.yandex.direct.web.core.WebLocaleResolver;
import ru.yandex.direct.web.core.security.WebLocaleResolverFilter;
import ru.yandex.direct.web.core.security.configuration.NetAclConfiguration;
import ru.yandex.direct.web.core.security.netacl.NetAclInterceptor;
import ru.yandex.grut.client.GrutClient;

import static org.mockito.Mockito.spy;

@EnableWebMvc
@ComponentScan(
        basePackages = {
                "ru.yandex.direct.teststeps"
        },
        excludeFilters = @ComponentScan.Filter(value = Configuration.class, type = FilterType.ANNOTATION)
)
@Import({
        LoggingConfiguration.class,
        CoreConfiguration.class,
        SwaggerConfiguration.class,
        NetAclConfiguration.class
})
public class TestStepsAppConfiguration implements WebMvcConfigurer {

    private final NetAclInterceptor netAclInterceptor;
    private final MetricsInterceptor metricsInterceptor;

    @Autowired
    public TestStepsAppConfiguration(NetAclInterceptor netAclInterceptor,
                                     MetricsInterceptor metricsInterceptor) {
        this.netAclInterceptor = netAclInterceptor;
        this.metricsInterceptor = metricsInterceptor;
    }

    @Bean(name = "directTestStepsFilter")
    @Autowired
    public Filter directTestStepsFilter(TraceContextFilter traceContextFilter,
                                        MetricsFilter metricsFilter) {
        CompositeFilter compositeFilter = new CompositeFilter();
        // Порядок следования фильтров важен. traceContextFilter должен идти первым
        compositeFilter.setFilters(Arrays.asList(
                traceContextFilter,
                webLocaleResolverFilter(),
                metricsFilter
        ));
        return compositeFilter;
    }

    @Bean
    @Primary
    public ModelMapper modelMapper() {
        return new DirectModelMapperImpl();
    }

    @Bean
    public LoggingSettings loggingDefaults() {
        return new LoggingSettings(1024 * 1024, 1024 * 1024);
    }

    @Bean
    public LoggingConfigurerInterceptor loggingConfigurerInterceptor() {
        return new LoggingConfigurerInterceptor(loggingDefaults());
    }

    @Bean
    public WebLocaleResolverFilter webLocaleResolverFilter() {
        return new WebLocaleResolverFilter();
    }

    @Bean
    public CharacterEncodingFilter characterEncodingFilter() {
        CharacterEncodingFilter encodingFilter = new CharacterEncodingFilter("UTF-8");
        encodingFilter.setForceRequestEncoding(true);
        encodingFilter.setForceResponseEncoding(true);
        return encodingFilter;
    }

    @Bean
    public LocaleResolver localeResolver() {
        return new WebLocaleResolver();
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        final String pathPattern = "/**";

        registry.addInterceptor(loggingConfigurerInterceptor())
                .addPathPatterns(pathPattern);

        registry.addInterceptor(netAclInterceptor)
                .addPathPatterns(pathPattern);

        registry.addInterceptor(metricsInterceptor)
                .addPathPatterns(pathPattern);
    }

    // Запрещаем преобразовывать float-литералы в int, long, BigInteger поля для mediatype: application/json
    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter = StreamEx.of(converters)
                .findFirst(MappingJackson2HttpMessageConverter.class::isInstance)
                .map(MappingJackson2HttpMessageConverter.class::cast)
                .orElse(null);

        if (mappingJackson2HttpMessageConverter != null) {
            Module dateTimeModule = JsonUtils.createLocalDateTimeModule();

            SimpleModule dateModule = new SimpleModule(TestStepsAppConfiguration.class.getName());
            dateModule.addSerializer(LocalDate.class, new LocalDateSerializer());
            dateModule.addDeserializer(LocalDate.class, new LocalDateDeserializer());

            mappingJackson2HttpMessageConverter.getObjectMapper()
                    .registerModule(dateTimeModule)
                    .registerModule(dateModule)
                    .disable(DeserializationFeature.ACCEPT_FLOAT_AS_INT);
        }
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry
                .addResourceHandler("/webjars/**",
                        "/tests-teps/webjars/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/");
        registry
                .addResourceHandler("/static/**",
                        "/test-steps/static/**")
                .addResourceLocations("/static/");
    }

    // переопределенные бины-заглушки
    //

    /**
     * Стаб клиента к RBAC
     */
    @Bean(name = RbacService.RBAC_SERVICE)
    public RbacService rbacService(
            ShardHelper shardHelper,
            DslContextProvider dslContextProvider,
            PpcRbac ppcRbac,
            RbacClientsRelations rbacClientsRelations
    ) {
        return new RbacService(
                shardHelper,
                dslContextProvider,
                ppcRbac,
                rbacClientsRelations
        );
    }

    @Bean
    public IdmGroupSteps idmGroupSteps() {
        return new IdmGroupSteps();
    }

    /**
     * Новый и недоработанный репозиторий для кампаний
     */
    @Bean
    public CampaignRepository campaignRepository0() {
        return new CampaignRepository();
    }

    /**
     * Репозиторий для сохранения тестовых клиентов
     */
    @Bean
    public TestClientRepository testClientRepository() {
        return new TestClientRepository();
    }


    @Bean
    public ClientSteps clientSteps(
            BaseUserSteps baseUserSteps,
            ClientRepository clientRepository,
            TestClientRepository testClientRepository,
            ShardSupport shardSupport,
            TestClientLimitsRepository testClientLimitsRepository,
            TestClientNdsRepository testClientNdsRepository,
            TestAgencyNdsRepository testAgencyNdsRepository,
            ClientLimitsRepository clientLimitsRepository,
            ClientOptionsRepository clientOptionsRepository,
            RbacClientsRelations rbacClientsRelations,
            BalanceClient balanceClient) {
        return new ClientSteps(baseUserSteps, clientRepository, testClientRepository, null, shardSupport,
                testClientLimitsRepository, clientLimitsRepository, testClientNdsRepository,
                testAgencyNdsRepository, clientOptionsRepository, rbacClientsRelations, balanceClient);
    }

    @Bean
    public TestAgencyRepository testAgencyRepository() {
        return new TestAgencyRepository();
    }

    @Bean
    public UserSteps userSteps(
            BaseUserSteps baseUserSteps,
            UserRepository userRepository,
            ClientSteps clientSteps, BalanceClient balanceClient,
            BlackboxUserService blackboxUserService) {
        return new UserSteps(baseUserSteps, userRepository, clientSteps,
                null, blackboxUserService, balanceClient, testAgencyRepository());
    }

    @Bean
    public PricePackageSteps pricePackageSteps(PricePackageRepository pricePackageRepository,
                                               DslContextProvider dslContextProvider) {
        return new PricePackageSteps(pricePackageRepository, dslContextProvider);
    }

    @Bean
    public CampaignSteps campaignStepsUnstubbed(ClientSteps clientSteps,
                                                InternalAdProductSteps internalAdProductSteps,
                                                PricePackageSteps pricePackageSteps,
                                                CampaignRepository campaignRepository0,
                                                ru.yandex.direct.core.entity.campaign.repository.CampaignRepository campaignRepository,
                                                TestCampaignRepository testCampaignRepository,
                                                TestCpmYndxFrontpageRepository testCpmYndxFrontpageRepository,
                                                CampaignsMulticurrencySumsRepository campaignsMulticurrencySumsRepository,
                                                DslContextProvider dslContextProvider,
                                                CampaignModifyRepository campaignModifyRepository,
                                                GrutUacCampaignService grutUacCampaignService,
                                                GrutTransactionProvider grutTransactionProvider,
                                                BrandSurveyRepository brandsurveyRepository,
                                                GrutApiService grutApiService) {
        return new CampaignSteps(clientSteps, internalAdProductSteps, pricePackageSteps, campaignRepository0,
                campaignRepository,
                testCampaignRepository,
                testCpmYndxFrontpageRepository, campaignsMulticurrencySumsRepository, dslContextProvider,
                campaignModifyRepository, grutUacCampaignService, grutTransactionProvider, brandsurveyRepository,
                grutApiService);
    }

    @Bean
    public TestUserRepository testUserRepository() {
        return new TestUserRepository();
    }

    @Bean
    public TestSmsQueueRepository testSmsQueueRepository() {
        return new TestSmsQueueRepository();
    }

    @Bean
    public TestCampaignRepository testCampaignRepository(DslContextProvider dslContextProvider) {
        return new TestCampaignRepository(dslContextProvider);
    }

    @Bean
    public PassportClientStub passportClientStub() {
        return null;
    }

    @Bean
    public MinusKeywordsPackSteps minusKeywordsPackSteps(
            ClientSteps clientSteps,
            MinusKeywordsPackRepository minusKeywordsPackRepository,
            TestMinusKeywordsPackRepository testMinusKeywordsPackRepository
    ) {
        return new MinusKeywordsPackSteps(clientSteps, minusKeywordsPackRepository, testMinusKeywordsPackRepository);
    }

    @Bean
    public TurboLandingStepsService turboLandingStepsService(
            TurboLandingSteps turboLandingsSteps,
            InfoHelper infoHelper
    ) {
        return new TurboLandingStepsService(turboLandingsSteps, infoHelper);
    }

    @Bean
    public TurboLandingSteps turboLandingSteps() {
        return new TurboLandingSteps();
    }

    @Bean
    public SitelinkRepository sitelinkRepository(ShardHelper shardHelper, DslContextProvider dslContextProvider) {
        return new SitelinkRepository(shardHelper, dslContextProvider);
    }

    @Bean
    public SitelinkSetRepository sitelinkSetRepository(ShardHelper shardHelper, ShardSupport shardSupport,
                                                       SitelinkRepository sitelinkRepository,
                                                       DslContextProvider dslContextProvider) {
        return new SitelinkSetRepository(shardHelper, shardSupport, sitelinkRepository, dslContextProvider);
    }

    @Bean
    public TestSitelinkSetRepository TestSitelinkSetRepository(DslContextProvider dslContextProvider) {
        return new TestSitelinkSetRepository(dslContextProvider);
    }

    @Bean
    public SitelinkSetSteps sitelinkSetSteps(ClientSteps clientSteps,
                                             SitelinkRepository sitelinkRepository,
                                             SitelinkSetRepository sitelinkSetRepository,
                                             TestSitelinkSetRepository testSitelinkSetRepository) {
        return new SitelinkSetSteps(clientSteps, sitelinkRepository, sitelinkSetRepository, testSitelinkSetRepository);
    }

    @Bean
    public SiteLinkStepsService siteLinkStepsService(SitelinkSetSteps sitelinkSetSteps, InfoHelper infoHelper) {
        return new SiteLinkStepsService(sitelinkSetSteps, infoHelper);
    }

    @Bean
    public TestMinusKeywordsPackRepository testMinusKeywordsPackRepository(
            DslContextProvider dslContextProvider
    ) {
        return new TestMinusKeywordsPackRepository(dslContextProvider);
    }

    @Bean(name = IntegrationsConfiguration.METRIKA_CLIENT)
    public MetrikaClient metrikaClient(MetrikaClientStub metrikaClientStub) {
        return metrikaClientStub;
    }

    @Bean(name = IntegrationsConfiguration.METRIKA_HELPER)
    public MetrikaHelper metrikaHelper(MetrikaClient metrikaClient) {
        return new MetrikaHelperStub(metrikaClient);
    }

    @Bean
    public MetrikaClientStub metrikaClientStub() {
        return spy(new MetrikaClientStub());
    }

    @Bean
    public TypedCampaignStepsUnstubbed typedCampaignStepsUnstubbed(
            CampaignModifyRepository campaignModifyRepository,
            CampaignAddOperationSupportFacade campaignAddOperationSupportFacade,
            DslContextProvider dslContextProvider,
            MetrikaClient metrikaClient) {
        return new TypedCampaignStepsUnstubbed(campaignModifyRepository, campaignAddOperationSupportFacade,
                dslContextProvider, metrikaClient);
    }

    @Bean
    public AdGroupSteps adGroupSteps() {
        return new AdGroupSteps();
    }

    @Bean
    public CampaignsMobileContentSteps campaignsMobileContentSteps(DslContextProvider dslContextProvider) {
        return new CampaignsMobileContentSteps(dslContextProvider);
    }

    @Bean
    public PlacementSteps placementSteps(TestPlacementRepository placementRepository,
                                         DslContextProvider dslContextProvider) {
        return new PlacementSteps(placementRepository, dslContextProvider);
    }

    @Bean
    public TestPlacementRepository testPlacementRepository(DslContextProvider dslContextProvider,
                                                           PlacementBlockRepository placementBlockRepository,
                                                           PlacementsOutdoorDataRepository placementsOutdoorDataRepository) {
        return new TestPlacementRepository(dslContextProvider, placementBlockRepository,
                placementsOutdoorDataRepository);
    }

    @Bean
    public FeedSteps feedSteps(ShardHelper shardHelper, ClientSteps clientSteps,
                               FeedRepository feedRepository, TestFeedCategoryRepository categoryRepository,
                               TestFeedHistoryRepository historyRepository) {
        return new FeedSteps(shardHelper, clientSteps, feedRepository, categoryRepository, historyRepository);
    }

    @Bean
    public TestFeedCategoryRepository testFeedCategoryRepository(DslContextProvider dslContextProvider) {
        return new TestFeedCategoryRepository(dslContextProvider);
    }

    @Bean
    public TestFeedHistoryRepository testFeedHistoryRepository(DslContextProvider dslContextProvider) {
        return new TestFeedHistoryRepository(dslContextProvider);
    }

    @Bean
    public TestLalSegmentRepository testLalSegmentsRepository(DslContextProvider dslContextProvider,
                                                              LalSegmentRepository lalSegmentRepository) {
        return new TestLalSegmentRepository(dslContextProvider, lalSegmentRepository);
    }

    @Bean
    public TestAdGroupRepository testAdGroupRepository() {
        return new TestAdGroupRepository();
    }

    @Bean
    public TestModerationRepository testModerationRepository() {
        return new TestModerationRepository();
    }

    @Bean
    public TestClientOptionsRepository testClientOptionsRepository() {
        return new TestClientOptionsRepository();
    }

    @Bean
    public BannerSteps bannerSteps() {
        return new BannerSteps();
    }

    @Bean
    public CreativeSteps creativeSteps() {
        return new CreativeSteps();
    }

    @Bean
    public KeywordSteps keywordSteps() {
        return new KeywordSteps();
    }

    @Bean
    public CalloutSteps calloutSteps() {
        return new CalloutSteps();
    }

    @Bean
    public PerformanceFiltersSteps performanceFiltersSteps() {
        return new PerformanceFiltersSteps();
    }

    @Bean
    public RetargetingSteps retargetingSteps() {
        return new RetargetingSteps();
    }

    @Bean
    public RetConditionSteps retConditionSteps() {
        return new RetConditionSteps();
    }

    @Bean
    public ClientOptionsSteps clientOptionsSteps(TestClientOptionsRepository testClientOptionsRepository,
                                                 ClientOptionsRepository clientOptionsRepository) {
        return new ClientOptionsSteps(testClientOptionsRepository, clientOptionsRepository);
    }

    @Bean
    public MetrikaHelperStub metrikaHelperStub() {
        return new MetrikaHelperStub();
    }

    @Bean
    public InternalAdProductSteps internalAdProductSteps(ClientSteps clientSteps,
                                                         ClientService clientService,
                                                         UserService userService,
                                                         InternalAdsProductService internalAdsProductService) {
        return new InternalAdProductSteps(clientSteps, clientService, userService, internalAdsProductService);
    }

    @Bean
    public TestBannerCreativeRepository testBannerCreativeRepository() {
        return new TestBannerCreativeRepository();
    }

    @Bean
    public TestBannerRepository testBannerRepository() {
        return new TestBannerRepository();
    }

    @Bean
    public TestBannerImageRepository testBannerImageRepository(DslContextProvider dslContextProvider,
                                                               ShardHelper shardHelper) {
        return new TestBannerImageRepository(dslContextProvider, shardHelper);
    }

    @Bean
    public TestBannerPixelsRepository testBannerPixelsRepository(OldBannerPixelsRepository bannerPixelsRepository) {
        return new TestBannerPixelsRepository(bannerPixelsRepository);
    }

    @Bean
    public TestImageRepository testImageRepository(DslContextProvider dslContextProvider,
                                                   ShardHelper shardHelper) {
        return new TestImageRepository(dslContextProvider, shardHelper);
    }

    @Bean
    public TestBannerImageFormatRepository testBannerImageFormatRepository() {
        return new TestBannerImageFormatRepository();
    }

    @Bean
    public TestCreativeRepository testCreativeRepository() {
        return new TestCreativeRepository();
    }

    @Bean
    public CryptaSegmentRepository cryptaSegmentRepository(DslContextProvider dslContextProvider) {
        return new CryptaSegmentRepository(dslContextProvider);
    }

    @Bean
    public TestCryptaSegmentRepository testCryptaSegmentRepository(DslContextProvider dslContextProvider,
                                                                   CryptaSegmentRepository cryptaSegmentRepository) {
        return new TestCryptaSegmentRepository(cryptaSegmentRepository, dslContextProvider);
    }

    @Bean
    public TestKeywordRepository testKeywordRepository(DslContextProvider dslContextProvider) {
        return new TestKeywordRepository(dslContextProvider);
    }

    @Bean
    @RequestScope
    public GrutContext grutContext(GrutClient grutClient) {
        return new RequestScopeGrutContext(grutClient);
    }

    @Bean
    public FeatureSteps featureSteps(FeatureManagingService featureManagingService,
                                     ClientFeaturesRepository clientFeaturesRepository,
                                     DirectAuthContextService directAuthContextService) {
        return new FeatureSteps(featureManagingService, clientFeaturesRepository, directAuthContextService);
    }
}
