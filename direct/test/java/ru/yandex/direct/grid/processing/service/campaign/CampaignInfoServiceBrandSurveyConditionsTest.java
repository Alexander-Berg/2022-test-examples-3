package ru.yandex.direct.grid.processing.service.campaign;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.ParametersAreNonnullByDefault;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.naming.TestCaseName;
import org.assertj.core.api.JUnitSoftAssertions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.common.db.PpcProperty;
import ru.yandex.direct.config.DirectConfig;
import ru.yandex.direct.core.aggregatedstatuses.AggregatedStatusesViewService;
import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository;
import ru.yandex.direct.core.entity.autobudget.service.AutobudgetAlertService;
import ru.yandex.direct.core.entity.banner.repository.BannerCommonRepository;
import ru.yandex.direct.core.entity.brandlift.service.targetestimation.TargetEstimation;
import ru.yandex.direct.core.entity.brandlift.service.targetestimation.TargetEstimationsService;
import ru.yandex.direct.core.entity.campaign.model.BrandSurveyStatus;
import ru.yandex.direct.core.entity.campaign.model.Campaign;
import ru.yandex.direct.core.entity.campaign.model.CpmBannerCampaign;
import ru.yandex.direct.core.entity.campaign.model.SurveyStatus;
import ru.yandex.direct.core.entity.campaign.repository.CampaignModifyRepository;
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository;
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository;
import ru.yandex.direct.core.entity.campaign.service.BrandSurveyConditionsService;
import ru.yandex.direct.core.entity.campaign.service.CampaignStrategyService;
import ru.yandex.direct.core.entity.campaign.service.TimeTargetStatusService;
import ru.yandex.direct.core.entity.campaign.service.accesschecker.CampaignSubObjectAccessCheckerFactory;
import ru.yandex.direct.core.entity.campaign.service.accesschecker.core.AllowedTypesCampaignAccessibilityChecker;
import ru.yandex.direct.core.entity.campaign.service.pricerecalculation.CommonCampaignPriceRecalculationService;
import ru.yandex.direct.core.entity.campaign.service.validation.type.update.CampaignWithCustomStrategyUpdateValidationTypeSupport;
import ru.yandex.direct.core.entity.cashback.service.CashbackClientsService;
import ru.yandex.direct.core.entity.client.model.Client;
import ru.yandex.direct.core.entity.client.service.ClientService;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.entity.measurers.repository.CampMeasurersRepository;
import ru.yandex.direct.core.entity.metrika.service.MetrikaGoalsService;
import ru.yandex.direct.core.entity.pricepackage.model.PricePackage;
import ru.yandex.direct.core.entity.pricepackage.model.PricePackageCampaignOptions;
import ru.yandex.direct.core.entity.pricepackage.repository.PricePackageRepository;
import ru.yandex.direct.core.entity.pricepackage.service.PricePackageService;
import ru.yandex.direct.core.entity.statistics.service.OrderStatService;
import ru.yandex.direct.core.entity.tag.repository.TagRepository;
import ru.yandex.direct.core.entity.timetarget.service.GeoTimezoneMappingService;
import ru.yandex.direct.core.entity.vcard.service.VcardHelper;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.grid.core.entity.campaign.repository.GridMobileContentSuggestInfoRepository;
import ru.yandex.direct.grid.core.entity.campaign.service.GridCampaignService;
import ru.yandex.direct.grid.core.entity.model.GdiEntityStats;
import ru.yandex.direct.grid.core.entity.model.campaign.GdiCampaignStats;
import ru.yandex.direct.grid.core.entity.recommendation.service.GridRecommendationService;
import ru.yandex.direct.grid.core.util.stats.GridStatNew;
import ru.yandex.direct.grid.model.campaign.GdBrandSurveyStopReason;
import ru.yandex.direct.grid.model.campaign.GdCampaignPlatform;
import ru.yandex.direct.grid.model.campaign.GdCampaignType;
import ru.yandex.direct.grid.model.campaign.GdiWalletAction;
import ru.yandex.direct.grid.model.entity.campaign.strategy.GdStrategyExtractorFacade;
import ru.yandex.direct.grid.processing.model.campaign.GdCpmCampaignBrandSurveyCheckData;
import ru.yandex.direct.grid.processing.model.campaign.GdCpmCampaignBrandSurveyCheckRequest;
import ru.yandex.direct.grid.processing.model.campaign.GdCpmCampaignBrandSurveyCheckResponseItem;
import ru.yandex.direct.grid.processing.model.campaign.GdMultipleCampaignsBrandSurveyCheckRequest;
import ru.yandex.direct.grid.processing.model.campaign.GdMultipleCampaignsBrandSurveyCheckResponse;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdCampaignBiddingStrategy;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdCampaignStrategyData;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdCampaignStrategyName;
import ru.yandex.direct.grid.processing.service.campaign.loader.BannerGeoLegalFlagsInAggrStatusDataLoader;
import ru.yandex.direct.grid.processing.service.campaign.loader.CampaignAdsCountDataLoader;
import ru.yandex.direct.grid.processing.service.campaign.loader.CampaignAgencyInfoByAgencyUidDataLoader;
import ru.yandex.direct.grid.processing.service.campaign.loader.CampaignBrandSurveyStatusDataLoader;
import ru.yandex.direct.grid.processing.service.campaign.loader.CampaignDomainsDataLoader;
import ru.yandex.direct.grid.processing.service.campaign.loader.CampaignHrefIsTurboDataLoader;
import ru.yandex.direct.grid.processing.service.campaign.loader.CampaignManagerInfoByManagerUidDataLoader;
import ru.yandex.direct.grid.processing.service.campaign.loader.CampaignPayForConversionInfoDataLoader;
import ru.yandex.direct.grid.processing.service.campaign.loader.CampaignsGroupsCountDataLoader;
import ru.yandex.direct.grid.processing.service.campaign.loader.CampaignsHasRunningUnmoderatedAdsDataLoader;
import ru.yandex.direct.grid.processing.service.campaign.loader.CampaignsLastChangedVcardIdDataLoader;
import ru.yandex.direct.grid.processing.service.client.ClientDataService;
import ru.yandex.direct.grid.processing.service.dataloader.GridContextProvider;
import ru.yandex.direct.grid.processing.service.placement.PlacementDataService;
import ru.yandex.direct.grid.processing.service.showcondition.retargeting.RetargetingConditionDataLoader;
import ru.yandex.direct.metrika.client.MetrikaClient;
import ru.yandex.direct.rbac.RbacService;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.common.db.PpcPropertyNames.BRAND_SURVEY_BUDGET_DATE;
import static ru.yandex.direct.grid.model.entity.campaign.strategy.GdStrategyExtractorHelper.STRATEGIES_EXTRACTORS_BY_TYPES;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@RunWith(JUnitParamsRunner.class)
@ParametersAreNonnullByDefault
public class CampaignInfoServiceBrandSurveyConditionsTest {
    private static final long TEST_CLIENT_ID = 19;
    private static final long TEST_CID = 2L;
    private static final GdiCampaignStats TEST_STAT = new GdiCampaignStats()
            .withStat(GridStatNew.addZeros(new GdiEntityStats()))
            .withGoalStats(emptyList());

    @Rule
    public JUnitSoftAssertions softly = new JUnitSoftAssertions();

    @Mock
    private GridCampaignService gridCampaignService;

    @Mock
    private GeoTimezoneMappingService geoTimezoneMappingService;

    @Mock
    private TimeTargetStatusService timeTargetStatusService;

    @Mock
    private CampaignAccessService campaignAccessService;

    @Mock
    private GridRecommendationService gridRecommendationService;

    @Mock
    private CampaignRepository campaignRepository;

    @Mock
    private CampaignSubObjectAccessCheckerFactory campaignSubObjectAccessCheckerFactory;

    @Mock
    private ClientService clientService;

    @Mock
    private FeatureService featureService;

    @Mock
    private PpcPropertiesSupport ppcPropertiesSupport;

    @Mock
    private RetargetingConditionDataLoader retargetingConditionDataLoader;

    @Mock
    ConversionStrategyLearningStatusDataLoader conversionStrategyLearningStatusDataLoader;

    @Mock
    private VcardDataLoader vcardDataLoader;

    @Mock
    private VcardHelper vcardHelper;

    @Mock
    private ShardHelper shardHelper;

    @Mock
    private CampaignsLastChangedVcardIdDataLoader campaignsLastChangedVcardIdDataLoader;

    @Mock
    private BannerGeoLegalFlagsInAggrStatusDataLoader bannerGeoLegalFlagsInAggrStatusDataLoader;

    @Mock
    private CampaignAdsCountDataLoader campaignAdsCountDataLoader;

    @Mock
    private CampaignsGroupsCountDataLoader campaignsGroupsCountDataLoader;

    @Mock
    private CampaignsHasRunningUnmoderatedAdsDataLoader campaignsHasRunningUnmoderatedBannersDataLoader;

    @Mock
    CampaignPayForConversionInfoDataLoader campaignPayForConversionInfoDataLoader;

    @Mock
    private CampaignHrefIsTurboDataLoader campaignHrefIsTurboDataLoader;

    @Mock
    private CampaignDomainsDataLoader campaignDomainsDataLoader;

    @Mock
    private AggregatedStatusesViewService aggregatedStatusesViewService;

    @Mock
    private AdGroupRepository adGroupRepository;

    @Mock
    private TagRepository tagRepository;

    @Mock
    private MetrikaGoalsService metrikaGoalsService;

    @Mock
    private PricePackageService pricePackageService;

    @Mock
    private CampaignTypedRepository campaignTypedRepository;

    @Mock
    private CampaignStrategyService campaignStrategyService;

    @Mock
    private CampaignValidationService campaignValidationService;

    @Mock
    private RegionDescriptionLocalizer regionDescriptionLocalizer;

    @Mock
    private GridContextProvider gridContextProvider;

    @Mock
    private BrandSurveyConditionsService brandSurveyConditionsService;

    @Mock
    private BannerCommonRepository bannerCommonRepository;

    @Mock
    private PricePackageRepository pricePackageRepository;

    @Mock
    private OrderStatService orderStatService;

    @Mock
    private AutobudgetAlertService autobudgetAlertService;

    @Mock
    private GridMobileContentSuggestInfoRepository gridMobileContentSuggestInfoRepository;

    @Mock
    private CashbackClientsService cashbackClientsService;

    @Mock
    private ClientDataService clientDataService;

    @Mock
    private PlacementDataService placementDataService;

    @Mock
    private RbacService rbacService;

    @Mock
    private CampMeasurersRepository measurersRepository;

    @Mock
    private TargetEstimationsService targetEstimationsService;

    @Mock
    private DirectConfig directConfig;

    @Mock
    CampaignAgencyInfoByAgencyUidDataLoader campaignAgencyInfoByAgencyUidDataLoader;

    @Mock
    CampaignManagerInfoByManagerUidDataLoader campaignManagerInfoByManagerUidDataLoader;

    @Mock
    CampaignBrandSurveyStatusDataLoader campaignBrandSurveyStatusDataLoader;

    @Mock
    CampaignModifyRepository campaignModifyRepository;

    @Mock
    CampaignWithCustomStrategyUpdateValidationTypeSupport campaignWithStrategyUpdateValidationTypeSupport;

    @Mock
    CommonCampaignPriceRecalculationService commonCampaignPriceRecalculationService;

    @Mock
    MetrikaClient metrikaClient;

    @InjectMocks
    private GridCampaignAggregationFieldsService gridCampaignAggregationFieldsService;


    private CampaignInfoService campaignInfoService;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);

        GdStrategyExtractorFacade gdStrategyExtractorFacade =
                new GdStrategyExtractorFacade(STRATEGIES_EXTRACTORS_BY_TYPES);

        GridCampaignStrategyService gridCampaignStrategyService = new GridCampaignStrategyService(
                gdStrategyExtractorFacade);
        doReturn(Collections.singletonMap(TEST_CID, TEST_STAT))
                .when(gridCampaignService).getCampaignStats(any(), any(), any(),
                        eq(emptySet()), isNull(), anySet());
        doReturn(null).when(gridCampaignService).getPaidDaysLeft(any(), anyCollection());
        doReturn(true)
                .when(featureService).isEnabledForClientId(any(ClientId.class),
                        eq(FeatureName.SHOW_CPM_BANNER_CAMPAIGNS_IN_GRID));

        doReturn(EnumSet.allOf(GdCampaignType.class))
                .when(campaignAccessService).getVisibleTypes(eq(ClientId.fromLong(TEST_CLIENT_ID)));
        when(campaignAccessService.getCampaignAccessibilityChecker(eq(ClientId.fromLong(TEST_CLIENT_ID))))
                .thenReturn(AllowedTypesCampaignAccessibilityChecker.ALL);
        doReturn(Set.of(GdiWalletAction.PAY))
                .when(campaignAccessService).getWalletActions(any(), any(), any());

        when(clientService.getClient(any())).thenReturn(new Client().withClientId(TEST_CLIENT_ID));

        when(directConfig.getBranch(anyString())).thenReturn(directConfig);

        campaignStrategyService = new CampaignStrategyService(campaignModifyRepository, shardHelper, rbacService,
                campaignRepository, campaignTypedRepository, campaignWithStrategyUpdateValidationTypeSupport,
                orderStatService,
                autobudgetAlertService, commonCampaignPriceRecalculationService, featureService, metrikaClient);

        campaignInfoService = new CampaignInfoService(gridCampaignService, gridCampaignAggregationFieldsService,
                gridCampaignStrategyService, campaignStrategyService, campaignAccessService,
                campaignValidationService, gridRecommendationService, campaignRepository,
                campaignSubObjectAccessCheckerFactory, clientService, shardHelper,
                campaignsLastChangedVcardIdDataLoader, campaignAdsCountDataLoader, campaignsGroupsCountDataLoader,
                campaignsHasRunningUnmoderatedBannersDataLoader, campaignPayForConversionInfoDataLoader,
                retargetingConditionDataLoader, campaignHrefIsTurboDataLoader, campaignDomainsDataLoader,
                bannerGeoLegalFlagsInAggrStatusDataLoader, campaignAgencyInfoByAgencyUidDataLoader,
                campaignManagerInfoByManagerUidDataLoader, campaignBrandSurveyStatusDataLoader, vcardHelper,
                vcardDataLoader, conversionStrategyLearningStatusDataLoader, featureService,
                aggregatedStatusesViewService, adGroupRepository, tagRepository,
                metrikaGoalsService, campaignTypedRepository, pricePackageService, regionDescriptionLocalizer,
                gridContextProvider, gdStrategyExtractorFacade, brandSurveyConditionsService, timeTargetStatusService,
                geoTimezoneMappingService, bannerCommonRepository, pricePackageRepository,
                orderStatService, autobudgetAlertService, gridMobileContentSuggestInfoRepository,
                cashbackClientsService, clientDataService, placementDataService, rbacService, measurersRepository,
                targetEstimationsService, directConfig, ppcPropertiesSupport);
    }

    @Test
    @Parameters(method = "brandSurveyBudgetDateParams")
    @TestCaseName("{0}")
    public void testGetBrandSurveyConditions_BrandSurveyBudgetDateProperty(String desc,
                                                                           LocalDateTime campCreateTime,
                                                                           LocalDate propertyVal,
                                                                           int expectedDayBudgetThreshold,
                                                                           int expectedTotalBudgetThreshold) {
        PpcProperty<LocalDate> property = mock(PpcProperty.class);
        when(property.get()).thenReturn(propertyVal);
        doReturn(property).when(ppcPropertiesSupport).get(BRAND_SURVEY_BUDGET_DATE);
        List<Campaign> campaigns = campCreateTime == null ? emptyList() :
                asList(new Campaign().withId(1L).withCreateTime(campCreateTime));

        doReturn(campaigns).when(campaignRepository).getCampaigns(anyInt(), anyCollection());
        doReturn(emptyMap()).when(pricePackageService).getPricePackageByCampaigns(anyInt(), any());
        doReturn(mapList(campaigns, c -> new CpmBannerCampaign().withId(c.getId())))
                .when(campaignTypedRepository).getTypedCampaigns(anyInt(), anyCollection());
        doReturn(new Client().withWorkCurrency(CurrencyCode.RUB)).when(clientService).getClient(any());

        doReturn(new BrandSurveyStatus()
                .withSumSpentByTotalPeriod(BigDecimal.valueOf(1L))
                .withSumSpentByDay(BigDecimal.valueOf(2L)))
                .when(brandSurveyConditionsService)
                .getBrandSurveyStatus(any(), any(), any(), any(), any(), anyBoolean(), any(), anyBoolean(), anyList());
        when(brandSurveyConditionsService.brandSurveyBudgetThreshold(any(), any(), any())).thenCallRealMethod();
        when(brandSurveyConditionsService.brandSurveyBudgetThresholdDaily(any(), any(), any())).thenCallRealMethod();
        when(directConfig.getLong(anyString())).thenReturn(1600000L);
        doReturn(List.of(new TargetEstimation(1L, 1000000L, 0),
                new TargetEstimation(2L, 2000000L, 0)))
                .when(targetEstimationsService).getTargetEstimations(any());

        GdMultipleCampaignsBrandSurveyCheckResponse response =
                campaignInfoService.getBrandSurveyConditions(ClientId.fromLong(TEST_CLIENT_ID), 0L,
                        new GdMultipleCampaignsBrandSurveyCheckRequest().withCampaignIds(campaigns.stream().map(Campaign::getId).collect(Collectors.toSet())));

        assertThat(response.getDayBudgetThreshold()).isEqualByComparingTo(BigDecimal.valueOf(expectedDayBudgetThreshold));
        assertThat(response.getTotalBudgetThreshold()).isEqualByComparingTo(BigDecimal.valueOf(expectedTotalBudgetThreshold));
    }


    public Object[][] brandSurveyBudgetDateParams() {
        return new Object[][]{
                {"Для выключенной проперти BRAND_SURVEY_BUDGET_DATE старые лимиты",
                        null, null, 70_000, 1_000_000},
                {"Для новой РК смотрим дату из проперти. Дата проперти в прошлом",
                        null, LocalDate.now().minusDays(1), 70_000, 1_000_000},
                {"Для новой РК смотрим дату из проперти. Дата проперти в будущем",
                        null, LocalDate.now().plusDays(1), 70_000, 1_000_000},
                {"Для существующей РК. Дата проперти в прошлом",
                        LocalDateTime.now(), LocalDate.now().minusDays(1), 70_000, 1_000_000},
                {"Для существующей РК. Дата проперти в будущем",
                        LocalDateTime.now(), LocalDate.now().plusDays(1), 70_000, 1_000_000},
        };
    }


    @Test
    public void testGetBrandSurveyConditions_multipleCampaigns_correctSum() {
        PpcProperty<LocalDate> property = mock(PpcProperty.class);
        when(property.get()).thenReturn(null);
        doReturn(property).when(ppcPropertiesSupport).get(BRAND_SURVEY_BUDGET_DATE);
        var campaign1 = new Campaign().withId(1L);
        var campaign2 = new Campaign().withId(2L);
        List<Campaign> campaigns = asList(campaign1, campaign2);

        doReturn(campaigns).when(campaignRepository).getCampaigns(anyInt(), anyCollection());
        doReturn(emptyMap()).when(pricePackageService).getPricePackageByCampaigns(anyInt(), any());
        doReturn(mapList(campaigns, c -> new CpmBannerCampaign().withId(c.getId())))
                .when(campaignTypedRepository).getTypedCampaigns(anyInt(), anyCollection());
        doReturn(new Client().withWorkCurrency(CurrencyCode.RUB)).when(clientService).getClient(any());

        doReturn(new BrandSurveyStatus()
                        .withSumSpentByTotalPeriod(CurrencyCode.RUB.getCurrency().getBrandSurveyBudgetThreshold().subtract(BigDecimal.ONE))
                        .withSumSpentByDay(CurrencyCode.RUB.getCurrency().getBrandSurveyBudgetThresholdDaily().subtract(BigDecimal.ONE)),
                new BrandSurveyStatus()
                        .withSumSpentByTotalPeriod(BigDecimal.valueOf(1L))
                        .withSumSpentByDay(BigDecimal.valueOf(2L)))
                .when(brandSurveyConditionsService)
                .getBrandSurveyStatus(any(), any(), any(), any(), any(), anyBoolean(), any(), anyBoolean(), anyList());
        when(brandSurveyConditionsService.brandSurveyBudgetThreshold(any(), any(), any())).thenCallRealMethod();
        when(brandSurveyConditionsService.brandSurveyBudgetThresholdDaily(any(), any(), any())).thenCallRealMethod();
        when(directConfig.getLong(anyString())).thenReturn(1600000L);
        doReturn(List.of(new TargetEstimation(1L, 1000000L, 0),
                new TargetEstimation(2L, 2000000L, 0)))
                .when(targetEstimationsService).getTargetEstimations(any());

        GdMultipleCampaignsBrandSurveyCheckResponse response =
                campaignInfoService.getBrandSurveyConditions(ClientId.fromLong(TEST_CLIENT_ID), 0L,
                        new GdMultipleCampaignsBrandSurveyCheckRequest().withCampaignIds(campaigns.stream().map(Campaign::getId).collect(Collectors.toSet())));

        assertThat(response.getForecast()).isEqualTo(3000000L);
        assertThat(response.getLowForecast()).isFalse();
        assertThat(response.getDayBudgetThreshold()).isEqualByComparingTo(CurrencyCode.RUB.getCurrency().getBrandSurveyBudgetThresholdDaily());
        assertThat(response.getTotalBudgetThreshold()).isEqualByComparingTo(CurrencyCode.RUB.getCurrency().getBrandSurveyBudgetThreshold());
        assertThat(response.getLowBudget()).isFalse();
    }


    @Test
    public void testGetBrandSurveyConditions_multipleCampaigns_emptyList() {
        List<Campaign> campaigns = emptyList();
        doReturn(campaigns).when(campaignRepository).getCampaigns(anyInt(), anyCollection());
        doReturn(emptyMap()).when(pricePackageService).getPricePackageByCampaigns(anyInt(), any());
        doReturn(new Client().withWorkCurrency(CurrencyCode.RUB)).when(clientService).getClient(any());
        when(directConfig.getLong(anyString())).thenReturn(1600000L);
        when(brandSurveyConditionsService.brandSurveyBudgetThreshold(any(), any(), any())).thenCallRealMethod();
        when(brandSurveyConditionsService.brandSurveyBudgetThresholdDaily(any(), any(), any())).thenCallRealMethod();
        doReturn(mock(PpcProperty.class)).when(ppcPropertiesSupport).get(BRAND_SURVEY_BUDGET_DATE);

        GdMultipleCampaignsBrandSurveyCheckResponse response =
                campaignInfoService.getBrandSurveyConditions(ClientId.fromLong(TEST_CLIENT_ID), 0L,
                        new GdMultipleCampaignsBrandSurveyCheckRequest().withCampaignIds(campaigns.stream().map(Campaign::getId).collect(Collectors.toSet())));

        assertThat(response.getForecast()).isEqualTo(0L);
        assertThat(response.getDayBudgetThreshold()).isEqualByComparingTo(CurrencyCode.RUB.getCurrency().getBrandSurveyBudgetThresholdDaily());
        assertThat(response.getTotalBudgetThreshold()).isEqualByComparingTo(CurrencyCode.RUB.getCurrency().getBrandSurveyBudgetThreshold());
        assertThat(response.getLowForecast()).isTrue();
        assertThat(response.getLowBudget()).isTrue();
    }

    @Test
    public void testGetBrandSurveyConditions_multipleCampaigns_withDisabledBL() {
        var campaign1 = new Campaign().withId(1L);
        var campaign2 = new Campaign().withId(2L);
        var campaign3 = new Campaign().withId(3L);
        var campaign4 = new Campaign().withId(4L);
        List<Campaign> campaigns = asList(campaign1, campaign2, campaign3, campaign4);
        List<CpmBannerCampaign> cpmBannerCampaigns = mapList(campaigns, c -> new CpmBannerCampaign().withId(c.getId())
                .withShows(c.getId() == 4L ? 1L : 0L));

        doReturn(campaigns).when(campaignRepository).getCampaigns(anyInt(), anyCollection());
        doReturn(Map.of(1L, new PricePackage()
                .withCampaignOptions(new PricePackageCampaignOptions().withAllowBrandLift(false)))
        ).when(pricePackageService).getPricePackageByCampaigns(anyInt(), any());
        doReturn(cpmBannerCampaigns)
                .when(campaignTypedRepository).getTypedCampaigns(anyInt(), anyCollection());
        doReturn(new Client().withWorkCurrency(CurrencyCode.RUB)).when(clientService).getClient(any());
        doReturn(
                new BrandSurveyStatus()
                        .withSumSpentByTotalPeriod(BigDecimal.valueOf(1L))
                        .withSumSpentByDay(BigDecimal.valueOf(2L)),
                new BrandSurveyStatus()
                        .withSumSpentByTotalPeriod(BigDecimal.valueOf(1L))
                        .withSumSpentByDay(BigDecimal.valueOf(2L)))
                .when(brandSurveyConditionsService)
                .getBrandSurveyStatus(any(), any(), any(), any(), any(), anyBoolean(), any(), anyBoolean(), anyList());
        when(brandSurveyConditionsService.brandSurveyBudgetThreshold(any(), any(), any())).thenCallRealMethod();
        when(brandSurveyConditionsService.brandSurveyBudgetThresholdDaily(any(), any(), any())).thenCallRealMethod();
        when(directConfig.getLong(anyString())).thenReturn(1600000L);
        doReturn(mock(PpcProperty.class)).when(ppcPropertiesSupport).get(BRAND_SURVEY_BUDGET_DATE);

        doReturn(List.of(new TargetEstimation(1L, 10000000000L, 0),
                new TargetEstimation(2L, 10000L, 0),
                new TargetEstimation(3L, 10000L, 0),
                new TargetEstimation(4L, 10000000000L, 0)))
                .when(targetEstimationsService).getTargetEstimations(any());

        GdMultipleCampaignsBrandSurveyCheckResponse response =
                campaignInfoService.getBrandSurveyConditions(ClientId.fromLong(TEST_CLIENT_ID), 0L,
                        new GdMultipleCampaignsBrandSurveyCheckRequest()
                                .withCampaignIds(campaigns.stream().map(Campaign::getId).collect(Collectors.toSet())));

        assertThat(response.getForecast()).isEqualTo(20000L);
        assertThat(response.getDayBudgetThreshold()).isEqualByComparingTo(CurrencyCode.RUB.getCurrency().getBrandSurveyBudgetThresholdDaily());
        assertThat(response.getTotalBudgetThreshold()).isEqualByComparingTo(CurrencyCode.RUB.getCurrency().getBrandSurveyBudgetThreshold());
        assertThat(response.getCampaignsWithDisabledBL()).isEqualTo(List.of(1L, 4L));
        assertThat(response.getLowForecast()).isTrue();
        assertThat(response.getLowBudget()).isTrue();
    }

    @Test
    public void newCampStrategyValidationTest() {
        PpcProperty<LocalDate> property = mock(PpcProperty.class);
        when(property.get()).thenReturn(LocalDate.now().minusDays(1));
        doReturn(property).when(ppcPropertiesSupport).get(BRAND_SURVEY_BUDGET_DATE);

        doReturn(emptyMap()).when(pricePackageService).getPricePackageByCampaigns(anyInt(), any());
        doReturn(new Client().withWorkCurrency(CurrencyCode.RUB)).when(clientService).getClient(any());

        when(brandSurveyConditionsService.brandSurveyBudgetThreshold(any(), any(), any())).thenCallRealMethod();
        when(brandSurveyConditionsService.brandSurveyBudgetThresholdDaily(any(), any(), any())).thenCallRealMethod();
        doReturn(new BrandSurveyStatus()
                .withBrandSurveyStopReasonsDaily(new HashSet<>())
                .withSurveyStatusDaily(SurveyStatus.ACTIVE)
                .withSumSpentByTotalPeriod(BigDecimal.valueOf(1L))
                .withSumSpentByDay(BigDecimal.valueOf(2L)))
                .when(brandSurveyConditionsService)
                .getBrandSurveyStatus(any(), any(), any(), any(), any(), anyBoolean(), any(), anyBoolean(), anyList());
        when(directConfig.getLong(anyString())).thenReturn(1600000L);
        doReturn(List.of(new TargetEstimation(1L, 1000000L, 0),
                new TargetEstimation(2L, 2000000L, 0)))
                .when(targetEstimationsService).getTargetEstimations(any());

        List<GdCpmCampaignBrandSurveyCheckResponseItem> response =
                campaignInfoService.getBrandSurveyConditions(ClientId.fromLong(TEST_CLIENT_ID), 0L,
                        new GdCpmCampaignBrandSurveyCheckRequest()
                                .withItems(List.of(defaultGdCpmCampaignBrandSurveyCheckData())));

        assertThat(response).hasSize(1);
        var item = response.get(0);
        assertThat(item.getStatus().getBrandSurveyStopReasonsDaily()).contains(GdBrandSurveyStopReason.LOW_REACH);
    }

    private GdCpmCampaignBrandSurveyCheckData defaultGdCpmCampaignBrandSurveyCheckData() {
        return new GdCpmCampaignBrandSurveyCheckData()
                .withDayBudget(BigDecimal.ZERO)
                .withEndDate(null)
                .withStartDate(LocalDate.now())
                .withCid(70676108L)
                .withBiddingStategy(
                        new GdCampaignBiddingStrategy()
                                .withPlatform(GdCampaignPlatform.CONTEXT)
                                .withStrategyName(GdCampaignStrategyName.AUTOBUDGET_MAX_IMPRESSIONS_CUSTOM_PERIOD)
                                .withStrategyData(new GdCampaignStrategyData()
                                        .withAutoProlongation(true)
                                        .withAvgCpm(BigDecimal.valueOf(100))
                                        .withSum(BigDecimal.valueOf(201000))
                                        .withFinishDate(LocalDate.now().plusDays(30))
                                        .withStartDate(LocalDate.now())
                                )
                );
    }
}
