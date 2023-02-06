package ru.yandex.direct.grid.processing.service.showcondition;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import ru.yandex.direct.core.aggregatedstatuses.AggregatedStatusesViewService;
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.entity.keyword.repository.KeywordRepository;
import ru.yandex.direct.core.entity.keyword.service.KeywordBsAuctionService;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.grid.core.entity.banner.repository.GridBannerRepository;
import ru.yandex.direct.grid.core.entity.banner.repository.GridImageRepository;
import ru.yandex.direct.grid.core.entity.fetchedfieldresolver.ShowConditionFetchedFieldsResolver;
import ru.yandex.direct.grid.core.entity.showcondition.model.GdiShowCondition;
import ru.yandex.direct.grid.core.entity.showcondition.model.GdiShowConditionFilter;
import ru.yandex.direct.grid.core.entity.showcondition.model.GdiShowConditionWithTotals;
import ru.yandex.direct.grid.core.entity.showcondition.service.GridShowConditionService;
import ru.yandex.direct.grid.core.util.FetchedFieldsResolverCoreUtil;
import ru.yandex.direct.grid.model.GdStatRequirements;
import ru.yandex.direct.grid.model.campaign.GdCampaign;
import ru.yandex.direct.grid.model.campaign.GdTextCampaign;
import ru.yandex.direct.grid.model.campaign.strategy.GdCampaignFlatStrategy;
import ru.yandex.direct.grid.model.campaign.strategy.GdCampaignStrategyManual;
import ru.yandex.direct.grid.processing.context.container.GridGraphQLContext;
import ru.yandex.direct.grid.processing.model.client.GdClientInfo;
import ru.yandex.direct.grid.processing.model.group.GdAdGroup;
import ru.yandex.direct.grid.processing.model.group.GdTextAdGroup;
import ru.yandex.direct.grid.processing.model.showcondition.GdKeyword;
import ru.yandex.direct.grid.processing.model.showcondition.GdShowCondition;
import ru.yandex.direct.grid.processing.model.showcondition.GdShowConditionFilter;
import ru.yandex.direct.grid.processing.model.showcondition.GdShowConditionWithTotals;
import ru.yandex.direct.grid.processing.model.showcondition.GdShowConditionsContainer;
import ru.yandex.direct.grid.processing.service.banner.BannerDataService;
import ru.yandex.direct.grid.processing.service.campaign.CampaignInfoService;
import ru.yandex.direct.grid.processing.service.group.GroupDataService;
import ru.yandex.direct.grid.processing.service.showcondition.keywords.ShowConditionDataService;
import ru.yandex.direct.grid.processing.util.ContextHelper;
import ru.yandex.direct.multitype.entity.LimitOffset;
import ru.yandex.direct.test.utils.RandomNumberUtils;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static ru.yandex.direct.grid.processing.service.cache.util.CacheUtils.normalizeLimitOffset;
import static ru.yandex.direct.grid.processing.util.CampaignTestDataUtils.defaultGdCampaign;
import static ru.yandex.direct.grid.processing.util.CampaignTestDataUtils.defaultGdCampaignStrategyAvgCpa;
import static ru.yandex.direct.grid.processing.util.CampaignTestDataUtils.defaultGdCampaignStrategyManual;
import static ru.yandex.direct.grid.processing.util.ShowConditionTestDataUtils.defaultGdiShowCondition;
import static ru.yandex.direct.utils.FunctionalUtils.listToSet;

public class ShowConditionDataServiceTest {

    private static final LocalDate TEST_FROM = LocalDate.now();
    private static final LocalDate TEST_TO = TEST_FROM.plusDays(10);
    private static final LimitOffset LIMIT_OFFSET = normalizeLimitOffset(null);
    private static final ShowConditionFetchedFieldsResolver SHOW_CONDITION_FETCHED_FIELDS_RESOLVER =
            FetchedFieldsResolverCoreUtil.buildShowConditionFetchedFieldsResolver(true);

    private static GridGraphQLContext gridGraphQLContext;
    private static GdClientInfo clientInfo;
    private static ClientId clientId;
    private static Long operatorUid;

    private GdShowConditionsContainer inputContainer;
    private GdiShowCondition keyword;
    private GdiShowCondition keywordWithUnknownGroup;
    private GdiShowCondition keywordWithoutPriceContext;

    @Mock
    private GridShowConditionService gridShowConditionService;
    @Mock
    private KeywordBsAuctionService keywordBsAuctionService;
    @Mock
    private BannerDataService bannerDataService;
    @Mock
    private GridBannerRepository gridBannerRepository;
    @Mock
    private GridImageRepository gridImageRepository;
    @Mock
    private CampaignRepository campaignRepository;
    @Mock
    private KeywordRepository keywordRepository;
    @Mock
    private CampaignInfoService campaignInfoService;
    @Mock
    private GroupDataService groupDataService;
    @Mock
    private AggregatedStatusesViewService aggregatedStatusesViewService;
    @Mock
    private FeatureService featureService;

    @Spy
    @InjectMocks
    private ShowConditionDataService showConditionDataService;

    @BeforeClass
    public static void initTestData() {
        gridGraphQLContext = ContextHelper.buildDefaultContext();
        gridGraphQLContext.getFetchedFieldsReslover().setShowCondition(SHOW_CONDITION_FETCHED_FIELDS_RESOLVER);
        clientInfo = gridGraphQLContext.getQueriedClient();
        clientId = ClientId.fromLong(clientInfo.getId());
        operatorUid = gridGraphQLContext.getOperator().getUid();
    }

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);

        keyword = defaultGdiShowCondition();
        keywordWithUnknownGroup = defaultGdiShowCondition()
                .withCampaignId(keyword.getCampaignId());

        keywordWithoutPriceContext = defaultGdiShowCondition()
                .withPriceContext(null)
                .withCampaignId(keyword.getCampaignId());

        GdCampaignStrategyManual strategy = defaultGdCampaignStrategyManual();
        GdCampaign campaign = defaultGdCampaign(keyword.getCampaignId())
                .withFlatStrategy(strategy)
                .withStrategy(strategy);
        campaign.getStatus().setReadOnly(true);

        doReturn(Collections.singletonMap(campaign.getId(), campaign))
                .when(campaignInfoService)
                .getTruncatedCampaigns(clientId, singleton(campaign.getId()));

        doReturn(new GdiShowConditionWithTotals()
                .withGdiShowConditions(ImmutableList.of(keyword, keywordWithoutPriceContext, keywordWithUnknownGroup)))
                .when(gridShowConditionService)
                .getShowConditions(eq(clientInfo.getShard()), eq(operatorUid), eq(clientId),
                        any(), any(), eq(TEST_FROM), eq(TEST_TO),
                        eq(emptySet()), eq(SHOW_CONDITION_FETCHED_FIELDS_RESOLVER), anyBoolean(), anyBoolean());

        GdAdGroup gdAdGroup = new GdTextAdGroup()
                .withId(keyword.getGroupId())
                .withCampaignId(keyword.getCampaignId());

        GdAdGroup gdAdGroup2 = new GdTextAdGroup()
                .withId(keywordWithoutPriceContext.getGroupId())
                .withCampaignId(keywordWithoutPriceContext.getCampaignId());

        doReturn(List.of(gdAdGroup2, gdAdGroup))
                .when(groupDataService)
                .getTruncatedAdGroups(eq(clientInfo.getShard()),
                        eq(clientInfo.getCountryRegionId()),
                        eq(clientId), eq(gridGraphQLContext.getOperator()), any(), any());

        doReturn(emptyList())
                .when(keywordBsAuctionService)
                .getTrafaretAuction(eq(clientId), anyList(), any());

        doReturn(emptyList())
                .when(keywordRepository)
                .getKeywordsByIds(eq(clientInfo.getShard()), eq(clientId), anyCollection());

        doReturn(emptyList())
                .when(campaignRepository)
                .getCampaigns(eq(clientInfo.getShard()), anyCollection());

        doReturn(emptyMap())
                .when(showConditionDataService)
                .getPokazometerData(anyList(), anyMap(), any());

        inputContainer = new GdShowConditionsContainer()
                .withOrderBy(emptyList())
                .withFilter(new GdShowConditionFilter())
                .withStatRequirements(new GdStatRequirements()
                        .withFrom(TEST_FROM)
                        .withTo(TEST_TO)
                );
    }


    @Test
    public void testGetShowConditions_WithEmptyFilter() {
        GdShowConditionWithTotals gdShowConditionWithTotals = showConditionDataService
                .getShowConditions(clientInfo, inputContainer, gridGraphQLContext, LIMIT_OFFSET, false);
        List<GdShowCondition> showConditions = gdShowConditionWithTotals.getGdShowConditions();

        verify(gridShowConditionService)
                .getShowConditions(eq(clientInfo.getShard()), eq(operatorUid), eq(clientId),
                        eq(new GdiShowConditionFilter().withShowConditionStatusIn(emptySet())),
                        eq(emptyList()), eq(TEST_FROM), eq(TEST_TO), eq(emptySet()), any(), eq(false), eq(false));

        Set<Long> showConditionIds = listToSet(showConditions, GdShowCondition::getId);
        assertThat(showConditionIds)
                .isEqualTo(ImmutableSet.of(keyword.getId(), keywordWithoutPriceContext.getId()));
    }

    @Test
    public void testGetShowConditions_HasIdIncluded() {
        long showConditionId2 = RandomNumberUtils.nextPositiveLong();
        inputContainer.getFilter().setShowConditionIdIn(Collections.singleton(showConditionId2));
        showConditionDataService.getShowConditions(clientInfo, inputContainer, gridGraphQLContext, LIMIT_OFFSET, false);

        verify(gridShowConditionService)
                .getShowConditions(eq(clientInfo.getShard()), eq(operatorUid), eq(clientId),
                        eq(new GdiShowConditionFilter()
                                .withShowConditionIdIn(Collections.singleton(showConditionId2))
                                .withShowConditionStatusIn(emptySet())),
                        eq(emptyList()), eq(TEST_FROM), eq(TEST_TO), eq(emptySet()), any(), eq(false), eq(false));
    }

    @Test
    public void testGetShowConditions_WithMinPriceContextFilter() {
        inputContainer.getFilter().setMinPriceContext(RandomNumberUtils.nextPositiveBigDecimal());

        GdShowConditionWithTotals gdShowConditionWithTotals = showConditionDataService
                .getShowConditions(clientInfo, inputContainer, gridGraphQLContext, LIMIT_OFFSET, false);
        List<GdShowCondition> showConditions = gdShowConditionWithTotals.getGdShowConditions();

        Set<Long> showConditionIds = listToSet(showConditions, GdShowCondition::getId);
        assertThat(showConditionIds)
                .isEqualTo(ImmutableSet.of(keyword.getId()));
    }

    @Test
    public void testGetShowConditions_WhenFetchedCampaignsIsEmpty() {
        doReturn(emptyMap())
                .when(campaignInfoService)
                .getTruncatedCampaigns(clientId, singleton(keyword.getCampaignId()));

        GdShowConditionWithTotals gdShowConditionWithTotals = showConditionDataService
                .getShowConditions(clientInfo, inputContainer, gridGraphQLContext, LIMIT_OFFSET, false);
        assertThat(gdShowConditionWithTotals.getGdShowConditions()).isEmpty();

        verify(campaignInfoService)
                .getTruncatedCampaigns(clientId, singleton(keyword.getCampaignId()));
        verifyZeroInteractions(groupDataService);
    }

    @Test
    public void testGetShowConditions_WhenFetchedCampaignsDontContainProperCampaign() {
        doReturn(Collections.singletonMap(111L, new GdTextCampaign()))
                .when(campaignInfoService)
                .getTruncatedCampaigns(clientId, singleton(keyword.getCampaignId()));

        GdShowConditionWithTotals gdShowConditionWithTotals = showConditionDataService
                .getShowConditions(clientInfo, inputContainer, gridGraphQLContext, LIMIT_OFFSET, false);
        assertThat(gdShowConditionWithTotals.getGdShowConditions()).isEmpty();

        verify(campaignInfoService)
                .getTruncatedCampaigns(clientId, singleton(keyword.getCampaignId()));
        verifyZeroInteractions(groupDataService);
    }

    @Test
    public void testGetShowConditions_WhenFetchedAdGroupsIsEmpty() {
        doReturn(emptyList())
                .when(groupDataService)
                .getTruncatedAdGroups(eq(clientInfo.getShard()),
                        eq(clientInfo.getCountryRegionId()),
                        eq(clientId), eq(gridGraphQLContext.getOperator()), any(), any());

        GdShowConditionWithTotals gdShowConditionWithTotals = showConditionDataService
                .getShowConditions(clientInfo, inputContainer, gridGraphQLContext, LIMIT_OFFSET, false);
        assertThat(gdShowConditionWithTotals.getGdShowConditions()).isEmpty();

        verify(groupDataService)
                .getTruncatedAdGroups(eq(clientInfo.getShard()),
                        eq(clientInfo.getCountryRegionId()),
                        eq(clientId), eq(gridGraphQLContext.getOperator()), any(), any());
        verifyZeroInteractions(bannerDataService);
    }

    @Test
    public void testGetShowConditions_PriceShownOnManualStrategy() {
        doReturn(new GdiShowConditionWithTotals().withGdiShowConditions(ImmutableList.of(keyword)))
                .when(gridShowConditionService)
                .getShowConditions(eq(clientInfo.getShard()), eq(operatorUid), eq(clientId),
                        any(), any(), eq(TEST_FROM), eq(TEST_TO),
                        eq(emptySet()), eq(SHOW_CONDITION_FETCHED_FIELDS_RESOLVER), anyBoolean(), anyBoolean());

        GdShowConditionWithTotals gdShowConditionWithTotals = showConditionDataService
                .getShowConditions(clientInfo, inputContainer, gridGraphQLContext, LIMIT_OFFSET, false);
        List<GdShowCondition> showConditions = gdShowConditionWithTotals.getGdShowConditions();
        assertThat(showConditions).hasSize(1);

        GdShowCondition condition = showConditions.get(0);
        assertThat(condition).isInstanceOf(GdKeyword.class);
        assertThat(((GdKeyword) condition).getPrice()).isEqualTo(keyword.getPrice());
        assertThat(((GdKeyword) condition).getPriceContext()).isEqualTo(keyword.getPriceContext());
    }

    @Test
    public void testGetShowConditions_PriceNotShownOnAutobudgetStrategy() {
        GdCampaignFlatStrategy strategy = defaultGdCampaignStrategyAvgCpa();
        GdCampaign campaign = defaultGdCampaign(keyword.getCampaignId())
                .withFlatStrategy(strategy)
                .withStrategy(strategy);
        campaign.getStatus().setReadOnly(true);

        doReturn(Collections.singletonMap(campaign.getId(), campaign))
                .when(campaignInfoService)
                .getTruncatedCampaigns(clientId, Set.of(campaign.getId()));

        doReturn(new GdiShowConditionWithTotals().withGdiShowConditions(ImmutableList.of(keyword)))
                .when(gridShowConditionService)
                .getShowConditions(eq(clientInfo.getShard()), eq(operatorUid), eq(clientId),
                        any(), any(), eq(TEST_FROM), eq(TEST_TO),
                        eq(emptySet()), eq(SHOW_CONDITION_FETCHED_FIELDS_RESOLVER), eq(false), eq(false));

        GdShowConditionWithTotals gdShowConditionWithTotals = showConditionDataService
                .getShowConditions(clientInfo, inputContainer, gridGraphQLContext, LIMIT_OFFSET, false);
        List<GdShowCondition> showConditions = gdShowConditionWithTotals.getGdShowConditions();
        assertThat(showConditions).hasSize(1);

        GdShowCondition condition = showConditions.get(0);
        assertThat(condition).isInstanceOf(GdKeyword.class);
        assertThat(((GdKeyword) condition).getPrice()).isNull();
        assertThat(((GdKeyword) condition).getPriceContext()).isNull();
    }

}
