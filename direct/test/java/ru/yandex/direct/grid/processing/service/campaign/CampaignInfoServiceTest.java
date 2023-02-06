package ru.yandex.direct.grid.processing.service.campaign;

import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.naming.TestCaseName;
import org.apache.commons.lang.math.RandomUtils;
import org.assertj.core.api.JUnitSoftAssertions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.config.DirectConfig;
import ru.yandex.direct.core.aggregatedstatuses.AggregatedStatusesViewService;
import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository;
import ru.yandex.direct.core.entity.autobudget.service.AutobudgetAlertService;
import ru.yandex.direct.core.entity.banner.repository.BannerCommonRepository;
import ru.yandex.direct.core.entity.brandlift.service.targetestimation.TargetEstimationsService;
import ru.yandex.direct.core.entity.campaign.model.CampaignAttributionModel;
import ru.yandex.direct.core.entity.campaign.model.CampaignForAccessCheckDefaultImpl;
import ru.yandex.direct.core.entity.campaign.model.CampaignStatusModerate;
import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.campaign.model.CampaignsPlatform;
import ru.yandex.direct.core.entity.campaign.model.CampaignsPromotion;
import ru.yandex.direct.core.entity.campaign.model.CpmBannerCampaign;
import ru.yandex.direct.core.entity.campaign.model.DbStrategy;
import ru.yandex.direct.core.entity.campaign.model.StrategyData;
import ru.yandex.direct.core.entity.campaign.model.StrategyName;
import ru.yandex.direct.core.entity.campaign.repository.CampaignModifyRepository;
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository;
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository;
import ru.yandex.direct.core.entity.campaign.service.BrandSurveyConditionsService;
import ru.yandex.direct.core.entity.campaign.service.CampaignStrategyService;
import ru.yandex.direct.core.entity.campaign.service.TimeTargetStatusService;
import ru.yandex.direct.core.entity.campaign.service.accesschecker.CampaignSubObjectAccessCheckerFactory;
import ru.yandex.direct.core.entity.campaign.service.accesschecker.core.AllowedTypesCampaignAccessibilityChecker;
import ru.yandex.direct.core.entity.campaign.service.pricerecalculation.CommonCampaignPriceRecalculationService;
import ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants;
import ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstantsService;
import ru.yandex.direct.core.entity.campaign.service.validation.type.update.CampaignWithCustomStrategyUpdateValidationTypeSupport;
import ru.yandex.direct.core.entity.cashback.service.CashbackClientsService;
import ru.yandex.direct.core.entity.client.model.Client;
import ru.yandex.direct.core.entity.client.service.ClientService;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.entity.measurers.repository.CampMeasurersRepository;
import ru.yandex.direct.core.entity.metrika.service.MetrikaGoalsService;
import ru.yandex.direct.core.entity.pricepackage.repository.PricePackageRepository;
import ru.yandex.direct.core.entity.pricepackage.service.PricePackageService;
import ru.yandex.direct.core.entity.statistics.service.OrderStatService;
import ru.yandex.direct.core.entity.tag.repository.TagRepository;
import ru.yandex.direct.core.entity.timetarget.service.GeoTimezoneMappingService;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.entity.vcard.service.VcardHelper;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.currency.Money;
import ru.yandex.direct.dbschema.ppc.enums.CampaignsStrategyName;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.model.UidClientIdShard;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.grid.core.entity.campaign.repository.GridMobileContentSuggestInfoRepository;
import ru.yandex.direct.grid.core.entity.campaign.service.GridCampaignService;
import ru.yandex.direct.grid.core.entity.fetchedfieldresolver.CampaignFetchedFieldsResolver;
import ru.yandex.direct.grid.core.entity.model.GdiEntityStats;
import ru.yandex.direct.grid.core.entity.model.campaign.GdiCampaignStats;
import ru.yandex.direct.grid.core.entity.recommendation.service.GridRecommendationService;
import ru.yandex.direct.grid.core.util.FetchedFieldsResolverCoreUtil;
import ru.yandex.direct.grid.core.util.GridCampaignTestUtil;
import ru.yandex.direct.grid.core.util.stats.GridStatNew;
import ru.yandex.direct.grid.model.GdEntityStats;
import ru.yandex.direct.grid.model.GdStatRequirements;
import ru.yandex.direct.grid.model.Order;
import ru.yandex.direct.grid.model.campaign.GdBroadMatch;
import ru.yandex.direct.grid.model.campaign.GdCampaign;
import ru.yandex.direct.grid.model.campaign.GdCampaignAccess;
import ru.yandex.direct.grid.model.campaign.GdCampaignBrandSafety;
import ru.yandex.direct.grid.model.campaign.GdCampaignPrimaryStatus;
import ru.yandex.direct.grid.model.campaign.GdCampaignPrimaryStatusDesc;
import ru.yandex.direct.grid.model.campaign.GdCampaignServicedState;
import ru.yandex.direct.grid.model.campaign.GdCampaignSource;
import ru.yandex.direct.grid.model.campaign.GdCampaignStatus;
import ru.yandex.direct.grid.model.campaign.GdCampaignStatusModerate;
import ru.yandex.direct.grid.model.campaign.GdCampaignType;
import ru.yandex.direct.grid.model.campaign.GdCpmBannerCampaign;
import ru.yandex.direct.grid.model.campaign.GdCpmDealsCampaign;
import ru.yandex.direct.grid.model.campaign.GdDayBudgetShowMode;
import ru.yandex.direct.grid.model.campaign.GdEshowsSettings;
import ru.yandex.direct.grid.model.campaign.GdMeaningfulGoal;
import ru.yandex.direct.grid.model.campaign.GdTextCampaign;
import ru.yandex.direct.grid.model.campaign.GdiBaseCampaign;
import ru.yandex.direct.grid.model.campaign.GdiCampaign;
import ru.yandex.direct.grid.model.campaign.GdiCampaignMediaplanStatus;
import ru.yandex.direct.grid.model.campaign.GdiCampaignSource;
import ru.yandex.direct.grid.model.campaign.GdiCampaignStrategyName;
import ru.yandex.direct.grid.model.campaign.GdiDayBudgetShowMode;
import ru.yandex.direct.grid.model.campaign.GdiWalletAction;
import ru.yandex.direct.grid.model.campaign.facelift.GdCampaignAdditionalData;
import ru.yandex.direct.grid.model.campaign.strategy.GdCampaignBudget;
import ru.yandex.direct.grid.model.campaign.strategy.GdCampaignBudgetPeriod;
import ru.yandex.direct.grid.model.campaign.strategy.GdCampaignBudgetShowMode;
import ru.yandex.direct.grid.model.campaign.strategy.GdCampaignStrategyManual;
import ru.yandex.direct.grid.model.campaign.strategy.GdCampaignStrategyType;
import ru.yandex.direct.grid.model.campaign.strategy.GdStrategyType;
import ru.yandex.direct.grid.model.campaign.timetarget.GdTimeTarget;
import ru.yandex.direct.grid.model.entity.adgroup.GdAdGroupType;
import ru.yandex.direct.grid.model.entity.campaign.strategy.GdStrategyExtractorFacade;
import ru.yandex.direct.grid.model.entity.recommendation.GdiRecommendationType;
import ru.yandex.direct.grid.processing.context.container.GridGraphQLContext;
import ru.yandex.direct.grid.processing.model.campaign.GdCampaignFilter;
import ru.yandex.direct.grid.processing.model.campaign.GdCampaignFilterSource;
import ru.yandex.direct.grid.processing.model.campaign.GdCampaignOrderBy;
import ru.yandex.direct.grid.processing.model.campaign.GdCampaignOrderByField;
import ru.yandex.direct.grid.processing.model.campaign.GdCampaignsContainer;
import ru.yandex.direct.grid.processing.model.campaign.GdCpmCampaignDayBudgetLimitsRequest;
import ru.yandex.direct.grid.processing.model.campaign.GdCpmCampaignDayBudgetLimitsRequestItem;
import ru.yandex.direct.grid.processing.model.campaign.GdWallet;
import ru.yandex.direct.grid.processing.model.campaign.GdWalletAction;
import ru.yandex.direct.grid.processing.model.campaign.GdWalletBudget;
import ru.yandex.direct.grid.processing.model.campaign.GdWalletBudgetShowMode;
import ru.yandex.direct.grid.processing.model.campaign.GdWalletBudgetType;
import ru.yandex.direct.grid.processing.model.campaign.GdWalletStatus;
import ru.yandex.direct.grid.processing.model.client.GdClientInfo;
import ru.yandex.direct.grid.processing.model.recommendation.GdRecommendation;
import ru.yandex.direct.grid.processing.model.recommendation.GdRecommendationWithKpi;
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
import ru.yandex.direct.grid.processing.service.constant.DefaultValuesUtils;
import ru.yandex.direct.grid.processing.service.dataloader.GridContextProvider;
import ru.yandex.direct.grid.processing.service.placement.PlacementDataService;
import ru.yandex.direct.grid.processing.service.showcondition.retargeting.RetargetingConditionDataLoader;
import ru.yandex.direct.metrika.client.MetrikaClient;
import ru.yandex.direct.rbac.RbacRole;
import ru.yandex.direct.rbac.RbacService;
import ru.yandex.direct.test.utils.RandomNumberUtils;
import ru.yandex.direct.utils.FunctionalUtils;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.DEFAULT_ADD_METRIKA_TAG_TO_URL;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.DEFAULT_ADD_OPENSTAT_TAG_TO_URL;
import static ru.yandex.direct.core.entity.campaign.service.validation.type.bean.strategy.CpmCampaignWithCustomStrategyBeforeApplyValidatorTest.NOW;
import static ru.yandex.direct.grid.core.util.GridCampaignTestUtil.BROAD_MATCH_GOAL_ID;
import static ru.yandex.direct.grid.core.util.GridCampaignTestUtil.DISABLED_DOMAINS;
import static ru.yandex.direct.grid.core.util.GridCampaignTestUtil.DISABLED_IPS;
import static ru.yandex.direct.grid.core.util.GridCampaignTestUtil.DISABLED_SSP;
import static ru.yandex.direct.grid.core.util.GridCampaignTestUtil.defaultCampaign;
import static ru.yandex.direct.grid.model.campaign.GdCampaignFeature.HAS_ACTIVE_ADS;
import static ru.yandex.direct.grid.model.campaign.GdCampaignFeature.HAS_ADS;
import static ru.yandex.direct.grid.model.campaign.GdCampaignFeature.HAS_NOT_ARCHIVED_ADS;
import static ru.yandex.direct.grid.model.campaign.GdCampaignFeature.IS_AIMING_ALLOWED;
import static ru.yandex.direct.grid.model.campaign.GdCampaignFeature.SHOW_GENERAL_PRICE_ON_GROUP_EDIT;
import static ru.yandex.direct.grid.model.entity.campaign.converter.CampaignDataConverter.WWW;
import static ru.yandex.direct.grid.model.entity.campaign.converter.CampaignDataConverter.toGdAttributionModel;
import static ru.yandex.direct.grid.model.entity.campaign.converter.CampaignDataConverter.toGdCampaignPlatform;
import static ru.yandex.direct.grid.model.entity.campaign.converter.CampaignDataConverter.toGdCampaignType;
import static ru.yandex.direct.grid.model.entity.campaign.strategy.GdStrategyExtractorHelper.STRATEGIES_EXTRACTORS_BY_TYPES;
import static ru.yandex.direct.grid.processing.service.autooverdraft.converter.AutoOverdraftDataConverter.toClientAutoOverdraftInfo;
import static ru.yandex.direct.grid.processing.service.campaign.CampaignDataConverter.extractGdBrandSafetyData;
import static ru.yandex.direct.grid.processing.service.campaign.CampaignDataConverter.extractGdNotificationData;
import static ru.yandex.direct.grid.processing.service.campaign.CampaignInfoService.DEFAULT_ACCESS;
import static ru.yandex.direct.grid.processing.service.campaign.CampaignInfoService.RECOMMENDATION_REWORKED_BUDGET_TYPES;
import static ru.yandex.direct.grid.processing.service.campaign.CampaignInfoService.toCampaignForAccess;
import static ru.yandex.direct.grid.processing.util.CampaignTestDataUtils.defaultGdClientAutoOverdraftInfo;
import static ru.yandex.direct.grid.processing.util.ContextHelper.buildDefaultContext;
import static ru.yandex.direct.grid.processing.util.ContextHelper.buildDefaultContextWithSubjectUser;
import static ru.yandex.direct.libs.timetarget.TimeTargetUtils.defaultTimeTarget;
import static ru.yandex.direct.regions.Region.KAZAKHSTAN_REGION_ID;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

@RunWith(JUnitParamsRunner.class)
@ParametersAreNonnullByDefault
public class CampaignInfoServiceTest {
    private static final int TEST_SHARD = 10;
    private static final long TEST_CLIENT_ID = 19;
    private static final long TEST_OPERATOR_UID = 100500L;
    private static final long TEST_CID = 2L;
    private static final long TEST_WALLET_ID = 2L;
    private static final BigDecimal EXPECTED_ZERO_SUM =
            BigDecimal.ZERO.setScale(Money.MONEY_CENT_SCALE, RoundingMode.HALF_UP);
    private static final LocalDate TEST_DATE = LocalDate.now();
    private static final Instant TEST_INSTANT = Instant.now();
    private static ImmutableList<GdiCampaign> testList;
    private static final GdStatRequirements TEST_STAT_REQS = new GdStatRequirements()
            .withFrom(TEST_DATE.minusDays(1))
            .withTo(TEST_DATE);
    private static final GdiCampaignStats TEST_STAT = new GdiCampaignStats()
            .withStat(GridStatNew.addZeros(new GdiEntityStats()))
            .withGoalStats(emptyList());
    private static final CampaignFetchedFieldsResolver CAMPAIGN_FETCHED_FIELDS_RESOLVER =
            FetchedFieldsResolverCoreUtil.buildCampaignFetchedFieldsResolver(true);
    private static final GdEntityStats EXPECTED_STATS = new GdEntityStats()
            .withCost(BigDecimal.ZERO)
            .withCostWithTax(BigDecimal.ZERO)
            .withRevenue(BigDecimal.ZERO)
            .withShows(0L)
            .withClicks(0L)
            .withGoals(0L)
            .withCpmPrice(BigDecimal.ZERO)
            .withCtr(BigDecimal.ZERO)
            .withAvgClickCost(BigDecimal.ZERO)
            .withAvgClickPosition(BigDecimal.ZERO)
            .withAvgShowPosition(BigDecimal.ZERO)
            .withAvgDepth(BigDecimal.ZERO)
            .withBounceRate(BigDecimal.ZERO)
            .withConversionRate(BigDecimal.ZERO);

    private static final GdClientInfo TEST_CLIENT = new GdClientInfo()
            .withShard(TEST_SHARD)
            .withId(TEST_CLIENT_ID)
            .withAutoOverdraftInfo(defaultGdClientAutoOverdraftInfo());

    private static final UidClientIdShard UID_CLIENT_ID_SHARD =
            UidClientIdShard.of(TEST_CLIENT_ID, TEST_CLIENT_ID, TEST_SHARD);

    private static final CampaignsPromotion CAMPAIGNS_PROMOTION = new CampaignsPromotion()
            .withPromotionId(25L)
            .withStart(LocalDate.now())
            .withFinish(LocalDate.now().plusDays(2))
            .withPercent(100L);

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

    @Mock
    private CampaignConstantsService campaignConstantsService;

    @InjectMocks
    private GridCampaignAggregationFieldsService gridCampaignAggregationFieldsService;

    private static CampaignAttributionModel defaultAttributionModel;

    private CampaignInfoService campaignInfoService;
    private GdCampaignsContainer inputContainer;
    private Set<GdCampaignType> visibleTypes;

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

        inputContainer = new GdCampaignsContainer()
                .withFilter(new GdCampaignFilter())
                .withStatRequirements(TEST_STAT_REQS);

        visibleTypes = campaignAccessService.getVisibleTypes(ClientId.fromLong(TEST_CLIENT_ID));

        defaultAttributionModel = CampaignAttributionModel.LAST_YANDEX_DIRECT_CLICK_CROSS_DEVICE;
        doReturn(defaultAttributionModel).when(campaignConstantsService).getDefaultAttributionModel();
        testList = ImmutableList.of(defaultCampaign(defaultAttributionModel));
    }

    @Test
    public void testExtractWallets_GotWallet_HasSubCampaigns() {
        when(gridContextProvider.getGridContext()).thenReturn(buildDefaultContext());

        GdiCampaign walletCampaign = defaultWalletCampaign(TEST_CID);
        List<GdiBaseCampaign> campaigns = asList(
                walletCampaign,
                defaultCampaign(2L)
                        .withWalletId(walletCampaign.getId())
                        .withSum(BigDecimal.ZERO)
                        .withSumSpent(BigDecimal.valueOf(1000)),
                defaultCampaign(3L)
                        .withMasterCid(2L)
                        .withWalletId(walletCampaign.getId())
                        .withSum(BigDecimal.ZERO)
                        .withSumSpent(BigDecimal.valueOf(200))
        );

        List<GdWallet> wallets = campaignInfoService
                .extractWalletsList(defaultOperator(), TEST_CLIENT,
                        toClientAutoOverdraftInfo(TEST_CLIENT), campaigns, TEST_INSTANT);

        assertThat(wallets)
                .size().isEqualTo(1);

        GdWallet expectedWallet = defaultExpectedWallet(walletCampaign)
                .withSum(getExpectedBigDecimalSum(800))
                .withSumWithNds(getExpectedBigDecimalSum(800))
                .withSumOnCampaigns(EXPECTED_ZERO_SUM);
        assertThat(wallets.get(0))
                .is(matchedBy(beanDiffer(expectedWallet)));
    }

    @Test
    public void testExtractWallets_GotWallet_HasDayBudget() {
        GdiCampaign walletCampaign = defaultWalletCampaign(TEST_CID)
                .withSumLast(BigDecimal.valueOf(20001))
                .withDayBudget(BigDecimal.valueOf(300))
                .withDayBudgetShowMode(GdiDayBudgetShowMode.STRETCHED);
        //noinspection ResultOfMethodCallIgnored
        doReturn(buildDefaultContext()).when(gridContextProvider).getGridContext();

        List<GdiBaseCampaign> campaigns = asList(
                walletCampaign,
                defaultCampaign(2L)
                        .withWalletId(walletCampaign.getId())
                        .withSum(BigDecimal.valueOf(1000))
                        .withSumSpent(BigDecimal.valueOf(1100)),
                defaultCampaign(3L)
                        .withWalletId(walletCampaign.getId())
                        .withSum(BigDecimal.valueOf(202))
                        .withSumSpent(BigDecimal.valueOf(200))
        );
        List<GdWallet> wallets = campaignInfoService
                .extractWalletsList(defaultOperator(), TEST_CLIENT,
                        toClientAutoOverdraftInfo(TEST_CLIENT), campaigns, TEST_INSTANT);

        assertThat(wallets)
                .size().isEqualTo(1);

        GdWallet expectedWallet = defaultExpectedWallet(walletCampaign)
                .withSum(getExpectedBigDecimalSum(1900))
                .withSumWithNds(getExpectedBigDecimalSum(1900))
                .withSumOnCampaigns(getExpectedBigDecimalSum(2))
                .withBudget(new GdWalletBudget()
                        .withType(GdWalletBudgetType.DAY_BUDGET)
                        .withShowMode(GdWalletBudgetShowMode.STRETCHED)
                        .withSum(BigDecimal.valueOf(300)));
        expectedWallet.withStatus(expectedWallet.getStatus().withNeedsNewPayment(true));

        assertThat(wallets.get(0))
                .is(matchedBy(beanDiffer(expectedWallet)));
    }

    @Test
    public void testExtractWallets_GotWallet_NoDayBudget() {
        GdiCampaign walletCampaign = defaultWalletCampaign(TEST_CID);
        //noinspection ResultOfMethodCallIgnored
        doReturn(buildDefaultContext()).when(gridContextProvider).getGridContext();

        List<GdiBaseCampaign> campaigns = asList(
                walletCampaign,
                defaultCampaign(2L)
                        .withWalletId(walletCampaign.getId())
                        .withSum(BigDecimal.valueOf(1000))
                        .withSumSpent(BigDecimal.valueOf(1000)),
                defaultCampaign(3L)
                        .withWalletId(walletCampaign.getId())
                        .withSum(BigDecimal.valueOf(200))
                        .withSumSpent(BigDecimal.valueOf(200))
        );
        List<GdWallet> wallets = campaignInfoService
                .extractWalletsList(defaultOperator(), TEST_CLIENT,
                        toClientAutoOverdraftInfo(TEST_CLIENT), campaigns, TEST_INSTANT);

        assertThat(wallets)
                .size().isEqualTo(1);

        GdWallet expectedWallet = defaultExpectedWallet(walletCampaign)
                .withSum(getExpectedBigDecimalSum(2000))
                .withSumWithNds(getExpectedBigDecimalSum(2000))
                .withSumOnCampaigns(EXPECTED_ZERO_SUM);
        assertThat(wallets.get(0))
                .is(matchedBy(beanDiffer(expectedWallet)));
    }

    @Test
    public void testExtractWallets_GotWallet_NoCampaigns() {
        GdiCampaign walletCampaign = defaultWalletCampaign(TEST_CID);
        //noinspection ResultOfMethodCallIgnored
        doReturn(buildDefaultContext()).when(gridContextProvider).getGridContext();

        List<GdiBaseCampaign> campaigns = Collections.singletonList(walletCampaign);
        List<GdWallet> wallets = campaignInfoService
                .extractWalletsList(defaultOperator(), TEST_CLIENT,
                        toClientAutoOverdraftInfo(TEST_CLIENT), campaigns, TEST_INSTANT);

        assertThat(wallets)
                .size().isEqualTo(1);

        GdWallet expectedWallet = defaultExpectedWallet(walletCampaign)
                .withSum(getExpectedBigDecimalSum(2000))
                .withSumWithNds(getExpectedBigDecimalSum(2000))
                .withSumOnCampaigns(EXPECTED_ZERO_SUM);
        expectedWallet.withStatus(expectedWallet.getStatus()
                .withEnabled(false)
                .withDisabledButHaveMoney(true));

        assertThat(wallets.get(0))
                .is(matchedBy(beanDiffer(expectedWallet)));
    }

    @Test
    public void testExtractWalletsWithPayBeforeModeration_GotWallet_NoCampaigns() {
        doReturn(new HashSet<>(Collections.singletonList(1L)))
                .when(clientService).clientIdsWithPayBeforeModeration(any());
        //noinspection ResultOfMethodCallIgnored
        doReturn(buildDefaultContext()).when(gridContextProvider).getGridContext();

        GdiCampaign walletCampaign = defaultWalletCampaign(TEST_CID);

        List<GdiBaseCampaign> campaigns = Collections.singletonList(walletCampaign);
        List<GdWallet> wallets = campaignInfoService.extractWalletsList(defaultOperator(), TEST_CLIENT,
                toClientAutoOverdraftInfo(TEST_CLIENT), campaigns, TEST_INSTANT);

        assertThat(wallets)
                .size().isEqualTo(1);

        GdWallet expectedWallet = defaultExpectedWallet(walletCampaign)
                .withSum(getExpectedBigDecimalSum(2000))
                .withSumWithNds(getExpectedBigDecimalSum(2000))
                .withSumOnCampaigns(EXPECTED_ZERO_SUM);
        expectedWallet.withStatus(expectedWallet.getStatus().withEnabled(true));

        assertThat(wallets.get(0))
                .is(matchedBy(beanDiffer(expectedWallet)));
    }

    @Test
    public void testExtractWallets_getStatusShowNds() {
        doReturn(new Client().withUsesQuasiCurrency(true).withCountryRegionId(KAZAKHSTAN_REGION_ID).withClientId(TEST_CLIENT_ID))
                .when(clientService).getClient(eq(ClientId.fromLong(TEST_CLIENT_ID)));

        //noinspection ResultOfMethodCallIgnored
        doReturn(buildDefaultContext()).when(gridContextProvider).getGridContext();

        GdiCampaign walletCampaign = defaultWalletCampaign(TEST_CID).withCurrencyCode(CurrencyCode.KZT);
        List<GdiBaseCampaign> campaigns = Collections.singletonList(walletCampaign);

        List<GdWallet> wallets = campaignInfoService.extractWalletsList(defaultOperator(), TEST_CLIENT,
                toClientAutoOverdraftInfo(TEST_CLIENT), campaigns, TEST_INSTANT);

        assertThat(wallets)
                .size().isEqualTo(1);

        assertThat(wallets.get(0).getStatus().getShowNds())
                .isFalse();
    }

    @Test
    public void testExtractWallets_NoWallets() {
        List<GdiBaseCampaign> campaigns = asList(
                defaultCampaign(2L),
                defaultCampaign(3L)
        );
        List<GdWallet> wallets = campaignInfoService
                .extractWalletsList(defaultOperator(), TEST_CLIENT,
                        toClientAutoOverdraftInfo(TEST_CLIENT), campaigns, TEST_INSTANT);

        assertThat(wallets)
                .isEmpty();
    }

    @Test
    public void testGetAllCampaigns_Request_NoListExists() {
        User user = new User()
                .withUid(UID_CLIENT_ID_SHARD.getUid())
                .withClientId(UID_CLIENT_ID_SHARD.getClientId());
        GridGraphQLContext context = buildDefaultContextWithSubjectUser(user)
                .withQueriedClient(TEST_CLIENT)
                .withClientGdiCampaigns(null);
        //noinspection ResultOfMethodCallIgnored
        doReturn(context)
                .when(gridContextProvider).getGridContext();

        doReturn(testList)
                .when(gridCampaignService).getAllCampaigns(eq(TEST_SHARD), eq(user), any(), any());
        ImmutableList<GdiCampaign> campaigns = campaignInfoService.getAllCampaigns(ClientId.fromLong(TEST_CLIENT_ID));

        verify(gridCampaignService)
                .getAllCampaigns(eq(TEST_SHARD), eq(user), any(), eq(context.getOperator()));
        assertThat(campaigns)
                .isEqualTo(testList);
        assertThat(context.getClientGdiCampaigns())
                .isEqualTo(testList);
    }

    @Test
    public void testGetAllCampaigns_NoRequest_ListExists() {
        GridGraphQLContext context = buildDefaultContext()
                .withQueriedClient(TEST_CLIENT)
                .withClientGdiCampaigns(testList);

        //noinspection ResultOfMethodCallIgnored
        doReturn(context)
                .when(gridContextProvider).getGridContext();

        ImmutableList<GdiCampaign> campaigns = campaignInfoService.getAllCampaigns(ClientId.fromLong(TEST_CLIENT_ID));

        verifyZeroInteractions(gridCampaignService);
        assertThat(campaigns)
                .isEqualTo(testList);
        assertThat(context.getClientGdiCampaigns())
                .isEqualTo(testList);
    }

    @Test
    public void testGetFilteredCampaigns_InternalCampaigns_CheckPlaceId() {
        long somePlaceId = 343L;

        GdiCampaign internalCampaign = defaultInternalDistribCampaign(TEST_CID)
                .withInternalAdPlaceId(somePlaceId);

        inputContainer.getFilter().setTypeIn(visibleTypes);
        List<GdCampaign> gdCampaigns = campaignInfoService
                .getFilteredCampaigns(TEST_CLIENT, new User(), singletonList(internalCampaign),
                        inputContainer, TEST_INSTANT, CAMPAIGN_FETCHED_FIELDS_RESOLVER);

        softly.assertThat(gdCampaigns).hasSize(1);
        softly.assertThat(gdCampaigns).extracting("placeId").isEqualTo(singletonList(somePlaceId));
    }

    @Test
    public void testGetFilteredCampaigns_InternalAutobudgetCampaigns_CheckPlaceId() {
        long somePlaceId = 343L;

        GdiCampaign internalCampaign = defaultInternalAutobudgetCampaign(TEST_CID)
                .withInternalAdPlaceId(somePlaceId);

        inputContainer.getFilter().setTypeIn(visibleTypes);
        List<GdCampaign> gdCampaigns = campaignInfoService
                .getFilteredCampaigns(TEST_CLIENT, new User(), singletonList(internalCampaign),
                        inputContainer, TEST_INSTANT, CAMPAIGN_FETCHED_FIELDS_RESOLVER);

        softly.assertThat(gdCampaigns).hasSize(1);
        softly.assertThat(gdCampaigns).extracting("placeId").isEqualTo(singletonList(somePlaceId));
    }

    @Test
    public void testGetFilteredCampaigns_Content() {
        GdiCampaign walletCampaign = defaultWalletCampaign(130);
        GdiCampaign cpmCampaign = defaultCpmCampaign(RandomNumberUtils.nextPositiveLong())
                .withTimeTarget(defaultTimeTarget())
                .withTimezoneId(1L);
        GdiCampaign campaign = defaultCampaign(TEST_CID)
                .withWalletId(walletCampaign.getId())
                .withTimeTarget(defaultTimeTarget())
                .withTimezoneId(1L)
                .withCampaignsPromotions(singletonList(CAMPAIGNS_PROMOTION.withCid(TEST_CID)));

        List<GdiCampaign> campaigns = asList(
                walletCampaign,
                cpmCampaign,
                campaign
        );
        //noinspection ResultOfMethodCallIgnored
        doReturn(buildDefaultContext()).when(gridContextProvider).getGridContext();

        inputContainer.getFilter().setTypeIn(visibleTypes);
        List<GdCampaign> gdCampaigns = campaignInfoService
                .getFilteredCampaigns(TEST_CLIENT, new User().withRole(RbacRole.EMPTY), campaigns,
                        inputContainer, TEST_INSTANT, CAMPAIGN_FETCHED_FIELDS_RESOLVER);

        verify(gridCampaignService)
                .getCampaignStats(eq(List.of(cpmCampaign, campaign)), eq(TEST_DATE.minusDays(1)),
                        eq(TEST_DATE), eq(emptySet()), isNull(), eq(emptySet()));

        GdCampaign expectedCpmCampaign = defaultExpectedCampaign(cpmCampaign, GdCpmBannerCampaign::new)
                .withMetrikaCounters(List.of(GridCampaignTestUtil.METRIKA_COUNTER))
                .withMeaningfulGoals(List.of(new GdMeaningfulGoal()
                        .withIsMetrikaSourceOfValue(false)
                        .withGoalId(CampaignConstants.ENGAGED_SESSION_GOAL_ID)))
                .withExportId(cpmCampaign.getOrderId())
                .withSum(new BigDecimal(cpmCampaign.getSum().toString()).setScale(2, RoundingMode.HALF_UP))
                .withSumRest(cpmCampaign.getSum().subtract(cpmCampaign.getSumSpent()))
                .withHasAds(true)
                .withDescription(cpmCampaign.getDescription())
                .withStats(null)
                .withUseCurrentRegion(false)
                .withUseRegularRegion(false)
                .withHasExtendedGeoTargeting(false)
                .withHasAddMetrikaTagToUrl(DEFAULT_ADD_METRIKA_TAG_TO_URL)
                .withHasAddOpenstatTagToUrl(DEFAULT_ADD_OPENSTAT_TAG_TO_URL)
                .withAttributionModel(toGdAttributionModel(defaultAttributionModel))
                .withAvailableAdGroupTypes(ImmutableSet.of(GdAdGroupType.CPM_BANNER, GdAdGroupType.CPM_VIDEO,
                        GdAdGroupType.CPM_AUDIO))
                .withIsCpvStrategiesEnabled(true)
                .withIsAimingAllowed(true)
                .withTimeTarget(defaultGdTimeTarget())
                .withDisabledPlaces(FunctionalUtils.setUnion(DISABLED_DOMAINS, DISABLED_SSP))
                .withEshowsSettings(new GdEshowsSettings())
                .withHasNotArchivedAds(true)
                .withFeatures(Set.of(HAS_NOT_ARCHIVED_ADS, HAS_ACTIVE_ADS, HAS_ADS, SHOW_GENERAL_PRICE_ON_GROUP_EDIT,
                        IS_AIMING_ALLOWED))
                .withAdditionalData(new GdCampaignAdditionalData());

        expectedCpmCampaign.getStatus()
                .withPrimaryStatus(GdCampaignPrimaryStatus.ACTIVE)
                .withPrimaryStatusDesc(null);

        GdCampaign expectedTextCampaign = defaultExpectedTextCampaign(campaign)
                .withExportId(campaign.getOrderId())
                .withMetrikaCounters(List.of(GridCampaignTestUtil.METRIKA_COUNTER))
                .withMeaningfulGoals(List.of(new GdMeaningfulGoal()
                        .withIsMetrikaSourceOfValue(false)
                        .withGoalId(CampaignConstants.ENGAGED_SESSION_GOAL_ID)))
                .withBroadMatch(defaultBroadMatch())
                .withIndex(1)
                .withUseCurrentRegion(false)
                .withUseRegularRegion(false)
                .withHasExtendedGeoTargeting(false)
                .withWalletId(walletCampaign.getId())
                .withSum(BigDecimal.ZERO)
                .withDescription(campaign.getDescription())
                .withSumRest(campaign.getSum().subtract(campaign.getSumSpent()))
                .withHasAds(true)
                .withIsAimingAllowed(true)
                .withAttributionModel(toGdAttributionModel(defaultAttributionModel))
                .withGoalStats(emptyList())
                .withDayBudget(BigDecimal.ZERO)
                .withDayBudgetShowMode(GdDayBudgetShowMode.DEFAULT_)
                .withTimeTarget(defaultGdTimeTarget())
                .withHasNotArchivedAds(true)
                .withFeatures(Set.of(HAS_NOT_ARCHIVED_ADS, HAS_ACTIVE_ADS, HAS_ADS, SHOW_GENERAL_PRICE_ON_GROUP_EDIT,
                        IS_AIMING_ALLOWED))
                .withIsUniversalCamp(false)
                .withCampaignsPromotions(singletonList(CAMPAIGNS_PROMOTION.withCid(TEST_CID)));

        expectedTextCampaign.getStatus()
                .withPrimaryStatus(GdCampaignPrimaryStatus.ACTIVE)
                .withPrimaryStatusDesc(null);

        assertThat(gdCampaigns).hasSize(2);
        assertThat(gdCampaigns)
                .is(matchedBy(beanDiffer(asList(expectedCpmCampaign, expectedTextCampaign))));
    }

    @Test
    public void testGetFilteredCampaigns_NoAiming() {
        GdiCampaign walletCampaign = defaultWalletCampaign(130);
        GdiCampaign campaign = defaultCampaign(TEST_CID).withPlatform(CampaignsPlatform.SEARCH)
                .withWalletId(walletCampaign.getId())
                .withTimeTarget(defaultTimeTarget())
                .withTimezoneId(1L);

        List<GdiCampaign> campaigns = asList(
                walletCampaign,
                campaign
        );
        //noinspection ResultOfMethodCallIgnored
        doReturn(buildDefaultContext()).when(gridContextProvider).getGridContext();


        inputContainer.getFilter().setTypeIn(visibleTypes);
        List<GdCampaign> gdCampaigns = campaignInfoService
                .getFilteredCampaigns(TEST_CLIENT, new User().withRole(RbacRole.EMPTY), campaigns,
                        inputContainer, TEST_INSTANT, CAMPAIGN_FETCHED_FIELDS_RESOLVER);

        verify(gridCampaignService)
                .getCampaignStats(eq(List.of(campaign)), eq(TEST_DATE.minusDays(1)), eq(TEST_DATE),
                        eq(emptySet()), isNull(), eq(emptySet()));

        GdCampaign expectedCampaign = defaultExpectedTextCampaign(campaign)
                .withMetrikaCounters(List.of(GridCampaignTestUtil.METRIKA_COUNTER))
                .withMeaningfulGoals(List.of(new GdMeaningfulGoal()
                        .withIsMetrikaSourceOfValue(false)
                        .withGoalId(CampaignConstants.ENGAGED_SESSION_GOAL_ID)))
                .withBroadMatch(defaultBroadMatch())
                .withExportId(campaign.getOrderId())
                .withSum(BigDecimal.ZERO)
                .withSumRest(campaign.getSum().subtract(campaign.getSumSpent()))
                .withWalletId(walletCampaign.getId())
                .withAttributionModel(toGdAttributionModel(defaultAttributionModel))
                .withHasAds(true)
                .withUseCurrentRegion(false)
                .withUseRegularRegion(false)
                .withHasExtendedGeoTargeting(false)
                .withStartDate(campaign.getStartDate())
                .withDescription(campaign.getDescription())
                .withStatus(new GdCampaignStatus()
                        .withCampaignId(campaign.getId())
                        .withMoneyBlocked(false)
                        .withReadOnly(false)
                        .withOver(false)
                        .withWaitingForPayment(false)
                        .withModerationStatus(GdCampaignStatusModerate.YES)
                        .withPrimaryStatus(GdCampaignPrimaryStatus.ACTIVE)
                        .withArchived(false)
                        .withWaitingForUnArchiving(false)
                        .withDraft(false)
                        .withNeedsNewPayment(false)
                        .withActivating(false)
                        .withWaitingForArchiving(false)
                        .withAllowDomainMonitoring(true)
                )
                .withGoalStats(emptyList())
                .withDayBudget(BigDecimal.ZERO)
                .withDayBudgetShowMode(GdDayBudgetShowMode.DEFAULT_)
                .withTimeTarget(defaultGdTimeTarget())
                .withHasNotArchivedAds(true)
                .withFeatures(Set.of(HAS_NOT_ARCHIVED_ADS, HAS_ACTIVE_ADS, HAS_ADS, SHOW_GENERAL_PRICE_ON_GROUP_EDIT))
                .withIsUniversalCamp(false);
        assertThat(gdCampaigns).hasSize(1);
        assertThat(gdCampaigns.get(0))
                .is(matchedBy(beanDiffer(expectedCampaign)));
    }

    @Test
    public void testGetFilteredCampaigns_Content_NoWallet() {
        GdiCampaign campaign = defaultCampaign(TEST_CID)
                .withTimeTarget(defaultTimeTarget())
                .withTimezoneId(1L);

        List<GdiCampaign> campaigns = Collections.singletonList(campaign);

        List<GdCampaign> gdCampaigns = campaignInfoService
                .getFilteredCampaigns(TEST_CLIENT, new User(), campaigns,
                        inputContainer, TEST_INSTANT, CAMPAIGN_FETCHED_FIELDS_RESOLVER);

        assertThat(gdCampaigns)
                .size().isEqualTo(1);

        GdCampaign expectedCampaign = defaultExpectedTextCampaign(campaign)
                .withExportId(campaign.getOrderId())
                .withMetrikaCounters(List.of(GridCampaignTestUtil.METRIKA_COUNTER))
                .withMeaningfulGoals(List.of(new GdMeaningfulGoal()
                        .withIsMetrikaSourceOfValue(false)
                        .withGoalId(CampaignConstants.ENGAGED_SESSION_GOAL_ID)))
                .withBroadMatch(defaultBroadMatch())
                .withSum(campaign.getSum().subtract(campaign.getSumSpent())
                        .setScale(Money.MONEY_CENT_SCALE, BigDecimal.ROUND_DOWN))
                .withSumRest(campaign.getSum().subtract(campaign.getSumSpent()))
                .withHasAds(true)
                .withDescription(campaign.getDescription())
                .withAttributionModel(toGdAttributionModel(defaultAttributionModel))
                .withIsAimingAllowed(true)
                .withDayBudget(BigDecimal.ZERO)
                .withDayBudgetShowMode(GdDayBudgetShowMode.DEFAULT_)
                .withTimeTarget(defaultGdTimeTarget())
                .withHasNotArchivedAds(true)
                .withUseCurrentRegion(false)
                .withUseRegularRegion(false)
                .withHasExtendedGeoTargeting(false)
                .withFeatures(Set.of(HAS_NOT_ARCHIVED_ADS, HAS_ACTIVE_ADS, HAS_ADS, SHOW_GENERAL_PRICE_ON_GROUP_EDIT,
                        IS_AIMING_ALLOWED))
                .withIsUniversalCamp(false);

        expectedCampaign.withStatus(expectedCampaign.getStatus()
                .withPrimaryStatus(GdCampaignPrimaryStatus.ACTIVE)
                .withPrimaryStatusDesc(null))
                .withGoalStats(emptyList());

        assertThat(gdCampaigns.get(0))
                .is(matchedBy(beanDiffer(expectedCampaign)));
    }

    @Test
    public void testGetFilteredCampaigns_SelectedCampaignsFilter() {
        GdiCampaign walletCampaign = defaultWalletCampaign(TEST_CID);
        List<GdiCampaign> campaigns = asList(
                walletCampaign,
                defaultCampaign(2L)
                        .withWalletId(walletCampaign.getId()),
                defaultCampaign(3L)
                        .withWalletId(walletCampaign.getId())
        );
        //noinspection ResultOfMethodCallIgnored
        doReturn(buildDefaultContext()).when(gridContextProvider).getGridContext();

        inputContainer.getFilter()
                .withCampaignIdIn(ImmutableSet.of(campaigns.get(0).getId()))
                .withTypeIn(visibleTypes);
        List<GdCampaign> gdCampaigns = campaignInfoService
                .getFilteredCampaigns(TEST_CLIENT, new User().withRole(RbacRole.EMPTY), campaigns,
                        inputContainer, TEST_INSTANT, CAMPAIGN_FETCHED_FIELDS_RESOLVER);

        assertThat(gdCampaigns)
                .size().isEqualTo(1);
        assertThat(gdCampaigns.get(0).getId())
                .isEqualTo(2L);
    }

    @Test
    public void testGetFilteredCampaigns_OrderTest() {
        List<GdiCampaign> campaigns = asList(
                defaultCampaign(2L),
                defaultCampaign(3L)
        );

        List<GdCampaignOrderBy> ordering = Collections.singletonList(new GdCampaignOrderBy()
                .withField(GdCampaignOrderByField.ID)
                .withOrder(Order.DESC));
        inputContainer.setOrderBy(ordering);

        List<GdCampaign> gdCampaigns = campaignInfoService
                .getFilteredCampaigns(TEST_CLIENT, new User(), campaigns,
                        inputContainer, TEST_INSTANT, CAMPAIGN_FETCHED_FIELDS_RESOLVER);

        assertThat(gdCampaigns)
                .size().isEqualTo(2);

        assertThat(gdCampaigns.get(0).getId())
                .isEqualTo(3L);
        assertThat(gdCampaigns.get(1).getId())
                .isEqualTo(2L);
    }

    @Test
    public void testGetFilteredCampaigns_FilterTest() {
        List<GdiCampaign> campaigns = asList(
                defaultCampaign(2L)
                        .withArchived(true),
                defaultCampaign(3L)
        );

        inputContainer.getFilter().setArchived(true);
        List<GdCampaign> gdCampaigns = campaignInfoService
                .getFilteredCampaigns(TEST_CLIENT, new User(), campaigns,
                        inputContainer, TEST_INSTANT, CAMPAIGN_FETCHED_FIELDS_RESOLVER);

        assertThat(gdCampaigns)
                .size().isEqualTo(1);

        assertThat(gdCampaigns.get(0).getId())
                .isEqualTo(2L);
    }

    @Test
    public void testGetFilteredCampaigns_PostFilterTest() {
        List<GdiCampaign> campaigns = asList(defaultCampaign(2L).withArchived(true),
                defaultCampaign(3L));

        inputContainer.getFilter()
                .setStrategyTypeIn(singleton(GdCampaignStrategyType.CPM_DEFAULT));
        List<GdCampaign> gdCampaigns = campaignInfoService
                .getFilteredCampaigns(TEST_CLIENT, new User(), campaigns,
                        inputContainer, TEST_INSTANT, CAMPAIGN_FETCHED_FIELDS_RESOLVER);

        assertThat(gdCampaigns).isEmpty();
    }

    @Test
    public void testGetFilteredCampaigns_PostFilterSelfServicedStateTest() {
        var servicedCampaign = defaultCampaign(RandomNumberUtils.nextPositiveLong());
        var selfServicedCampaign = defaultCampaign(servicedCampaign.getId() + 1);

        Map<Long, GdCampaignAccess> campaignsAccessMap = Map.of(
                servicedCampaign.getId(), new GdCampaignAccess()
                        .withServicedState(GdCampaignServicedState.SERVICED),
                selfServicedCampaign.getId(), new GdCampaignAccess()
                        .withServicedState(GdCampaignServicedState.SELF_SERVICED));
        doReturn(campaignsAccessMap)
                .when(campaignAccessService).getCampaignsAccess(any(), any(), any(), any());

        inputContainer.getFilter()
                .setServicedStateIn(Set.of(GdCampaignServicedState.SELF_SERVICED));
        List<GdCampaign> gdCampaigns = campaignInfoService
                .getFilteredCampaigns(TEST_CLIENT, new User(), List.of(servicedCampaign, selfServicedCampaign),
                        inputContainer, TEST_INSTANT, CAMPAIGN_FETCHED_FIELDS_RESOLVER);

        assertThat(gdCampaigns)
                .hasSize(1)
                .extracting(GdCampaign::getId)
                .containsExactly(selfServicedCampaign.getId());
    }

    @Test
    public void testGetFilteredCampaigns_brandSafetyTest() {
        List<GdiCampaign> campaigns = asList(
                defaultCampaign(2L),
                defaultCampaign(3L)
                        .withBrandSafetyCategories(List.of(1L)),
                defaultCampaign(4L)
                        .withBrandSafetyCategories(List.of(4294967297L)),
                defaultCampaign(5L)
                        .withBrandSafetyCategories(List.of(1L, 2L, 4294967297L, 4294967302L, 4294967307L)),
                defaultCampaign(6L)
                        .withBrandSafetyCategories(List.of(1L, 2L, 4294967302L, 4294967307L))
        );

        List<GdCampaign> gdCampaigns = campaignInfoService
                .getFilteredCampaigns(TEST_CLIENT, new User(), campaigns,
                        inputContainer, TEST_INSTANT, CAMPAIGN_FETCHED_FIELDS_RESOLVER);

        assertThat(gdCampaigns)
                .size().isEqualTo(5);

        assertThat(gdCampaigns.get(0).getBrandSafety())
                .isEqualTo(new GdCampaignBrandSafety()
                        .withIsEnabled(false)
                        .withAdditionalCategories(emptySet()));
        assertThat(gdCampaigns.get(1).getBrandSafety())
                .isEqualTo(new GdCampaignBrandSafety()
                        .withIsEnabled(false)
                        .withAdditionalCategories(emptySet()));
        assertThat(gdCampaigns.get(2).getBrandSafety())
                .isEqualTo(new GdCampaignBrandSafety()
                        .withIsEnabled(true)
                        .withAdditionalCategories(emptySet()));
        assertThat(gdCampaigns.get(3).getBrandSafety())
                .isEqualTo(new GdCampaignBrandSafety()
                        .withIsEnabled(true)
                        .withAdditionalCategories(Set.of(4294967302L, 4294967307L)));
        assertThat(gdCampaigns.get(4).getBrandSafety())
                .isEqualTo(new GdCampaignBrandSafety()
                        .withIsEnabled(false)
                        .withAdditionalCategories(Set.of(4294967302L, 4294967307L)));
    }

    @Test
    public void testGetFilteredCampaigns_PostFilterServicedStateTest() {
        var servicedCampaign = defaultCampaign(RandomNumberUtils.nextPositiveLong());
        var selfServicedCampaign = defaultCampaign(servicedCampaign.getId() + 1);
        var acceptServicingCampaign = defaultCampaign(servicedCampaign.getId() + 2);

        Map<Long, GdCampaignAccess> campaignsAccessMap = Map.of(
                servicedCampaign.getId(), new GdCampaignAccess()
                        .withServicedState(GdCampaignServicedState.SERVICED),
                selfServicedCampaign.getId(), new GdCampaignAccess()
                        .withServicedState(GdCampaignServicedState.SELF_SERVICED),
                acceptServicingCampaign.getId(), new GdCampaignAccess()
                        .withServicedState(GdCampaignServicedState.ACCEPT_SERVICING));
        doReturn(campaignsAccessMap)
                .when(campaignAccessService).getCampaignsAccess(any(), any(), any(), any());

        inputContainer.getFilter()
                .setServicedStateIn(Set.of(GdCampaignServicedState.SERVICED, GdCampaignServicedState.ACCEPT_SERVICING));
        List<GdCampaign> gdCampaigns = campaignInfoService
                .getFilteredCampaigns(TEST_CLIENT, new User(),
                        List.of(servicedCampaign, selfServicedCampaign, acceptServicingCampaign),
                        inputContainer, TEST_INSTANT, CAMPAIGN_FETCHED_FIELDS_RESOLVER);

        assertThat(gdCampaigns)
                .hasSize(2)
                .extracting(GdCampaign::getId)
                .containsExactly(servicedCampaign.getId(), acceptServicingCampaign.getId());
    }

    @Test
    public void testGetFilteredCampaings_NoBanners() {
        GdiCampaign campaign = defaultNoBannersCampaigns(TEST_CID)
                .withTimeTarget(defaultTimeTarget())
                .withTimezoneId(1L);

        List<GdiCampaign> campaigns = Collections.singletonList(campaign);

        List<GdCampaign> gdCampaigns = campaignInfoService
                .getFilteredCampaigns(TEST_CLIENT, new User(), campaigns,
                        inputContainer, TEST_INSTANT, CAMPAIGN_FETCHED_FIELDS_RESOLVER);

        assertThat(gdCampaigns)
                .size().isEqualTo(1);

        GdCampaign expectedCampaign = defaultExpectedTextCampaign(campaign)
                .withExportId(campaign.getOrderId())
                .withMetrikaCounters(List.of(GridCampaignTestUtil.METRIKA_COUNTER))
                .withMeaningfulGoals(List.of(new GdMeaningfulGoal()
                        .withIsMetrikaSourceOfValue(false)
                        .withGoalId(CampaignConstants.ENGAGED_SESSION_GOAL_ID)))
                .withBroadMatch(defaultBroadMatch())
                .withDescription(campaign.getDescription())
                .withIsAimingAllowed(true)
                .withAttributionModel(toGdAttributionModel(defaultAttributionModel))
                .withGoalStats(emptyList())
                .withDayBudget(BigDecimal.ZERO)
                .withSumRest(campaign.getSum().subtract(campaign.getSumSpent()))
                .withDayBudgetShowMode(GdDayBudgetShowMode.DEFAULT_)
                .withTimeTarget(defaultGdTimeTarget())
                .withHasNotArchivedAds(false)
                .withUseCurrentRegion(false)
                .withUseRegularRegion(false)
                .withHasExtendedGeoTargeting(false)
                .withFeatures(Set.of(SHOW_GENERAL_PRICE_ON_GROUP_EDIT, IS_AIMING_ALLOWED))
                .withIsUniversalCamp(false);

        assertThat(gdCampaigns.get(0))
                .is(matchedBy(beanDiffer(expectedCampaign)));
    }

    @Test
    public void testGetFilteredCampaigns_WalletHasPromocode_CampaignHasPromocodeRestrictedDomain() {
        String promocodeRestrictedDomain = "яндекс.рф";
        String punyCode = "xn--d1acpjx3f.xn--p1ai";

        GdiCampaign wallet = defaultWalletCampaign(TEST_WALLET_ID);
        wallet.setPromocodeRestrictedDomain(promocodeRestrictedDomain);
        GdiCampaign campaign = defaultCampaign(defaultAttributionModel);
        campaign.setWalletId(wallet.getId());
        //noinspection ResultOfMethodCallIgnored
        doReturn(buildDefaultContext()).when(gridContextProvider).getGridContext();

        List<GdiCampaign> campaigns = asList(wallet, campaign);

        List<GdCampaign> gdCampaigns = campaignInfoService
                .getFilteredCampaigns(TEST_CLIENT, defaultOperator(), campaigns, inputContainer, TEST_INSTANT,
                        CAMPAIGN_FETCHED_FIELDS_RESOLVER);

        GdCampaign expectedCampaign =
                new GdTextCampaign().withPromocodeRestrictedDomain(asList(promocodeRestrictedDomain,
                        WWW + "." + promocodeRestrictedDomain, punyCode, WWW + "." + punyCode));

        assertThat(gdCampaigns)
                .is(matchedBy(beanDiffer(Collections.singletonList(expectedCampaign))
                        .useCompareStrategy(DefaultCompareStrategies.onlyExpectedFields())));
    }

    @Test
    public void testGetFilteredCampaigns_Favorite() {
        GdiCampaign notFavoriteCampaign = defaultCampaign(defaultAttributionModel)
                .withFavorite(false);
        GdiCampaign favoriteCampaign = defaultCampaign(defaultAttributionModel)
                .withFavorite(true);
        List<GdiCampaign> campaigns = asList(notFavoriteCampaign, favoriteCampaign);

        inputContainer.getFilter().setFavorite(true);
        List<GdCampaign> gdCampaigns = campaignInfoService
                .getFilteredCampaigns(TEST_CLIENT, defaultOperator(), campaigns, inputContainer, TEST_INSTANT,
                        CAMPAIGN_FETCHED_FIELDS_RESOLVER);

        GdCampaign expectedCampaign = new GdTextCampaign()
                .withId(favoriteCampaign.getId());
        assertThat(gdCampaigns)
                .is(matchedBy(beanDiffer(Collections.singletonList(expectedCampaign))
                        .useCompareStrategy(DefaultCompareStrategies.onlyExpectedFields())));
    }

    /**
     * Проверяем что с отключенной фичей REDESIGN_BUDGET_LIMIT_EDITOR и не заданным недельным бюджетом в поле
     * budget приходит null
     */
    @Test
    public void testGetFilteredCampaigns_EmptyWeeklyBudgetWithoutRecommendationFeature() {
        GdiCampaign campaign = defaultCampaign(defaultAttributionModel)
                .withStrategyName(GdiCampaignStrategyName.AUTOBUDGET_AVG_CLICK)
                .withStrategyData(
                        "{\"name\": \"autobudget_avg_click\", \"avg_bid\": 0.09, " +
                                "\"version\": 1}");

        doReturn(false)
                .when(featureService).isEnabledForClientId(any(),
                eq(FeatureName.REDESIGN_BUDGET_LIMIT_EDITOR));

        List<GdCampaign> gdCampaigns = campaignInfoService.getFilteredCampaigns(TEST_CLIENT, defaultOperator(),
                List.of(campaign), inputContainer, TEST_INSTANT, CAMPAIGN_FETCHED_FIELDS_RESOLVER);
        GdCampaign actualCampaign = gdCampaigns.get(0);

        assertNotNull(actualCampaign);
        assertNotNull(actualCampaign.getStrategy());
        assertNull(actualCampaign.getStrategy().getBudget());

        assertNotNull(actualCampaign.getFlatStrategy());
        assertNull(actualCampaign.getFlatStrategy().getBudget());
    }

    /**
     * Проверяем что с включенной фичей REDESIGN_BUDGET_LIMIT_EDITOR и не заданным недельным бюджетом в поле
     * budget приходит непустой объект с sum = 0
     */
    @Test
    public void testGetFilteredCampaigns_EmptyWeeklyBudgetWithRecommendationFeature() {
        GdiCampaign campaign = defaultCampaign(defaultAttributionModel)
                .withStrategyName(GdiCampaignStrategyName.AUTOBUDGET_AVG_CLICK)
                .withStrategyData(
                        "{\"name\": \"autobudget_avg_click\", \"avg_bid\": 0.09, " +
                                "\"version\": 1}");

        doReturn(true)
                .when(featureService).isEnabledForClientId(any(),
                eq(FeatureName.REDESIGN_BUDGET_LIMIT_EDITOR));
        List<GdCampaign> gdCampaigns = campaignInfoService.getFilteredCampaigns(TEST_CLIENT, defaultOperator(),
                List.of(campaign), inputContainer, TEST_INSTANT, CAMPAIGN_FETCHED_FIELDS_RESOLVER);
        GdCampaign actualCampaign = gdCampaigns.get(0);

        assertNotNull(actualCampaign);
        assertNotNull(actualCampaign.getStrategy());
        assertNotNull(actualCampaign.getStrategy().getBudget());
        assertEquals(GdCampaignBudgetPeriod.WEEK, actualCampaign.getStrategy().getBudget().getPeriod());
        assertEquals(BigDecimal.ZERO, actualCampaign.getStrategy().getBudget().getSum());

        assertNotNull(actualCampaign.getFlatStrategy());
        assertNotNull(actualCampaign.getFlatStrategy().getBudget());
        assertEquals(GdCampaignBudgetPeriod.WEEK, actualCampaign.getFlatStrategy().getBudget().getPeriod());
        assertEquals(BigDecimal.ZERO, actualCampaign.getFlatStrategy().getBudget().getSum());
    }

    /**
     * С включенной фичей ENABLE_REWORKED_RECOMMENDATIONS получаем кампанию без рекомендаций и без фильтраций по типу.
     * Кампания должна вернуться.
     */
    @Test
    public void testGetFilteredCampaignsWithReworkedRecommendations_noRecommendationsWithEmptyFilter() {
        GdiCampaign campaign = defaultCampaign(defaultAttributionModel);

        setupServicesForRecommendationTest(emptyMap(), campaign, emptySet());
        List<GdCampaign> gdCampaigns = campaignInfoService.getFilteredCampaigns(TEST_CLIENT, defaultOperator(),
                List.of(campaign), inputContainer, TEST_INSTANT, CAMPAIGN_FETCHED_FIELDS_RESOLVER);
        verify(gridRecommendationService)
                .getCampaignRecommendations(eq(TEST_CLIENT_ID),
                        eq(defaultOperator()),
                        eq(emptySet()),
                        eq(Set.of(campaign.getId())));
        assertNotNull(gdCampaigns);
        //проверяем что кампания есть
        assertThat(gdCampaigns).hasSize(1);

        GdCampaign actualCampaign = gdCampaigns.get(0);
        //проверяем что рекомендации все пустые
        assertNull(actualCampaign.getRecommendations());
        assertNull(actualCampaign.getDailyBudgetRecommendation());
        assertNull(actualCampaign.getIncreaseStrategyWeeklyBudgetRecommendation());
    }

    /**
     * С включенной фичей ENABLE_REWORKED_RECOMMENDATIONS получаем кампанию без рекомендаций и с фильтрацией по типу.
     * Должен прийти пустой список.
     */
    @Test
    public void testGetFilteredCampaignsWithReworkedRecommendations_noRecommendationsWithFilter() {
        GdiCampaign campaign = defaultCampaign(defaultAttributionModel);

        var recommendationTypes = Set.of(GdiRecommendationType.increaseStrategyTargetCPA);
        inputContainer.getFilter().setRecommendations(recommendationTypes);
        var actualRecommendationTypes = Sets.union(recommendationTypes, RECOMMENDATION_REWORKED_BUDGET_TYPES);

        setupServicesForRecommendationTest(emptyMap(), campaign, actualRecommendationTypes);

        List<GdCampaign> gdCampaigns = campaignInfoService.getFilteredCampaigns(TEST_CLIENT, defaultOperator(),
                List.of(campaign), inputContainer, TEST_INSTANT, CAMPAIGN_FETCHED_FIELDS_RESOLVER);
        verify(gridRecommendationService)
                .getCampaignRecommendations(eq(TEST_CLIENT_ID),
                        eq(defaultOperator()),
                        eq(actualRecommendationTypes),
                        eq(Set.of(campaign.getId())));
        assertNotNull(gdCampaigns);
        //проверяем, что кампания не вернулась
        assertThat(gdCampaigns).hasSize(0);
        inputContainer.getFilter().setRecommendations(emptySet());
    }

    /**
     * С включенной фичей ENABLE_REWORKED_RECOMMENDATIONS получаем кампанию с рекомендациями
     * типов increaseStrategyTargetCPA, dailyBudget и без фильтрации.
     * Две рекомендации должны вернуться в общем списке и одна в отдельном поле dailyBudgetRecommendation.
     */
    @Test
    public void testGetFilteredCampaignsWithReworkedRecommendations_recommendationsWithoutFilter() {
        GdiCampaign campaign = defaultCampaign(defaultAttributionModel);

        var actualRecommendations = List.of(
                emptyRecommendation(GdiRecommendationType.increaseStrategyTargetCPA),
                emptyRecommendation(GdiRecommendationType.dailyBudget)
        );
        setupServicesForRecommendationTest(Map.of(campaign.getId(), actualRecommendations), campaign, emptySet());

        List<GdCampaign> gdCampaigns = campaignInfoService.getFilteredCampaigns(TEST_CLIENT, defaultOperator(),
                List.of(campaign), inputContainer, TEST_INSTANT, CAMPAIGN_FETCHED_FIELDS_RESOLVER);
        verify(gridRecommendationService)
                .getCampaignRecommendations(eq(TEST_CLIENT_ID),
                        eq(defaultOperator()),
                        eq(emptySet()),
                        eq(Set.of(campaign.getId())));
        assertNotNull(gdCampaigns);
        assertThat(gdCampaigns).hasSize(1);

        GdCampaign actualCampaign = gdCampaigns.get(0);
        //проверяем что рекомендации во всех полях присутствуют
        assertThat(actualCampaign.getRecommendations()).hasSize(2);
        assertNotNull(actualCampaign.getDailyBudgetRecommendation());
        assertNull(actualCampaign.getIncreaseStrategyWeeklyBudgetRecommendation());
    }

    /**
     * С включенной фичей ENABLE_REWORKED_RECOMMENDATIONS получаем кампанию с рекомендациями
     * типов increaseStrategyWeeklyBudget и increaseStrategyTargetCPA, c фильтрацией по типу increaseStrategyTargetCPA.
     * Поле recommendations должно содержать только одну рекомендацию, но в отдельном поле
     * increaseStrategyWeeklyBudgetRecommendation вторая рекомендация должна быть.
     */
    @Test
    public void testGetFilteredCampaignsWithReworkedRecommendations_recommendationsWithFilter() {
        GdiCampaign campaign = defaultCampaign(defaultAttributionModel);

        var recommendationTypes = Set.of(GdiRecommendationType.increaseStrategyTargetCPA);
        inputContainer.getFilter().setRecommendations(recommendationTypes);
        var actualRecommendationTypes = Sets.union(recommendationTypes, RECOMMENDATION_REWORKED_BUDGET_TYPES);

        var actualRecommendations = List.of(
                emptyRecommendation(GdiRecommendationType.increaseStrategyTargetCPA),
                emptyRecommendation(GdiRecommendationType.increaseStrategyWeeklyBudget)
        );
        setupServicesForRecommendationTest(Map.of(campaign.getId(), actualRecommendations), campaign,
                actualRecommendationTypes);

        List<GdCampaign> gdCampaigns = campaignInfoService.getFilteredCampaigns(TEST_CLIENT, defaultOperator(),
                List.of(campaign), inputContainer, TEST_INSTANT, CAMPAIGN_FETCHED_FIELDS_RESOLVER);
        verify(gridRecommendationService)
                .getCampaignRecommendations(eq(TEST_CLIENT_ID),
                        eq(defaultOperator()),
                        eq(actualRecommendationTypes),
                        eq(Set.of(campaign.getId())));
        assertNotNull(gdCampaigns);
        assertThat(gdCampaigns).hasSize(1);

        GdCampaign actualCampaign = gdCampaigns.get(0);
        List<GdRecommendation> returnedRecommendations = actualCampaign.getRecommendations();
        //проверяем, что есть одна рекомендация нужного типа
        assertThat(returnedRecommendations).hasSize(1);
        assertThat(returnedRecommendations.get(0).getType())
                .isEqualTo(GdiRecommendationType.increaseStrategyTargetCPA);

        assertNull(actualCampaign.getDailyBudgetRecommendation());
        //рекомендация по увеличению недельного бюджета все равно должна показываться
        assertNotNull(actualCampaign.getIncreaseStrategyWeeklyBudgetRecommendation());

        inputContainer.getFilter().setRecommendations(emptySet());
    }

    /**
     * С включенной фичей ENABLE_REWORKED_RECOMMENDATIONS получаем кампанию с рекомендациями типа dailyBudget и
     * increaseStrategyWeeklyBudget, но поле recommendations не запрошено. Оно должно быть пустым, но поля
     * increaseStrategyWeeklyBudgetRecommendation и dailyBudgetRecommendation должны быть заполнены
     */
    @Test
    public void testGetFilteredCampaignsWithReworkedRecommendations_recommendationsWithoutFilterAndWithoutField() {
        GdiCampaign campaign = defaultCampaign(defaultAttributionModel);

        CAMPAIGN_FETCHED_FIELDS_RESOLVER.setRecommendations(false);


        var actualRecommendations = List.of(
                emptyRecommendation(GdiRecommendationType.dailyBudget),
                emptyRecommendation(GdiRecommendationType.increaseStrategyWeeklyBudget)
        );
        setupServicesForRecommendationTest(
                Map.of(campaign.getId(), actualRecommendations), campaign, RECOMMENDATION_REWORKED_BUDGET_TYPES);

        List<GdCampaign> gdCampaigns = campaignInfoService.getFilteredCampaigns(TEST_CLIENT, defaultOperator(),
                List.of(campaign), inputContainer, TEST_INSTANT, CAMPAIGN_FETCHED_FIELDS_RESOLVER);
        verify(gridRecommendationService)
                .getCampaignRecommendations(eq(TEST_CLIENT_ID),
                        eq(defaultOperator()),
                        eq(RECOMMENDATION_REWORKED_BUDGET_TYPES),
                        eq(Set.of(campaign.getId())));
        assertNotNull(gdCampaigns);
        assertThat(gdCampaigns).hasSize(1);

        GdCampaign actualCampaign = gdCampaigns.get(0);
        assertNull(actualCampaign.getRecommendations());

        assertNotNull(actualCampaign.getDailyBudgetRecommendation());
        assertNotNull(actualCampaign.getIncreaseStrategyWeeklyBudgetRecommendation());
        CAMPAIGN_FETCHED_FIELDS_RESOLVER.setRecommendations(true);
    }

    private void setupServicesForRecommendationTest(Map<Long, List<GdRecommendation>> toReturn,
                                                    GdiCampaign campaign,
                                                    Set<GdiRecommendationType> types) {
        doReturn(true)
                .when(featureService).isEnabledForClientId(any(),
                eq(FeatureName.ENABLE_REWORKED_RECOMMENDATIONS));
        doReturn(toReturn)
                .when(gridRecommendationService)
                .getCampaignRecommendations(
                        eq(TEST_CLIENT_ID),
                        eq(defaultOperator()),
                        eq(types),
                        eq(Set.of(campaign.getId())));
    }

    GdRecommendation emptyRecommendation(GdiRecommendationType type) {
        return new GdRecommendationWithKpi()
                .withType(type);
    }

    @Test
    public void testGetFilteredCampaigns_NotFavorite() {
        GdiCampaign notFavoriteCampaign = defaultCampaign(defaultAttributionModel)
                .withFavorite(false);
        GdiCampaign favoriteCampaign = defaultCampaign(defaultAttributionModel)
                .withFavorite(true);
        List<GdiCampaign> campaigns = asList(notFavoriteCampaign, favoriteCampaign);

        inputContainer.getFilter().setFavorite(false);
        List<GdCampaign> gdCampaigns = campaignInfoService
                .getFilteredCampaigns(TEST_CLIENT, defaultOperator(), campaigns, inputContainer, TEST_INSTANT,
                        CAMPAIGN_FETCHED_FIELDS_RESOLVER);

        GdCampaign expectedCampaign = new GdTextCampaign()
                .withId(notFavoriteCampaign.getId());
        assertThat(gdCampaigns)
                .is(matchedBy(beanDiffer(Collections.singletonList(expectedCampaign))
                        .useCompareStrategy(DefaultCompareStrategies.onlyExpectedFields())));
    }

    @Test
    public void testGetFilteredCampaigns_HasCompletedMediaplan() {
        GdiCampaign hasCompletedMediaplanCampaign = defaultCampaign(defaultAttributionModel)
                .withMediaplanStatus(GdiCampaignMediaplanStatus.COMPLETE)
                .withHasNewMediaplan(true)
                .withHasMediaplanBanners(true);
        GdiCampaign hasNotCompletedMediaplanCampaign = defaultCampaign(defaultAttributionModel)
                .withHasNewMediaplan(false);
        List<GdiCampaign> campaigns = asList(hasCompletedMediaplanCampaign, hasNotCompletedMediaplanCampaign);

        inputContainer.getFilter().setHasCompletedMediaplan(true);
        List<GdCampaign> gdCampaigns = campaignInfoService
                .getFilteredCampaigns(TEST_CLIENT, defaultOperator(), campaigns, inputContainer, TEST_INSTANT,
                        CAMPAIGN_FETCHED_FIELDS_RESOLVER);

        GdCampaign expectedCampaign = new GdTextCampaign()
                .withId(hasCompletedMediaplanCampaign.getId());
        assertThat(gdCampaigns)
                .is(matchedBy(beanDiffer(Collections.singletonList(expectedCampaign))
                        .useCompareStrategy(DefaultCompareStrategies.onlyExpectedFields())));
    }

    @Test
    public void testGetFilteredCampaigns_CpmDealsCampaign() {
        GdiCampaign cpmDealsCampaign = defaultCampaign(defaultAttributionModel)
                .withType(CampaignType.CPM_DEALS)
                .withAllowedPageIds(List.of(RandomNumberUtils.nextPositiveLong()))
                .withHasExtendedGeoTargeting(RandomUtils.nextBoolean());
        List<GdiCampaign> campaigns = List.of(cpmDealsCampaign);

        List<GdCampaign> gdCampaigns = campaignInfoService
                .getFilteredCampaigns(TEST_CLIENT, defaultOperator(), campaigns, inputContainer, TEST_INSTANT,
                        CAMPAIGN_FETCHED_FIELDS_RESOLVER);

        GdCampaign expectedCampaign = new GdCpmDealsCampaign()
                .withId(cpmDealsCampaign.getId())
                .withType(GdCampaignType.CPM_DEALS)
                .withAvailableAdGroupTypes(Set.of(GdAdGroupType.CPM_BANNER))
                .withAllowedPageIds(cpmDealsCampaign.getAllowedPageIds())
                .withHasExtendedGeoTargeting(cpmDealsCampaign.getHasExtendedGeoTargeting());
        assertThat(gdCampaigns)
                .is(matchedBy(beanDiffer(Collections.singletonList(expectedCampaign))
                        .useCompareStrategy(DefaultCompareStrategies.onlyExpectedFields())));
    }

    @Test
    public void getFilteredCampaigns_FilterByStatusThatExistsInCampaign() {
        GdiCampaign draftCampaign = defaultCampaign(TEST_CID)
                .withStatusModerate(CampaignStatusModerate.NEW);

        inputContainer.getFilter()
                .setStatusIn(Set.of(GdCampaignPrimaryStatus.DRAFT));

        List<GdCampaign> gdCampaigns = campaignInfoService
                .getFilteredCampaigns(TEST_CLIENT, new User(), List.of(draftCampaign),
                        inputContainer, TEST_INSTANT, CAMPAIGN_FETCHED_FIELDS_RESOLVER);

        assertThat(gdCampaigns).hasSize(1);
        assertThat(gdCampaigns.get(0).getId()).isEqualTo(TEST_CID);
    }

    @Test
    public void getFilteredCampaigns_FilterByStatusThatDoesNotExistInCampaign() {
        GdiCampaign draftCampaign = defaultCampaign(TEST_CID)
                .withStatusModerate(CampaignStatusModerate.NEW);

        Set<GdCampaignPrimaryStatus> filterStatus = Arrays.stream(GdCampaignPrimaryStatus.values())
                .filter(status -> status != GdCampaignPrimaryStatus.DRAFT)
                .collect(Collectors.toSet());

        inputContainer.getFilter()
                .setStatusIn(filterStatus);

        List<GdCampaign> gdCampaigns = campaignInfoService
                .getFilteredCampaigns(TEST_CLIENT, new User(), List.of(draftCampaign),
                        inputContainer, TEST_INSTANT, CAMPAIGN_FETCHED_FIELDS_RESOLVER);

        assertThat(gdCampaigns).hasSize(0);
    }

    @Test
    public void getFilteredCampaigns_FilterSubCampaigns() {
        GdiCampaign masterCampaign = defaultCampaign(TEST_CID);
        GdiCampaign subCampaign = defaultCampaign().withMasterCid(TEST_CID);

        List<GdCampaign> gdCampaigns = campaignInfoService
                .getFilteredCampaigns(TEST_CLIENT, new User(), List.of(masterCampaign, subCampaign),
                        inputContainer, TEST_INSTANT, CAMPAIGN_FETCHED_FIELDS_RESOLVER);

        assertThat(gdCampaigns).hasSize(1);
        assertThat(gdCampaigns.get(0).getId()).isEqualTo(TEST_CID);
    }

    @Test
    public void test_toCampaignForAccess() throws IllegalAccessException, InvocationTargetException {
        GdiCampaign gdiCampaign = new GdiCampaign()
                .withId(1L)
                .withType(CampaignType.TEXT)
                .withArchived(false)
                .withSource(GdiCampaignSource.DIRECT);
        CampaignForAccessCheckDefaultImpl campaignForAccessCheck = toCampaignForAccess(gdiCampaign);
        for (var field : campaignForAccessCheck.getClass().getDeclaredFields()) {
            field.setAccessible(true);
            assertNotNull(field.getName(), field.get(campaignForAccessCheck));
        }
    }

    @Test
    @Parameters(method = "validateBudgetChange")
    public void test_CpmCampaignMinimalBudget_withZeroSpent(CpmCampaignBudgetTestData input) {
        var campaign = new CpmBannerCampaign()
                .withId(1L)
                .withOrderId(1L)
                .withSumSpent(input.getSumSpent())
                .withStrategy((DbStrategy) new DbStrategy()
                        .withStrategyName(StrategyName.AUTOBUDGET_MAX_REACH_CUSTOM_PERIOD)
                        .withStrategyData(
                                new StrategyData()
                                        .withBudget(input.getBudget())
                                        .withName(CampaignsStrategyName.autobudget_max_impressions_custom_period.getLiteral())
                                        .withDailyChangeCount(3L)
                                        .withLastUpdateTime(NOW)
                                        .withStart(NOW.toLocalDate().minusDays(input.getStartedDaysAgo()))
                                        .withFinish(NOW.toLocalDate().plusDays(input.getOldFinishInDays()))));
        doReturn(Map.of(
                1L, campaign
        )).when(campaignTypedRepository).getTypedCampaignsMap(eq(TEST_SHARD), any());

        doReturn(Map.of(
                1L, Money.valueOf(campaign.getSumSpent(), CurrencyCode.RUB)
        )).when(orderStatService).getSpentSumForOrderDuringPeriod(any(), any(), any(), any(), eq(true));

        var item = new GdCpmCampaignDayBudgetLimitsRequestItem()
                .withFinishDate(LocalDate.now().plusDays(input.getNewFinishInDays()))
                .withStartDate(LocalDate.now().minusDays(10))
                .withIsRestarting(false)
                .withCampaignId(1L);
        var result = campaignInfoService.getBudgetLimits(TEST_SHARD, CurrencyCode.RUB,
                new GdCpmCampaignDayBudgetLimitsRequest().withItems(List.of(item)));

        assertThat(result.getLimits().get(0).getMinimalBudget()).isEqualTo(input.getResult());
    }

    public void getFilteredCampaigns_FilterBySource() {
        final var campaigns = List.of(
                defaultCpmCampaign(1L).withSource(GdiCampaignSource.UAC),
                defaultCpmCampaign(2L).withSource(GdiCampaignSource.WIDGET),
                defaultCpmCampaign(3L).withSource(GdiCampaignSource.DIRECT),
                defaultCpmCampaign(4L).withSource(GdiCampaignSource.DC),
                defaultCpmCampaign(5L).withSource(GdiCampaignSource.API),
                defaultCpmCampaign(6L).withSource(GdiCampaignSource.EDA),
                defaultCpmCampaign(7L).withSource(GdiCampaignSource.GEO),
                defaultCpmCampaign(8L).withSource(GdiCampaignSource.USLUGI),
                defaultCpmCampaign(9L).withSource(GdiCampaignSource.XLS),
                defaultCpmCampaign(10L).withSource(GdiCampaignSource.ZEN));

        final var sources = List.of(
                GdCampaignFilterSource.UAC,
                GdCampaignFilterSource.WIDGET,
                GdCampaignFilterSource.DIRECT,
                GdCampaignFilterSource.DC,
                GdCampaignFilterSource.API,
                GdCampaignFilterSource.EDA,
                GdCampaignFilterSource.GEO,
                GdCampaignFilterSource.USLUGI,
                GdCampaignFilterSource.XLS,
                GdCampaignFilterSource.ZEN);

        for (var source : sources) {
            inputContainer.getFilter()
                    .withSource(source);

            List<GdCampaign> gdCampaigns = campaignInfoService
                    .getFilteredCampaigns(TEST_CLIENT, new User(), campaigns,
                            inputContainer, TEST_INSTANT, CAMPAIGN_FETCHED_FIELDS_RESOLVER);

            assertThat(gdCampaigns).hasSize(1);
            assertThat(gdCampaigns.get(0).getSource().name()).isEqualTo(source.name());
        }
    }

    @Test
    public void getFilteredCampaigns_FilterByWidgetPartnerId() {
        final var campaigns = List.of(
                defaultCpmCampaign(1L).withSource(GdiCampaignSource.WIDGET).withWidgetPartnerId(1L),
                defaultCpmCampaign(2L).withSource(GdiCampaignSource.WIDGET).withWidgetPartnerId(1L),
                defaultCpmCampaign(3L).withSource(GdiCampaignSource.WIDGET).withWidgetPartnerId(2L),
                defaultCpmCampaign(4L).withSource(GdiCampaignSource.WIDGET));

        inputContainer.getFilter()
                .withWidgetPartnerId(1L);

        List<GdCampaign> gdCampaigns = campaignInfoService
                .getFilteredCampaigns(TEST_CLIENT, new User(), campaigns,
                        inputContainer, TEST_INSTANT, CAMPAIGN_FETCHED_FIELDS_RESOLVER);

        assertThat(gdCampaigns).hasSize(2);
        assertThat(gdCampaigns.stream().map(GdCampaign::getId).collect(Collectors.toSet())).contains(1L, 2L);
    }

    @Test
    @TestCaseName("{0}, useCurrentRegion : {2}, useRegularRegion: {3}, hasExtendedGeotargeting: {4}")
    @Parameters(method = "advancedGeoTargetingParams")
    public void getCampaign_WithAdvancedGeoTargeting(
            String description,
            boolean advancedGeoTargeting,
            boolean useCurrentRegion,
            boolean useRegularRegion,
            boolean hasExtendedGeotargeting,
            boolean expectedUseCurrentRegion,
            boolean expectedUseRegularRegion,
            boolean expectedHasExtendedGeotargeting
            ) {
        GdiCampaign masterCampaign = defaultCampaign(TEST_CID)
                .withUseCurrentRegion(useCurrentRegion)
                .withUseRegularRegion(useRegularRegion)
                .withHasExtendedGeoTargeting(hasExtendedGeotargeting);

        doReturn(advancedGeoTargeting)
                .when(featureService).isEnabledForClientId(any(),
                eq(FeatureName.ADVANCED_GEOTARGETING));

        List<GdCampaign> gdCampaigns = campaignInfoService
                .getFilteredCampaigns(TEST_CLIENT, new User(), List.of(masterCampaign),
                        inputContainer, TEST_INSTANT, CAMPAIGN_FETCHED_FIELDS_RESOLVER);


        softly.assertThat(gdCampaigns).hasSize(1);
        var actual = (GdTextCampaign) gdCampaigns.get(0);
        softly.assertThat(actual.getId()).isEqualTo(TEST_CID);
        softly.assertThat(actual.getUseCurrentRegion()).isEqualTo(expectedUseCurrentRegion);
        softly.assertThat(actual.getUseRegularRegion()).isEqualTo(expectedUseRegularRegion);
        softly.assertThat(actual.getHasExtendedGeoTargeting()).isEqualTo(expectedHasExtendedGeotargeting);
    }

    private class CpmCampaignBudgetTestData {
        private final BigDecimal sumSpent;
        private final BigDecimal budget;
        private final BigDecimal result;
        private final int startedDaysAgo;
        private final int oldFinishInDays;
        private final int newFinishInDays;

        private CpmCampaignBudgetTestData(BigDecimal sumSpent, BigDecimal budget, int startedDaysAgo,
                                          int oldFinishInDays, int newFinishInDays, BigDecimal result) {
            this.sumSpent = sumSpent;
            this.budget = budget;
            this.result = result;
            this.startedDaysAgo = startedDaysAgo;
            this.oldFinishInDays = oldFinishInDays;
            this.newFinishInDays = newFinishInDays;
        }

        public BigDecimal getSumSpent() {
            return sumSpent;
        }

        public BigDecimal getBudget() {
            return budget;
        }

        public BigDecimal getResult() {
            return result;
        }

        public int getStartedDaysAgo() {
            return startedDaysAgo;
        }

        public int getOldFinishInDays() {
            return oldFinishInDays;
        }

        public int getNewFinishInDays() {
            return newFinishInDays;
        }
    }

    private Iterable<Object[]> advancedGeoTargetingParams() {
        return asList(new Object[][]{
                {"feature is on", true, true, true, true, true, true, true},
                {"feature is on", true, false, true, false, false, true, false},
                {"feature is on", true, true, false, true, true, false, true},
                {"feature is on", true, false, false, false, true, false, false},
                {"feature is on", true, false, false, true, true, true, true},
                {"feature is off", false, false, false, false, false, false, false},
                {"feature is off", false, true, false, true, false, false, true},
                {"feature is off", false, true, true, true, false, false, true},
        });
    }

    private Iterable<CpmCampaignBudgetTestData> validateBudgetChange() {
        return asList(
                new CpmCampaignBudgetTestData(
                        BigDecimal.valueOf(0),
                        BigDecimal.valueOf(3000),
                        9,
                        10,
                        30,
                        BigDecimal.valueOf(12000)
                ),
                new CpmCampaignBudgetTestData(
                        BigDecimal.valueOf(15000),
                        BigDecimal.valueOf(30000),
                        9,
                        10,
                        30,
                        BigDecimal.valueOf(25500)
                ),
                new CpmCampaignBudgetTestData(
                        BigDecimal.valueOf(0),
                        BigDecimal.valueOf(5000),
                        9,
                        10,
                        1,
                        BigDecimal.valueOf(3300)
                ),
                new CpmCampaignBudgetTestData(
                        BigDecimal.valueOf(15000),
                        BigDecimal.valueOf(16000),
                        9,
                        10,
                        1,
                        BigDecimal.valueOf(16000)
                ),
                new CpmCampaignBudgetTestData(
                        BigDecimal.valueOf(2000),
                        BigDecimal.valueOf(15000),
                        6,
                        10,
                        23,
                        BigDecimal.valueOf(9100)
                ),
                new CpmCampaignBudgetTestData(
                        BigDecimal.valueOf(13670),
                        BigDecimal.valueOf(25000),
                        19,
                        50,
                        50,
                        BigDecimal.valueOf(25000)
                ),
                new CpmCampaignBudgetTestData(
                        BigDecimal.valueOf(3000),
                        BigDecimal.valueOf(15000),
                        10,
                        19,
                        19,
                        BigDecimal.valueOf(9000)
                ),
                new CpmCampaignBudgetTestData(
                        BigDecimal.valueOf(2000),
                        BigDecimal.valueOf(9999),
                        9,
                        30,
                        30,
                        BigDecimal.valueOf(12000)
                ),
                new CpmCampaignBudgetTestData(
                        BigDecimal.valueOf(5000),
                        BigDecimal.valueOf(17500),
                        4,
                        10,
                        40,
                        BigDecimal.valueOf(17500)

                )
        );
    }

    private static GdiCampaign defaultWalletCampaign(long id) {
        return defaultCampaign(id)
                .withMoneyBlocked(false)
                .withSum(BigDecimal.valueOf(2000))
                .withSumSpent(BigDecimal.ZERO)
                .withType(CampaignType.WALLET);
    }

    private static GdiCampaign defaultCpmCampaign(long id) {
        return defaultCampaign(id)
                .withType(CampaignType.CPM_BANNER);
    }

    private static GdiCampaign defaultNoBannersCampaigns(long id) {
        return defaultCampaign(id)
                .withSum(BigDecimal.ZERO)
                .withSumRest(BigDecimal.ZERO)
                .withHasActiveBanners(false)
                .withHasNotArchiveBanners(false)
                .withHasBanners(false);
    }

    private static GdiCampaign defaultInternalDistribCampaign(long id) {
        return defaultCampaign(id)
                .withType(CampaignType.INTERNAL_DISTRIB);
    }

    private static GdiCampaign defaultInternalAutobudgetCampaign(long id) {
        return defaultCampaign(id)
                .withType(CampaignType.INTERNAL_AUTOBUDGET);
    }

    private static User defaultOperator() {
        return new User()
                .withUid(TEST_OPERATOR_UID)
                .withRole(RbacRole.CLIENT)
                .withSuperManager(false)
                .withDeveloper(false);
    }

    private static BigDecimal getExpectedBigDecimalSum(long sum) {
        return BigDecimal.valueOf(sum).setScale(Money.MONEY_CENT_SCALE, BigDecimal.ROUND_DOWN);
    }

    private static GdWallet defaultExpectedWallet(GdiCampaign walletCampaign) {
        return new GdWallet()
                .withId(walletCampaign.getId())
                .withCurrency(walletCampaign.getCurrencyCode())
                .withActions(Set.of(GdWalletAction.PAY))
                .withBudget(new GdWalletBudget().withType(GdWalletBudgetType.NO_BUDGET))
                .withAutoOverdraftAddition(Money.valueOf(BigDecimal.ZERO, walletCampaign.getCurrencyCode())
                        .roundToCentDown().bigDecimalValue())
                .withIsAgencyWallet(false)
                .withStatus(new GdWalletStatus()
                        .withEnabled(true)
                        .withMoneyBlocked(false)
                        .withWaitingForPayment(false)
                        .withShowNds(true)
                        .withDisabledButHaveMoney(false)
                        .withNeedCampModerationToPay(false)
                        .withNeedsNewPayment(false));

    }

    private static GdCampaign defaultExpectedCampaign(GdiCampaign campaign) {
        return defaultExpectedCampaign(campaign, GdTextCampaign::new);
    }

    private static GdBroadMatch defaultBroadMatch() {
        return new GdBroadMatch()
                .withBroadMatchFlag(true)
                .withBroadMatchLimit(CampaignConstants.BROAD_MATCH_LIMIT_DEFAULT)
                .withBroadMatchGoalId(BROAD_MATCH_GOAL_ID);
    }

    private static GdTimeTarget defaultGdTimeTarget() {
        return DefaultValuesUtils.defaultGdTimeTarget()
                .withUseWorkingWeekends(false)
                .withIdTimeZone(1L);
    }

    private static GdTextCampaign defaultExpectedTextCampaign(GdiCampaign campaign) {
        return ((GdTextCampaign) defaultExpectedCampaign(campaign))
                .withDisabledPlaces(FunctionalUtils.setUnion(DISABLED_DOMAINS, DISABLED_SSP))
                .withHasAddMetrikaTagToUrl(DEFAULT_ADD_METRIKA_TAG_TO_URL)
                .withHasAddOpenstatTagToUrl(DEFAULT_ADD_OPENSTAT_TAG_TO_URL)
                .withAdditionalData(new GdCampaignAdditionalData());
    }

    private static <T extends GdCampaign> T defaultExpectedCampaign(GdiCampaign campaign, Supplier<T> constructor) {
        T gdCampaign = constructor.get();
        gdCampaign
                .withIndex(0)
                .withId(campaign.getId())
                .withName(campaign.getName())
                .withSum(EXPECTED_ZERO_SUM)
                .withShows(0L)
                .withClicks(0L)
                .withCurrency(campaign.getCurrencyCode())
                .withType(toGdCampaignType(campaign.getType()))
                .withHasAds(false)
                .withNoPay(false)
                .withStartDate(campaign.getStartDate())
                .withFlatStrategy(new GdCampaignStrategyManual()
                        .withType(GdCampaignStrategyType.DEFAULT)
                        .withStrategyType(GdStrategyType.DEFAULT)
                        .withIsAutoBudget(false)
                        .withPlatform(toGdCampaignPlatform(campaign.getPlatform()))
                        .withSeparateBidding(false)
                        .withCanSetNetworkBids(false)
                        .withBudget(new GdCampaignBudget()
                                .withSum(BigDecimal.ZERO)
                                .withPeriod(GdCampaignBudgetPeriod.DAY)
                                .withShowMode(GdCampaignBudgetShowMode.DEFAULT)))
                .withStrategy(new GdCampaignStrategyManual()
                        .withType(GdCampaignStrategyType.NO_PREMIUM)
                        .withStrategyType(GdStrategyType.DEFAULT)
                        .withIsAutoBudget(false)
                        .withPlatform(toGdCampaignPlatform(campaign.getPlatform()))
                        .withSeparateBidding(false)
                        .withCanSetNetworkBids(false)
                        .withBudget(new GdCampaignBudget()
                                .withSum(BigDecimal.ZERO)
                                .withPeriod(GdCampaignBudgetPeriod.DAY)
                                .withShowMode(GdCampaignBudgetShowMode.DEFAULT)))
                .withStats(EXPECTED_STATS)
                .withStatus(new GdCampaignStatus()
                        .withCampaignId(campaign.getId())
                        .withMoneyBlocked(false)
                        .withReadOnly(false)
                        .withOver(false)
                        .withWaitingForPayment(false)
                        .withModerationStatus(GdCampaignStatusModerate.YES)
                        .withPrimaryStatus(GdCampaignPrimaryStatus.STOPPED)
                        .withPrimaryStatusDesc(GdCampaignPrimaryStatusDesc.NO_ACTIVE_BANNERS)
                        .withArchived(false)
                        .withWaitingForUnArchiving(false)
                        .withDraft(false)
                        .withNeedsNewPayment(false)
                        .withActivating(false)
                        .withWaitingForArchiving(false)
                        .withAllowDomainMonitoring(true)
                )
                .withNotification(extractGdNotificationData(campaign))
                .withIsAimingAllowed(false)
                .withAccess(DEFAULT_ACCESS)
                .withAvailableAdGroupTypes(singleton(GdAdGroupType.TEXT))
                .withAgencyUserId(campaign.getAgencyUserId())
                .withManagerUserId(campaign.getManagerUserId())
                .withDisabledIps(DISABLED_IPS)
                .withFeatures(emptySet())
                .withSource(GdCampaignSource.DIRECT)
                .withIsRecommendationsManagementEnabled(false)
                .withIsPriceRecommendationsManagementEnabled(false)
                .withBrandSafety(extractGdBrandSafetyData(campaign));
        return gdCampaign;
    }
}
