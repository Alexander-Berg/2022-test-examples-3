package ru.yandex.direct.grid.core.entity.group.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import one.util.streamex.StreamEx;
import org.apache.commons.lang3.RandomStringUtils;
import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.CompareStrategy;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.direct.core.entity.adgroup.container.AdGroupsSelectionCriteria;
import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.model.AdGroupStates;
import ru.yandex.direct.core.entity.adgroup.model.AdGroupType;
import ru.yandex.direct.core.entity.adgroup.model.ContentPromotionVideoAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.PerformanceAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.StatusModerate;
import ru.yandex.direct.core.entity.adgroup.model.StatusPostModerate;
import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository;
import ru.yandex.direct.core.entity.adgroup.service.AdGroupService;
import ru.yandex.direct.core.entity.adgroup.service.geotree.AdGroupGeoTreeProvider;
import ru.yandex.direct.core.entity.adgroup.service.geotree.AdGroupGeoTreeProviderFactory;
import ru.yandex.direct.core.entity.banner.model.PerformanceBannerMain;
import ru.yandex.direct.core.entity.banner.repository.BannerTypedRepository;
import ru.yandex.direct.core.entity.banner.service.BannerService;
import ru.yandex.direct.core.entity.bidmodifier.container.MultipliersBounds;
import ru.yandex.direct.core.entity.bidmodifiers.service.BidModifierService;
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository;
import ru.yandex.direct.core.entity.client.service.ClientGeoService;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.entity.minuskeywordspack.repository.MinusKeywordsPackRepository;
import ru.yandex.direct.core.entity.relevancematch.repository.RelevanceMatchRepository;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.grid.core.entity.banner.model.GdiBannerLogo;
import ru.yandex.direct.grid.core.entity.banner.repository.GridImageRepository;
import ru.yandex.direct.grid.core.entity.fetchedfieldresolver.AdGroupFetchedFieldsResolver;
import ru.yandex.direct.grid.core.entity.group.container.GdiAdGroupRegionsInfo;
import ru.yandex.direct.grid.core.entity.group.container.GdiGroupRelevanceMatch;
import ru.yandex.direct.grid.core.entity.group.container.GdiRelevanceMatchCategory;
import ru.yandex.direct.grid.core.entity.group.model.GdiBaseGroup;
import ru.yandex.direct.grid.core.entity.group.model.GdiDynamicGroup;
import ru.yandex.direct.grid.core.entity.group.model.GdiGroup;
import ru.yandex.direct.grid.core.entity.group.model.GdiGroupBlGenerationStatus;
import ru.yandex.direct.grid.core.entity.group.model.GdiGroupFilter;
import ru.yandex.direct.grid.core.entity.group.model.GdiGroupModerationStatus;
import ru.yandex.direct.grid.core.entity.group.model.GdiGroupStatus;
import ru.yandex.direct.grid.core.entity.group.model.GdiGroupsWithTotals;
import ru.yandex.direct.grid.core.entity.group.model.GdiMobileContentGroup;
import ru.yandex.direct.grid.core.entity.group.model.GdiPerformanceGroup;
import ru.yandex.direct.grid.core.entity.group.repository.GridAdGroupYtRepository;
import ru.yandex.direct.grid.core.entity.recommendation.model.GdiRecommendation;
import ru.yandex.direct.grid.core.entity.recommendation.service.GridRecommendationService;
import ru.yandex.direct.grid.core.util.FetchedFieldsResolverCoreUtil;
import ru.yandex.direct.grid.model.GdStatRequirements;
import ru.yandex.direct.multitype.entity.LimitOffset;
import ru.yandex.direct.regions.GeoTree;
import ru.yandex.direct.regions.GeoTreeFactory;
import ru.yandex.direct.regions.Region;
import ru.yandex.direct.test.utils.RandomNumberUtils;

import static com.google.common.primitives.Longs.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.direct.core.testing.data.TestGroups.defaultPerformanceAdGroup;
import static ru.yandex.direct.grid.core.util.adgroup.AdGroupUtils.getTestAdGroupState;
import static ru.yandex.direct.grid.core.util.adgroup.AdGroupUtils.toCoreAdGroup;
import static ru.yandex.direct.grid.model.entity.recommendation.GdiRecommendationType.dailyBudget;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.utils.CommonUtils.ifNotNull;
import static ru.yandex.direct.utils.CommonUtils.nvl;
import static ru.yandex.direct.utils.FunctionalUtils.listToSet;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

public class GridAdGroupServiceTest {

    private static final int TEST_SHARD = 3;
    private static final long CAMPAIGN_ID = RandomNumberUtils.nextPositiveLong();
    private static final Set<Long> TEST_CAMPAIGNS_IDS = ImmutableSet.of(CAMPAIGN_ID);
    private static final LocalDate TEST_FROM = LocalDate.now();
    private static final LocalDate TEST_TO = TEST_FROM.plusDays(10);
    private static final GdStatRequirements STAT_REQUIREMENTS = new GdStatRequirements()
            .withFrom(TEST_FROM)
            .withTo(TEST_TO);

    private static final long DYNAMIC_GROUP_ID = RandomNumberUtils.nextPositiveLong();
    private static final long MOBILE_CONTENT_GROUP_ID = DYNAMIC_GROUP_ID + 1;
    private static final long BASE_GROUP_ID = MOBILE_CONTENT_GROUP_ID + 1;
    private static final long SMART_GROUP_ID = BASE_GROUP_ID + 1;
    private static final Set<Long> GROUP_IDS =
            ImmutableSet.of(DYNAMIC_GROUP_ID, MOBILE_CONTENT_GROUP_ID, BASE_GROUP_ID, SMART_GROUP_ID);
    private static final GdiGroupRelevanceMatch DYNAMIC_RELEVANCE_MATCH = new GdiGroupRelevanceMatch()
            .withId(null)
            .withIsActive(true)
            .withRelevanceMatchCategories(
                    EnumSet.of(GdiRelevanceMatchCategory.EXACT_MARK, GdiRelevanceMatchCategory.BROADER_MARK));
    private static final String DYNAMIC_MAIN_DOMAIN = RandomStringUtils.randomAlphabetic(7);
    private static final String MOBILE_STORE_CONTENT_HREF = RandomStringUtils.randomAlphabetic(7);
    private static final GdiBannerLogo PERFORMANCE_LOGO = new GdiBannerLogo()
            .withImageHash("logoImageHash");
    private static final PerformanceBannerMain PERFORMANCE_MAIN_BANNER = new PerformanceBannerMain()
            .withId(RandomNumberUtils.nextPositiveLong())
            .withAdGroupId(SMART_GROUP_ID)
            .withLogoImageHash(PERFORMANCE_LOGO.getImageHash());
    private static final ClientId CLIENT_ID = ClientId.fromLong(1);
    private static final long CLIENT_COUNTRY_REGION_ID = Region.RUSSIA_REGION_ID;
    private static final long UID = 1L;
    private static final long FEED_ID = 1L;
    private static final AdGroupFetchedFieldsResolver AD_GROUP_FETCHED_FIELDS_RESOLVER =
            FetchedFieldsResolverCoreUtil.buildAdGroupFetchedFieldsResolver(true);

    private List<GdiGroup> intGroups;
    private List<AdGroup> coreAdGroups;

    @Mock
    private GridAdGroupYtRepository gridAdGroupYtRepository;

    @Mock
    private AdGroupRepository adGroupRepository;

    @Mock
    private AdGroupService adGroupService;

    @Mock
    private ClientGeoService clientGeoService;

    @Mock
    private GeoTreeFactory geoTreeFactory;

    @Mock
    private AdGroupGeoTreeProvider geoTreeProvider;

    @Mock
    private AdGroupGeoTreeProviderFactory geoTreeProviderFactory;

    @Mock
    private BidModifierService bidModifierService;

    @Mock
    private GridRecommendationService gridRecommendationService;

    @Mock
    private GridImageRepository gridImageRepository;

    @Mock
    private BannerTypedRepository bannerTypedRepository;

    @Mock
    private BannerService bannerService;

    @Mock
    private MinusKeywordsPackRepository minusKeywordsPackRepository;

    @Mock
    private CampaignRepository campaignRepository;

    @Mock
    private FeatureService featureService;

    @Mock
    private RelevanceMatchRepository relevanceMatchRepository;

    @InjectMocks
    private GridAdGroupService gridAdGroupService;

    @Captor
    private ArgumentCaptor<AdGroupsSelectionCriteria> criteriaArgumentCaptor;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);

        intGroups = Arrays.asList(
                new GdiDynamicGroup()
                        .withId(DYNAMIC_GROUP_ID)
                        .withCampaignId(CAMPAIGN_ID)
                        .withType(AdGroupType.DYNAMIC)
                        .withRelevanceMatch(DYNAMIC_RELEVANCE_MATCH)
                        .withMainDomain(DYNAMIC_MAIN_DOMAIN)
                        .withStatus(
                                new GdiGroupStatus().withBlGenerationStatus(GdiGroupBlGenerationStatus.NO)),
                new GdiMobileContentGroup()
                        .withId(MOBILE_CONTENT_GROUP_ID)
                        .withCampaignId(CAMPAIGN_ID)
                        .withType(AdGroupType.MOBILE_CONTENT)
                        .withStoreHref(MOBILE_STORE_CONTENT_HREF)
                        .withStatus(
                                new GdiGroupStatus().withBlGenerationStatus(GdiGroupBlGenerationStatus.INAPPLICABLE)),
                new GdiBaseGroup()
                        .withId(BASE_GROUP_ID)
                        .withCampaignId(CAMPAIGN_ID)
                        .withType(AdGroupType.BASE)
                        .withStatus(
                                new GdiGroupStatus().withBlGenerationStatus(GdiGroupBlGenerationStatus.INAPPLICABLE))
                        .withCollectAudience(false),
                new GdiPerformanceGroup()
                        .withId(SMART_GROUP_ID)
                        .withCampaignId(CAMPAIGN_ID)
                        .withType(AdGroupType.PERFORMANCE)
                        .withFeedId(FEED_ID)
                        .withStatus(
                                new GdiGroupStatus().withBlGenerationStatus(GdiGroupBlGenerationStatus.PROCESSING))
        );

        doReturn(new GdiGroupsWithTotals().withGdiGroups(intGroups))
                .when(gridAdGroupYtRepository)
                .getGroups(eq(TEST_SHARD), any(), any(), eq(TEST_FROM), eq(TEST_TO), any(), eq(emptySet()), isNull(),
                        any(), anyBoolean());

        MultipliersBounds multipliersBounds = new MultipliersBounds().withLower(100).withUpper(110);
        ImmutableMap<Object, Object> multipliersBoundsForAdGroups = ImmutableMap.builder()
                .put(DYNAMIC_GROUP_ID, multipliersBounds)
                .put(MOBILE_CONTENT_GROUP_ID, multipliersBounds).build();

        doReturn(multipliersBoundsForAdGroups)
                .when(bidModifierService)
                .calculateMultipliersBoundsForAdGroups(eq(CLIENT_ID), eq(UID), any());

        coreAdGroups = mapList(intGroups, intGroup -> toCoreAdGroup(intGroup, CLIENT_COUNTRY_REGION_ID)
                .withGeo(asList(CLIENT_COUNTRY_REGION_ID, Region.MOSCOW_REGION_ID)));
        doReturn(coreAdGroups)
                .when(adGroupService)
                .getAdGroupsBySelectionCriteria(eq(TEST_SHARD), any(), any(), anyBoolean());

        Map<Long, AdGroupStates> adGroupStates = ImmutableMap.of(
                DYNAMIC_GROUP_ID, getTestAdGroupState(true, false, false, false),
                MOBILE_CONTENT_GROUP_ID, getTestAdGroupState(false, true, false, false),
                BASE_GROUP_ID, getTestAdGroupState(true, false, false, false),
                SMART_GROUP_ID, getTestAdGroupState(false, false, false, false));
        doReturn(adGroupStates)
                .when(adGroupRepository)
                .getAdGroupStatuses(anyInt(), anyCollection());

        doReturn(List.of(PERFORMANCE_MAIN_BANNER))
                .when(bannerTypedRepository)
                .getBannersByGroupIds(eq(TEST_SHARD), any(), eq(CLIENT_ID), eq(PerformanceBannerMain.class));

        doReturn(List.of(PERFORMANCE_LOGO))
                .when(gridImageRepository)
                .getBannerLogosByHash(eq(TEST_SHARD), eq(CLIENT_ID), any());

        doAnswer(invocation -> invocation.getArgument(0))
                .when(clientGeoService)
                .convertForWeb(any(), any());

        doReturn(geoTreeProvider)
                .when(geoTreeProviderFactory)
                .create(any(), any());
    }


    @Test
    public void checkGetGroups() {
        GdiGroupFilter filter = new GdiGroupFilter()
                .withCampaignIdIn(TEST_CAMPAIGNS_IDS);
        gridAdGroupService.getGroups(TEST_SHARD, CLIENT_COUNTRY_REGION_ID, CLIENT_ID, new User().withUid(UID),
                filter, emptySet(), emptyList(), STAT_REQUIREMENTS, false, emptySet(),
                AD_GROUP_FETCHED_FIELDS_RESOLVER);

        verify(gridAdGroupYtRepository)
                .getGroups(eq(TEST_SHARD), eq(filter), any(), eq(TEST_FROM), eq(TEST_TO), any(), eq(emptySet()), isNull(),
                        any(), anyBoolean());
        verify(adGroupRepository).getAdGroupStatuses(TEST_SHARD, GROUP_IDS);

        verify(geoTreeFactory).getTranslocalGeoTree(CLIENT_COUNTRY_REGION_ID);
        verify(clientGeoService, times(8)).convertForWeb(any(), any());
        verify(adGroupService).getAdGroupsBySelectionCriteria(eq(TEST_SHARD), any(AdGroupsSelectionCriteria.class),
                eq(LimitOffset.limited(GridAdGroupConstants.getMaxGroupRows())), eq(true));

        verify(bidModifierService).calculateMultipliersBoundsForAdGroups(eq(CLIENT_ID), eq(UID), any());
    }

    @Test
    public void checkGetGroups_UnsupportedType() {
        GdiBaseGroup wrongGroup = new GdiBaseGroup()
                .withId(111L)
                .withCampaignId(CAMPAIGN_ID)
                .withType(AdGroupType.CONTENT_PROMOTION_VIDEO)
                .withStatus(new GdiGroupStatus().withBlGenerationStatus(GdiGroupBlGenerationStatus.INAPPLICABLE));

        AdGroup wrongCoreGroup = new ContentPromotionVideoAdGroup()
                .withId(wrongGroup.getId())
                .withCampaignId(wrongGroup.getCampaignId())
                .withType(wrongGroup.getType())
                .withGeo(asList(CLIENT_COUNTRY_REGION_ID))
                .withEffectiveGeo(asList(CLIENT_COUNTRY_REGION_ID));

        List<GdiGroup> intGroups1 = new ArrayList<>(intGroups);
        intGroups1.add(wrongGroup);
        doReturn(new GdiGroupsWithTotals().withGdiGroups(intGroups1))
                .when(gridAdGroupYtRepository)
                .getGroups(eq(TEST_SHARD), any(), any(), eq(TEST_FROM), eq(TEST_TO), any(), eq(emptySet()), isNull(),
                        any(), anyBoolean());

        List<AdGroup> coreAdGroups1 = new ArrayList<>(coreAdGroups);
        coreAdGroups1.add(wrongCoreGroup);
        doReturn(coreAdGroups1)
                .when(adGroupService)
                .getAdGroupsBySelectionCriteria(eq(TEST_SHARD), any(), any(), anyBoolean());

        GdiGroupFilter filter = new GdiGroupFilter()
                .withCampaignIdIn(TEST_CAMPAIGNS_IDS);

        GdiGroupsWithTotals gdiGroupsWithTotals = gridAdGroupService
                .getGroups(TEST_SHARD, CLIENT_COUNTRY_REGION_ID, CLIENT_ID, new User().withUid(UID),
                        filter, emptySet(), emptyList(), STAT_REQUIREMENTS, false, emptySet(),
                        AD_GROUP_FETCHED_FIELDS_RESOLVER);
        List<GdiGroup> groups = gdiGroupsWithTotals.getGdiGroups();

        assertEquals(listToSet(intGroups, GdiGroup::getId), listToSet(groups, GdiGroup::getId));
    }

    @Test
    public void checkGetNotArchivedGroups() {
        GdiGroupFilter filter = new GdiGroupFilter()
                .withCampaignIdIn(TEST_CAMPAIGNS_IDS)
                .withArchived(false);
        GdiGroupsWithTotals gdiGroupsWithTotals = gridAdGroupService
                .getGroups(TEST_SHARD, CLIENT_COUNTRY_REGION_ID, CLIENT_ID, new User().withUid(UID), filter,
                        emptySet(), emptyList(), STAT_REQUIREMENTS, false, emptySet(),
                        AD_GROUP_FETCHED_FIELDS_RESOLVER);
        List<GdiGroup> groups = gdiGroupsWithTotals.getGdiGroups();

        assertThat(mapList(groups, GdiGroup::getId))
                .containsExactly(MOBILE_CONTENT_GROUP_ID, SMART_GROUP_ID);
    }

    @Test
    public void checkGetArchivedGroups() {
        GdiGroupFilter filter = new GdiGroupFilter()
                .withCampaignIdIn(TEST_CAMPAIGNS_IDS)
                .withArchived(true);

        GdiGroupsWithTotals gdiGroupsWithTotals = gridAdGroupService
                .getGroups(TEST_SHARD, CLIENT_COUNTRY_REGION_ID, CLIENT_ID, new User().withUid(UID), filter,
                        emptySet(), emptyList(), STAT_REQUIREMENTS, false, emptySet(),
                        AD_GROUP_FETCHED_FIELDS_RESOLVER);
        List<GdiGroup> groups = gdiGroupsWithTotals.getGdiGroups();

        assertThat(mapList(groups, GdiGroup::getId))
                .containsExactly(DYNAMIC_GROUP_ID, BASE_GROUP_ID);
    }

    @Test
    public void checkGetGroups_WhenFetchedEmptyAdGroupsFromMysql() {
        GdiGroupFilter filter = new GdiGroupFilter()
                .withCampaignIdIn(TEST_CAMPAIGNS_IDS)
                .withArchived(true);
        doReturn(emptyList())
                .when(adGroupService)
                .getAdGroupsBySelectionCriteria(eq(TEST_SHARD), any(), any(), anyBoolean());

        GdiGroupsWithTotals gdiGroupsWithTotals = gridAdGroupService
                .getGroups(TEST_SHARD, CLIENT_COUNTRY_REGION_ID, CLIENT_ID, new User().withUid(UID), filter,
                        emptySet(), emptyList(), STAT_REQUIREMENTS, false, emptySet(),
                        AD_GROUP_FETCHED_FIELDS_RESOLVER);
        List<GdiGroup> groups = gdiGroupsWithTotals.getGdiGroups();

        assertThat(groups)
                .isEmpty();
    }

    @Test
    public void checkGetGroups_WithRecommendationsFilter() {
        Map<Long, List<GdiRecommendation>> groupIdToRecommendations =
                ImmutableMap.<Long, List<GdiRecommendation>>builder()
                        .put(DYNAMIC_GROUP_ID, singletonList(new GdiRecommendation()
                                .withCid(CAMPAIGN_ID)
                                .withPid(DYNAMIC_GROUP_ID)))
                        .put(MOBILE_CONTENT_GROUP_ID, singletonList(new GdiRecommendation()
                                .withCid(CAMPAIGN_ID)
                                .withPid(MOBILE_CONTENT_GROUP_ID)))
                        .build();
        doReturn(groupIdToRecommendations)
                .when(gridRecommendationService)
                .getGroupRecommendations(eq(CLIENT_ID), any(), any(), any());

        GdiGroupFilter filter = new GdiGroupFilter()
                .withCampaignIdIn(TEST_CAMPAIGNS_IDS)
                .withGroupIdIn(singleton(DYNAMIC_GROUP_ID))
                .withGroupIdNotIn(singleton(MOBILE_CONTENT_GROUP_ID));

        gridAdGroupService.getGroups(TEST_SHARD, CLIENT_COUNTRY_REGION_ID, CLIENT_ID, new User().withUid(UID),
                filter, singleton(dailyBudget), emptyList(), STAT_REQUIREMENTS, false, emptySet(),
                AD_GROUP_FETCHED_FIELDS_RESOLVER);

        GdiGroupFilter expectedFilter = new GdiGroupFilter()
                .withRecommendations(groupIdToRecommendations.get(DYNAMIC_GROUP_ID))
                .withCampaignIdIn(null)
                .withGroupIdIn(null)
                .withGroupIdNotIn(null);

        verify(gridAdGroupYtRepository).getGroups(eq(TEST_SHARD), eq(expectedFilter), any(), eq(TEST_FROM), eq(TEST_TO),
                any(), eq(emptySet()), isNull(), any(), anyBoolean());
    }

    @Test
    public void checkAddSpecificTypeParams() {
        GdiGroupFilter filter = new GdiGroupFilter()
                .withCampaignIdIn(TEST_CAMPAIGNS_IDS);
        GdiGroupsWithTotals gdiGroupsWithTotals = gridAdGroupService
                .getGroups(TEST_SHARD, CLIENT_COUNTRY_REGION_ID, CLIENT_ID, new User().withUid(UID), filter,
                        emptySet(), emptyList(), STAT_REQUIREMENTS, false, emptySet(),
                        AD_GROUP_FETCHED_FIELDS_RESOLVER);
        List<GdiGroup> groups = gdiGroupsWithTotals.getGdiGroups();

        Set<String> groupSpecificTypeParams = ImmutableSet.of(
                Objects.toString(groups.get(0).getRelevanceMatch().getId()),
                Objects.toString(groups.get(0).getRelevanceMatch().getIsActive()),
                StreamEx.of(nvl(groups.get(0).getRelevanceMatch().getRelevanceMatchCategories(), Set.of()))
                        .sorted().toList().toString(),
                ((GdiDynamicGroup) groups.get(0)).getMainDomain(),
                groups.get(0).getStatus().getBlGenerationStatus().name(),
                ((GdiMobileContentGroup) groups.get(1)).getStoreHref(),
                ifNotNull(((GdiPerformanceGroup) groups.get(3)).getLogo(), GdiBannerLogo::getImageHash),
                groups.get(3).getStatus().getBlGenerationStatus().name()
        );

        Set<String> expectedGroupSpecificTypeParams = ImmutableSet.of(
                Objects.toString(DYNAMIC_RELEVANCE_MATCH.getId()),
                Objects.toString(DYNAMIC_RELEVANCE_MATCH.getIsActive()),
                StreamEx.of(nvl(DYNAMIC_RELEVANCE_MATCH.getRelevanceMatchCategories(), Set.of()))
                    .sorted().toList().toString(),
                DYNAMIC_MAIN_DOMAIN,
                GdiGroupBlGenerationStatus.NO.name(),
                MOBILE_STORE_CONTENT_HREF,
                PERFORMANCE_MAIN_BANNER.getLogoImageHash(),
                GdiGroupBlGenerationStatus.PROCESSING.name()
        );

        assertThat(groupSpecificTypeParams)
                .is(matchedBy(beanDiffer(expectedGroupSpecificTypeParams)));
    }

    @Test
    public void checkGetRegionsInfo() {
        AdGroup adGroup = coreAdGroups.get(0);
        GeoTree translocalGeoTree = geoTreeFactory.getTranslocalGeoTree(CLIENT_COUNTRY_REGION_ID);
        AdGroupGeoTreeProvider geoTreeProvider = geoTreeProviderFactory.create(translocalGeoTree, emptyMap());
        GdiAdGroupRegionsInfo regionsInfo = gridAdGroupService.getRegionsInfo(adGroup, geoTreeProvider);

        verify(clientGeoService).convertForWeb(eq(adGroup.getGeo()), eq(translocalGeoTree));
        verify(clientGeoService).convertForWeb(eq(adGroup.getEffectiveGeo()), eq(translocalGeoTree));

        GdiAdGroupRegionsInfo expected = new GdiAdGroupRegionsInfo()
                .withRegionIds(adGroup.getGeo())
                .withEffectiveRegionIds(adGroup.getEffectiveGeo())
                .withRestrictedRegionIds(adGroup.getRestrictedGeo());
        assertThat(regionsInfo)
                .is(matchedBy(beanDiffer(expected)));
    }

    @Test
    public void checkGetTruncatedAdGroupsFromMysql() {
        List<GdiGroup> truncatedAdGroups = gridAdGroupService
                .getTruncatedAdGroupsFromMysql(TEST_SHARD, CLIENT_COUNTRY_REGION_ID, GROUP_IDS, Map.of());

        verify(geoTreeFactory).getTranslocalGeoTree(CLIENT_COUNTRY_REGION_ID);
        verify(adGroupService).getAdGroupsBySelectionCriteria(eq(TEST_SHARD), criteriaArgumentCaptor.capture(),
                eq(LimitOffset.limited(GridAdGroupConstants.getMaxGroupRows())), eq(true));
        verify(adGroupRepository).getAdGroupStatuses(TEST_SHARD, GROUP_IDS);

        AdGroupsSelectionCriteria expectedSelectionCriteria = new AdGroupsSelectionCriteria()
                .withAdGroupIds(ImmutableSet.copyOf(GROUP_IDS));
        assertThat(criteriaArgumentCaptor.getValue())
                .is(matchedBy(beanDiffer(expectedSelectionCriteria)));

        CompareStrategy compareStrategy = DefaultCompareStrategies.allFields()
                .forFields(newPath("\\d+", GdiGroup.REGIONS_INFO.name()), newPath("\\d+", GdiGroup.STATUS.name()))
                .useMatcher(notNullValue());
        assertThat(truncatedAdGroups)
                .is(matchedBy(beanDiffer(intGroups).useCompareStrategy(compareStrategy)));
    }

    @Test
    public void checkGetTruncatedAdGroupsFromMysql_moderationResultNotEmpty() {
        PerformanceAdGroup adGroup = defaultPerformanceAdGroup(CAMPAIGN_ID, FEED_ID);
        coreAdGroups = singletonList(adGroup);
        doReturn(coreAdGroups)
                .when(adGroupService)
                .getAdGroupsBySelectionCriteria(eq(TEST_SHARD), any(), any(), anyBoolean());

        GdiGroup gdiGroup = gridAdGroupService.getTruncatedAdGroupsFromMysql(TEST_SHARD, CLIENT_COUNTRY_REGION_ID,
                GROUP_IDS, Map.of()).get(0);

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(gdiGroup.getStatusModerate()).as("statusModerate")
                    .isEqualTo(StatusModerate.YES);
            soft.assertThat(gdiGroup.getStatusPostModerate()).as("statusPostModerate")
                    .isEqualTo(StatusPostModerate.YES);
            soft.assertThat(gdiGroup.getStatus().getModerationStatus()).as("status.gdiGroupModerationStatus")
                    .isEqualTo(GdiGroupModerationStatus.ACCEPTED);
        });
    }

}
