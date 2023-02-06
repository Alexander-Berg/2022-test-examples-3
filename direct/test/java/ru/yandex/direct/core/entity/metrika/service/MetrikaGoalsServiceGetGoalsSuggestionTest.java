package ru.yandex.direct.core.entity.metrika.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import one.util.streamex.StreamEx;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.retargeting.model.ConversionLevel;
import ru.yandex.direct.core.entity.retargeting.model.Goal;
import ru.yandex.direct.core.entity.retargeting.model.MetrikaCounterGoalType;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.test.utils.RandomNumberUtils;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class MetrikaGoalsServiceGetGoalsSuggestionTest {

    @Autowired
    private Steps steps;

    @Autowired
    private MetrikaGoalsService metrikaGoalsService;

    private ClientInfo clientInfo;

    @Before
    public void setUp() {
        clientInfo = steps.clientSteps().createDefaultClient();
        var clientId = Objects.requireNonNull(clientInfo.getClientId());
        steps.featureSteps().addClientFeature(clientId, FeatureName.ENABLE_SUGGESTION_FOR_RECOMMENDED_GOALS, true);
    }

    @Test
    public void get_success() {
        var expectedGoals = getSortedGoals();

        var result = metrikaGoalsService.getGoalsSuggestion(clientInfo.getClientId(), Set.copyOf(expectedGoals));

        assertThat(result.getSortedGoalsToSuggestion()).isEqualTo(expectedGoals);
        assertThat(result.getTop1GoalId()).isEqualTo(expectedGoals.get(0).getId());
    }

    @Test
    public void get_emptyGoals_success() {
        var result = metrikaGoalsService.getGoalsSuggestion(clientInfo.getClientId(), emptySet());

        assertThat(result.getSortedGoalsToSuggestion()).isEqualTo(emptyList());
        assertThat(result.getTop1GoalId()).isNull();
    }

    @Test
    public void get_oneGreenGoal_success() {
        var goal = (Goal) new Goal()
                .withId(123L)
                .withMetrikaCounterGoalType(MetrikaCounterGoalType.MESSENGER)
                .withConversionVisitsCount(20L);

        var result = metrikaGoalsService.getGoalsSuggestion(clientInfo.getClientId(), Set.of(goal));

        assertThat(result.getSortedGoalsToSuggestion()).isEqualTo(List.of(goal));
        assertThat(result.getTop1GoalId()).isEqualTo(123L);
    }

    @Test
    public void get_oneNotGreenGoal_success() {
        var goal = (Goal) new Goal()
                .withId(123L)
                .withMetrikaCounterGoalType(MetrikaCounterGoalType.MESSENGER)
                .withConversionVisitsCount(19L);

        var result = metrikaGoalsService.getGoalsSuggestion(clientInfo.getClientId(), Set.of(goal));

        assertThat(result.getSortedGoalsToSuggestion()).isEqualTo(List.of(goal));
        assertThat(result.getTop1GoalId()).isNull();
    }

    @Test
    public void get_greenGoalsWithoutEcomEmailPhoneTypes_success() {
        var expectedGoals = new ArrayList<Goal>();
        var greenGoalsOfOtherTypes = getGoals2();
        expectedGoals.addAll(greenGoalsOfOtherTypes);
        var greenPerfectGoals = getGoals3();
        expectedGoals.addAll(greenPerfectGoals);
        expectedGoals.addAll(getGoals4());
        expectedGoals.addAll(getGoals5());
        addIdAndConversionLevelToGoals(expectedGoals);

        var expectedTop1GoalId = expectedGoals.get(0).getId();

        var result = metrikaGoalsService.getGoalsSuggestion(clientInfo.getClientId(), Set.copyOf(expectedGoals));

        assertThat(result.getSortedGoalsToSuggestion()).isEqualTo(expectedGoals);
        assertThat(result.getTop1GoalId()).isEqualTo(expectedTop1GoalId);
    }

    @Test
    public void get_notGreenOrNotPerfectGoals_success() {
        var goals = new ArrayList<Goal>();
        goals.addAll(getGoals4());
        goals.addAll(getGoals5());
        addIdAndConversionLevelToGoals(goals);

        var result = metrikaGoalsService.getGoalsSuggestion(clientInfo.getClientId(), Set.copyOf(goals));

        assertThat(result.getSortedGoalsToSuggestion()).isEqualTo(goals);
        assertThat(result.getTop1GoalId()).isNull();
    }

    @Test
    public void get_goalsWithNullConversionVisitsCount_success() {
        var expectedGoals = new ArrayList<Goal>();
        expectedGoals.addAll(getGoals1());
        expectedGoals.addAll(getGoals2());
        expectedGoals.addAll(getGoals3());
        expectedGoals.addAll(getGoals4());
        expectedGoals.addAll(getGoals5());
        expectedGoals.addAll(getGoalsWithoutConversionVisitsCount());

        addIdAndConversionLevelToGoals(expectedGoals);

        var result = metrikaGoalsService.getGoalsSuggestion(clientInfo.getClientId(), Set.copyOf(expectedGoals));

        assertThat(result.getSortedGoalsToSuggestion()).isEqualTo(expectedGoals);
        assertThat(result.getTop1GoalId()).isEqualTo(expectedGoals.get(0).getId());
    }

    private List<Goal> getGoals1() {
        return List.of(
                (Goal) new Goal()
                        .withMetrikaCounterGoalType(MetrikaCounterGoalType.ECOMMERCE)
                        .withConversionVisitsCount(100L),
                (Goal) new Goal()
                        .withId(111L)
                        .withMetrikaCounterGoalType(MetrikaCounterGoalType.ECOMMERCE)
                        .withConversionVisitsCount(20L),
                (Goal) new Goal()
                        .withId(101L)
                        .withMetrikaCounterGoalType(MetrikaCounterGoalType.ECOMMERCE)
                        .withConversionVisitsCount(20L),
                (Goal) new Goal()
                        .withMetrikaCounterGoalType(MetrikaCounterGoalType.PHONE)
                        .withConversionVisitsCount(500L),
                (Goal) new Goal()
                        .withMetrikaCounterGoalType(MetrikaCounterGoalType.EMAIL)
                        .withConversionVisitsCount(1000L),
                (Goal) new Goal()
                        .withMetrikaCounterGoalType(MetrikaCounterGoalType.EMAIL)
                        .withConversionVisitsCount(100L),
                (Goal) new Goal()
                        .withMetrikaCounterGoalType(MetrikaCounterGoalType.EMAIL)
                        .withConversionVisitsCount(20L));
    }

    private List<Goal> getGoals2() {
        return List.of(
                (Goal) new Goal()
                        .withMetrikaCounterGoalType(MetrikaCounterGoalType.MESSENGER)
                        .withConversionVisitsCount(2000L),
                (Goal) new Goal()
                        .withMetrikaCounterGoalType(MetrikaCounterGoalType.FILE)
                        .withConversionVisitsCount(1500L),
                (Goal) new Goal()
                        .withMetrikaCounterGoalType(MetrikaCounterGoalType.BUTTON)
                        .withConversionVisitsCount(1000L),
                (Goal) new Goal()
                        .withMetrikaCounterGoalType(MetrikaCounterGoalType.MESSENGER)
                        .withConversionVisitsCount(500L));
    }

    private List<Goal> getGoals3() {
        return List.of(
                (Goal) new Goal()
                        .withMetrikaCounterGoalType(MetrikaCounterGoalType.ACTION)
                        .withConversionLevel(ConversionLevel.PERFECT)
                        .withConversionVisitsCount(3000L),
                (Goal) new Goal()
                        .withId(91L)
                        .withMetrikaCounterGoalType(MetrikaCounterGoalType.URL)
                        .withConversionLevel(ConversionLevel.PERFECT)
                        .withConversionVisitsCount(2000L),
                (Goal) new Goal()
                        .withId(81L)
                        .withMetrikaCounterGoalType(MetrikaCounterGoalType.ACTION)
                        .withConversionLevel(ConversionLevel.PERFECT)
                        .withConversionVisitsCount(2000L),
                (Goal) new Goal()
                        .withMetrikaCounterGoalType(MetrikaCounterGoalType.URL)
                        .withConversionLevel(ConversionLevel.PERFECT)
                        .withConversionVisitsCount(1000L));
    }

    private List<Goal> getGoals4() {
        return List.of(
                (Goal) new Goal()
                        .withMetrikaCounterGoalType(MetrikaCounterGoalType.ACTION)
                        .withConversionLevel(ConversionLevel.SOSO)
                        .withConversionVisitsCount(4000L),
                (Goal) new Goal()
                        .withMetrikaCounterGoalType(MetrikaCounterGoalType.URL)
                        .withConversionLevel(ConversionLevel.BAD)
                        .withConversionVisitsCount(3000L),
                (Goal) new Goal()
                        .withMetrikaCounterGoalType(MetrikaCounterGoalType.ACTION)
                        .withConversionLevel(ConversionLevel.UNKNOWN)
                        .withConversionVisitsCount(2000L),
                (Goal) new Goal()
                        .withMetrikaCounterGoalType(MetrikaCounterGoalType.URL)
                        .withConversionLevel(ConversionLevel.GOOD)
                        .withConversionVisitsCount(1000L));
    }

    private List<Goal> getGoals5() {
        return List.of(
                (Goal) new Goal()
                        .withMetrikaCounterGoalType(MetrikaCounterGoalType.MESSENGER)
                        .withConversionVisitsCount(19L),
                (Goal) new Goal()
                        .withMetrikaCounterGoalType(MetrikaCounterGoalType.URL)
                        .withConversionLevel(ConversionLevel.PERFECT)
                        .withConversionVisitsCount(18L),
                (Goal) new Goal()
                        .withMetrikaCounterGoalType(MetrikaCounterGoalType.PHONE)
                        .withConversionVisitsCount(15L),
                (Goal) new Goal()
                        .withMetrikaCounterGoalType(MetrikaCounterGoalType.ECOMMERCE)
                        .withConversionVisitsCount(14L),
                (Goal) new Goal()
                        .withMetrikaCounterGoalType(MetrikaCounterGoalType.PHONE)
                        .withConversionVisitsCount(13L),
                (Goal) new Goal()
                        .withMetrikaCounterGoalType(MetrikaCounterGoalType.BUTTON)
                        .withConversionVisitsCount(10L),
                (Goal) new Goal()
                        .withMetrikaCounterGoalType(MetrikaCounterGoalType.ACTION)
                        .withConversionLevel(ConversionLevel.GOOD)
                        .withConversionVisitsCount(1L),
                (Goal) new Goal()
                        .withId(71L)
                        .withMetrikaCounterGoalType(MetrikaCounterGoalType.EMAIL)
                        .withConversionVisitsCount(0L),
                (Goal) new Goal()
                        .withId(61L)
                        .withMetrikaCounterGoalType(MetrikaCounterGoalType.ECOMMERCE)
                        .withConversionVisitsCount(0L));
    }

    private void addIdAndConversionLevelToGoals(Collection<Goal> goals) {
        StreamEx.of(goals)
                .forEach(goal -> {
                    if (goal.getId() == null) {
                        goal.setId(RandomNumberUtils.nextPositiveLong() + 1000);
                    }
                    if (goal.getConversionLevel() == null) {
                        goal.setConversionLevel(ConversionLevel.UNKNOWN);
                    }
                });
    }

    private List<Goal> getSortedGoals() {
        var goals = new ArrayList<Goal>();
        goals.addAll(getGoals1());
        goals.addAll(getGoals2());
        goals.addAll(getGoals3());
        goals.addAll(getGoals4());
        goals.addAll(getGoals5());

        addIdAndConversionLevelToGoals(goals);

        return goals;
    }

    private List<Goal> getGoalsWithoutConversionVisitsCount() {
        return List.of(
                (Goal) new Goal()
                        .withId(51L)
                        .withMetrikaCounterGoalType(MetrikaCounterGoalType.ACTION)
                        .withConversionLevel(ConversionLevel.PERFECT),
                (Goal) new Goal()
                        .withId(41L)
                        .withMetrikaCounterGoalType(MetrikaCounterGoalType.ECOMMERCE),
                (Goal) new Goal()
                        .withId(31L)
                        .withMetrikaCounterGoalType(MetrikaCounterGoalType.MESSENGER),
                (Goal) new Goal()
                        .withId(21L)
                        .withMetrikaCounterGoalType(MetrikaCounterGoalType.URL)
                        .withConversionLevel(ConversionLevel.GOOD));
    }
}
