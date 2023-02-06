package ru.yandex.market.mbo.gwt.models.rules.conditions;

import org.junit.Test;

public class TestMatchesCondition extends TestConditionBase {

    private static final long OPTION_ID3 = 3L;
    private static final int NUMERIC3 = 3;

    @Test
    public void testMatchNumericConditionSuccess() {
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
                        .param("num1").matchesNumeric(1)
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
    public void testMatchMultiNumericConditionSuccess() {
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
                        .param("num1").matchesNumeric(2)
                    .then()
                    .param("num2").matchesNumeric(2)
                .endRule()
            .endRuleSet()
            .doInference()
            .results()
                .param("num2").numeric(2).endParam()
            .endResults();
    }

    @Test
    public void testMatchNumericConditionFail() {
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
                        .param("num1").matchesNumeric(NUMERIC3)
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
    public void testMatchStringConditionSuccess() {
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
                        .param("str1").matchesString("1")
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
    public void testMatchMultiStringConditionSuccess() {
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
                        .param("str1").matchesString("cd")
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
    public void testMatchStringConditionFail() {
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
                        .param("str1").matchesString("2")
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
    public void testMatchMultiStringConditionFail() {
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
                        .param("str1").matchesString("dd")
                    .then()
                        .param("str2").matchesString("g")
                .endRule()
            .endRuleSet()
            .doInference()
            .results()
                .empty()
            .endResults();
    }

    @Test
    public void testMatchOptionConditionSuccess1() {
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
                        .param("enum1").matchesEnum(1L)
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
    public void testMatchOptionConditionSuccess2() {
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
                        .param("enum1").matchesEnum(1L, 2L)
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
    public void testMatchMultiOptionConditionSuccess() {
        tester
            .startModel()
                .id(1).category(1)
                .param("enum1").setOption(1)
                .param("enum1").setOption(2)
                .param("enum1").setOption(OPTION_ID3)
                .param("num1").setNumeric(2)
            .endModel()
            .startRuleSet()
                .id(1)
                .startRule()
                    .name("Rule 1").group("Test")
                    ._if()
                        .param("enum1").matchesEnum(1L, 2L)
                    .then()
                        .param("num1").matchesNumeric(1)
                .endRule()
            .endRuleSet()
            .doInference()
            .results()
                .param("num1").numeric(1)
            .endParam()
            .endResults();
    }

    @Test
    public void testMatchOptionConditionCurrentMultiSuccess() {
        tester
            .startModel()
                .id(1).category(1)
                .param("enum1").setOption(1)
                .param("num1").setNumeric(2)
                .param("num1").setNumeric(NUMERIC3)
            .endModel()
            .startRuleSet()
                .id(1)
                .startRule()
                    .name("Rule 1").group("Test")
                    ._if()
                        .param("enum1").matchesEnum(1L)
                    .then()
                        .param("num1").matchesNumeric(1)
                .endRule()
            .endRuleSet()
            .doInference()
            .results()
                .param("num1").numeric(1)
            .endParam()
            .endResults();
    }

    @Test
    public void testMatchOptionConditionFail1() {
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
                        .param("enum1").matchesEnum(2L)
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
    public void testMatchOptionConditionFail2() {
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
                        .param("enum1").matchesEnum(2L, OPTION_ID3)
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
