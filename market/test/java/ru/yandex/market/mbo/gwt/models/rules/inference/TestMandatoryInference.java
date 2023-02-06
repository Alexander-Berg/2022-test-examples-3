package ru.yandex.market.mbo.gwt.models.rules.inference;

import org.junit.Test;
import ru.yandex.market.mbo.gwt.utils.XslNames;

/**
 * @author gilmulla
 */
public class TestMandatoryInference extends TestInferenceBase {
    @Test
    public void testOnEmptyStringParameterAndNotSignedModel() {
        tester
            .startModel()
                .id(1).category(1)
                .param(XslNames.OPERATOR_SIGN).setBoolean(false)
                .param("str1").setString("abc")
                .param("str2").setEmpty()
            .endModel()
            .startRuleSet()
                .id(1)
                .startRule()
                    .name("Rule 1").group("Test")
                    ._if()
                        .param("str1").matchesString("abc")
                    .then()
                        .param("str2").mandatory()
                .endRule()
            .endRuleSet()
            .doInference()
            .results()
                .count(1)
                .iterationCount(2)
                .param("str2").valid().shouldNull().stringDomain().any().endDomain().endParam()
            .endResults();
    }

    @Test
    public void testOnEmptyStringParameterAndSignedModel() {
        tester
            .startModel()
                .id(1).category(1)
                .param(XslNames.OPERATOR_SIGN).setBoolean(true)
                .param("str1").setString("abc")
                .param("str2").setEmpty()
            .endModel()
            .startRuleSet()
                .id(1)
                .startRule()
                    .name("Rule 1").group("Test")
                    ._if()
                        .param("str1").matchesString("abc")
                    .then()
                        .param("str2").mandatory()
                .endRule()
            .endRuleSet()
            .doInference()
            .results()
                .count(1)
                .iterationCount(2)
                .param("str2").invalid().shouldNull().stringDomain().notContainsEmpty().endDomain().endParam()
            .endResults();
    }

    @Test
    public void testOnNotEmptyStringParameterAndSignedModel() {
        tester
            .startModel()
                .id(1).category(1)
                .param(XslNames.OPERATOR_SIGN).setBoolean(true)
                .param("str1").setString("abc")
                .param("str2").setString("cde")
            .endModel()
            .startRuleSet()
                .id(1)
                .startRule()
                    .name("Rule 1").group("Test")
                    ._if()
                        .param("str1").matchesString("abc")
                    .then()
                        .param("str2").mandatory()
                .endRule()
            .endRuleSet()
            .doInference()
            .results()
                .count(1)
                .iterationCount(2)
                .param("str2").valid().shouldNull().stringDomain().notContainsEmpty().endDomain().endParam()
            .endResults();
    }

    @Test
    public void testOnEmptyNumericParameterAndNotSignedModel() {
        tester
            .startModel()
                .id(1).category(1)
                .param(XslNames.OPERATOR_SIGN).setBoolean(false)
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
                        .param("num2").mandatory()
                .endRule()
            .endRuleSet()
            .doInference()
            .results()
                .count(1)
                .iterationCount(2)
                .param("num2").valid().shouldNull().numericDomain().any().endDomain().endParam()
            .endResults();
    }

    @Test
    public void testOnEmptyNumericParameterAndSignedModel() {
        tester
            .startModel()
                .id(1).category(1)
                .param(XslNames.OPERATOR_SIGN).setBoolean(true)
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
                        .param("num2").mandatory()
                .endRule()
            .endRuleSet()
            .doInference()
            .results()
                .count(1)
                .iterationCount(2)
                .param("num2").invalid().shouldNull().numericDomain().notContainsEmpty().endDomain().endParam()
            .endResults();
    }

    @Test
    public void testOnNotEmptyNumericParameterAndSignedModel() {
        tester
            .startModel()
                .id(1).category(1)
                .param(XslNames.OPERATOR_SIGN).setBoolean(true)
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
                        .param("num2").mandatory()
                .endRule()
            .endRuleSet()
            .doInference()
            .results()
                .count(1)
                .iterationCount(2)
                .param("num2").valid().shouldNull().numericDomain().notContainsEmpty().endDomain().endParam()
            .endResults();
    }

    @Test
    public void testOnEmptyEnumParameterAndNotSignedModel() {
        tester
            .startModel()
                .id(1).category(1)
                .param(XslNames.OPERATOR_SIGN).setBoolean(false)
                .param("enum1").setOption(1L)
                .param("enum2").setEmpty()
            .endModel()
            .startRuleSet()
                .id(1)
                .startRule()
                    .name("Rule 1").group("Test")
                    ._if()
                        .param("enum1").matchesEnum(1L)
                    .then()
                        .param("enum2").mandatory()
                .endRule()
            .endRuleSet()
            .doInference()
            .results()
                .count(1)
                .iterationCount(2)
                .param("enum2").valid().shouldNull().enumDomain().any().endDomain().endParam()
            .endResults();
    }

    @Test
    public void testOnEmptyEnumParameterAndNoSignedModel() {
        tester
            .startModel()
                .id(1).category(1)
                .param("enum1").setOption(1L)
                .param("enum2").setEmpty()
            .endModel()
            .startRuleSet()
                .id(1)
                .startRule()
                    .name("Rule 1").group("Test")
                    ._if()
                        .param("enum1").matchesEnum(1L)
                    .then()
                        .param("enum2").mandatory()
                .endRule()
            .endRuleSet()
            .doInference()
            .results()
                .count(1)
                .iterationCount(2)
                .param("enum2").valid().shouldNull().enumDomain().any().endDomain().endParam()
            .endResults();
    }

    @Test
    public void testOnEmptyEnumParameterAndSignedModel() {
        tester
            .startModel()
                .id(1).category(1)
                .param(XslNames.OPERATOR_SIGN).setBoolean(true)
                .param("enum1").setOption(1L)
                .param("enum2").setEmpty()
            .endModel()
            .startRuleSet()
                .id(1)
                .startRule()
                    .name("Rule 1").group("Test")
                    ._if()
                        .param("enum1").matchesEnum(1L)
                    .then()
                        .param("enum2").mandatory()
                .endRule()
            .endRuleSet()
            .doInference()
            .results()
                .count(1)
                .iterationCount(2)
                .param("enum2").invalid().shouldNull().enumDomain().notContainsEmpty().endDomain().endParam()
            .endResults();
    }

    @Test
    public void testOnNotEmptyEnumParameterAndSignedModel() {
        tester
            .startModel()
                .id(1).category(1)
                .param(XslNames.OPERATOR_SIGN).setBoolean(true)
                .param("enum1").setOption(1L)
                .param("enum2").setOption(2L)
            .endModel()
            .startRuleSet()
                .id(1)
                .startRule()
                    .name("Rule 1").group("Test")
                    ._if()
                        .param("enum1").matchesEnum(1L)
                    .then()
                        .param("enum2").mandatory()
                .endRule()
            .endRuleSet()
            .doInference()
            .results()
                .count(1)
                .iterationCount(2)
                .param("enum2").valid().shouldNull().enumDomain().notContainsEmpty().endDomain().endParam()
            .endResults();
    }
}
