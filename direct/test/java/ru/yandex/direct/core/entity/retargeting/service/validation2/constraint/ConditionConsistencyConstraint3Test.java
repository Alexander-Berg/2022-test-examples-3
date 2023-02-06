package ru.yandex.direct.core.entity.retargeting.service.validation2.constraint;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Test;

import ru.yandex.direct.core.entity.retargeting.model.Goal;
import ru.yandex.direct.core.entity.retargeting.model.RetargetingCondition;
import ru.yandex.direct.core.entity.retargeting.model.Rule;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.entity.retargeting.model.Goal.CDP_SEGMENT_UPPER_BOUND;
import static ru.yandex.direct.core.entity.retargeting.model.Goal.METRIKA_AUDIENCE_UPPER_BOUND;
import static ru.yandex.direct.core.entity.retargeting.model.Goal.METRIKA_ECOMMERCE_UPPER_BOUND;
import static ru.yandex.direct.core.entity.retargeting.model.Goal.METRIKA_SEGMENT_UPPER_BOUND;
import static ru.yandex.direct.core.entity.retargeting.model.Goal.MOBILE_GOAL_UPPER_BOUND;
import static ru.yandex.direct.core.entity.retargeting.model.RuleType.ALL;
import static ru.yandex.direct.core.entity.retargeting.model.RuleType.NOT;
import static ru.yandex.direct.core.entity.retargeting.service.validation2.RetargetingDefects.invalidGoalsForType;
import static ru.yandex.direct.core.entity.retargeting.service.validation2.constraint.ConditionConsistencyConstraint3.conditionIsConsistent;

@ParametersAreNonnullByDefault
public class ConditionConsistencyConstraint3Test {

    @Test
    public void testNullRetargetingCondition() {
        var defect = conditionIsConsistent().apply(null);

        assertThat(defect).isNull();
    }

    @Test
    public void testRetargetingConditionHasNoRules() {
        var condition = (RetargetingCondition) new RetargetingCondition()
                .withRules(null); // Just to be clear
        var defect = conditionIsConsistent().apply(condition);

        assertThat(defect).isNull();
    }

    @Test
    public void testRetargetingConditionHasEmptyRules() {
        var condition = (RetargetingCondition) new RetargetingCondition()
                .withRules(List.of());
        var defect = conditionIsConsistent().apply(condition);

        assertThat(defect).isNull();
    }

    @Test
    public void testRetargetingConditionContainsNullInRules() {
        List<Rule> badRules = new ArrayList<>();
        badRules.add(null);
        var condition = (RetargetingCondition) new RetargetingCondition()
                .withRules(badRules);

        var defect = conditionIsConsistent().apply(condition);

        assertThat(defect).isNull();
    }

    @Test
    public void testRetargetingConditionContainsPositiveRule() {
        var rule = new Rule().withType(ALL);
        var condition = (RetargetingCondition) new RetargetingCondition()
                .withRules(List.of(rule));

        var defect = conditionIsConsistent().apply(condition);

        assertThat(defect).isNull();
    }

    @Test
    public void testRetargetingConditionContainsNullGoalsInRules() {
        List<Goal> badGoals = new ArrayList<>();
        badGoals.add(null);
        var rule = new Rule().withType(NOT).withGoals(badGoals);
        var condition = (RetargetingCondition) new RetargetingCondition()
                .withRules(List.of(rule));

        var defect = conditionIsConsistent().apply(condition);

        assertThat(defect).isNull();
    }

    @Test
    public void testGoalOfWrongType() {
        var goalOfWrongType = (Goal) new Goal()
                .withId(2_500_000_001L); // AB_SEGMENT
        var ruleWithGoalOfWrongType = new Rule().withType(NOT).withGoals(List.of(goalOfWrongType));
        var conditionWithRuleWithGoalOfWrongType = (RetargetingCondition) new RetargetingCondition()
                .withRules(List.of(ruleWithGoalOfWrongType));

        var defect = conditionIsConsistent().apply(conditionWithRuleWithGoalOfWrongType);

        assertThat(defect).isEqualTo(invalidGoalsForType());
    }

    @Test
    public void testCorrectRetargetingCondition() {
        var goal = (Goal) new Goal()
                .withId(1L); // GOAL
        var rule = new Rule().withType(NOT).withGoals(List.of(goal));
        var conditionWithRuleWithGoalOfWrongType = (RetargetingCondition) new RetargetingCondition()
                .withRules(List.of(rule));

        var defect = conditionIsConsistent().apply(conditionWithRuleWithGoalOfWrongType);

        assertThat(defect).isNull();
    }

    @Test
    public void testCorrectRetargetingConditionWithSeveralGoals() {
        var goal1 = (Goal) new Goal()
                .withId(1L); // GOAL
        var goal2 = (Goal) new Goal()
                .withId(METRIKA_SEGMENT_UPPER_BOUND - 1); // SEGMENT
        var goal3 = (Goal) new Goal()
                .withId(METRIKA_AUDIENCE_UPPER_BOUND - 1); // AUDIENCE
        var goal4 = (Goal) new Goal()
                .withId(CDP_SEGMENT_UPPER_BOUND - 1); // CDP_SEGMENT
        var goal5 = (Goal) new Goal()
                .withId(METRIKA_ECOMMERCE_UPPER_BOUND - 1); // ECOMMERCE
        var goal6 = (Goal) new Goal()
                .withId(MOBILE_GOAL_UPPER_BOUND - 1); // MOBILE
        var rule = new Rule().withType(NOT).withGoals(List.of(goal1, goal2, goal3, goal4, goal5, goal6));
        var conditionWithRuleWithGoalOfWrongType = (RetargetingCondition) new RetargetingCondition()
                .withRules(List.of(rule));

        var defect = conditionIsConsistent().apply(conditionWithRuleWithGoalOfWrongType);

        assertThat(defect).isNull();
    }

    @Test
    public void tesFullOfNulls() {
        var goal = (Goal) new Goal().withId(1L); // GOAL
        List<Goal> goals = new ArrayList<>();
        goals.add(goal);
        goals.add(null);
        var rule = new Rule().withType(NOT).withGoals(goals);
        List<Rule> rules = new ArrayList<>();
        rules.add(rule);
        rules.add(null);
        var conditionWithRuleWithGoalOfWrongType = (RetargetingCondition) new RetargetingCondition().withRules(rules);

        var defect = conditionIsConsistent().apply(conditionWithRuleWithGoalOfWrongType);

        assertThat(defect).isNull();
    }
}
