package ru.yandex.direct.core.entity.retargeting.repository;

import java.util.Collections;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.direct.core.entity.retargeting.model.CryptaInterestType;
import ru.yandex.direct.core.entity.retargeting.model.Goal;
import ru.yandex.direct.core.entity.retargeting.model.GoalType;
import ru.yandex.direct.core.entity.retargeting.model.Rule;
import ru.yandex.direct.core.entity.retargeting.model.RuleType;
import ru.yandex.direct.core.testing.configuration.CoreTest;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.testing.data.TestRetargetingConditions.bigRules;

@CoreTest
public class RetargetingConditionMappingsTest {
    private String defaultJsonRules;

    @Before
    public void before() throws Exception {
        defaultJsonRules = IOUtils.toString(this.getClass().getResourceAsStream("defaultRules.json"), UTF_8);
    }

    @Test
    public void rulesToJson() {
        List<Rule> rules = bigRules();
        RetargetingConditionMappings.rulesToJson(rules);
    }

    @Test
    public void rulesToJsonGoalType() {
        Goal goal = new Goal();
        goal.withType(GoalType.GOAL)
                .withId(637665L)
                .withTime(50);
        Rule rule = new Rule();
        rule.withGoals(Collections.singletonList(goal)).withType(RuleType.ALL).withInterestType(CryptaInterestType.all);
        List<Rule> rules = Collections.singletonList(rule);
        assertThat("json rule соответствует ожиданиям", RetargetingConditionMappings.rulesToJson(rules),
                equalTo(defaultJsonRules));
    }

    @Test
    public void rulesFromJson() {
        Goal goal = new Goal();
        goal.withId(637665L)
                .withTime(50);
        Rule rule = new Rule();
        rule.withGoals(Collections.singletonList(goal)).withType(RuleType.ALL).withInterestType(CryptaInterestType.all);
        List<Rule> rules = Collections.singletonList(rule);

        List<Rule> actualRules = RetargetingConditionMappings.rulesFromJson(defaultJsonRules);
        assertThat("rule соответствует ожиданиям", rules, beanDiffer(actualRules).useCompareStrategy(
                DefaultCompareStrategies.onlyExpectedFields()));
    }

    @Test
    public void rulesFromJsonGoalType() throws Exception {
        String jsonRules = IOUtils.toString(this.getClass().getResourceAsStream("rules.json"), UTF_8);
        Goal goal = new Goal();
        goal.withType(GoalType.GOAL)
                .withTime(50);
        Rule rule = new Rule();
        rule.withGoals(Collections.singletonList(goal)).withType(RuleType.ALL);
        List<Rule> rules = Collections.singletonList(rule);

        List<Rule> actualRules = RetargetingConditionMappings.rulesFromJson(jsonRules);
        assertThat("rule соответствует ожиданиям", rules, beanDiffer(actualRules).useCompareStrategy(
                DefaultCompareStrategies.onlyExpectedFields()));
    }
}
