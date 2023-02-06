package ru.yandex.market.mbo.gwt.models.rules.complex;

import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.mbo.gwt.models.params.Param;
import ru.yandex.market.mbo.gwt.models.rules.ModelRuleTester;

import static ru.yandex.market.mbo.gwt.models.rules.ModelRuleTester.testCase;

/**
 * @author gilmulla
 */
@SuppressWarnings("checkstyle:magicNumber")
public class TestRulesChaining {
    private ModelRuleTester tester;

    @Before
    public void prepareTestCase() {
        tester = testCase()
            .startParameters()
                .startParameter()
                    .xsl("param1").type(Param.Type.NUMERIC)
                .endParameter()
                .startParameter()
                    .xsl("param2").type(Param.Type.NUMERIC)
                .endParameter()
                .startParameter()
                    .xsl("param3").type(Param.Type.NUMERIC)
                .endParameter()
                .startParameter()
                    .xsl("param4").type(Param.Type.NUMERIC)
                .endParameter()
            .endParameters();
    }

    @Test
    public void testTwoRulesChaining() {
        tester
        .startModel()
            .id(1).category(1)
            .param("param1").setNumeric(1)
            .param("param2").setNumeric(2)
            .param("param3").setNumeric(3)
        .endModel()
        .startRuleSet()
            .id(1)
            .startRule()
                .name("First range").group("Test")
                ._if()
                    .param("param1").matchesNumeric(1)
                .then()
                    .param("param2").matchesNumeric(4)
            .endRule()
            .startRule()
                .name("param2").group("Test")
                ._if()
                    .param("param2").matchesNumeric(4)
                .then()
                    .param("param3").matchesNumeric(5)
            .endRule()
        .endRuleSet()
        .doInference()
        .results()
            .count(2)
            .iterationCount(2)
            .param("param2").valid().modified().numeric(4).numericDomain().single(4).endDomain().endParam()
            .param("param3").valid().modified().numeric(5).numericDomain().single(5).endDomain().endParam()
        .endResults();
    }

    @Test
    public void testTwoRulesChainingBackOrder() {
        tester
        .startModel()
            .id(1).category(1)
            .param("param1").setNumeric(1)
            .param("param2").setNumeric(2)
            .param("param3").setNumeric(3)
        .endModel()
        .startRuleSet()
            .id(1)
            .startRule()
                .name("param2").group("Test")
                ._if()
                    .param("param2").matchesNumeric(4)
                .then()
                    .param("param3").matchesNumeric(5)
            .endRule()
            .startRule()
                .name("First range").group("Test")
                ._if()
                    .param("param1").matchesNumeric(1)
                .then()
                    .param("param2").matchesNumeric(4)
            .endRule()
        .endRuleSet()
        .doInference()
        .results()
            .count(2)
            .iterationCount(3)
            .param("param2").valid().modified().numeric(4).numericDomain().single(4).endDomain().endParam()
            .param("param3").valid().modified().numeric(5).numericDomain().single(5).endDomain().endParam()
        .endResults();
    }

    @Test
    public void testThreeRulesChaining() {
        tester
        .startModel()
            .id(1).category(1)
            .param("param1").setNumeric(1)
            .param("param2").setNumeric(2)
            .param("param3").setNumeric(3)
            .param("param4").setNumeric(4)
        .endModel()
        .startRuleSet()
            .id(1)
            .startRule()
                .name("First range").group("Test")
                ._if()
                    .param("param1").matchesNumeric(1)
                .then()
                    .param("param2").matchesNumeric(4)
            .endRule()
            .startRule()
                .name("param2").group("Test")
                ._if()
                    .param("param2").matchesNumeric(4)
                .then()
                    .param("param3").matchesNumeric(5)
            .endRule()
            .startRule()
                .name("param2").group("Test")
                ._if()
                    .param("param3").matchesNumeric(5)
                .then()
                    .param("param4").matchesNumeric(6)
            .endRule()
        .endRuleSet()
        .doInference()
        .results()
            .count(3)
            .iterationCount(2)
            .param("param2").valid().modified().numeric(4).numericDomain().single(4).endDomain().endParam()
            .param("param3").valid().modified().numeric(5).numericDomain().single(5).endDomain().endParam()
            .param("param4").valid().modified().numeric(6).numericDomain().single(6).endDomain().endParam()
        .endResults();
    }

    @Test
    public void testThreeRulesChainingBackOrder() {
        tester
        .startModel()
            .id(1).category(1)
            .param("param1").setNumeric(1)
            .param("param2").setNumeric(2)
            .param("param3").setNumeric(3)
            .param("param4").setNumeric(4)
        .endModel()
        .startRuleSet()
            .id(1)
            .startRule()
                .name("param2").group("Test")
                ._if()
                    .param("param3").matchesNumeric(5)
                .then()
                    .param("param4").matchesNumeric(6)
            .endRule()
            .startRule()
                .name("param2").group("Test")
                ._if()
                    .param("param2").matchesNumeric(4)
                .then()
                    .param("param3").matchesNumeric(5)
            .endRule()
            .startRule()
                .name("First range").group("Test")
                ._if()
                    .param("param1").matchesNumeric(1)
                .then()
                    .param("param2").matchesNumeric(4)
            .endRule()
        .endRuleSet()
        .doInference()
        .results()
            .count(3)
            .iterationCount(4)
            .param("param2").valid().modified().numeric(4).numericDomain().single(4).endDomain().endParam()
            .param("param3").valid().modified().numeric(5).numericDomain().single(5).endDomain().endParam()
            .param("param4").valid().modified().numeric(6).numericDomain().single(6).endDomain().endParam()
        .endResults();
    }

    @Test
    public void testNoNPE() {
        tester
        .startModel()
            .id(1).category(1)
            .param("param1").setNumeric(1)
            .param("param2").setNumeric(2)
            .param("param3").setNumeric(3)
        .endModel()
        .startRuleSet()
            .id(1)
            .startRule()
                .name("First range").group("Test")
                ._if()
                    .param("param1").matchesNumeric(1)
                .then()
                    .param("param3").matchesNumeric(4)
            .endRule()
            .startRule()
                .name("param2").group("Test")
                ._if()
                    .param("param2").matchesNumeric(2)
                .then()
                    .param("param1").matchesNumeric(5)
            .endRule()
        .endRuleSet()
        .doInference()
        .results()
            .count(2)
            .iterationCount(2)
            .param("param1").valid().modified().numeric(5).numericDomain().single(5).endDomain().endParam()
            .param("param3").valid().modified().numeric(4).numericDomain().single(4).endDomain().endParam()
        .endResults();
    }
}
