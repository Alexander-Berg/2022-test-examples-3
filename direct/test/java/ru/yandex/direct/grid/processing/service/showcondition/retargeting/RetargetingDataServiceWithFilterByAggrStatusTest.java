package ru.yandex.direct.grid.processing.service.showcondition.retargeting;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.RandomUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.direct.core.aggregatedstatuses.AggregatedStatusesViewService;
import ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusEnum;
import ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusReason;
import ru.yandex.direct.core.entity.aggregatedstatuses.retargeting.AggregatedStatusRetargetingData;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.grid.core.entity.bidmodifiers.service.GridBidModifiersService;
import ru.yandex.direct.grid.core.entity.showcondition.model.GdiBidsRetargeting;
import ru.yandex.direct.grid.core.entity.showcondition.model.GdiBidsRetargetingWithTotals;
import ru.yandex.direct.grid.core.entity.showcondition.service.GridRetargetingConditionService;
import ru.yandex.direct.grid.core.entity.showcondition.service.GridRetargetingService;
import ru.yandex.direct.grid.model.aggregatedstatuses.GdRetargetingAggregatedStatusInfo;
import ru.yandex.direct.grid.model.campaign.GdCampaign;
import ru.yandex.direct.grid.model.campaign.strategy.GdCampaignStrategyManual;
import ru.yandex.direct.grid.processing.context.container.GridGraphQLContext;
import ru.yandex.direct.grid.processing.model.client.GdClientInfo;
import ru.yandex.direct.grid.processing.model.group.GdTextAdGroup;
import ru.yandex.direct.grid.processing.model.retargeting.GdRetargetingFilter;
import ru.yandex.direct.grid.processing.model.retargeting.GdRetargetingsContainer;
import ru.yandex.direct.grid.processing.service.campaign.CampaignInfoService;
import ru.yandex.direct.grid.processing.service.group.GroupDataService;
import ru.yandex.direct.grid.processing.util.ContextHelper;

import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static ru.yandex.direct.feature.FeatureName.SHOW_DNA_BY_DEFAULT;
import static ru.yandex.direct.grid.processing.util.AdGroupTestDataUtils.defaultGdAdGroupStatus;
import static ru.yandex.direct.grid.processing.util.CampaignTestDataUtils.defaultGdCampaign;
import static ru.yandex.direct.grid.processing.util.CampaignTestDataUtils.defaultGdCampaignStrategyManual;
import static ru.yandex.direct.grid.processing.util.RetargetingTestDataUtils.getDefaultGdRetargetingsContainer;

public class RetargetingDataServiceWithFilterByAggrStatusTest {
    private static final long CAMPAIGN_ID = RandomUtils.nextLong(1, Long.MAX_VALUE - 1);
    private static final long AD_GROUP_ID = RandomUtils.nextLong(1, Long.MAX_VALUE - 1);
    private static final BigDecimal PRICE_CONTEXT = BigDecimal.TEN;

    private static GridGraphQLContext gridGraphQLContext;
    private static GdClientInfo clientInfo;
    private static ClientId clientId;

    @Mock
    private GridRetargetingConditionService gridRetargetingConditionService;
    @Mock
    private GridRetargetingService gridReatargetingService;
    @Mock
    private GroupDataService groupDataService;
    @Mock
    private GridBidModifiersService gridBidModifiersService;
    @Mock
    private CampaignInfoService campaignInfoService;
    @Mock
    private FeatureService featureService;
    @Mock
    private AggregatedStatusesViewService aggregatedStatusesViewService;

    @InjectMocks
    private RetargetingDataService showConditionDataService;
    private GdRetargetingsContainer input;
    private GdTextAdGroup gdAdGroup;

    @BeforeClass
    public static void beforeClass() {
        gridGraphQLContext = ContextHelper.buildDefaultContext();
        clientInfo = gridGraphQLContext.getQueriedClient();
        clientId = ClientId.fromLong(clientInfo.getId());
    }

    @Before
    public void before() {
        MockitoAnnotations.openMocks(this);
        input = getDefaultGdRetargetingsContainer();

        GdCampaignStrategyManual strategy = defaultGdCampaignStrategyManual();
        GdCampaign campaign = defaultGdCampaign(CAMPAIGN_ID)
                .withFlatStrategy(strategy)
                .withStrategy(strategy);

        doReturn(Collections.singletonMap(campaign.getId(), campaign))
                .when(campaignInfoService)
                .getTruncatedCampaigns(clientId, singleton(campaign.getId()));

        gdAdGroup = new GdTextAdGroup()
                .withId(AD_GROUP_ID)
                .withCampaignId(CAMPAIGN_ID)
                .withStatus(defaultGdAdGroupStatus());

        doReturn(Collections.singletonList(gdAdGroup))
                .when(groupDataService)
                .getTruncatedAdGroups(eq(clientInfo.getShard()),
                        eq(clientInfo.getCountryRegionId()), eq(clientId), eq(gridGraphQLContext.getOperator()), any(),
                        any());

        doReturn(true)
                .when(featureService)
                .isEnabledForClientId(any(), eq(SHOW_DNA_BY_DEFAULT));
    }

    @Test
    public void hasOneSameReason() {
        doReturn(Map.of(1L, new AggregatedStatusRetargetingData(List.of(), GdSelfStatusEnum.RUN_OK,
                List.of(GdSelfStatusReason.DRAFT, GdSelfStatusReason.RETARGETING_SUSPENDED_BY_USER)), 2L,
                new AggregatedStatusRetargetingData(List.of(), GdSelfStatusEnum.RUN_WARN, List.of())))
                .when(aggregatedStatusesViewService)
                .getRetargetingStatusesByIds(clientInfo.getShard(), Set.of(1L, 2L));

        GdiBidsRetargeting retargeting1 = new GdiBidsRetargeting()
                .withRetargetingId(1L)
                .withAdGroupId(AD_GROUP_ID)
                .withCampaignId(CAMPAIGN_ID)
                .withPriceContext(PRICE_CONTEXT);

        GdiBidsRetargeting retargeting2 = new GdiBidsRetargeting()
                .withRetargetingId(2L)
                .withAdGroupId(AD_GROUP_ID)
                .withCampaignId(CAMPAIGN_ID)
                .withPriceContext(PRICE_CONTEXT);

        doReturn(new GdiBidsRetargetingWithTotals().withGdiBidsRetargetings(List.of(retargeting1, retargeting2)))
                .when(gridReatargetingService)
                .getRetargetings(eq(clientId), any(), any(), any(), any(), any(), anyBoolean(), anyBoolean());

        input = input.withFilter(new GdRetargetingFilter().withReasonsContainSome(Set.of(GdSelfStatusReason.RETARGETING_SUSPENDED_BY_USER)));
        final var result = showConditionDataService.getRetargetingsRowset(gridGraphQLContext, input);

        assertThat(result.getGdRetargetings()).hasSize(1);
        assertThat(result.getGdRetargetings().get(0).getRetargetingId()).isEqualTo(retargeting1.getRetargetingId());
    }

    @Test
    public void noHasSameReason() {
        doReturn(Map.of(1L, new AggregatedStatusRetargetingData(List.of(), GdSelfStatusEnum.RUN_OK, List.of())))
                .when(aggregatedStatusesViewService)
                .getRetargetingStatusesByIds(clientInfo.getShard(), Set.of(1L));

        GdiBidsRetargeting retargeting = new GdiBidsRetargeting()
                .withRetargetingId(1L)
                .withAdGroupId(AD_GROUP_ID)
                .withCampaignId(CAMPAIGN_ID)
                .withAggregatedStatusInfo(new GdRetargetingAggregatedStatusInfo().withReasons(List.of(GdSelfStatusReason.DRAFT)))
                .withPriceContext(PRICE_CONTEXT);

        doReturn(new GdiBidsRetargetingWithTotals().withGdiBidsRetargetings(singletonList(retargeting)))
                .when(gridReatargetingService)
                .getRetargetings(eq(clientId), any(), any(), any(), any(), any(), anyBoolean(), anyBoolean());

        input = input.withFilter(new GdRetargetingFilter().withReasonsContainSome(Set.of(GdSelfStatusReason.RETARGETING_SUSPENDED_BY_USER)));
        final var result = showConditionDataService.getRetargetingsRowset(gridGraphQLContext, input);

        assertThat(result.getGdRetargetings()).isEmpty();
    }

    @Test
    public void noHasReasonsFilter() {
        doReturn(Map.of(1L, new AggregatedStatusRetargetingData(List.of(), GdSelfStatusEnum.RUN_OK,
                List.of(GdSelfStatusReason.DRAFT, GdSelfStatusReason.RETARGETING_SUSPENDED_BY_USER))))
                .when(aggregatedStatusesViewService)
                .getRetargetingStatusesByIds(clientInfo.getShard(), Set.of(1L, 2L));

        GdiBidsRetargeting retargeting = new GdiBidsRetargeting()
                .withRetargetingId(1L)
                .withAdGroupId(AD_GROUP_ID)
                .withCampaignId(CAMPAIGN_ID)
                .withAggregatedStatusInfo(new GdRetargetingAggregatedStatusInfo().withReasons(List.of(GdSelfStatusReason.DRAFT, GdSelfStatusReason.RETARGETING_SUSPENDED_BY_USER)))
                .withPriceContext(PRICE_CONTEXT);

        doReturn(new GdiBidsRetargetingWithTotals().withGdiBidsRetargetings(singletonList(retargeting)))
                .when(gridReatargetingService)
                .getRetargetings(eq(clientId), any(), any(), any(), any(), any(), anyBoolean(), anyBoolean());

        input = input.withFilter(new GdRetargetingFilter());
        final var result = showConditionDataService.getRetargetingsRowset(gridGraphQLContext, input);

        assertThat(result.getGdRetargetings()).hasSize(1);
    }
}
