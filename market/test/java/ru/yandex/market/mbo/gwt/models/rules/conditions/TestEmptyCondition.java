package ru.yandex.market.mbo.gwt.models.rules.conditions;

import org.junit.Test;

/**
 * @author gilmulla
 */
public class TestEmptyCondition extends TestConditionBase {

    private static final int NUMERIC_VALUE = 3;

    @Test
    public void testEmptyConditionSuccess1() {
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
                        .param("num1").isEmpty()
                    .then()
                        .param("num2").matchesNumeric(NUMERIC_VALUE)
                .endRule()
            .endRuleSet()
            .doInference()
            .results()
                .count(1)
                .param("num2").numeric(NUMERIC_VALUE).endParam()
            .endResults();
    }

    @Test
    public void testEmptyConditionSuccess2() {
        tester
            .startModel()
                .id(1).category(1)
                .param("enum1").setEmpty()
                .param("num2").setNumeric(2)
            .endModel()
            .startRuleSet()
                .id(1)
                .startRule()
                    .name("Rule 1").group("Test")
                    ._if()
                        .param("enum1").isEmpty()
                    .then()
                        .param("num2").matchesNumeric(NUMERIC_VALUE)
                .endRule()
            .endRuleSet()
            .doInference()
            .results()
                .count(1)
                .iterationCount(2)
                .param("num2").numeric(NUMERIC_VALUE).endParam()
            .endResults();
    }

    @Test
    public void testEmptyMultiConditionSuccess3() {
        tester
            .startModel()
                .id(1).category(1)
                .param("enum1").setEmpty()
                .param("enum1").setEmpty()
                .param("num2").setNumeric(2)
            .endModel()
            .startRuleSet()
                .id(1)
                .startRule()
                    .name("Rule 1").group("Test")
                    ._if()
                        .param("enum1").isEmpty()
                    .then()
                        .param("num2").matchesNumeric(NUMERIC_VALUE)
                .endRule()
            .endRuleSet()
            .doInference()
            .results()
                .count(1)
                .iterationCount(2)
                .param("num2").numeric(NUMERIC_VALUE).endParam()
            .endResults();
    }

    @Test
    public void testEmptyConditionFail() {
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
                        .param("num1").isEmpty()
                    .then()
                        .param("num2").matchesNumeric(NUMERIC_VALUE)
                .endRule()
            .endRuleSet()
            .doInference()
            .results()
                .iterationCount(1)
                .empty()
            .endResults();
    }

    @Test
    public void testEmptyMultiConditionFail() {
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
                        .param("num1").isEmpty()
                    .then()
                        .param("num2").matchesNumeric(NUMERIC_VALUE)
                .endRule()
            .endRuleSet()
            .doInference()
            .results()
                .iterationCount(1)
                .empty()
            .endResults();
    }
}
