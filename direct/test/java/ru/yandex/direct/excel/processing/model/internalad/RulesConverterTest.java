package ru.yandex.direct.excel.processing.model.internalad;

import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.naming.TestCaseName;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import ru.yandex.direct.core.entity.retargeting.model.Goal;
import ru.yandex.direct.core.entity.retargeting.model.Rule;
import ru.yandex.direct.core.entity.retargeting.model.RuleType;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.direct.excel.processing.model.internalad.RulesConverter.convertRulesToString;
import static ru.yandex.direct.excel.processing.model.internalad.RulesConverter.convertStringToRules;

@RunWith(JUnitParamsRunner.class)
@ParametersAreNonnullByDefault
public class RulesConverterTest {

    @Before
    public void initTestData() {
    }

    public static Object[][] getParametersForConvertValidStringToRules() {
        return new Object[][]{
                {"(~20141023:6)&(20141021:6)&(20141024:78|20141026:456)",
                        List.of(
                                new Rule().withType(RuleType.NOT).withGoals(List.of(goal(20141023L, 6))),
                                new Rule().withType(RuleType.OR).withGoals(List.of(goal(20141021L, 6))),
                                new Rule().withGoals(List.of(goal(20141024L, 78), goal(20141026L, 456)))
                                        .withType(RuleType.OR))},
                {"(~20141023:6)", List.of(new Rule().withType(RuleType.NOT).withGoals(List.of(goal(20141023L, 6))))},
                {"(20141021:6)", List.of(new Rule().withType(RuleType.OR).withGoals(List.of(goal(20141021L, 6))))},
                {"(20141024:78|20141026:456)", List.of(new Rule().withGoals(List.of(goal(20141024L, 78),
                        goal(20141026L, 456)))
                        .withType(RuleType.OR))},
        };
    }

    public static Object[][] getParametersForConvertValidRulesToString() {
        return new Object[][]{
                {List.of(new Rule().withType(RuleType.NOT).withGoals(List.of(goal(20141023L, 6))),
                        new Rule().withType(RuleType.OR).withGoals(List.of(goal(20141021L, 6))),
                        new Rule().withGoals(List.of(goal(20141024L, 78), goal(20141026L, 456)))
                                .withType(RuleType.OR)), "(~20141023:6)&(20141021:6)&(20141024:78|20141026:456)"},
                {List.of(new Rule().withType(RuleType.NOT).withGoals(List.of(goal(20141023L, 6)))), "(~20141023:6)"},
                {List.of(new Rule().withType(RuleType.OR).withGoals(List.of(goal(20141021L, 6)))), "(20141021:6)"},
                {List.of(new Rule().withType(RuleType.OR).withGoals(List.of(goal(20141024L, 78),
                        goal(20141026L, 456)))), "(20141024:78|20141026:456)"},
                {List.of(), ""},
                {List.of(new Rule().withType(RuleType.ALL).withGoals(List.of(goal(20141023L, 6), goal(20141021L, 6)))),
                        "(20141023:6)&(20141021:6)"},
                {List.of(new Rule().withType(RuleType.ALL).withGoals(List.of(goal(20141021L, 6)))), "(20141021:6)"},
        };
    }

    @Test
    @TestCaseName("inputValue = {0}")
    @Parameters(method = "getParametersForConvertValidStringToRules")
    public void checkConvertValidStringToRules(String expression, List<Rule> expectedRules) {
        var rules = convertStringToRules(expression);
        assertThat(rules, equalTo(expectedRules));
    }

    @Test
    @TestCaseName("inputValue = {0}")
    @Parameters(method = "getParametersForConvertValidRulesToString")
    public void checkConvertRulesToString(List<Rule> rules, String expectedExpression) {
        var stringRules = convertRulesToString(rules);
        assertThat(stringRules, equalTo(expectedExpression));
    }

    private static Goal goal(Long goalId, Integer time) {
        return (Goal) new Goal().withId(goalId).withTime(time);
    }
}
