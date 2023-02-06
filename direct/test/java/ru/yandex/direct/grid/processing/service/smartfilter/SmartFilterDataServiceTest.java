package ru.yandex.direct.grid.processing.service.smartfilter;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableSet;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.context.annotation.Description;

import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository;
import ru.yandex.direct.core.entity.campaign.model.Campaign;
import ru.yandex.direct.core.entity.campaign.model.DbStrategy;
import ru.yandex.direct.core.entity.campaign.model.StrategyName;
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.entity.performancefilter.container.PerformanceFiltersQueryFilter;
import ru.yandex.direct.core.entity.performancefilter.model.PerformanceFilter;
import ru.yandex.direct.core.entity.performancefilter.schema.compiled.OtherYandexCustomSchema;
import ru.yandex.direct.core.entity.performancefilter.service.PerformanceFilterService;
import ru.yandex.direct.core.entity.performancefilter.service.PerformanceFilterStorage;
import ru.yandex.direct.core.testing.data.TestPerformanceFilters;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.grid.core.entity.fetchedfieldresolver.FetchedFieldsResolver;
import ru.yandex.direct.grid.core.entity.fetchedfieldresolver.SmartFilterFetchedFieldsResolver;
import ru.yandex.direct.grid.core.entity.model.GdiEntityStats;
import ru.yandex.direct.grid.core.entity.smartfilter.GridSmartFilterYtRepository;
import ru.yandex.direct.grid.core.util.stats.GridStatNew;
import ru.yandex.direct.grid.model.GdEntityStats;
import ru.yandex.direct.grid.model.GdEntityStatsFilter;
import ru.yandex.direct.grid.model.GdGoalStats;
import ru.yandex.direct.grid.model.GdGoalStatsFilter;
import ru.yandex.direct.grid.model.GdStatPreset;
import ru.yandex.direct.grid.model.GdStatRequirements;
import ru.yandex.direct.grid.model.campaign.GdCampaign;
import ru.yandex.direct.grid.model.campaign.GdCampaignType;
import ru.yandex.direct.grid.processing.context.container.GridGraphQLContext;
import ru.yandex.direct.grid.processing.model.client.GdClientInfo;
import ru.yandex.direct.grid.processing.model.group.GdAdGroup;
import ru.yandex.direct.grid.processing.model.group.GdSmartAdGroup;
import ru.yandex.direct.grid.processing.model.smartfilter.GdSmartFilter;
import ru.yandex.direct.grid.processing.model.smartfilter.GdSmartFilterFilter;
import ru.yandex.direct.grid.processing.model.smartfilter.GdSmartFiltersContainer;
import ru.yandex.direct.grid.processing.model.smartfilter.GdSmartFiltersContext;
import ru.yandex.direct.grid.processing.service.campaign.CampaignInfoService;
import ru.yandex.direct.grid.processing.service.group.GroupDataService;
import ru.yandex.direct.grid.processing.util.ContextHelper;
import ru.yandex.direct.grid.processing.util.SmartFilterTestDataUtils;
import ru.yandex.direct.grid.processing.util.StatHelper;

import static freemarker.template.utility.Collections12.singletonList;
import static freemarker.template.utility.Collections12.singletonMap;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.MockitoAnnotations.initMocks;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.grid.processing.util.AdGroupTestDataUtils.defaultGdAdGroupStatus;
import static ru.yandex.direct.grid.processing.util.CampaignTestDataUtils.defaultGdCampaign;

public class SmartFilterDataServiceTest {

    @Mock
    private ShardHelper shardHelper;
    @Mock
    private PerformanceFilterService performanceFilterService;
    @Mock
    private AdGroupRepository adGroupRepository;
    @Mock
    private CampaignRepository campaignRepository;
    @Mock
    private GridSmartFilterYtRepository gridSmartFilterYtRepository;
    @Mock
    private CampaignInfoService campaignInfoService;
    @Mock
    private GroupDataService groupDataService;
    @Mock
    private FeatureService featureService;

    @Mock
    private PerformanceFilterStorage performanceFilterStorage;

    @InjectMocks
    private SmartFilterDataService smartFilterDataService;

    private ClientId clientId;
    private Long campaignId;
    private GdAdGroup gdAdGroup;
    private PerformanceFilter performanceFilter;
    private GridGraphQLContext context;

    @Before
    public void setUp() {
        initMocks(this);
        Long adGroupId = 1L;
        campaignId = 1L;

        performanceFilter = TestPerformanceFilters.defaultPerformanceFilter(adGroupId, null);
        Map campaignIdsByAdGroupIds = singletonMap(adGroupId, campaignId);
        DbStrategy strategy = new DbStrategy();
        strategy.setStrategyName(StrategyName.AUTOBUDGET_AVG_CPC_PER_CAMP);
        List campaignsWithStrategy = singletonList(new Campaign().withId(campaignId).withStrategy(strategy));

        context = ContextHelper.buildDefaultContext()
                .withFetchedFieldsReslover(
                        new FetchedFieldsResolver()
                                .withSmartFilter(new SmartFilterFetchedFieldsResolver().withStats(true)));

        GdClientInfo clientInfo = context.getQueriedClient();
        clientId = ClientId.fromLong(clientInfo.getId());
        int shard = clientInfo.getShard();

        doReturn(shard)
                .when(shardHelper).getShardByClientIdStrictly(eq(clientId));
        doReturn(singletonList(performanceFilter))
                .when(performanceFilterService).getPerformanceFilters(eq(clientId), any(PerformanceFiltersQueryFilter.class));
        doReturn(campaignIdsByAdGroupIds)
                .when(adGroupRepository).getCampaignIdsByAdGroupIds(eq(shard), anySet());
        doReturn(campaignsWithStrategy)
                .when(campaignRepository).getCampaignsWithStrategy(eq(shard), anyCollection());

        GdCampaign gdCampaign = defaultGdCampaign(campaignId);
        doReturn(Collections.singletonMap(gdCampaign.getId(), gdCampaign))
                .when(campaignInfoService)
                .getTruncatedCampaigns(clientId, singleton(gdCampaign.getId()));

        doReturn(new OtherYandexCustomSchema()).when(performanceFilterStorage).getFilterSchema(any(), any(), any());

        gdAdGroup = new GdSmartAdGroup()
                .withId(adGroupId)
                .withCampaign(gdCampaign)
                .withStatus(defaultGdAdGroupStatus());
        doReturn(Collections.singletonList(gdAdGroup))
                .when(groupDataService)
                .getTruncatedAdGroups(eq(shard), eq(clientInfo.getCountryRegionId()),
                        eq(clientId), eq(context.getOperator()), any(), any());
    }

    @Test
    @Description("Если не было фильтра на статистику и цели и они не найдены, "
            + "то отдаем смарт-фильтр с нулевой статистикой и пустым списком целей.")
    public void getSmartFilters_success() {
        GdSmartFiltersContainer input =
                SmartFilterTestDataUtils.getDefaultGdSmartFiltersContainer()
                        .withFilter(new GdSmartFilterFilter()
                                .withCampaignIdIn(ImmutableSet.of(campaignId)))
                        .withStatRequirements(new GdStatRequirements()
                                .withPreset(GdStatPreset.TODAY)
                                .withGoalIds(ImmutableSet.of(1L)));

        doReturn(emptyMap())
                .when(gridSmartFilterYtRepository)
                .getStatistic(anyList(), any(LocalDate.class), any(LocalDate.class), anySet(), anySet());

        GdEntityStats expectedStats = StatHelper.internalStatsToOuter(GridStatNew.addZeros(new GdiEntityStats()),
                GdCampaignType.PERFORMANCE);
        List<GdGoalStats> expectedGoalStats = emptyList();
        GdSmartFilter expected = SmartFilterConverter
                .toGdSmartFilter(performanceFilter, gdAdGroup, performanceFilterStorage)
                .withStats(expectedStats)
                .withGoalStats(expectedGoalStats);

        GdSmartFiltersContext smartFilters = smartFilterDataService.getSmartFilters(clientId, input, context);
        assertThat(smartFilters.getRowset()).hasSize(1);
        Assert.assertThat(smartFilters.getRowset().get(0), beanDiffer(expected));
    }

    @Test
    @Description("Если был фильтр на статистику и она не найдена, "
            + "то не отдаем смарт-фильтр, если 0 не входит в искомый интервал.")
    public void getSmartFilters_noResults_whenNoStatsAndMinClicksFilter() {
        GdSmartFiltersContainer input =
                SmartFilterTestDataUtils.getDefaultGdSmartFiltersContainer()
                        .withFilter(new GdSmartFilterFilter()
                                .withCampaignIdIn(ImmutableSet.of(campaignId))
                                .withStats(new GdEntityStatsFilter().withMinClicks(1L)))
                        .withStatRequirements(new GdStatRequirements()
                                .withPreset(GdStatPreset.TODAY));
        doReturn(emptyMap())
                .when(gridSmartFilterYtRepository)
                .getStatistic(anyList(), any(LocalDate.class), any(LocalDate.class), anySet(), anySet());


        GdSmartFiltersContext smartFilters = smartFilterDataService.getSmartFilters(clientId, input, context);
        assertThat(smartFilters.getRowset()).hasSize(0);
    }

    @Test
    @Description("Если был фильтр на статистику и она не найдена, "
            + "то отдаем смарт-фильтр, если 0 входит в искомый интервал.")
    public void getSmartFilters_success_whenNoStatsAndMaxClicksFilter() {
        GdSmartFiltersContainer input =
                SmartFilterTestDataUtils.getDefaultGdSmartFiltersContainer()
                        .withFilter(new GdSmartFilterFilter()
                                .withCampaignIdIn(ImmutableSet.of(campaignId))
                                .withStats(new GdEntityStatsFilter().withMaxClicks(1L)))
                        .withStatRequirements(new GdStatRequirements()
                                .withPreset(GdStatPreset.TODAY));
        doReturn(emptyMap())
                .when(gridSmartFilterYtRepository)
                .getStatistic(anyList(), any(LocalDate.class), any(LocalDate.class), anySet(), anySet());


        GdSmartFiltersContext smartFilters = smartFilterDataService.getSmartFilters(clientId, input, context);
        assertThat(smartFilters.getRowset()).hasSize(1);
    }

    @Test
    @Description("Если был фильтр на цели и они не найдены, "
            + "то не отдаем смарт-фильтр, если 0 не входит в искомый интервал.")
    public void getSmartFilters_noResults_whenNoStatsAndMinGoalsFilter() {
        GdSmartFiltersContainer input =
                SmartFilterTestDataUtils.getDefaultGdSmartFiltersContainer()
                        .withFilter(new GdSmartFilterFilter()
                                .withCampaignIdIn(ImmutableSet.of(campaignId))
                                .withGoalStats(Collections.singletonList(
                                        new GdGoalStatsFilter().withGoalId(1L).withMinGoals(30L))))
                        .withStatRequirements(new GdStatRequirements()
                                .withPreset(GdStatPreset.TODAY));
        doReturn(emptyMap())
                .when(gridSmartFilterYtRepository)
                .getStatistic(anyList(), any(LocalDate.class), any(LocalDate.class), anySet(), anySet());


        GdSmartFiltersContext smartFilters = smartFilterDataService.getSmartFilters(clientId, input, context);
        assertThat(smartFilters.getRowset()).hasSize(0);
    }

    @Test
    @Description("Если был фильтр на цели и они не найдены, "
            + "то отдаем смарт-фильтр, если 0 входит в искомый интервал.")
    public void getSmartFilters_success_whenNoStatsAndMaxGoalsFilter() {
        GdSmartFiltersContainer input =
                SmartFilterTestDataUtils.getDefaultGdSmartFiltersContainer()
                        .withFilter(new GdSmartFilterFilter()
                                .withCampaignIdIn(ImmutableSet.of(campaignId))
                                .withGoalStats(Collections.singletonList(
                                        new GdGoalStatsFilter().withGoalId(1L).withMaxGoals(30L))))
                        .withStatRequirements(new GdStatRequirements()
                                .withPreset(GdStatPreset.TODAY));
        doReturn(emptyMap())
                .when(gridSmartFilterYtRepository)
                .getStatistic(anyList(), any(LocalDate.class), any(LocalDate.class), anySet(), anySet());


        GdSmartFiltersContext smartFilters = smartFilterDataService.getSmartFilters(clientId, input, context);
        assertThat(smartFilters.getRowset()).hasSize(1);
    }
}
