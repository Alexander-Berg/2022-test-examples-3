package ru.yandex.direct.grid.processing.service.goal;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository;
import ru.yandex.direct.core.entity.campaign.service.CampMetrikaCountersService;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.entity.metrika.service.MetrikaGoalsConversionService;
import ru.yandex.direct.core.entity.metrika.service.MetrikaGoalsService;
import ru.yandex.direct.core.entity.metrika.service.campaigngoals.CampaignGoalsService;
import ru.yandex.direct.core.entity.mobilegoals.repository.MobileGoalsStatisticRepository;
import ru.yandex.direct.core.entity.retargeting.model.Goal;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.grid.processing.model.goal.ConversionGrade;
import ru.yandex.direct.grid.processing.model.goal.GdCounterWithGoals;
import ru.yandex.direct.grid.processing.model.goal.GdCountersWithGoals;
import ru.yandex.direct.grid.processing.model.goal.GdGoal;
import ru.yandex.direct.grid.processing.model.goal.GdGoalType;
import ru.yandex.direct.grid.processing.model.goal.GdMetrikaCounterGoalType;
import ru.yandex.direct.test.utils.RandomNumberUtils;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(Parameterized.class)
public class GetCounterWithGoalsTest {
    private static final String NAME = "Цель";
    private static final ClientId CLIENT_ID = ClientId.fromLong(RandomNumberUtils.nextPositiveLong());
    private static final Long OWNED_COUNTER_ID = RandomNumberUtils.nextPositiveLong();
    private static final Long NOT_OWNED_COUNTER_ID = RandomNumberUtils.nextPositiveLong();
    private static final Goal GOAL = (Goal) new Goal().withId(257L)
            .withName(NAME);

    private GoalDataService goalDataService;

    @Parameterized.Parameter
    public Set<Long> requestedCounterIds;

    @Parameterized.Parameter(1)
    public List<GdCounterWithGoals> expectedGoals;

    @Parameterized.Parameters
    public static Collection testData() {
        Object[][] data = new Object[][]{
                {
                        Set.of(OWNED_COUNTER_ID),
                        List.of(getExpectedGdCounterWithGoals())
                },
                {
                        Set.of(OWNED_COUNTER_ID, NOT_OWNED_COUNTER_ID),
                        List.of(getExpectedGdCounterWithGoals())
                },
                {
                        Set.of(NOT_OWNED_COUNTER_ID),
                        emptyList()
                },
        };
        return asList(data);
    }

    @Before
    public void prepare() {
        MetrikaGoalsService metrikaGoalsService = mock(MetrikaGoalsService.class);
        CampaignGoalsService campaignMeaningfulGoalsService = mock(CampaignGoalsService.class);
        GoalConversionsCacheService goalConversionsCacheService = mock(GoalConversionsCacheService.class);
        CampMetrikaCountersService campMetrikaCountersService = mock(CampMetrikaCountersService.class);
        CampaignRepository campaignRepository = mock(CampaignRepository.class);
        ShardHelper shardHelper = mock(ShardHelper.class);
        MetrikaGoalsConversionService metrikaGoalsConversionService = mock(MetrikaGoalsConversionService.class);
        FeatureService featureService = mock(FeatureService.class);
        MobileGoalsStatisticRepository mobileGoalsStatisticRepository = mock(MobileGoalsStatisticRepository.class);
        when(metrikaGoalsService.getMetrikaGoalsByCounterIds(any(), anySet())).thenReturn(Collections.emptyMap());
        when(metrikaGoalsService.getMetrikaGoalsByCounterIds(eq(CLIENT_ID), eq(Set.of(OWNED_COUNTER_ID))))
                .thenReturn(Map.of(OWNED_COUNTER_ID, Set.of(GOAL)));
        when(campMetrikaCountersService.getAvailableCounterIdsByClientId(eq(CLIENT_ID), eq(Set.of(OWNED_COUNTER_ID))))
                .thenReturn(Set.of(OWNED_COUNTER_ID));
        when(campMetrikaCountersService.getAvailableCounterIdsByClientId(eq(CLIENT_ID),
                eq(Set.of(OWNED_COUNTER_ID, NOT_OWNED_COUNTER_ID))))
                .thenReturn(Set.of(OWNED_COUNTER_ID));

        goalDataService = new GoalDataService(metrikaGoalsService, null,
                campaignMeaningfulGoalsService, null, campMetrikaCountersService,
                metrikaGoalsConversionService, mobileGoalsStatisticRepository, null, featureService,
                goalConversionsCacheService, campaignRepository, shardHelper);
    }

    @Test
    public void test() {
        GdCountersWithGoals res = goalDataService
                .getGdGoalsByCounterIds(OWNED_COUNTER_ID, requestedCounterIds, CLIENT_ID);
        assertThat(res.getCounterWithGoals()).containsExactlyElementsOf(expectedGoals);
    }

    private static GdCounterWithGoals getExpectedGdCounterWithGoals() {
        return new GdCounterWithGoals().withCounterId(OWNED_COUNTER_ID).
                withGoals(Set.of(
                        new GdGoal()
                                .withType(GdGoalType.GOAL)
                                .withIsMobileGoal(false)
                                .withHasPrice(false)
                                .withMetrikaGoalType(GdMetrikaCounterGoalType.URL)
                                .withName(GOAL.getName())
                                .withSimpleName(GOAL.getName())
                                .withId(GOAL.getId())
                                .withIsPerfectGoal(false)
                                .withConversionGrade(ConversionGrade.LOW_CONVERSION)));
    }

}
