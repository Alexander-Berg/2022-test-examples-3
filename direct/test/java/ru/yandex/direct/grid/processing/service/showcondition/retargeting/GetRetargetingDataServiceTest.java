package ru.yandex.direct.grid.processing.service.showcondition.retargeting;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.RandomUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.direct.core.aggregatedstatuses.AggregatedStatusesViewService;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.grid.core.entity.bidmodifiers.service.GridBidModifiersService;
import ru.yandex.direct.grid.core.entity.showcondition.model.GdiBidsRetargeting;
import ru.yandex.direct.grid.core.entity.showcondition.model.GdiBidsRetargetingWithTotals;
import ru.yandex.direct.grid.core.entity.showcondition.service.GridRetargetingConditionService;
import ru.yandex.direct.grid.core.entity.showcondition.service.GridRetargetingService;
import ru.yandex.direct.grid.model.campaign.GdCampaign;
import ru.yandex.direct.grid.model.campaign.strategy.GdCampaignStrategyManual;
import ru.yandex.direct.grid.processing.context.container.GridGraphQLContext;
import ru.yandex.direct.grid.processing.model.client.GdClientInfo;
import ru.yandex.direct.grid.processing.model.group.GdTextAdGroup;
import ru.yandex.direct.grid.processing.model.retargeting.GdRetargeting;
import ru.yandex.direct.grid.processing.model.retargeting.GdRetargetingAccess;
import ru.yandex.direct.grid.processing.model.retargeting.GdRetargetingWithTotals;
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
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.grid.processing.util.AdGroupTestDataUtils.defaultGdAdGroupStatus;
import static ru.yandex.direct.grid.processing.util.CampaignTestDataUtils.defaultGdCampaign;
import static ru.yandex.direct.grid.processing.util.CampaignTestDataUtils.defaultGdCampaignStrategyManual;
import static ru.yandex.direct.grid.processing.util.RetargetingTestDataUtils.getDefaultGdRetargetingsContainer;
import static ru.yandex.direct.grid.processing.util.StatHelperTest.zeroStats;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

public class GetRetargetingDataServiceTest {
    private static final long CAMPAIGN_ID = RandomUtils.nextLong(1, Long.MAX_VALUE - 1);
    private static final long AD_GROUP_ID = RandomUtils.nextLong(1, Long.MAX_VALUE - 1);
    private static final long RETARGETING_CONDITION_ID = RandomUtils.nextLong(1, Long.MAX_VALUE);
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
        MockitoAnnotations.initMocks(this);
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
    }

    @Test
    public void getRetargetingsRowset_RetargetingCampaignIsUnsupported_RetargetingNotReturns() {
        mockGetRetargeting(AD_GROUP_ID, Long.MAX_VALUE);

        GdRetargetingWithTotals retargetingWithTotals =
                showConditionDataService.getRetargetingsRowset(gridGraphQLContext, input);

        assertThat(retargetingWithTotals.getGdRetargetings()).isEmpty();
    }

    @Test
    public void getRetargetingsRowset_RetargetingAdGroupIsUnsupported_RetargetingNotReturns() {
        mockGetRetargeting(Long.MAX_VALUE, CAMPAIGN_ID);

        GdRetargetingWithTotals retargetingWithTotals =
                showConditionDataService.getRetargetingsRowset(gridGraphQLContext, input);

        assertThat(retargetingWithTotals.getGdRetargetings()).isEmpty();
    }

    @Test
    public void getRetargetingsRowset_RetargetingReturns() {
        mockGetRetargeting(AD_GROUP_ID, CAMPAIGN_ID);

        GdRetargetingWithTotals retargetingWithTotals =
                showConditionDataService.getRetargetingsRowset(gridGraphQLContext, input);
        List<GdRetargeting> retargetingsRowset = retargetingWithTotals.getGdRetargetings();

        assertThat(retargetingsRowset).isNotEmpty();

        GdRetargeting expectedGdRetargeting = new GdRetargeting()
                .withStats(zeroStats())
                .withAdGroupId(AD_GROUP_ID)
                .withCampaignId(CAMPAIGN_ID)
                .withAdGroup(gdAdGroup)
                .withPriceContext(PRICE_CONTEXT)
                .withRetargetingConditionId(RETARGETING_CONDITION_ID)
                .withAccess(new GdRetargetingAccess().withCanEdit(true));
        assertThat(retargetingsRowset).is(matchedBy(beanDiffer(singletonList(expectedGdRetargeting))));
    }

    private void mockGetRetargeting(long adGroupId, long campaignId) {
        GdiBidsRetargeting retargeting = new GdiBidsRetargeting()
                .withRetargetingConditionId(RETARGETING_CONDITION_ID)
                .withAdGroupId(adGroupId)
                .withCampaignId(campaignId)
                .withPriceContext(PRICE_CONTEXT);
        doReturn(new GdiBidsRetargetingWithTotals().withGdiBidsRetargetings(singletonList(retargeting)))
                .when(gridReatargetingService)
                .getRetargetings(eq(clientId), any(), any(), any(), any(), any(), anyBoolean(), anyBoolean());
    }
}
