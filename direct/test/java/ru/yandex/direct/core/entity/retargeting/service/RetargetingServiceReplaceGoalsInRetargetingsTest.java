package ru.yandex.direct.core.entity.retargeting.service;

import java.util.Collections;
import java.util.List;

import one.util.streamex.StreamEx;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.retargeting.container.ReplaceRetargetingConditionGoal;
import ru.yandex.direct.core.entity.retargeting.model.Goal;
import ru.yandex.direct.core.entity.retargeting.model.RetargetingCondition;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.RetConditionInfo;
import ru.yandex.direct.core.testing.steps.RetConditionSteps;
import ru.yandex.direct.core.testing.steps.RetargetingGoalsSteps;
import ru.yandex.direct.result.Result;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static ru.yandex.direct.core.testing.data.TestFullGoals.defaultGoals;
import static ru.yandex.direct.multitype.entity.LimitOffset.maxLimited;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class RetargetingServiceReplaceGoalsInRetargetingsTest extends BaseRetargetingConditionServiceTest {
    private static final Long UNEXISTED_GOAL_ID = 111111L;

    @Autowired
    private RetConditionSteps retConditionSteps;

    @Autowired
    private RetargetingGoalsSteps retargetingGoalsSteps;
    private RetConditionInfo retargetingCondition;
    private List<Goal> goals;

    public void before() {
        super.before();
        goals = defaultGoals();
        retargetingCondition = retConditionSteps.createDefaultRetCondition(goals, clientInfo);
    }

    @Test
    public void replaceGoalsInRetargetings_OneGoalReplace() {
        retargetingGoalsSteps.createMetrikaGoalsInPpcDict(goals);
        List<Long> expectedGoalsIds = getGoalsIds();
        retargetingConditionOperationFactory.replaceGoalsInRetargetings(clientId,
                Collections.singletonList(new ReplaceRetargetingConditionGoal()
                        .withOldGoalId(goals.get(0).getId())
                        .withNewGoalId(goals.get(1).getId())
                ));

        expectedGoalsIds.replaceAll(x -> x.equals(goals.get(0).getId()) ? goals.get(1).getId() : x);
        check(expectedGoalsIds);
    }

    @Test
    public void replaceGoalsInRetargetings_ReplaceDeletedFromMetrikaGoal() {
        retargetingGoalsSteps.createMetrikaGoalsInPpcDict(goals.subList(0, goals.size() - 1));
        List<Long> expectedGoalsIds = getGoalsIds();
        retargetingConditionOperationFactory.replaceGoalsInRetargetings(clientId,
                Collections.singletonList(new ReplaceRetargetingConditionGoal()
                        .withOldGoalId(goals.get(2).getId())
                        .withNewGoalId(goals.get(1).getId())
                ));

        expectedGoalsIds.replaceAll(x -> x.equals(goals.get(2).getId()) ? goals.get(1).getId() : x);
        check(expectedGoalsIds);

    }

    @Test
    public void replaceGoalsInRetargetings_ReplaceUnexistedNewGoalId() {
        retargetingGoalsSteps.createMetrikaGoalsInPpcDict(goals);
        Result result = retargetingConditionOperationFactory.replaceGoalsInRetargetings(clientId,
                Collections.singletonList(new ReplaceRetargetingConditionGoal()
                        .withOldGoalId(goals.get(1).getId())
                        .withNewGoalId(UNEXISTED_GOAL_ID)
                ));

        assertThat("Ошибка соответствует ожиданиям", result.isSuccessful(), equalTo(false));

    }


    private void check(List<Long> expectedGoalsIds) {
        assertThat("Идентификаторы целей изменились в соответствии с ожиданиями", getGoalsIds(),
                hasItems(expectedGoalsIds.toArray(new Long[]{})));
    }

    private List<Long> getGoalsIds() {
        List<RetargetingCondition> actualRetargetingConditions =
                retConditionRepository.getFromRetargetingConditionsTable(shard, clientId, maxLimited());
        return StreamEx.of(actualRetargetingConditions)
                .flatCollection(RetargetingCondition::collectGoals)
                .map(Goal::getId)
                .toList();
    }
}
