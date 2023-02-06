package ru.yandex.direct.grid.processing.service.banner;

import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.assertj.core.api.SoftAssertions;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.common.db.PpcProperty;
import ru.yandex.direct.core.aggregatedstatuses.AggregatedStatusesViewService;
import ru.yandex.direct.core.entity.creative.model.Creative;
import ru.yandex.direct.core.entity.creative.model.CreativeType;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.entity.moderationreason.service.ModerationReasonService;
import ru.yandex.direct.core.entity.organizations.service.OrganizationService;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.dbschema.ppc.enums.BannersBannerType;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.grid.core.entity.banner.model.GdiBanner;
import ru.yandex.direct.grid.core.entity.banner.model.GdiBannerFilter;
import ru.yandex.direct.grid.core.entity.banner.model.GdiBannerOrderBy;
import ru.yandex.direct.grid.core.entity.banner.model.GdiBannerStatusBsSynced;
import ru.yandex.direct.grid.core.entity.banner.model.GdiBannerStatusModerate;
import ru.yandex.direct.grid.core.entity.banner.model.GdiBannerStatusPostModerate;
import ru.yandex.direct.grid.core.entity.banner.model.GdiBannersWithTotals;
import ru.yandex.direct.grid.core.entity.banner.service.GridBannerService;
import ru.yandex.direct.grid.core.entity.fetchedfieldresolver.AdFetchedFieldsResolver;
import ru.yandex.direct.grid.core.entity.model.GdiEntityStats;
import ru.yandex.direct.grid.core.entity.recommendation.service.GridRecommendationService;
import ru.yandex.direct.grid.core.util.FetchedFieldsResolverCoreUtil;
import ru.yandex.direct.grid.core.util.stats.GridStatNew;
import ru.yandex.direct.grid.model.GdStatRequirements;
import ru.yandex.direct.grid.model.Order;
import ru.yandex.direct.grid.model.campaign.GdCampaign;
import ru.yandex.direct.grid.model.campaign.GdCampaignStatus;
import ru.yandex.direct.grid.model.campaign.GdTextCampaign;
import ru.yandex.direct.grid.processing.context.container.GridGraphQLContext;
import ru.yandex.direct.grid.processing.model.banner.GdAd;
import ru.yandex.direct.grid.processing.model.banner.GdAdFilter;
import ru.yandex.direct.grid.processing.model.banner.GdAdOrderBy;
import ru.yandex.direct.grid.processing.model.banner.GdAdOrderByField;
import ru.yandex.direct.grid.processing.model.banner.GdAdPrimaryStatus;
import ru.yandex.direct.grid.processing.model.banner.GdAdType;
import ru.yandex.direct.grid.processing.model.banner.GdAdWithTotals;
import ru.yandex.direct.grid.processing.model.banner.GdAdsContainer;
import ru.yandex.direct.grid.processing.model.banner.GdImageAd;
import ru.yandex.direct.grid.processing.model.banner.mutation.GdAddAds;
import ru.yandex.direct.grid.processing.model.banner.mutation.GdAddAdsPayload;
import ru.yandex.direct.grid.processing.model.banner.mutation.GdAddSmartAds;
import ru.yandex.direct.grid.processing.model.banner.mutation.GdUpdateAds;
import ru.yandex.direct.grid.processing.model.banner.mutation.GdUpdateAdsPayload;
import ru.yandex.direct.grid.processing.model.banner.mutation.GdUpdateSmartAds;
import ru.yandex.direct.grid.processing.model.client.GdClientInfo;
import ru.yandex.direct.grid.processing.model.cliententity.GdCanvasCreative;
import ru.yandex.direct.grid.processing.model.group.GdAdGroup;
import ru.yandex.direct.grid.processing.model.group.GdAdGroupAccess;
import ru.yandex.direct.grid.processing.model.group.GdTextAdGroup;
import ru.yandex.direct.grid.processing.model.recommendation.GdRecommendation;
import ru.yandex.direct.grid.processing.model.recommendation.GdRecommendationKey;
import ru.yandex.direct.grid.processing.model.recommendation.GdRecommendationWithoutKpi;
import ru.yandex.direct.grid.processing.service.campaign.CampaignInfoService;
import ru.yandex.direct.grid.processing.service.group.GroupDataService;
import ru.yandex.direct.grid.processing.util.ContextHelper;
import ru.yandex.direct.turbopages.client.TurbopagesClient;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;
import static ru.yandex.direct.common.db.PpcPropertyNames.MODERATION_REASONS_ALLOWABLE_TO_REMODERATE_BY_CLIENT;
import static ru.yandex.direct.core.testing.steps.BannerPriceSteps.defaultNewBannerPrice;
import static ru.yandex.direct.grid.model.entity.recommendation.GdiRecommendationType.dailyBudget;
import static ru.yandex.direct.grid.processing.data.TestGdAds.getDefaultGdAdsContainer;
import static ru.yandex.direct.grid.processing.util.CampaignTestDataUtils.defaultGdCampaignStatus;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

public class BannerDataServiceTest {
    private static final long BANNER_ONE = 193;
    private static final long BANNER_TWO = 293;
    private static final long GROUP = 153;
    private static final long CAMPAIGN = 10001L;
    private static final GdAdGroup GROUP_OBJ = new GdTextAdGroup()
            .withCampaignId(CAMPAIGN)
            .withCampaign(new GdTextCampaign()
                    .withId(CAMPAIGN)
                    .withStatus(new GdCampaignStatus().withAllowDomainMonitoring(true)))
            .withId(GROUP)
            .withAccess(new GdAdGroupAccess()
                    .withCanEdit(true));
    private static final LocalDate TEST_FROM = LocalDate.now();
    private static final LocalDate TEST_TO = TEST_FROM.plusDays(10);
    private static final AdFetchedFieldsResolver AD_FETCHED_FIELDS_RESOLVER =
            FetchedFieldsResolverCoreUtil.buildAdFetchedFieldsResolver(true);

    private static GridGraphQLContext gridGraphQLContext;
    private static GdClientInfo clientInfo;
    private static ClientId clientId;
    private static Long operatorUid;
    private static Map<Long, GdCampaign> campaignsById;

    private GdAdsContainer adsContainer;

    @Mock
    private GridBannerService gridBannerService;

    @Mock
    private GridRecommendationService gridRecommendationService;

    @Mock
    private GroupDataService groupDataService;

    @Mock
    private CampaignInfoService campaignInfoService;

    @Mock
    private FeatureService featureService;

    @Mock
    private TurbopagesClient turbopagesClient;

    @Mock
    private AdValidationService adValidationService;

    @Mock
    private OrganizationService organizationService;

    @Mock
    private AggregatedStatusesViewService aggregatedStatusesViewService;

    @Mock
    private ModerationReasonService moderationReasonService;

    @Mock
    private PpcPropertiesSupport ppcPropertiesSupport;

    @Mock
    private PpcProperty<Set<Long>> moderationReasonsAllowableToRemoderateByClient;

    @InjectMocks
    private BannerDataService bannerDataService;

    @BeforeClass
    public static void initTestData() {
        gridGraphQLContext = ContextHelper.buildDefaultContext();
        gridGraphQLContext.getFetchedFieldsReslover().setAd(AD_FETCHED_FIELDS_RESOLVER);
        clientInfo = gridGraphQLContext.getQueriedClient();
        clientId = ClientId.fromLong(clientInfo.getId());
        operatorUid = gridGraphQLContext.getOperator().getUid();

        campaignsById = Collections.singletonMap(GROUP_OBJ.getCampaignId(), (GdCampaign) GROUP_OBJ.getCampaign());
    }

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);

        doReturn(new GdiBannersWithTotals().withGdiBanners(emptyList()))
                .when(gridBannerService)
                .getBanners(eq(clientInfo.getShard()), eq(operatorUid), eq(clientId),
                        anySet(), any(), any(),
                        eq(TEST_FROM), eq(TEST_TO), any(), eq(emptySet()), eq(TEST_FROM), eq(TEST_TO), any(),
                        anyBoolean());

        doReturn(Collections.emptyMap())
                .when(gridRecommendationService)
                .getBannerRecommendations(eq(clientInfo.getId()), any(), any(), any());

        doReturn(campaignsById)
                .when(campaignInfoService)
                .getTruncatedCampaigns(ClientId.fromLong(clientInfo.getId()), singleton(CAMPAIGN));

        doReturn(emptyMap())
                .when(aggregatedStatusesViewService)
                .getAdStatusesByIds(anyInt(), anySet());

        doReturn(emptyMap())
                .when(moderationReasonService)
                .getReasonIdsForBannerAndResources(anyInt(), anySet());


        doReturn(null)
                .when(moderationReasonsAllowableToRemoderateByClient)
                .get();

        doReturn(moderationReasonsAllowableToRemoderateByClient)
                .when(ppcPropertiesSupport)
                .get(eq(MODERATION_REASONS_ALLOWABLE_TO_REMODERATE_BY_CLIENT));

        adsContainer = getDefaultGdAdsContainer()
                .withStatRequirements(new GdStatRequirements()
                        .withFrom(TEST_FROM)
                        .withTo(TEST_TO)
                        .withStatsByDaysFrom(TEST_FROM)
                        .withStatsByDaysTo(TEST_TO));
    }


    @Test
    public void testGetBanners_NoSelected() {
        bannerDataService.getBanners(clientInfo, adsContainer, gridGraphQLContext);

        verify(gridBannerService).getBanners(eq(clientInfo.getShard()), eq(operatorUid), eq(clientId),
                anySet(), eq(new GdiBannerFilter()), anyList(), eq(TEST_FROM), eq(TEST_TO),
                any(), eq(emptySet()), eq(TEST_FROM), eq(TEST_TO), any(), anyBoolean());
    }

    @Test
    public void testGetBanners_HasSelected() {
        adsContainer.getFilter().setAdIdIn(Collections.singleton(BANNER_ONE));
        bannerDataService.getBanners(clientInfo, adsContainer, gridGraphQLContext);

        GdiBannerFilter expectedInternalFilter = new GdiBannerFilter()
                .withBannerIdIn(adsContainer.getFilter().getAdIdIn());
        verify(gridBannerService).getBanners(eq(clientInfo.getShard()), eq(operatorUid), eq(clientId),
                anySet(), eq(expectedInternalFilter), anyList(), eq(TEST_FROM), eq(TEST_TO),
                any(), eq(emptySet()), eq(TEST_FROM), eq(TEST_TO), any(), anyBoolean());
    }

    @Test
    public void testGetBanners_WithRecommendationsFilter() {
        GdRecommendationKey key1 = new GdRecommendationKey()
                .withCid(CAMPAIGN)
                .withPid(GROUP)
                .withBid(BANNER_ONE);
        GdRecommendationKey key2 = new GdRecommendationKey()
                .withCid(CAMPAIGN)
                .withPid(GROUP)
                .withBid(BANNER_TWO);
        Map<Long, List<GdRecommendation>> bannerIdToRecommendations =
                ImmutableMap.<Long, List<GdRecommendation>>builder()
                        .put(BANNER_ONE, singletonList(new GdRecommendationWithoutKpi()
                                .withKeys(singletonList(key1))))
                        .put(BANNER_TWO, singletonList(new GdRecommendationWithoutKpi()
                                .withKeys(singletonList(key2))))
                        .build();
        doReturn(bannerIdToRecommendations)
                .when(gridRecommendationService)
                .getBannerRecommendations(eq(clientInfo.getId()), any(), any(), any());

        adsContainer.getFilter()
                .withCampaignIdIn(singleton(CAMPAIGN))
                .withAdGroupIdIn(singleton(BANNER_ONE))
                .withAdIdNotIn(singleton(BANNER_TWO))
                .withAdGroupIdIn(singleton(GROUP))
                .withRecommendations(singleton(dailyBudget));
        bannerDataService.getBanners(clientInfo, adsContainer, gridGraphQLContext);

        GdiBannerFilter expectedInternalFilter = new GdiBannerFilter()
                .withRecommendations(singletonList(key1))
                .withCampaignIdIn(null)
                .withAdGroupIdIn(null)
                .withBannerIdIn(null)
                .withBannerIdNotIn(null);
        verify(gridBannerService).getBanners(eq(clientInfo.getShard()), eq(operatorUid), eq(clientId),
                anySet(), eq(expectedInternalFilter), anyList(), eq(TEST_FROM), eq(TEST_TO),
                any(), eq(emptySet()), eq(TEST_FROM), eq(TEST_TO), any(), anyBoolean());
    }

    @Test
    public void testGetBanners_OrderBy() {
        GdAdOrderBy gdAdOrderByWhichSkip = new GdAdOrderBy()
                .withOrder(Order.DESC)
                .withField(GdAdOrderByField.PRIMARY_STATUS);
        GdAdOrderBy gdAdOrderBy = new GdAdOrderBy()
                .withOrder(Order.ASC)
                .withField(GdAdOrderByField.TITLE_EXTENSION);
        adsContainer.setOrderBy(asList(gdAdOrderBy, gdAdOrderByWhichSkip));
        adsContainer.getFilter().setAdIdIn(Collections.singleton(BANNER_ONE));

        bannerDataService.getBanners(clientInfo, adsContainer, gridGraphQLContext);

        GdiBannerFilter expectedInternalFilter = new GdiBannerFilter()
                .withBannerIdIn(adsContainer.getFilter().getAdIdIn());
        List<GdiBannerOrderBy> expectedBannerOrderByList = ImmutableList.of(
                new GdiBannerOrderBy()
                        .withField(GdAdOrderByField.toSource(gdAdOrderBy.getField()))
                        .withOrder(gdAdOrderBy.getOrder())
        );
        verify(gridBannerService).getBanners(eq(clientInfo.getShard()), eq(operatorUid), eq(clientId),
                anySet(), eq(expectedInternalFilter), eq(expectedBannerOrderByList), eq(TEST_FROM), eq(TEST_TO),
                any(), eq(emptySet()), eq(TEST_FROM), eq(TEST_TO), eq(AD_FETCHED_FIELDS_RESOLVER), eq(false));
    }

    @Test
    public void testGetBanners_HasGetMonitoringStoppedBanners_WhenFilterIsNull() {
        doReturn(new GdiBannersWithTotals()
                .withGdiBanners(ImmutableList.of(generateTestBanner(BANNER_ONE), generateTestBanner(BANNER_TWO))))
                .when(gridBannerService)
                .getBanners(eq(clientInfo.getShard()), eq(operatorUid), eq(clientId), anySet(),
                        any(), anyList(), eq(TEST_FROM), eq(TEST_TO), any(), eq(emptySet()), any(), any(), any(),
                        eq(false));
        doReturn(ImmutableSet.of(BANNER_ONE))
                .when(gridBannerService)
                .getMonitoringStoppedBanners(eq(clientInfo.getShard()), eq(ImmutableSet.of(BANNER_ONE, BANNER_TWO)));
        doReturn(Collections.singletonList(GROUP_OBJ))
                .when(groupDataService)
                .getTruncatedAdGroups(eq(clientInfo.getShard()),
                        eq(clientInfo.getCountryRegionId()), eq(clientId),
                        eq(gridGraphQLContext.getOperator()), any(), any());

        adsContainer.getFilter().setAdIdIn(ImmutableSet.of(BANNER_ONE, BANNER_TWO));
        GdAdWithTotals gdAdWithTotals = bannerDataService.getBanners(clientInfo, adsContainer, gridGraphQLContext);
        List<GdAd> banners = gdAdWithTotals.getGdAds();

        verify(gridBannerService)
                .getMonitoringStoppedBanners(eq(clientInfo.getShard()), eq(ImmutableSet.of(BANNER_ONE, BANNER_TWO)));

        assertThat(mapList(banners, GdAd::getId)).containsExactly(BANNER_ONE, BANNER_TWO);
    }

    @Test
    public void testGetBannersIsEmpty_WhenStatusFilterNotEmpty_AndBannerHasTemporarilySuspendedStatus() {
        doReturn(new GdiBannersWithTotals()
                .withGdiBanners(ImmutableList.of(generateTestBanner(BANNER_ONE), generateTestBanner(BANNER_TWO))))
                .when(gridBannerService)
                .getBanners(eq(clientInfo.getShard()), eq(operatorUid), eq(clientId),
                        anySet(), any(), anyList(), eq(TEST_FROM), eq(TEST_TO), any(), eq(emptySet()),
                        eq(TEST_FROM), eq(TEST_TO), any(), eq(false));
        doReturn(ImmutableSet.of(BANNER_ONE, BANNER_TWO))
                .when(gridBannerService)
                .getMonitoringStoppedBanners(eq(clientInfo.getShard()), eq(ImmutableSet.of(BANNER_ONE, BANNER_TWO)));
        doReturn(Collections.singletonList(GROUP_OBJ))
                .when(groupDataService)
                .getTruncatedAdGroups(eq(clientInfo.getShard()),
                        eq(clientInfo.getCountryRegionId()), eq(clientId),
                        eq(gridGraphQLContext.getOperator()), any(), any());

        adsContainer.getFilter()
                .withAdIdIn(ImmutableSet.of(BANNER_ONE, BANNER_TWO))
                .withPrimaryStatusContains(ImmutableSet.of(GdAdPrimaryStatus.ACTIVE));
        GdAdWithTotals gdAdWithTotals = bannerDataService.getBanners(clientInfo, adsContainer, gridGraphQLContext);

        verify(gridBannerService)
                .getMonitoringStoppedBanners(eq(clientInfo.getShard()), eq(ImmutableSet.of(BANNER_ONE, BANNER_TWO)));

        assertThat(gdAdWithTotals.getGdAds()).isEmpty();
    }

    @Test
    public void testGetBanners_WhenFetchedCampaignsIsEmpty() {
        doReturn(new GdiBannersWithTotals()
                .withGdiBanners(ImmutableList.of(generateTestBanner(BANNER_ONE))))
                .when(gridBannerService)
                .getBanners(eq(clientInfo.getShard()), eq(operatorUid), eq(clientId),
                        anySet(), any(), anyList(), eq(TEST_FROM), eq(TEST_TO), any(), eq(emptySet()),
                        eq(TEST_FROM), eq(TEST_TO), any(), eq(false));
        doReturn(emptyMap())
                .when(campaignInfoService)
                .getTruncatedCampaigns(ClientId.fromLong(clientInfo.getId()), singleton(CAMPAIGN));

        GdAdWithTotals gdAdWithTotals = bannerDataService.getBanners(clientInfo, adsContainer, gridGraphQLContext);
        assertThat(gdAdWithTotals.getGdAds()).isEmpty();

        verify(campaignInfoService)
                .getTruncatedCampaigns(ClientId.fromLong(clientInfo.getId()), singleton(CAMPAIGN));
        verifyZeroInteractions(groupDataService);
    }

    @Test
    public void testGetTouchBanners_WhenClientDontHaveTouchCampaigns() {
        when(campaignInfoService.getTouchCampaignIds(
                eq(clientInfo.getShard()), eq(clientId)
        )).thenReturn(Set.of());

        var container = getDefaultGdAdsContainer().withFilter(defaultTouchAdsFilter());
        GdAdWithTotals gdAdWithTotals = bannerDataService.getBanners(clientInfo, container, gridGraphQLContext);
        assertThat(gdAdWithTotals.getGdAds()).isEmpty();
    }

    @Test
    public void testGetTouchBanners_WhenClientHasTouchCampaigns() {
        var touchCampaignIds = Set.of(CAMPAIGN);
        when(campaignInfoService.getTouchCampaignIds(
                eq(clientInfo.getShard()), eq(clientId)
        )).thenReturn(touchCampaignIds);

        when(gridBannerService.getBanners(
                eq(clientInfo.getShard()), eq(operatorUid), eq(clientId), anySet(), any(), any(), any(),
                any(), any(), any(), any(), any(), any(), anyBoolean())
        ).thenReturn(
                new GdiBannersWithTotals().withGdiBanners(emptyList())
        );

        var container = getDefaultGdAdsContainer().withFilter(defaultTouchAdsFilter());
        bannerDataService.getBanners(clientInfo, container, gridGraphQLContext);

        verify(gridBannerService).getBanners(
                eq(clientInfo.getShard()),
                eq(operatorUid), eq(clientId), anySet(),
                argThat(filter -> filter.getCampaignIdIn().equals(touchCampaignIds)),
                any(), any(), any(), any(), any(), any(), any(), any(), anyBoolean()
        );
    }

    @Test
    public void testGetTouchBanners_FullTest() {
        var touchCampaignIds = Set.of(CAMPAIGN);
        var shard = clientInfo.getShard();
        var clientId = ClientId.fromLong(clientInfo.getId());

        when(campaignInfoService.getTouchCampaignIds(eq(shard), eq(clientId))).thenReturn(touchCampaignIds);

        when(gridBannerService.getBanners(
                eq(shard), eq(operatorUid), eq(clientId), anySet(), any(), any(), any(),
                any(), any(), any(), any(), any(), any(), anyBoolean())
        ).thenReturn(
                new GdiBannersWithTotals().withGdiBanners(ImmutableList.of(
                        generateTouchBanner(42L, canvasCreative().withIsAdaptive(false)),
                        generateTouchBanner(BANNER_ONE, canvasCreative().withIsAdaptive(true))))
        );

        when(groupDataService.getTruncatedAdGroups(
                eq(shard), eq(clientInfo.getCountryRegionId()), eq(clientId),
                eq(gridGraphQLContext.getOperator()), any(), any()
        )).thenReturn(singletonList(GROUP_OBJ));

        var container = getDefaultGdAdsContainer().withFilter(
                defaultTouchAdsFilter().withTypeIn(Set.of(GdAdType.IMAGE_AD)).withArchived(false)
        );
        GdAdWithTotals gdAdWithTotals = bannerDataService.getBanners(clientInfo, container, gridGraphQLContext);
        List<GdAd> banners = gdAdWithTotals.getGdAds();

        GdAd expected = new GdImageAd().withId(BANNER_ONE)
                .withTypedCreative(new GdCanvasCreative().withIsAdaptive(true));
        Assert.assertThat(banners, contains(beanDiffer(expected).useCompareStrategy(onlyExpectedFields())));
    }

    @Test
    public void testGetBanners_WhenFetchedAdGroupsIsEmpty() {
        doReturn(new GdiBannersWithTotals().withGdiBanners(ImmutableList.of(generateTestBanner(BANNER_ONE))))
                .when(gridBannerService)
                .getBanners(eq(clientInfo.getShard()), eq(operatorUid), eq(clientId),
                        anySet(), any(), anyList(), eq(TEST_FROM), eq(TEST_TO), any(), eq(emptySet()),
                        eq(TEST_FROM), eq(TEST_TO), any(), eq(false));

        GdAdWithTotals gdAdWithTotals = bannerDataService.getBanners(clientInfo, adsContainer, gridGraphQLContext);
        assertThat(gdAdWithTotals.getGdAds()).isEmpty();

        verify(groupDataService)
                .getTruncatedAdGroups(eq(clientInfo.getShard()),
                        eq(clientInfo.getCountryRegionId()), eq(clientId),
                        eq(gridGraphQLContext.getOperator()), any(), any());
        verify(gridBannerService, never())
                .getMonitoringStoppedBanners(eq(clientInfo.getShard()), eq(ImmutableSet.of(BANNER_ONE, BANNER_TWO)));
    }

    @Test
    public void testGetBanners_WhenFetchedAdGroupsDontContainProperGroup() {
        GdTextAdGroup wrongGroup = new GdTextAdGroup().withId(111L)
                .withCampaign(new GdTextCampaign().withId(111L).withStatus(defaultGdCampaignStatus()));

        doReturn(new GdiBannersWithTotals().withGdiBanners(ImmutableList.of(generateTestBanner(BANNER_ONE))))
                .when(gridBannerService)
                .getBanners(eq(clientInfo.getShard()), eq(operatorUid), eq(clientId),
                        anySet(), any(), anyList(), eq(TEST_FROM), eq(TEST_TO), any(), eq(emptySet()),
                        eq(TEST_FROM), eq(TEST_TO), any(), eq(false));
        doReturn(Collections.singletonList(wrongGroup))
                .when(groupDataService)
                .getTruncatedAdGroups(eq(clientInfo.getShard()),
                        eq(clientInfo.getCountryRegionId()), eq(clientId),
                        eq(gridGraphQLContext.getOperator()), any(), any());

        GdAdWithTotals gdAdWithTotals = bannerDataService.getBanners(clientInfo, adsContainer, gridGraphQLContext);
        assertThat(gdAdWithTotals.getGdAds()).isEmpty();
    }

    @Test
    public void addAds_success_onEmptyItemList() {
        ClientId clientId = ClientId.fromLong(clientInfo.getId());
        User operator = gridGraphQLContext.getOperator();
        GdAddAds input = new GdAddAds()
                .withAdAddItems(emptyList())
                .withSaveDraft(false);

        GdAddAdsPayload result = bannerDataService.addAds(clientId, operator, input);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(result.getValidationResult())
                    .as("validationResult")
                    .isNull();
            softly.assertThat(result.getAddedAds())
                    .as("addedAdIds")
                    .isNotNull()
                    .isEmpty();
        });
    }

    @Test
    public void addSmartAds_success_onEmptyItemList() {
        ClientId clientId = ClientId.fromLong(clientInfo.getId());
        User operator = gridGraphQLContext.getOperator();
        GdAddSmartAds input = new GdAddSmartAds()
                .withAdAddItems(emptyList())
                .withSaveDraft(false);

        GdAddAdsPayload result = bannerDataService.addSmartAds(clientId, operator, input);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(result.getValidationResult())
                    .as("validationResult")
                    .isNull();
            softly.assertThat(result.getAddedAds())
                    .as("addedAdIds")
                    .isNotNull()
                    .isEmpty();
        });
    }

    @Test
    public void updateAds_success_onEmptyItemList() {
        ClientId clientId = ClientId.fromLong(clientInfo.getId());
        User operator = gridGraphQLContext.getOperator();
        GdUpdateAds input = new GdUpdateAds()
                .withAdUpdateItems(emptyList())
                .withSaveDraft(false);

        GdUpdateAdsPayload result = bannerDataService.updateAds(clientId, operator, input);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(result.getValidationResult())
                    .as("validationResult")
                    .isNull();
            softly.assertThat(result.getUpdatedAds())
                    .as("updatedAdIds")
                    .isNotNull()
                    .isEmpty();
        });
    }

    @Test
    public void updateSmartAds_success_onEmptyItemList() {
        ClientId clientId = ClientId.fromLong(clientInfo.getId());
        User operator = gridGraphQLContext.getOperator();
        GdUpdateSmartAds input = new GdUpdateSmartAds()
                .withAdUpdateItems(emptyList())
                .withSaveDraft(false);

        GdUpdateAdsPayload result = bannerDataService.updateSmartAds(clientId, operator, input);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(result.getValidationResult())
                    .as("validationResult")
                    .isNull();
            softly.assertThat(result.getUpdatedAds())
                    .as("updatedAdIds")
                    .isNotNull()
                    .isEmpty();
        });
    }

    @Test
    public void checkTurboGalleryHrefs_success() {
        String originalUrl = "https://testing.url.ru";
        String turboUrl = "https://yandex.ru/turbo?text=listing" + originalUrl;
        Map<String, String> clientResponse = new HashMap<>();
        clientResponse.put(originalUrl, turboUrl);
        doReturn(clientResponse).when(turbopagesClient).checkUrls(anyList());
        doNothing().when(adValidationService).validateHref(anyString());

        String result = bannerDataService.checkTurboGalleryHref(originalUrl);

        verify(adValidationService).validateHref(eq(originalUrl));
        assertThat(result).isEqualTo(turboUrl);
    }

    @Test
    public void checkTurboGalleryHrefs_emptyResult() {
        String originalUrl = "https://testing.url.ru";
        Map<String, String> clientResponse = new HashMap<>();
        doReturn(clientResponse).when(turbopagesClient).checkUrls(anyList());
        doNothing().when(adValidationService).validateHref(anyString());

        String result = bannerDataService.checkTurboGalleryHref(originalUrl);

        verify(adValidationService).validateHref(eq(originalUrl));
        assertThat(result).isEqualTo("");
    }

    private static GdiBanner generateTestBanner(Long bannerId) {
        return new GdiBanner()
                .withCampaignId(CAMPAIGN)
                .withId(bannerId)
                .withGroupId(GROUP)
                .withBannerType(BannersBannerType.text)
                .withStatusShow(true)
                .withStatusActive(false)
                .withStatusArchived(false)
                .withStatusModerate(GdiBannerStatusModerate.YES)
                .withStatusPostModerate(GdiBannerStatusPostModerate.YES)
                .withStatusBsSynced(GdiBannerStatusBsSynced.NO)
                .withStatusMonitoringDomainStop(true)
                .withStat(GridStatNew.addZeros(new GdiEntityStats()))
                .withBannerPrice(defaultNewBannerPrice());
    }

    private static GdiBanner generateTouchBanner(Long bannerId, Creative creative) {
        return generateTestBanner(bannerId).withBannerType(BannersBannerType.image_ad).withTypedCreative(creative);
    }

    private static Creative canvasCreative() {
        return new Creative().withType(CreativeType.CANVAS)
                .withLivePreviewUrl("https://canvas.yandex.ru/creatives/1234567890abcdef/12345/preview");
    }

    private static GdAdFilter defaultTouchAdsFilter() {
        return new GdAdFilter().withCampaignIdIn(Set.of()).withIsTouch(true);
    }

}

