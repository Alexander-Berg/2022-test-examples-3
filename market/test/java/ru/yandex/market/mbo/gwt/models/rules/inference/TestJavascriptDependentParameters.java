package ru.yandex.market.mbo.gwt.models.rules.inference;

import org.junit.Test;

/**
 * @author gilmulla
 */
public class TestJavascriptDependentParameters extends TestInferenceBase {
    @Test
    public void testNumericDependency() {
        tester
            .startModel()
                .id(1).category(1)
                .param("num1").setNumeric(1)
                .param("num2").setNumeric(2)
                .param("num3").setEmpty()
            .endModel()
            .startRuleSet()
                .id(1)
                .startRule()
                    .name("Rule 1").group("Test")
                    ._if()
                        .param("num1").matchesNumeric(1)
                    .then()
                        .param("num3").javascript("return val('num2');")
                .endRule()
            .endRuleSet()
            .doInference()
            .results()
                .count(1)
                .iterationCount(2)
                .param("num3").valid().modified().numeric(2).numericDomain().single(2).endDomain().endParam()
            .endResults();
    }

    @Test
    public void testSingeStringDependency() {
        tester
            .startModel()
                .id(1).category(1)
                .param("str1").setString("abc")
                .param("str2").setString("def")
                .param("str3").setEmpty()
            .endModel()
            .startRuleSet()
                .id(1)
                .startRule()
                    .name("Rule 1").group("Test")
                    ._if()
                        .param("str1").matchesString("abc")
                    .then()
                        .param("str3").javascript("return val('str2');")
                .endRule()
            .endRuleSet()
            .doInference()
            .results()
                .count(1)
                .iterationCount(2)
                .param("str3").valid().modified().string("def").stringDomain().match("def").endDomain().endParam()
            .endResults();
    }

    @Test
    public void testStringArrayDependency() {
        tester
            .startModel()
                .id(1).category(1)
                .param("str1").setString("abc")
                .param("str2").setString("def", "ldc", "edc")
                .param("str3").setEmpty()
            .endModel()
            .startRuleSet()
                .id(1)
                .startRule()
                    .name("Rule 1").group("Test")
                    ._if()
                        .param("str1").matchesString("abc")
                    .then()
                        .param("str3").javascript("return val('str2');")
                .endRule()
            .endRuleSet()
            .doInference()
            .results()
                .count(1)
                .iterationCount(2)
                .param("str3").valid().modified().string("def", "ldc", "edc")
                    .stringDomain().match("def", "ldc", "edc").endDomain()
                .endParam()
            .endResults();
    }

    @Test
    public void testEnumDependency() {
        tester
            .startModel()
                .id(1).category(1)
                .param("enum1").setOption(1L)
                .param("enum2").setOption(1L)
                .param("enum3").setEmpty()
            .endModel()
            .startRuleSet()
                .id(1)
                .startRule()
                    .name("Rule 1").group("Test")
                    ._if()
                        .param("enum1").matchesEnum(1L, 2L)
                    .then()
                        .param("enum3").javascript("return val('enum2');")
                .endRule()
            .endRuleSet()
            .doInference()
            .results()
                .count(1)
                .iterationCount(2)
                .param("enum3").valid().modified().optionId(1L).enumDomain().options(1L).endDomain().endParam()
            .endResults();
    }

    @Test
    public void testBoolDependencyTrue() {
        tester
            .startModel()
                .id(1).category(1)
                .param("bool1").setBoolean(true)
                .param("bool2").setBoolean(true)
                .param("bool3").setEmpty()
            .endModel()
            .startRuleSet()
                .id(1)
                .startRule()
                    .name("Rule 1").group("Test")
                    ._if()
                        .param("bool1").matchesBoolean(true)
                    .then()
                        .param("bool3").javascript("return val('bool2');")
                .endRule()
            .endRuleSet()
            .doInference()
            .results()
                .count(1)
                .iterationCount(2)
                .param("bool3").valid().modified().bool(true).enumDomain().options(1L).endDomain().endParam()
            .endResults();
    }

    @Test
    public void testBoolDependencyFalse() {
        tester
            .startModel()
                .id(1).category(1)
                .param("bool1").setBoolean(true)
                .param("bool2").setBoolean(false)
                .param("bool3").setEmpty()
            .endModel()
            .startRuleSet()
                .id(1)
                .startRule()
                    .name("Rule 1").group("Test")
                    ._if()
                        .param("bool1").matchesBoolean(true)
                    .then()
                        .param("bool3").javascript("return val('bool2');")
                .endRule()
            .endRuleSet()
            .doInference()
            .results()
                .count(1)
                .iterationCount(2)
                .param("bool3").valid().modified().bool(false).enumDomain().options(2L).endDomain().endParam()
            .endResults();
    }
}
