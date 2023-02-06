package ru.yandex.direct.grid.processing.service.smartfilter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import one.util.streamex.StreamEx;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository;
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
import ru.yandex.direct.grid.core.entity.smartfilter.GridSmartFilterYtRepository;
import ru.yandex.direct.grid.model.Order;
import ru.yandex.direct.grid.model.campaign.GdCampaign;
import ru.yandex.direct.grid.processing.context.container.GridGraphQLContext;
import ru.yandex.direct.grid.processing.model.client.GdClientInfo;
import ru.yandex.direct.grid.processing.model.group.GdSmartAdGroup;
import ru.yandex.direct.grid.processing.model.smartfilter.GdSmartFilter;
import ru.yandex.direct.grid.processing.model.smartfilter.GdSmartFilterFilter;
import ru.yandex.direct.grid.processing.model.smartfilter.GdSmartFilterOrderBy;
import ru.yandex.direct.grid.processing.model.smartfilter.GdSmartFilterOrderByField;
import ru.yandex.direct.grid.processing.model.smartfilter.GdSmartFiltersContainer;
import ru.yandex.direct.grid.processing.model.smartfilter.GdSmartFiltersContext;
import ru.yandex.direct.grid.processing.service.campaign.CampaignInfoService;
import ru.yandex.direct.grid.processing.service.group.GroupDataService;
import ru.yandex.direct.grid.processing.util.CampaignTestDataUtils;
import ru.yandex.direct.grid.processing.util.ContextHelper;
import ru.yandex.direct.grid.processing.util.SmartFilterTestDataUtils;

import static freemarker.template.utility.Collections12.singletonList;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.MockitoAnnotations.initMocks;
import static ru.yandex.direct.grid.processing.util.AdGroupTestDataUtils.defaultGdAdGroupStatus;
import static ru.yandex.direct.utils.FunctionalUtils.listToMap;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

public class SmartFilterDataServiceOrderByTest {

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
    private GridGraphQLContext context;
    private List<Long> campaignIds;
    private List<Long> idNaturalOrder = asList(1L, 2L, 3L, 4L);

    @Before
    public void setUp() {
        initMocks(this);
        List<Long> adGroupIds = new ArrayList<>(idNaturalOrder);
        campaignIds = new ArrayList<>(idNaturalOrder);

        PerformanceFilter performanceFilter1 = TestPerformanceFilters.defaultPerformanceFilter(adGroupIds.get(0), null)
                .withId(1L)
                .withPriceCpa(BigDecimal.valueOf(10));
        PerformanceFilter performanceFilter2 = TestPerformanceFilters.defaultPerformanceFilter(adGroupIds.get(1), null)
                .withId(2L)
                .withPriceCpa(BigDecimal.valueOf(20));
        PerformanceFilter performanceFilter3 = TestPerformanceFilters.defaultPerformanceFilter(adGroupIds.get(2), null)
                .withId(3L)
                .withPriceCpa(BigDecimal.valueOf(30));
        PerformanceFilter performanceFilter4 = TestPerformanceFilters.defaultPerformanceFilter(adGroupIds.get(3), null)
                .withId(4L)
                .withPriceCpa(BigDecimal.valueOf(15));

        Map<Long, Long> campaignIdsByAdGroupIds = listToMap(adGroupIds,
                adGroupId -> campaignIds.get(adGroupId.intValue() - 1));

        DbStrategy cpaStrategy = new DbStrategy();
        cpaStrategy.setStrategyName(StrategyName.AUTOBUDGET_AVG_CPA_PER_CAMP);
        DbStrategy cpcStrategy = new DbStrategy();
        cpcStrategy.setStrategyName(StrategyName.AUTOBUDGET_AVG_CPC_PER_CAMP);

        Map<Long, DbStrategy> strategyByFilterId = new HashMap<Long, DbStrategy>() {{
            put(1L, cpaStrategy);
            put(2L, cpaStrategy);
            put(3L, cpaStrategy);
            put(4L, cpcStrategy);
        }};

        context = ContextHelper.buildDefaultContext()
                .withFetchedFieldsReslover(
                        new FetchedFieldsResolver()
                                .withSmartFilter(new SmartFilterFetchedFieldsResolver().withStats(true)));

        GdClientInfo clientInfo = context.getQueriedClient();
        clientId = ClientId.fromLong(clientInfo.getId());
        int shard = clientInfo.getShard();

        doReturn(shard)
                .when(shardHelper).getShardByClientIdStrictly(eq(clientId));
        doReturn(asList(performanceFilter1, performanceFilter2, performanceFilter3, performanceFilter4))
                .when(performanceFilterService).getPerformanceFilters(eq(clientId), any(PerformanceFiltersQueryFilter.class));
        doReturn(campaignIdsByAdGroupIds)
                .when(adGroupRepository).getCampaignIdsByAdGroupIds(eq(shard), anySet());
        doReturn(strategyByFilterId)
                .when(campaignRepository).getStrategyByFilterIds(eq(shard), anyCollection());
        doReturn(emptyMap())
                .when(gridSmartFilterYtRepository)
                .getStatistic(anyList(), any(LocalDate.class), any(LocalDate.class), anySet(), anySet());
        doReturn(new OtherYandexCustomSchema()).when(performanceFilterStorage).getFilterSchema(any(), any(), any());

        List<GdCampaign> gdCampaigns = mapList(campaignIds, CampaignTestDataUtils::defaultGdCampaign);
        Map<Long, GdCampaign> gdCampaignsByIds = listToMap(gdCampaigns, GdCampaign::getId);
        doReturn(gdCampaignsByIds)
                .when(campaignInfoService)
                .getTruncatedCampaigns(eq(clientId), any());

        List<GdSmartAdGroup> gdAdGroups = StreamEx.of(adGroupIds)
                .map(adGroupId -> new GdSmartAdGroup()
                        .withId(adGroupId)
                        .withCampaign(gdCampaignsByIds.get(campaignIdsByAdGroupIds.get(adGroupId)))
                        .withStatus(defaultGdAdGroupStatus())).toList();
        doReturn(gdAdGroups)
                .when(groupDataService)
                .getTruncatedAdGroups(eq(shard), eq(clientInfo.getCountryRegionId()),
                        eq(clientId), eq(context.getOperator()), any(), any());
    }

    @Test
    public void getSmartFilters_orderBy_asc() {
        GdSmartFilterOrderBy orderBy = new GdSmartFilterOrderBy()
                .withField(GdSmartFilterOrderByField.PRICE_CPA)
                .withOrder(Order.ASC);
        GdSmartFiltersContainer input =
                SmartFilterTestDataUtils.getDefaultGdSmartFiltersContainer()
                        .withFilter(new GdSmartFilterFilter()
                                .withCampaignIdIn(ImmutableSet.copyOf(campaignIds)))
                        .withOrderBy(singletonList(orderBy));

        List<Long> expectedFilterIds = new ArrayList<>(idNaturalOrder);

        GdSmartFiltersContext smartFilters = smartFilterDataService.getSmartFilters(clientId, input, context);
        List<Long> actualFilterIds = mapList(smartFilters.getRowset(), GdSmartFilter::getId);
        assertThat(actualFilterIds).isEqualTo(expectedFilterIds);
    }

    @Test
    public void getSmartFilters_orderBy_desc() {
        GdSmartFilterOrderBy orderBy = new GdSmartFilterOrderBy()
                .withField(GdSmartFilterOrderByField.PRICE_CPA)
                .withOrder(Order.DESC);
        GdSmartFiltersContainer input =
                SmartFilterTestDataUtils.getDefaultGdSmartFiltersContainer()
                        .withFilter(new GdSmartFilterFilter()
                                .withCampaignIdIn(ImmutableSet.copyOf(campaignIds)))
                        .withOrderBy(singletonList(orderBy));

        List<Long> expectedFilterIds = Lists.reverse(idNaturalOrder);

        GdSmartFiltersContext smartFilters = smartFilterDataService.getSmartFilters(clientId, input, context);
        List<Long> actualFilterIds = mapList(smartFilters.getRowset(), GdSmartFilter::getId);
        assertThat(actualFilterIds).isEqualTo(expectedFilterIds);
    }
}
