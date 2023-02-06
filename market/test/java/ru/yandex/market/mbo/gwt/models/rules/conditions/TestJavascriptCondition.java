package ru.yandex.market.mbo.gwt.models.rules.conditions;

import org.junit.Test;
import ru.yandex.market.mbo.gwt.models.rules.EvalJavascriptException;

/**
 * @author gilmulla
 */
public class TestJavascriptCondition extends TestConditionBase {

    private static final int NUMERIC3 = 3;
    private static final int NUMERIC10 = 10;

    @Test
    public void testJavascriptConditionSuccess1() {
        tester
            .startModel()
                .id(1).category(1)
                .param("num1").setNumeric(1)
                .param("num2").setNumeric(2)
            .endModel()
            .startRuleSet()
                .id(1)
                .startRule()
                    .name("Rule 1").group("Test")
                    ._if()
                        .param("num1").javascript("return true;")
                    .then()
                        .param("num2").matchesNumeric(NUMERIC3)
                .endRule()
            .endRuleSet()
            .doInference()
            .results()
                .count(1)
                .iterationCount(2)
                .param("num2").numeric(NUMERIC3).endParam()
            .endResults();
    }

    @Test
    public void testJavascriptConditionSuccess2() {
        tester
            .startModel()
                .id(1).category(1)
                .param("num1").setNumeric(1)
                .param("num2").setNumeric(2)
            .endModel()
            .startRuleSet()
                .id(1)
                .startRule()
                    .name("Rule 1").group("Test")
                    ._if()
                        .param("num1").javascript("return 'true';")
                    .then()
                        .param("num2").matchesNumeric(NUMERIC3)
                .endRule()
            .endRuleSet()
            .doInference()
            .results()
                .count(1)
                .iterationCount(2)
                .param("num2").numeric(NUMERIC3).endParam()
            .endResults();
    }

    @Test
    public void testJavascriptConditionSuccess3() {
        tester
            .startModel()
                .id(1).category(1)
                .param("num1").setNumeric(NUMERIC10)
                .param("num2").setNumeric(NUMERIC10)
                .param("num3").setNumeric(2)
            .endModel()
            .startRuleSet()
                .id(1)
                .startRule()
                    .name("Rule 1").group("Test")
                    ._if()
                        .param("num1").javascript("return val('num1') === val('num2');")
                    .then()
                        .param("num3").matchesNumeric(NUMERIC3)
                .endRule()
            .endRuleSet()
            .doInference()
            .results()
                .count(1)
                .iterationCount(2)
                .param("num3").numeric(NUMERIC3).endParam()
            .endResults();
    }

    @Test
    public void testJavascriptConditionSuccess4() {
        tester
            .startModel()
                .id(1).category(1)
                .param("str1").setString("abcd")
                .param("num3").setNumeric(2)
            .endModel()
            .startRuleSet()
                .id(1)
                .startRule()
                    .name("Rule 1").group("Test")
                    ._if()
                        .param("str1").javascript("return val('str1')[0] === 'abcd';")
                    .then()
                        .param("num3").matchesNumeric(NUMERIC3)
                .endRule()
            .endRuleSet()
            .doInference()
            .results()
                .count(1)
                .iterationCount(2)
                .param("num3").numeric(NUMERIC3).endParam()
            .endResults();
    }

    @Test
    public void testJavascriptConditionSuccess5() {
        tester
            .startModel()
                .id(1).category(1)
                .param("str1").setString("ab")
                .param("str1").setString("cd")
                .param("num3").setNumeric(2)
            .endModel()
            .startRuleSet()
                .id(1)
                .startRule()
                    .name("Rule 1").group("Test")
                    ._if()
                        .param("str1").javascript("var str1 = val('str1'); return str1[0] == 'ab' && str1[1] == 'cd';")
                    .then()
                        .param("num3").matchesNumeric(NUMERIC3)
                .endRule()
            .endRuleSet()
            .doInference()
            .results()
                .count(1)
                .iterationCount(2)
                .param("num3").numeric(NUMERIC3).endParam()
            .endResults();
    }

    @Test
    public void testJavascriptConditionFail1() {
        tester
            .startModel()
                .id(1).category(1)
                .param("str1").setString("abcd")
                .param("num3").setNumeric(2)
            .endModel()
            .startRuleSet()
                .id(1)
                .startRule()
                    .name("Rule 1").group("Test")
                    ._if()
                        .param("str1").javascript("return false")
                    .then()
                        .param("num3").matchesNumeric(NUMERIC3)
                .endRule()
            .endRuleSet()
            .doInference()
            .results()
                .empty()
                .iterationCount(1)
            .endResults();
    }

    @Test
    public void testJavascriptConditionFail2() {
        tester
            .startModel()
                .id(1).category(1)
                .param("str1").setString("abcd")
                .param("num3").setNumeric(2)
            .endModel()
            .startRuleSet()
                .id(1)
                .startRule()
                    .name("Rule 1").group("Test")
                    ._if()
                        .param("str1").javascript("return 'false'")
                    .then()
                        .param("num3").matchesNumeric(NUMERIC3)
                .endRule()
            .endRuleSet()
            .doInference()
            .results()
                .empty()
                .iterationCount(1)
            .endResults();
    }

    @Test
    public void testJavascriptConditionFail3() {
        tester
            .startModel()
                .id(1).category(1)
                .param("str1").setString("abcd")
                .param("num3").setNumeric(2)
            .endModel()
            .startRuleSet()
                .id(1)
                .startRule()
                    .name("Rule 1").group("Test")
                    ._if()
                        .param("str1").javascript("return val('str1')[0] === 'abcde';")
                    .then()
                        .param("num3").matchesNumeric(NUMERIC3)
                .endRule()
            .endRuleSet()
            .doInference()
            .results()
                .empty()
                .iterationCount(1)
            .endResults();
    }

    @Test(expected = EvalJavascriptException.class)
    public void testJavascriptConditionFail4() {
        tester
            .startModel()
                .id(1).category(1)
                .param("num1").setNumeric(1)
                .param("num1").setNumeric(2)
                .param("num2").setNumeric(NUMERIC3)
            .endModel()
            .startRuleSet()
                .id(1)
                .startRule()
                    .name("Rule 1").group("Test")
                    ._if()
                        .param("num1").javascript("return val('num1').length;")
                    .then()
                        .param("num2").matchesNumeric(NUMERIC3)
                .endRule()
            .endRuleSet()
            .doInference()
            .results()
                .empty()
                .iterationCount(1)
            .endResults();
    }
}
