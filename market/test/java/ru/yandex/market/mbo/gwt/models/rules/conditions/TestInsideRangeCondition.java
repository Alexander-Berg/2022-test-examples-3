package ru.yandex.market.mbo.gwt.models.rules.conditions;

import org.junit.Test;

/**
 * @author gilmulla
 */
public class TestInsideRangeCondition extends TestConditionBase {

    private static final int NUMERIC3 = 3;
    private static final int NUMERIC5 = 5;
    private static final int NUMERIC8 = 8;
    private static final int NUMERIC9 = 9;
    private static final int NUMERIC10 = 10;
    private static final int NUMERIC11 = 11;
    private static final int NUMERIC12 = 12;
    private static final int NUMERIC15 = 15;

    @Test
    public void testInsideRangeConditionSuccess1() {
        tester
            .startModel()
                .id(1).category(1)
                .param("num1").setNumeric(NUMERIC10)
                .param("num2").setNumeric(2)
            .endModel()
            .startRuleSet()
                .id(1)
                .startRule()
                    .name("Rule 1").group("Test")
                    ._if()
                        .param("num1").insideRange(NUMERIC5, NUMERIC15)
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
    public void testInsideRangeConditionSuccess2() {
        tester
            .startModel()
                .id(1).category(1)
                .param("num1").setNumeric(NUMERIC10)
                .param("num2").setNumeric(2)
            .endModel()
            .startRuleSet()
                .id(1)
                .startRule()
                    .name("Rule 1").group("Test")
                    ._if()
                        .param("num1").insideRange(NUMERIC10, NUMERIC15)
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
    public void testInsideRangeConditionSuccess3() {
        tester
            .startModel()
                .id(1).category(1)
                .param("num1").setNumeric(NUMERIC10)
                .param("num2").setNumeric(2)
            .endModel()
            .startRuleSet()
                .id(1)
                .startRule()
                    .name("Rule 1").group("Test")
                    ._if()
                        .param("num1").insideRange(NUMERIC5, NUMERIC10)
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
    public void testInsideRangeMultiConditionSuccess1() {
        tester
            .startModel()
                .id(1).category(1)
                .param("num1").setNumeric(NUMERIC8)
                .param("num1").setNumeric(NUMERIC10)
                .param("num2").setNumeric(2)
            .endModel()
            .startRuleSet()
                .id(1)
                .startRule()
                    .name("Rule 1").group("Test")
                    ._if()
                        .param("num1").insideRange(NUMERIC5, NUMERIC15)
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
    public void testInsideRangeMultiConditionSuccess2() {
        tester
            .startModel()
                .id(1).category(1)
                .param("num1").setNumeric(NUMERIC10)
                .param("num1").setNumeric(NUMERIC15)
                .param("num2").setNumeric(2)
            .endModel()
            .startRuleSet()
                .id(1)
                .startRule()
                    .name("Rule 1").group("Test")
                    ._if()
                        .param("num1").insideRange(NUMERIC10, NUMERIC15)
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
    public void testInsideRangeMultiConditionSuccess3() {
        tester
            .startModel()
                .id(1).category(1)
                .param("num1").setNumeric(NUMERIC8)
                .param("num1").setNumeric(NUMERIC10)
                .param("num2").setNumeric(2)
            .endModel()
            .startRuleSet()
                .id(1)
                .startRule()
                    .name("Rule 1").group("Test")
                    ._if()
                        .param("num1").insideRange(NUMERIC5, NUMERIC10)
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
    public void testInsideRangeConditionFail1() {
        tester
            .startModel()
                .id(1).category(1)
                .param("num1").setNumeric(NUMERIC10)
                .param("num2").setNumeric(2)
            .endModel()
            .startRuleSet()
                .id(1)
                .startRule()
                    .name("Rule 1").group("Test")
                    ._if()
                        .param("num1").insideRange(NUMERIC11, NUMERIC15)
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
    public void testInsideRangeMultiConditionFail1() {
        tester
            .startModel()
                .id(1).category(1)
                .param("num1").setNumeric(NUMERIC12)
                .param("num1").setNumeric(NUMERIC10)
                .param("num2").setNumeric(2)
            .endModel()
            .startRuleSet()
                .id(1)
                .startRule()
                    .name("Rule 1").group("Test")
                    ._if()
                        .param("num1").insideRange(NUMERIC11, NUMERIC15)
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
    public void testInsideRangeConditionFail2() {
        tester
            .startModel()
                .id(1).category(1)
                .param("num1").setNumeric(NUMERIC10)
                .param("num2").setNumeric(2)
            .endModel()
            .startRuleSet()
                .id(1)
                .startRule()
                    .name("Rule 1").group("Test")
                    ._if()
                        .param("num1").insideRange(1, NUMERIC9)
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
    public void testInsideRangeMultiConditionFail2() {
        tester
            .startModel()
                .id(1).category(1)
                .param("num1").setNumeric(NUMERIC8)
                .param("num1").setNumeric(NUMERIC10)
                .param("num2").setNumeric(2)
            .endModel()
            .startRuleSet()
                .id(1)
                .startRule()
                    .name("Rule 1").group("Test")
                    ._if()
                        .param("num1").insideRange(1, NUMERIC9)
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
    public void testInsideRangeConditionFail3() {
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
                        .param("num1").insideRange(1, NUMERIC9)
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
