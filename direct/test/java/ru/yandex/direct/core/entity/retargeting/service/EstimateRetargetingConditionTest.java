package ru.yandex.direct.core.entity.retargeting.service;

import java.util.HashSet;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.retargeting.model.ConditionType;
import ru.yandex.direct.core.entity.retargeting.model.Goal;
import ru.yandex.direct.core.entity.retargeting.model.GoalType;
import ru.yandex.direct.core.entity.retargeting.model.RetargetingCondition;
import ru.yandex.direct.core.entity.retargeting.model.Rule;
import ru.yandex.direct.core.entity.retargeting.model.RuleType;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.RetConditionInfo;
import ru.yandex.direct.core.testing.steps.RetConditionSteps;
import ru.yandex.direct.core.testing.steps.RetargetingGoalsSteps;
import ru.yandex.direct.core.testing.stub.MetrikaClientStub;
import ru.yandex.direct.result.Result;

import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.direct.core.testing.data.TestFullGoals.defaultMetrikaGoals;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class EstimateRetargetingConditionTest extends BaseRetargetingConditionServiceTest {
    @Autowired
    protected MetrikaClientStub metrikaClientStub;
    @Autowired
    private RetConditionSteps retConditionSteps;
    @Autowired
    private RetargetingGoalsSteps retargetingGoalsSteps;

    private List<Goal> metrikaGoals;
    private RetConditionInfo retConditionInfo;

    @Override
    public void before() {
        super.before();
        metrikaGoals = defaultMetrikaGoals();
        retargetingGoalsSteps.createMetrikaGoalsInPpcDict(metrikaGoals);
        metrikaClientStub.addGoals(
                clientInfo.getUid(),
                new HashSet<>(metrikaGoals)
        );
        retConditionInfo = retConditionSteps.createDefaultRetCondition(
                metrikaGoals,
                clientInfo
        );
    }

    @Test
    public void retargetingConditionEstimate() {
        Result<Long> result =
                retargetingConditionService.estimateRetargetingCondition(clientId, retConditionInfo.getRetCondition());
        assertThat("Результат не содержит ошибок",
                result.getValidationResult(),
                hasNoDefectsDefinitions()
        );
    }

    @Test
    public void negativeRetargetingConditionSegmentEstimate() {
        RetargetingCondition valid = new RetargetingCondition();
        Goal goal = metrikaGoals.stream().filter(g -> g.getType() == GoalType.SEGMENT).findFirst().orElse(new Goal());
        Rule rule = new Rule();
        rule.withType(RuleType.NOT)
                .withGoals(singletonList(goal));
        valid.withType(ConditionType.metrika_goals).withName("asdfg1").withClientId(clientId.asLong())
                .withRules(singletonList(rule));
        Result<Long> est = retargetingConditionService.estimateRetargetingCondition(clientId, valid);
        assertThat("Результат не содержит ошибок",
                est.getValidationResult(),
                hasNoDefectsDefinitions()
        );
    }

}
