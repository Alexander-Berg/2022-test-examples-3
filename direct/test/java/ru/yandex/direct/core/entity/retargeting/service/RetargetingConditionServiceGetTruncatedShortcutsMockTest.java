package ru.yandex.direct.core.entity.retargeting.service;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository;
import ru.yandex.direct.core.entity.campaign.service.CampMetrikaCountersService;
import ru.yandex.direct.core.entity.metrika.repository.MetrikaCampaignRepository;
import ru.yandex.direct.core.entity.metrika.service.MetrikaSegmentService;
import ru.yandex.direct.core.entity.retargeting.model.RetargetingConditionBase;
import ru.yandex.direct.core.entity.retargeting.repository.RetargetingConditionRepository;
import ru.yandex.direct.core.entity.retargeting.service.common.GoalUtilsService;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.metrika.client.MetrikaClient;
import ru.yandex.direct.metrika.client.model.request.UserCountersExtendedFilter;
import ru.yandex.direct.metrika.client.model.response.CounterInfoDirect;
import ru.yandex.direct.metrika.client.model.response.RetargetingCondition;
import ru.yandex.direct.metrika.client.model.response.Segment;
import ru.yandex.direct.metrika.client.model.response.UserCountersExtended;
import ru.yandex.direct.metrika.client.model.response.UserCountersExtendedResponse;
import ru.yandex.direct.rbac.RbacService;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.core.entity.metrika.service.MetrikaSegmentService.ECOM_ABANDONED_CART_EXPRESSION;
import static ru.yandex.direct.core.entity.metrika.service.MetrikaSegmentService.ECOM_PURCHASE_EXPRESSION;
import static ru.yandex.direct.core.entity.metrika.service.MetrikaSegmentService.ECOM_VIEWED_WITHOUT_PURCHASE_EXPRESSION;
import static ru.yandex.direct.core.entity.metrika.service.MetrikaSegmentService.NOT_BOUNCE_EXPRESSION;
import static ru.yandex.direct.core.entity.retargeting.Constants.MAX_RET_CONDITIONS_PER_CLIENT;
import static ru.yandex.direct.core.entity.retargeting.service.RetargetingConditionShortcutService.ALLOWED_CAMPAIGN_TYPES_FOR_SHORTCUTS;
import static ru.yandex.direct.core.entity.retargeting.service.RetargetingConditionShortcutService.CAMPAIGN_GOALS_LAL_SHORTCUT_DEFAULT_ID;
import static ru.yandex.direct.core.entity.retargeting.service.RetargetingConditionShortcutService.CAMPAIGN_GOALS_SHORTCUT_DEFAULT_ID;
import static ru.yandex.direct.core.entity.retargeting.service.RetargetingConditionShortcutService.ECOM_ABANDONED_CART_SHORTCUT_DEFAULT_ID;
import static ru.yandex.direct.core.entity.retargeting.service.RetargetingConditionShortcutService.ECOM_PURCHASE_LAL_SHORTCUT_DEFAULT_ID;
import static ru.yandex.direct.core.entity.retargeting.service.RetargetingConditionShortcutService.ECOM_VIEWED_WITHOUT_PURCHASE_SHORTCUT_DEFAULT_ID;
import static ru.yandex.direct.core.entity.retargeting.service.RetargetingConditionShortcutService.NOT_BOUNCE_LAL_SHORTCUT_DEFAULT_ID;
import static ru.yandex.direct.core.entity.retargeting.service.RetargetingConditionShortcutService.NOT_BOUNCE_SHORTCUT_DEFAULT_ID;
import static ru.yandex.direct.core.entity.retargeting.service.RetargetingConditionShortcutService.RETARGETING_CONDITION_SHORTCUT_DEFAULT_IDS;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@RunWith(MockitoJUnitRunner.class)
@Ignore("Переписать без моков")
public class RetargetingConditionServiceGetTruncatedShortcutsMockTest {
    private static final Long COUNTER_ID = 1234L;
    private static final Integer COUNTER_ID_INT = 1234;
    private static final Long GOAL_ID = 2345L;
    private static final Long CLIENT_REPRESENTATIVE_UID = 555L;

    @Mock
    private ShardHelper shardHelper;

    @Mock
    private CampaignRepository campaignRepository;

    @Mock
    private CampMetrikaCountersService campMetrikaCountersService;

    @InjectMocks
    private MetrikaSegmentService metrikaSegmentService;

    @Mock
    private MetrikaCampaignRepository metrikaCampaignRepository;

    @Spy
    private RetargetingConditionShortcutService retargetingConditionShortcutService =
            new RetargetingConditionShortcutService(mock(PpcPropertiesSupport.class));

    @Mock
    private RbacService rbacService;

    @Mock
    private RetargetingConditionRepository retConditionRepository;

    @Mock
    GoalUtilsService goalUtilsService;

    @Mock
    private MetrikaClient metrikaClient;

    @InjectMocks
    private RetargetingConditionService retargetingConditionService;

    private int shard;
    private ClientId clientId;
    private Long campaignId;

    @Before
    public void before() {
        shard = 1;
        clientId = ClientId.fromLong(2L);
        campaignId = 3L;

        doReturn(shard).when(shardHelper).getShardByClientId(any());

        doReturn(Map.of(campaignId, ALLOWED_CAMPAIGN_TYPES_FOR_SHORTCUTS.stream().findFirst().get()))
                .when(campaignRepository).getCampaignsTypeMap(shard, List.of(campaignId));
    }

    @Test
    public void getTruncatedRetargetingConditionShortcuts_campaignHasEditableCounters_notBounceShortcuts() {
        when(campMetrikaCountersService.getCounterByCampaignIds(clientId, List.of(campaignId)))
                .thenReturn(Map.of(campaignId, List.of(COUNTER_ID)));
        when(metrikaSegmentService.getExistingSegments(Set.of(COUNTER_ID_INT),
                MetrikaSegmentService.SegmentType.NOT_BOUNCE))
                .thenReturn(List.of(new Segment()));
        when(metrikaClient.getSegments(COUNTER_ID_INT, null))
                .thenReturn(List.of(new Segment().withExpression(NOT_BOUNCE_EXPRESSION)));
        when(metrikaClient.getUsersCountersNumExtended2(anyList(), any())).thenReturn(new UserCountersExtendedResponse()
                .withUsers(emptyList()));

        var shortcuts = retargetingConditionService.getTruncatedRetargetingConditionShortcuts(clientId, campaignId);
        assertThat(shortcuts).hasSize(2);
        var shortcutIds = mapList(shortcuts, RetargetingConditionBase::getId);
        assertThat(shortcutIds).containsExactly(NOT_BOUNCE_SHORTCUT_DEFAULT_ID, NOT_BOUNCE_LAL_SHORTCUT_DEFAULT_ID);
    }

    @Test
    public void getTruncatedRetargetingConditionShortcuts_campaignHasUnavailableGoals_empty() {
        when(metrikaCampaignRepository.getStrategyOrMeaningfulGoalIdsByCampaignId(shard, Set.of(campaignId)))
                .thenReturn(Map.of(campaignId, Set.of(GOAL_ID)));

        var shortcuts = retargetingConditionService.getTruncatedRetargetingConditionShortcuts(clientId, campaignId);
        assertThat(shortcuts).isEmpty();
    }

    @Test
    public void getTruncatedRetargetingConditionShortcuts_campaignHasAvailableGoals_campaignGoalsShortcuts() {
        when(metrikaCampaignRepository.getStrategyOrMeaningfulGoalIdsByCampaignId(shard, Set.of(campaignId)))
                .thenReturn(Map.of(campaignId, Set.of(GOAL_ID)));
        doReturn(List.of(new RetargetingCondition().withId(GOAL_ID)))
                .when(goalUtilsService)
                .getAvailableMetrikaGoals(clientId, Set.of(GOAL_ID));

        var shortcuts = retargetingConditionService.getTruncatedRetargetingConditionShortcuts(clientId, campaignId);
        assertThat(shortcuts).hasSize(2);
        var shortcutIds = mapList(shortcuts, RetargetingConditionBase::getId);
        assertThat(shortcutIds).containsExactly(CAMPAIGN_GOALS_SHORTCUT_DEFAULT_ID,
                CAMPAIGN_GOALS_LAL_SHORTCUT_DEFAULT_ID);
    }

    @Test
    public void getTruncatedRetargetingConditionShortcuts_campaignHasEcomCounter_ecomShortcuts() {
        when(campMetrikaCountersService.getCounterByCampaignIds(clientId, List.of(campaignId)))
                .thenReturn(Map.of(campaignId, List.of(COUNTER_ID)));
        when(rbacService.getClientRepresentativesUidsForGetMetrikaCounters(clientId))
                .thenReturn(List.of(CLIENT_REPRESENTATIVE_UID));
        var filter = new UserCountersExtendedFilter()
                .withCounterIds(List.of(COUNTER_ID));
        when(metrikaClient.getUsersCountersNumExtended2(List.of(CLIENT_REPRESENTATIVE_UID), filter))
                .thenReturn(new UserCountersExtendedResponse()
                        .withUsers(List.of(new UserCountersExtended()
                                .withCounters(List.of(new CounterInfoDirect()
                                        .withId(COUNTER_ID_INT)
                                        .withEcommerce(true))))));
        when(metrikaClient.getSegments(COUNTER_ID_INT, null))
                .thenReturn(List.of(
                        new Segment().withExpression(ECOM_PURCHASE_EXPRESSION),
                        new Segment().withExpression(ECOM_ABANDONED_CART_EXPRESSION),
                        new Segment().withExpression(ECOM_VIEWED_WITHOUT_PURCHASE_EXPRESSION)));

        var shortcuts = retargetingConditionService.getTruncatedRetargetingConditionShortcuts(clientId, campaignId);
        assertThat(shortcuts).hasSize(3);
        var shortcutIds = mapList(shortcuts, RetargetingConditionBase::getId);
        assertThat(shortcutIds).containsExactlyInAnyOrder(ECOM_PURCHASE_LAL_SHORTCUT_DEFAULT_ID,
                ECOM_ABANDONED_CART_SHORTCUT_DEFAULT_ID, ECOM_VIEWED_WITHOUT_PURCHASE_SHORTCUT_DEFAULT_ID);
    }

    @Test
    public void getTruncatedRetargetingConditionShortcuts_overMaxRetargetingConditions_empty() {
        when(retConditionRepository.getExistingRetargetingConditionsCount(shard, clientId))
                .thenReturn(MAX_RET_CONDITIONS_PER_CLIENT - RETARGETING_CONDITION_SHORTCUT_DEFAULT_IDS.size() + 1);
        when(metrikaCampaignRepository.getStrategyOrMeaningfulGoalIdsByCampaignId(shard, Set.of(campaignId)))
                .thenReturn(Map.of(campaignId, Set.of(GOAL_ID)));

        var shortcuts = retargetingConditionService.getTruncatedRetargetingConditionShortcuts(clientId, campaignId);
        assertThat(shortcuts).isEmpty();
    }
}
