package ru.yandex.direct.grid.processing.service.goal;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository;
import ru.yandex.direct.core.entity.campaign.service.CampMetrikaCountersService;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.entity.metrika.service.MetrikaGoalsService;
import ru.yandex.direct.core.entity.metrika.service.campaigngoals.CampaignGoalsService;
import ru.yandex.direct.core.entity.retargeting.model.Goal;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.model.goal.ConversionGrade;
import ru.yandex.direct.grid.processing.model.goal.GdCampaignGoalFilter;
import ru.yandex.direct.grid.processing.model.goal.GdGoal;
import ru.yandex.direct.grid.processing.model.goal.GdGoalType;
import ru.yandex.direct.grid.processing.model.goal.GdMetrikaCounterGoalType;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.core.entity.retargeting.model.Goal.METRIKA_ECOMMERCE_UPPER_BOUND;

@GridProcessingTest
@RunWith(Parameterized.class)
public class GetCampaignGoalsWithMethodsFromCampaignGoalsServiceTest {

    private static final String NAME_1 = "Goal";
    private static final String NAME_2 = "Goal 2";
    private static final String NAME_3 = "Goal 3";

    public static final long CAMPAIGN_ID = 1L;
    public static final Map<Long, CampaignType> CAMPAIGN_TYPE_MAP = Map.of(CAMPAIGN_ID, CampaignType.TEXT);
    public static final long COUNTER_ID = 1L;
    public static final Map<Long, List<Long>> COUNTERS_BY_CAMPAIGNS_ID = Map.of(CAMPAIGN_ID, List.of(COUNTER_ID));
    public static final long OPERATOR_UID = 1L;
    public static final ClientId CLIENT_ID = ClientId.fromLong(OPERATOR_UID);
    public static final int SHARD = 1;

    private GoalDataService goalDataService;

    @Parameterized.Parameter
    public List<Goal> goals;

    @Parameterized.Parameter(1)
    public List<GdGoal> expectedGoals;

    @Parameterized.Parameters
    public static Collection testData() {
        Object[][] data = new Object[][]{
                {
                        Arrays.asList(
                                new Goal().withName(NAME_1).withId(123L),
                                new Goal().withName(NAME_2).withId(4L),
                                new Goal().withName("999").withId(METRIKA_ECOMMERCE_UPPER_BOUND - 1)
                        ),
                        Arrays.asList(
                                getGdGoal(NAME_2, NAME_2, 4L, true),
                                getGdGoal(NAME_1, NAME_1, 123L, false),
                                getGdGoal("eCommerce: Покупка (счетчик № 999)", "eCommerce: Покупка (счетчик № 999)",
                                        METRIKA_ECOMMERCE_UPPER_BOUND - 1, false)
                                        .withType(GdGoalType.ECOMMERCE)
                                        .withHasPrice(true)
                        )
                },
                {
                        Arrays.asList(
                                new Goal().withName(NAME_2).withId(2L).withParentId(1L),
                                new Goal().withName(NAME_2).withId(3L),
                                new Goal().withName(NAME_1).withId(1L).withParentId(0L),
                                new Goal().withName(NAME_3).withId(4L).withParentId(1L),
                                new Goal().withName(NAME_2).withId(5L).withParentId(6L),
                                new Goal().withName(NAME_3).withId(6L)
                        ),
                        Arrays.asList(
                                getGdGoal(NAME_1, NAME_1, 1L, false).withParentId(0L),
                                getGdGoal(NAME_1 + "; " + NAME_2, NAME_2, 2L, false).withParentId(1L),
                                getGdGoal(NAME_1 + "; " + NAME_3, NAME_3, 4L, true).withParentId(1L),
                                getGdGoal(NAME_2, NAME_2, 3L, true),
                                getGdGoal(NAME_3, NAME_3, 6L, true),
                                getGdGoal(NAME_3 + "; " + NAME_2, NAME_2, 5L, true).withParentId(6L)
                        )
                }
        };
        return asList(data);
    }

    @Before
    public void prepare() {
        MetrikaGoalsService metrikaGoalsService = mock(MetrikaGoalsService.class);
        CampaignGoalsService campaignGoalsService = mock(CampaignGoalsService.class);
        GoalConversionsCacheService goalConversionsCacheService = mock(GoalConversionsCacheService.class);
        FeatureService featureService = mock(FeatureService.class);
        CampaignRepository campaignRepository = mock(CampaignRepository.class);
        ShardHelper shardHelper = mock(ShardHelper.class);
        CampMetrikaCountersService campMetrikaCountersService = mock(CampMetrikaCountersService.class);

        // set up for interactions
        when(featureService.isEnabledForClientId(any(ClientId.class),
                eq(FeatureName.MIGRATING_TO_NEW_METHODS_IN_GET_CAMPAIGN_GOALS)))
                .thenReturn(true);
        when(shardHelper.getShardByClientId(any()))
                .thenReturn(SHARD);
        when(campaignRepository.getCampaignsTypeMap(any(), any()))
                .thenReturn(CAMPAIGN_TYPE_MAP);
        when(campMetrikaCountersService.getCounterByCampaignIds(any(), any()))
                .thenReturn(COUNTERS_BY_CAMPAIGNS_ID);
        when(campaignGoalsService.getAvailableGoalsForCampaignId(eq(OPERATOR_UID), eq(CLIENT_ID), any(), any()))
                .thenReturn(Map.of(OPERATOR_UID, Set.copyOf(goals)));

        goalDataService = new GoalDataService(metrikaGoalsService, null,
                campaignGoalsService, null, campMetrikaCountersService, null,
                null, null, featureService, goalConversionsCacheService,
                campaignRepository, shardHelper);
    }

    @Test
    public void getCampaignsGoals_MethodsFromCampaignGoalsService() {
        List<GdGoal> campaignsGoals = goalDataService.getCampaignsGoals(OPERATOR_UID, CLIENT_ID, new GdCampaignGoalFilter(),
                true);
        assertThat(campaignsGoals).containsExactlyElementsOf(expectedGoals);
    }

    private static GdGoal getGdGoal(String name, String simpleName, Long id, boolean isMobile) {
        return new GdGoal()
                .withId(id)
                .withIsMobileGoal(isMobile)
                .withType(GdGoalType.GOAL)
                .withMetrikaGoalType(GdMetrikaCounterGoalType.URL)
                .withName(name)
                .withSimpleName(simpleName)
                .withHasPrice(false)
                .withConversionGrade(ConversionGrade.LOW_CONVERSION)
                .withIsPerfectGoal(false);
    }
}
