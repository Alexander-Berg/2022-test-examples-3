package ru.yandex.market.mbo.gwt.models.rules.conditions;

import org.junit.Test;

/**
 * @author gilmulla
 */
public class TestNotEmptyCondition extends TestConditionBase {

    private static final int NUMERIC3 = 3;

    @Test
    public void testNotEmptyConditionSuccess() {
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
                        .param("num1").isNotEmpty()
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
    public void testNotEmptyMultiConditionSuccess() {
        tester
            .startModel()
                .id(1).category(1)
                .param("num1").setEmpty()
                .param("num1").setNumeric(1)
                .param("num2").setNumeric(2)
            .endModel()
            .startRuleSet()
                .id(1)
                .startRule()
                    .name("Rule 1").group("Test")
                    ._if()
                        .param("num1").isNotEmpty()
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
    public void testNotEmptyConditionFail() {
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
                        .param("num1").isNotEmpty()
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
    public void testNotEmptyMultiConditionFail() {
        tester
            .startModel()
                .id(1).category(1)
                .param("num1").setEmpty()
                .param("num1").setEmpty()
                .param("num2").setNumeric(2)
            .endModel()
            .startRuleSet()
                .id(1)
                .startRule()
                    .name("Rule 1").group("Test")
                    ._if()
                        .param("num1").isNotEmpty()
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
