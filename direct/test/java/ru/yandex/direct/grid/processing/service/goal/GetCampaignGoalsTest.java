package ru.yandex.direct.grid.processing.service.goal;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.entity.metrika.service.MetrikaGoalsService;
import ru.yandex.direct.core.entity.metrika.service.campaigngoals.CampaignGoalsService;
import ru.yandex.direct.core.entity.retargeting.model.Goal;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.feature.FeatureName;
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

@RunWith(Parameterized.class)
public class GetCampaignGoalsTest {

    private static final String NAME_1 = "Цель";
    private static final String NAME_2 = "Цель 2";
    private static final String NAME_3 = "Цель 3";

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
        when(metrikaGoalsService.getGoals(eq(1L), eq(ClientId.fromLong(1L)), any())).thenReturn(goals);
        when(featureService.isEnabledForClientId(any(ClientId.class),
                eq(FeatureName.GOALS_ONLY_WITH_CAMPAIGN_COUNTERS_USED))).thenReturn(false);
        goalDataService = new GoalDataService(metrikaGoalsService, null,
                campaignGoalsService, null, null, null, null, null, featureService, goalConversionsCacheService,
                campaignRepository, shardHelper);
    }

    @Test
    public void test() {
        List<GdGoal> res = goalDataService.getCampaignsGoals(1L, ClientId.fromLong(1L), new GdCampaignGoalFilter(),
                true);
        assertThat(res).containsExactlyElementsOf(expectedGoals);
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
                .withIsPerfectGoal(false)
                .withConversionGrade(ConversionGrade.LOW_CONVERSION);
    }
}
