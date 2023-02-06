package ru.yandex.direct.grid.processing.service.banner;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.common.db.PpcProperty;
import ru.yandex.direct.core.aggregatedstatuses.AggregatedStatusesViewService;
import ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusEnum;
import ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusReason;
import ru.yandex.direct.core.entity.aggregatedstatuses.ad.AdStatesEnum;
import ru.yandex.direct.core.entity.aggregatedstatuses.ad.AggregatedStatusAdData;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.entity.moderationreason.service.ModerationReasonService;
import ru.yandex.direct.core.entity.organizations.service.OrganizationService;
import ru.yandex.direct.dbschema.ppc.enums.BannersBannerType;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.grid.core.entity.banner.model.GdiBanner;
import ru.yandex.direct.grid.core.entity.banner.model.GdiBannerFilter;
import ru.yandex.direct.grid.core.entity.banner.model.GdiBannersWithTotals;
import ru.yandex.direct.grid.core.entity.banner.service.GridBannerService;
import ru.yandex.direct.grid.core.entity.fetchedfieldresolver.AdFetchedFieldsResolver;
import ru.yandex.direct.grid.core.entity.model.GdiEntityStats;
import ru.yandex.direct.grid.core.entity.recommendation.service.GridRecommendationService;
import ru.yandex.direct.grid.core.util.FetchedFieldsResolverCoreUtil;
import ru.yandex.direct.grid.core.util.stats.GridStatNew;
import ru.yandex.direct.grid.model.GdStatRequirements;
import ru.yandex.direct.grid.model.campaign.GdCampaign;
import ru.yandex.direct.grid.model.campaign.GdCampaignStatus;
import ru.yandex.direct.grid.model.campaign.GdCampaignType;
import ru.yandex.direct.grid.model.campaign.GdTextCampaign;
import ru.yandex.direct.grid.processing.context.container.GridGraphQLContext;
import ru.yandex.direct.grid.processing.model.banner.GdAdFilter;
import ru.yandex.direct.grid.processing.model.client.GdClientInfo;
import ru.yandex.direct.grid.processing.model.group.GdAdGroup;
import ru.yandex.direct.grid.processing.model.group.GdAdGroupAccess;
import ru.yandex.direct.grid.processing.model.group.GdTextAdGroup;
import ru.yandex.direct.grid.processing.service.campaign.CampaignInfoService;
import ru.yandex.direct.grid.processing.service.group.GroupDataService;
import ru.yandex.direct.grid.processing.util.ContextHelper;
import ru.yandex.direct.turbopages.client.TurbopagesClient;

import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static ru.yandex.direct.common.db.PpcPropertyNames.MODERATION_REASONS_ALLOWABLE_TO_REMODERATE_BY_CLIENT;
import static ru.yandex.direct.feature.FeatureName.SHOW_DNA_BY_DEFAULT;
import static ru.yandex.direct.grid.processing.data.TestGdAds.getDefaultGdAdsContainer;

public class BannerDataServiceWithFilterByAggrStatusTest {
    private static final long BANNER_ID = 193;
    private static final long GROUP_ID = 153;
    private static final long CAMPAIGN_ID = 10001L;
    private static final GdAdGroup GROUP_OBJ = new GdTextAdGroup()
            .withCampaignId(CAMPAIGN_ID)
            .withCampaign(new GdTextCampaign()
                    .withType(GdCampaignType.TEXT)
                    .withId(CAMPAIGN_ID)
                    .withStatus(new GdCampaignStatus().withAllowDomainMonitoring(true)))
            .withId(GROUP_ID)
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
        MockitoAnnotations.openMocks(this);

        doReturn(Set.of(SHOW_DNA_BY_DEFAULT.getName()))
                .when(featureService)
                .getEnabledForClientId(any(ClientId.class));

        doReturn(true)
                .when(featureService)
                .isEnabledForClientId(any(ClientId.class), eq(SHOW_DNA_BY_DEFAULT));

        doReturn(Collections.emptyMap())
                .when(gridRecommendationService)
                .getBannerRecommendations(eq(clientInfo.getId()), any(), any(), any());

        doReturn(campaignsById)
                .when(campaignInfoService)
                .getTruncatedCampaigns(ClientId.fromLong(clientInfo.getId()), singleton(CAMPAIGN_ID));

        doReturn(emptyMap())
                .when(moderationReasonService)
                .getReasonIdsForBannerAndResources(anyInt(), anySet());

        doReturn(null)
                .when(moderationReasonsAllowableToRemoderateByClient)
                .get();

        doReturn(moderationReasonsAllowableToRemoderateByClient)
                .when(ppcPropertiesSupport)
                .get(MODERATION_REASONS_ALLOWABLE_TO_REMODERATE_BY_CLIENT);

        doReturn(Collections.singletonList(GROUP_OBJ))
                .when(groupDataService)
                .getTruncatedAdGroups(eq(clientInfo.getShard()),
                        eq(clientInfo.getCountryRegionId()), eq(clientId),
                        eq(gridGraphQLContext.getOperator()), any(), any());
    }

    @Test
    public void hasOneSameReason() {
        GdiBannerFilter internalFilter = new GdiBannerFilter()
                .withReasonsContainSome(Set.of(GdSelfStatusReason.REJECTED_ON_MODERATION,
                        GdSelfStatusReason.AD_LOGO_REJECTED_MODERATION));

        final var adsContainer = getDefaultGdAdsContainer()
                .withStatRequirements(new GdStatRequirements()
                        .withFrom(TEST_FROM)
                        .withTo(TEST_TO)
                        .withStatsByDaysFrom(TEST_FROM)
                        .withStatsByDaysTo(TEST_TO))
                .withFilter(new GdAdFilter().withReasonsContainSome(internalFilter.getReasonsContainSome()).withAdIdIn(internalFilter.getBannerIdIn()));

        List<GdiBanner> intBanners = List.of(
                new GdiBanner()
                        .withCampaignId(CAMPAIGN_ID)
                        .withId(BANNER_ID)
                        .withGroupId(GROUP_ID)
                        .withBannerType(BannersBannerType.text)
                        .withStatusShow(true)
                        .withStatusActive(false)
                        .withStatusArchived(false)
                        .withStat(GridStatNew.addZeros(new GdiEntityStats()))
        );

        doReturn(new GdiBannersWithTotals().withGdiBanners(intBanners))
                .when(gridBannerService)
                .getBanners(eq(clientInfo.getShard()), eq(operatorUid), eq(clientId), anySet(), any(),
                        any(),
                        eq(TEST_FROM), eq(TEST_TO), any(), eq(emptySet()), eq(TEST_FROM), eq(TEST_TO), any(),
                        anyBoolean());

        doReturn(Map.of(BANNER_ID, new AggregatedStatusAdData(List.of(AdStatesEnum.DRAFT), GdSelfStatusEnum.DRAFT,
                List.of(GdSelfStatusReason.REJECTED_ON_MODERATION))))
                .when(aggregatedStatusesViewService)
                .getAdStatusesByIds(anyInt(), anySet());

        final var result = bannerDataService.getBanners(clientInfo, adsContainer, gridGraphQLContext);

        Assertions.assertThat(result.getGdAds()).hasSize(1);
        Assertions.assertThat(result.getGdAds().get(0).getId()).isEqualTo(BANNER_ID);
    }

    @Test
    public void noHasSameReason() {
        GdiBannerFilter internalFilter = new GdiBannerFilter()
                .withReasonsContainSome(Set.of(GdSelfStatusReason.AD_LOGO_REJECTED_MODERATION));

        final var adsContainer = getDefaultGdAdsContainer()
                .withStatRequirements(new GdStatRequirements()
                        .withFrom(TEST_FROM)
                        .withTo(TEST_TO)
                        .withStatsByDaysFrom(TEST_FROM)
                        .withStatsByDaysTo(TEST_TO))
                .withFilter(new GdAdFilter().withReasonsContainSome(internalFilter.getReasonsContainSome()).withAdIdIn(internalFilter.getBannerIdIn()));

        List<GdiBanner> intBanners = List.of(
                new GdiBanner()
                        .withCampaignId(CAMPAIGN_ID)
                        .withId(BANNER_ID)
                        .withGroupId(GROUP_ID)
                        .withBannerType(BannersBannerType.text)
                        .withStatusShow(true)
                        .withStatusActive(false)
                        .withStatusArchived(false)
                        .withStat(GridStatNew.addZeros(new GdiEntityStats()))
        );

        doReturn(new GdiBannersWithTotals().withGdiBanners(intBanners))
                .when(gridBannerService)
                .getBanners(eq(clientInfo.getShard()), eq(operatorUid), eq(clientId), anySet(), any(),
                        any(),
                        eq(TEST_FROM), eq(TEST_TO), any(), eq(emptySet()), eq(TEST_FROM), eq(TEST_TO), any(),
                        anyBoolean());

        doReturn(Map.of(BANNER_ID, new AggregatedStatusAdData(List.of(AdStatesEnum.DRAFT), GdSelfStatusEnum.DRAFT,
                List.of(GdSelfStatusReason.REJECTED_ON_MODERATION))))
                .when(aggregatedStatusesViewService)
                .getAdStatusesByIds(anyInt(), anySet());

        final var result = bannerDataService.getBanners(clientInfo, adsContainer, gridGraphQLContext);

        Assertions.assertThat(result.getGdAds()).isEmpty();
    }

    @Test
    public void noHasReasonsFilter() {
        GdiBannerFilter internalFilter = new GdiBannerFilter();

        final var adsContainer = getDefaultGdAdsContainer()
                .withStatRequirements(new GdStatRequirements()
                        .withFrom(TEST_FROM)
                        .withTo(TEST_TO)
                        .withStatsByDaysFrom(TEST_FROM)
                        .withStatsByDaysTo(TEST_TO))
                .withFilter(new GdAdFilter().withReasonsContainSome(internalFilter.getReasonsContainSome()).withAdIdIn(internalFilter.getBannerIdIn()));

        List<GdiBanner> intBanners = List.of(
                new GdiBanner()
                        .withCampaignId(CAMPAIGN_ID)
                        .withId(BANNER_ID)
                        .withGroupId(GROUP_ID)
                        .withBannerType(BannersBannerType.text)
                        .withStatusShow(true)
                        .withStatusActive(false)
                        .withStatusArchived(false)
                        .withStat(GridStatNew.addZeros(new GdiEntityStats()))
        );

        doReturn(new GdiBannersWithTotals().withGdiBanners(intBanners))
                .when(gridBannerService)
                .getBanners(eq(clientInfo.getShard()), eq(operatorUid), eq(clientId), anySet(), any(),
                        any(),
                        eq(TEST_FROM), eq(TEST_TO), any(), eq(emptySet()), eq(TEST_FROM), eq(TEST_TO), any(),
                        anyBoolean());

        doReturn(Map.of(BANNER_ID, new AggregatedStatusAdData(List.of(AdStatesEnum.DRAFT), GdSelfStatusEnum.DRAFT,
                List.of(GdSelfStatusReason.REJECTED_ON_MODERATION))))
                .when(aggregatedStatusesViewService)
                .getAdStatusesByIds(anyInt(), anySet());

        final var result = bannerDataService.getBanners(clientInfo, adsContainer, gridGraphQLContext);

        Assertions.assertThat(result.getGdAds()).hasSize(1);
    }
}
