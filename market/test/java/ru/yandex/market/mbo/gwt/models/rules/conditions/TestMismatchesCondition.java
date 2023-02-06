package ru.yandex.market.mbo.gwt.models.rules.conditions;

import org.junit.Test;

/**
 * @author gilmulla
 */
public class TestMismatchesCondition extends TestConditionBase {

    private static final long OPTION_ID3 = 3L;
    private static final int NUMERIC3 = 3;

    @Test
    public void testMismatchNumericConditionSuccess1() {
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
                        .param("num1").mismatchesNumeric(2)
                    .then()
                        .param("num2").matchesNumeric(NUMERIC3)
                .endRule()
            .endRuleSet()
            .doInference()
            .results()
                .count(1)
                .param("num2").numeric(NUMERIC3).endParam()
            .endResults();
    }

    @Test
    public void testMismatchNumericConditionSuccess2() {
        tester
            .startModel()
                .id(1).category(1)
                .param("num1").setEmpty()
                .param("num2").setNumeric(2)
            .endModel()
            .startRuleSet()
                .id(1)
                .startRule()
                    .name("Rule 1").group("Test")
                    ._if()
                        .param("num1").mismatchesNumeric(2)
                    .then()
                        .param("num2").matchesNumeric(NUMERIC3)
                .endRule()
            .endRuleSet()
            .doInference()
            .results()
                .count(1)
                .param("num2").numeric(NUMERIC3).endParam()
            .endResults();
    }

    @Test
    public void testMismatchMultiNumericConditionSuccess() {
        tester
            .startModel()
                .id(1).category(1)
                .param("num1").setNumeric(1)
                .param("num1").setNumeric(NUMERIC3)
                .param("num2").setNumeric(2)
            .endModel()
            .startRuleSet()
                .id(1)
                .startRule()
                    .name("Rule 1").group("Test")
                    ._if()
                        .param("num1").mismatchesNumeric(2)
                    .then()
                        .param("num2").matchesNumeric(NUMERIC3)
                .endRule()
            .endRuleSet()
            .doInference()
            .results()
                .count(1)
                .param("num2").numeric(NUMERIC3).endParam()
            .endResults();
    }

    @Test
    public void testMismatchNumericConditionFail() {
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
                        .param("num1").mismatchesNumeric(1)
                    .then()
                        .param("num2").matchesNumeric(NUMERIC3)
                .endRule()
            .endRuleSet()
            .doInference()
            .results()
                .empty()
            .endResults();
    }

    @Test
    public void testMismatchMultiNumericConditionFail() {
        tester
            .startModel()
                .id(1).category(1)
                .param("num1").setNumeric(1)
                .param("num1").setNumeric(NUMERIC3)
                .param("num2").setNumeric(2)
            .endModel()
            .startRuleSet()
                .id(1)
                .startRule()
                    .name("Rule 1").group("Test")
                    ._if()
                        .param("num1").mismatchesNumeric(1)
                    .then()
                        .param("num2").matchesNumeric(NUMERIC3)
                .endRule()
            .endRuleSet()
            .doInference()
            .results()
                .empty()
            .endResults();
    }

    @Test
    public void testMismatchStringConditionSuccess() {
        tester
            .startModel()
                .id(1).category(1)
                .param("str1").setString("1")
                .param("num2").setNumeric(2)
            .endModel()
            .startRuleSet()
                .id(1)
                .startRule()
                    .name("Rule 1").group("Test")
                    ._if()
                        .param("str1").mismatchesString("2")
                    .then()
                        .param("num2").matchesNumeric(NUMERIC3)
                .endRule()
            .endRuleSet()
            .doInference()
            .results()
                .count(1)
                .param("num2").numeric(NUMERIC3).endParam()
            .endResults();
    }

    @Test
    public void testMismatchMultiStringConditionSuccess() {
        tester
            .startModel()
                .id(1).category(1)
                .param("str1").setString("ab", "bc")
                .param("str1").setString("cd")
                .param("str2").setString("f")
            .endModel()
            .startRuleSet()
                .id(1)
                .startRule()
                    .name("Rule 1").group("Test")
                    ._if()
                        .param("str1").mismatchesString("dd")
                    .then()
                        .param("str2").matchesString("g")
                .endRule()
            .endRuleSet()
            .doInference()
            .results()
                .param("str2").string("g").endParam()
            .endResults();
    }

    @Test
    public void testMismatchStringConditionFail() {
        tester
            .startModel()
                .id(1).category(1)
                .param("str1").setString("1")
                .param("num2").setNumeric(2)
            .endModel()
            .startRuleSet()
                .id(1)
                .startRule()
                    .name("Rule 1").group("Test")
                    ._if()
                        .param("str1").mismatchesString("1")
                    .then()
                        .param("num2").matchesNumeric(NUMERIC3)
                .endRule()
            .endRuleSet()
            .doInference()
            .results()
                .empty()
            .endResults();
    }

    @Test
    public void testMismatchMultiStringConditionFail() {
        tester
            .startModel()
                .id(1).category(1)
                .param("str1").setString("1")
                .param("str1").setString("2")
                .param("str1").setString("3", "4")
                .param("num2").setNumeric(2)
            .endModel()
            .startRuleSet()
                .id(1)
                .startRule()
                    .name("Rule 1").group("Test")
                    ._if()
                        .param("str1").mismatchesString("1")
                    .then()
                        .param("num2").matchesNumeric(NUMERIC3)
                .endRule()
            .endRuleSet()
            .doInference()
            .results()
                .empty()
            .endResults();
    }

    @Test
    public void testMismatchOptionConditionSuccess1() {
        tester
            .startModel()
                .id(1).category(1)
                .param("enum1").setOption(1)
                .param("num2").setNumeric(2)
            .endModel()
            .startRuleSet()
                .id(1)
                .startRule()
                    .name("Rule 1").group("Test")
                    ._if()
                        .param("enum1").mismatchesEnum(2L)
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
    public void testMismatchMultiOptionConditionSuccess1() {
        tester
            .startModel()
                .id(1).category(1)
                .param("enum1").setOption(1)
                .param("enum1").setOption(2)
                .param("num2").setNumeric(2)
            .endModel()
            .startRuleSet()
                .id(1)
                .startRule()
                    .name("Rule 1").group("Test")
                    ._if()
                        .param("enum1").mismatchesEnum(OPTION_ID3)
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
    public void testMismatchOptionConditionSuccess2() {
        tester
            .startModel()
                .id(1).category(1)
                .param("enum1").setOption(1)
                .param("num2").setNumeric(2)
            .endModel()
            .startRuleSet()
                .id(1)
                .startRule()
                    .name("Rule 1").group("Test")
                    ._if()
                        .param("enum1").mismatchesEnum(2L, OPTION_ID3)
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
    public void testMismatchOptionConditionFail1() {
        tester
            .startModel()
                .id(1).category(1)
                .param("enum1").setOption(1)
                .param("num2").setNumeric(2)
            .endModel()
            .startRuleSet()
                .id(1)
                .startRule()
                    .name("Rule 1").group("Test")
                    ._if()
                        .param("enum1").mismatchesEnum(1L)
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

    @Test
    public void testMismatchMultiOptionConditionFail() {
        tester
            .startModel()
                .id(1).category(1)
                .param("enum1").setOption(1)
                .param("enum1").setOption(2)
                .param("num2").setNumeric(2)
            .endModel()
            .startRuleSet()
                .id(1)
                .startRule()
                    .name("Rule 1").group("Test")
                    ._if()
                        .param("enum1").mismatchesEnum(1L)
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

    @Test
    public void testMismatchOptionConditionFail2() {
        tester
            .startModel()
                .id(1).category(1)
                .param("enum1").setOption(1)
                .param("num2").setNumeric(2)
            .endModel()
            .startRuleSet()
                .id(1)
                .startRule()
                    .name("Rule 1").group("Test")
                    ._if()
                        .param("enum1").mismatchesEnum(1L, 2L)
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
