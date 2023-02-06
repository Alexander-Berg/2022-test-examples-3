package ru.yandex.direct.core.entity.retargeting.service;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import one.util.streamex.StreamEx;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.CompareStrategy;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.direct.core.entity.retargeting.model.Goal;
import ru.yandex.direct.core.entity.retargeting.model.GoalType;
import ru.yandex.direct.core.entity.retargeting.service.common.GoalUtilsService;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.steps.RetConditionSteps;
import ru.yandex.direct.core.testing.steps.RetargetingGoalsSteps;
import ru.yandex.direct.core.testing.stub.MetrikaClientStub;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.direct.core.testing.data.TestFullGoals.defaultAudience;
import static ru.yandex.direct.core.testing.data.TestFullGoals.defaultGoals;
import static ru.yandex.direct.core.testing.data.TestFullGoals.defaultMetrikaGoals;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class RetargetingConditionServiceGetGoalsForRetargetingTest extends BaseRetargetingConditionServiceTest {
    @Autowired
    private RetConditionSteps retConditionSteps;

    @Autowired
    protected MetrikaClientStub metrikaClientStub;

    @Autowired
    private RetargetingGoalsSteps retargetingGoalsSteps;

    private List<Goal> goals;
    private List<Goal> goalsForMetrika;
    private List<Goal> goalsForPpcDict;

    private static final Long BIG_GOAL_ID = 999999L;

    @Test
    public void getGoalsForRetargetings_OneUsedGoalDeletedFromMetrika_DeletedGoalReturns() {
        goals = defaultMetrikaGoals();
        goals.get(goals.size() - 1).setAllowToUse(false);
        goalsForPpcDict = goals;

        Collection<Goal> expectedGoals = goals;
        goalsForMetrika = goals.subList(0, goals.size() - 1);

        checkGoals(expectedGoals);
    }

    @Test
    public void getGoalsForRetargetings_WithoutCripta() {
        goals = defaultGoals();
        goalsForPpcDict = goals.stream().filter(goal -> goal.getType().isMetrika()).collect(Collectors.toList());
        Collection<Goal> expectedGoals = goalsForPpcDict;
        goalsForMetrika = goalsForPpcDict;

        checkGoals(expectedGoals);
    }

    @Test
    public void getGoalsForRetargetings_OneGoal() {
        goals = defaultGoals(1);
        goalsForPpcDict = goals;
        Collection<Goal> expectedGoals = goals;
        goalsForMetrika = goals;

        checkGoals(expectedGoals);
    }

    @Test
    public void getGoalsForRetargetings_LotOfGoals() {
        goals = defaultGoals(10);
        goalsForPpcDict = goals;
        Collection<Goal> expectedGoals = goals;
        goalsForMetrika = goals;

        checkGoals(expectedGoals);
    }

    @Test
    public void getGoalsForRetargetings_AudienceWithSubtype() {
        goals = Collections.singletonList(defaultAudience());
        goalsForPpcDict = goals;
        Collection<Goal> expectedGoals = goals;
        goalsForMetrika = goals;

        checkGoals(expectedGoals);
    }

    @Test
    public void getGoalsForRetargetings_OneUsedGoalDeletedFromMetrikaAndNotSaveInPpcDict() {
        goals = defaultMetrikaGoals();
        Goal goal = new Goal();
        goal.withId(BIG_GOAL_ID)
                .withType(GoalType.GOAL)
                .withAllowToUse(false)
                .withName(null);
        goals.add(goal);
        goalsForPpcDict = goals.subList(0, goals.size() - 1);

        List<Goal> expectedGoals = goals;
        goalsForMetrika = goals.subList(0, goals.size() - 1);

        checkGoals(expectedGoals);
    }

    private void checkGoals(Collection<Goal> expectedGoals) {
        prepareData();
        List<Goal> actualGoals = retargetingConditionService.getMetrikaGoalsForRetargeting(clientId);
        actualGoals.forEach(g -> g.setStatus(null));

        expectedGoals = mapList(expectedGoals, GoalUtilsService::changeEcommerceGoalName);

        CompareStrategy strategy = DefaultCompareStrategies.allFieldsExcept(
                newPath("time"),
                newPath("owner"),
                newPath("counterId"),
                newPath("metrikaCounterGoalType"),
                newPath("conversionLevel"),
                newPath("percent"),
                newPath("allowToUse"));

        assertThat("Полученные цели соответствуют ожиданиям",
                actualGoals,
                containsInAnyOrder(mapList(expectedGoals, expectedGoal ->
                        beanDiffer(expectedGoal).useCompareStrategy(strategy)))
        );
    }

    private void prepareData() {
        retargetingGoalsSteps.createMetrikaGoalsInPpcDict(goalsForPpcDict);
        retConditionSteps.createDefaultRetCondition(
                goals,
                clientInfo
        );

        metrikaClientStub.addGoals(
                clientInfo.getUid(),
                StreamEx.of(goalsForMetrika).toSet()
        );
    }
}
