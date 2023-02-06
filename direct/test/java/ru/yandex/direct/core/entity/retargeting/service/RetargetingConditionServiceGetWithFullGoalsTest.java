package ru.yandex.direct.core.entity.retargeting.service;

import java.util.Collection;
import java.util.List;

import one.util.streamex.StreamEx;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.CompareStrategy;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.direct.core.entity.retargeting.model.Goal;
import ru.yandex.direct.core.entity.retargeting.model.RetargetingCondition;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.RetConditionInfo;
import ru.yandex.direct.core.testing.steps.RetConditionSteps;
import ru.yandex.direct.core.testing.steps.RetargetingGoalsSteps;
import ru.yandex.direct.core.testing.stub.MetrikaClientStub;

import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.direct.core.testing.data.TestFullGoals.defaultAudience;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class RetargetingConditionServiceGetWithFullGoalsTest extends BaseRetargetingConditionServiceTest {
    @Autowired
    protected MetrikaClientStub metrikaClientStub;
    @Autowired
    private RetConditionSteps retConditionSteps;
    @Autowired
    private RetargetingGoalsSteps retargetingGoalsSteps;

    private List<Goal> goals;
    private RetConditionInfo retargetingCodition;

    @Override
    public void before() {
        super.before();
        goals = singletonList(defaultAudience());
        retargetingGoalsSteps.createMetrikaGoalsInPpcDict(goals);
        retargetingCodition = retConditionSteps.createDefaultRetCondition(
                goals,
                clientInfo
        );

        metrikaClientStub.addGoals(
                clientInfo.getUid(),
                StreamEx.of(goals).toSet()
        );
    }


    @Test
    public void getRetargetingConditionWitFullGoals() {
        CompareStrategy strategyForRetCond = DefaultCompareStrategies
                .allFieldsExcept(newPath("lastChangeTime"), newPath("rules"), newPath("available"));
        CompareStrategy strategyForGoals = DefaultCompareStrategies
                .allFieldsExcept(newPath("time"), newPath("owner"), newPath("counterId"), newPath(
                        "metrikaCounterGoalType"));

        Collection<Goal> expectedGoals = goals;

        List<RetargetingCondition> retargetingConditions = retargetingConditionService.getRetargetingConditionsWithFullGoals(
                singletonList(retargetingCodition.getRetConditionId()), clientId, null, null, null);

        assertThat("полученное условие соответсвует ожиданиям", retargetingConditions.get(0),
                beanDiffer(retargetingCodition.getRetCondition()).useCompareStrategy(strategyForRetCond));

        assertThat("Полученные цели соответствуют ожиданиям",
                retargetingConditions.get(0).collectGoals(),
                containsInAnyOrder(mapList(expectedGoals, expectedGoal ->
                        beanDiffer(expectedGoal).useCompareStrategy(strategyForGoals)))
        );
    }
}
