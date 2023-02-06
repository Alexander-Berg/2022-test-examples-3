package ru.yandex.direct.core.entity.retargeting.repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.CompareStrategy;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.direct.core.entity.retargeting.model.Goal;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.data.TestFullGoals;
import ru.yandex.direct.core.testing.steps.RetargetingGoalsSteps;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.direct.core.testing.data.TestFullGoals.defaultGoals;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class RetargetingGoalsPpcDictRepositoryTest {
    @Autowired
    private RetargetingGoalsSteps retargetingGoalsSteps;

    @Autowired
    private RetargetingGoalsPpcDictRepository retargetingGoalsPpcDictRepository;
    private List<Goal> chiefGoals;
    private List<Goal> allGoals;

    @Before
    public void before() {
        chiefGoals = defaultGoals();
        Goal parentGoal = chiefGoals.get(0);
        Goal subGoal = (Goal) TestFullGoals.defaultGoalByType(parentGoal.getType())
                .withParentId(parentGoal.getId());

        allGoals = new ArrayList<>(chiefGoals);
        allGoals.add(subGoal);

        retargetingGoalsSteps.createMetrikaGoalsInPpcDict(allGoals);
    }

    @Test
    public void getMetrikaGoals_withoutSubgoals() {
        Collection<Goal> actualGoals = retargetingGoalsPpcDictRepository
                .getMetrikaGoalsFromPpcDict(
                        mapList(chiefGoals, Goal::getId),
                        false
                );


        CompareStrategy strategy = DefaultCompareStrategies.onlyFields(
                newPath("id"),
                newPath("goalName")
        );
        assertThat("Полученные цели соответствуют ожиданиям",
                actualGoals,
                containsInAnyOrder(mapList(chiefGoals, expectedGoal ->
                        beanDiffer(expectedGoal).useCompareStrategy(strategy))
                )
        );
    }

    @Test
    public void getMetrikaGoals_withSubgoals() {
        Collection<Goal> actualGoals = retargetingGoalsPpcDictRepository
                .getMetrikaGoalsFromPpcDict(
                        mapList(chiefGoals, Goal::getId),
                        true
                );


        CompareStrategy strategy = DefaultCompareStrategies.onlyFields(
                newPath("id"),
                newPath("goalName")
        );
        assertThat("Полученные цели соответствуют ожиданиям",
                actualGoals,
                containsInAnyOrder(mapList(allGoals, expectedGoal ->
                        beanDiffer(expectedGoal).useCompareStrategy(strategy))
                )
        );
    }

}
