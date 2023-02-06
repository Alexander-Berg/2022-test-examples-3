package ru.yandex.market.mbo.gwt.models.rules.inference;

import org.junit.Test;

/**
 * @author gilmulla
 */
public class TestInsideRangeInference extends TestInferenceBase {

    private static final int NUMERIC10 = 10;
    private static final int NUMERIC20 = 20;

    @Test
    public void testRangeOnEmptyNumeric() {
        tester
            .startModel()
                .id(1).category(1)
                .param("num1").setNumeric(1)
                .param("num2").setEmpty()
            .endModel()
            .startRuleSet()
                .id(1)
                .startRule()
                    .name("Rule 1").group("Test")
                    ._if()
                        .param("num1").matchesNumeric(1)
                    .then()
                        .param("num2").insideRange(NUMERIC10, NUMERIC20)
                .endRule()
            .endRuleSet()
            .doInference()
            .results()
                .count(1)
                .iterationCount(2)
                .param("num2").valid().shouldNull().numericDomain().range(NUMERIC10, NUMERIC20).endDomain().endParam()
            .endResults();
    }

    @Test
    public void testSingleOnEmptyNumeric() {
        tester
            .startModel()
                .id(1).category(1)
                .param("num1").setNumeric(1)
                .param("num2").setEmpty()
            .endModel()
            .startRuleSet()
                .id(1)
                .startRule()
                    .name("Rule 1").group("Test")
                    ._if()
                        .param("num1").matchesNumeric(1)
                    .then()
                        .param("num2").insideRange(NUMERIC10, NUMERIC10)
                .endRule()
            .endRuleSet()
            .doInference()
            .results()
                .count(1)
                .iterationCount(2)
                .param("num2").valid().modified()
                    .numeric(NUMERIC10).numericDomain().single(NUMERIC10).endDomain().endParam()
            .endResults();
    }

    @Test
    public void testRangeOnValidNumeric() {
        final int num2Val = 15;
        tester
            .startModel()
                .id(1).category(1)
                .param("num1").setNumeric(1)
                .param("num2").setNumeric(num2Val)
            .endModel()
            .startRuleSet()
                .id(1)
                .startRule()
                    .name("Rule 1").group("Test")
                    ._if()
                        .param("num1").matchesNumeric(1)
                    .then()
                        .param("num2").insideRange(NUMERIC10, NUMERIC20)
                .endRule()
            .endRuleSet()
            .doInference()
            .results()
                .count(1)
                .iterationCount(2)
                .param("num2").valid().shouldNull().numericDomain().range(NUMERIC10, NUMERIC20).endDomain().endParam()
            .endResults();
    }

    @Test
    public void testRangeOnInvalidNumeric() {
        final int num2Val = 30;
        tester
            .startModel()
                .id(1).category(1)
                .param("num1").setNumeric(1)
                .param("num2").setNumeric(num2Val)
            .endModel()
            .startRuleSet()
                .id(1)
                .startRule()
                    .name("Rule 1").group("Test")
                    ._if()
                        .param("num1").matchesNumeric(1)
                    .then()
                        .param("num2").insideRange(NUMERIC10, NUMERIC20)
                .endRule()
            .endRuleSet()
            .doInference()
            .results()
                .count(1)
                .iterationCount(2)
                .param("num2").invalid().shouldNull().numericDomain().range(NUMERIC10, NUMERIC20).endDomain().endParam()
            .endResults();
    }

    @Test
    public void testSingleOnEqualNumeric() {
        tester
            .startModel()
                .id(1).category(1)
                .param("num1").setNumeric(1)
                .param("num2").setNumeric(NUMERIC10)
            .endModel()
            .startRuleSet()
                .id(1)
                .startRule()
                    .name("Rule 1").group("Test")
                    ._if()
                        .param("num1").matchesNumeric(1)
                    .then()
                        .param("num2").insideRange(NUMERIC10, NUMERIC10)
                .endRule()
            .endRuleSet()
            .doInference()
            .results()
                .count(1)
                .iterationCount(2)
                .param("num2").valid().numeric(NUMERIC10).numericDomain().single(NUMERIC10).endDomain().endParam()
            .endResults();
    }
}
