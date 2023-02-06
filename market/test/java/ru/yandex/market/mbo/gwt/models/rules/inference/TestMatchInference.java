package ru.yandex.market.mbo.gwt.models.rules.inference;

import org.junit.Test;

/**
 * @author gilmulla
 */
public class TestMatchInference extends TestInferenceBase {
    @Test
    public void testOnEmptyNumeric() {
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
                        .param("num2").matchesNumeric(2)
                .endRule()
            .endRuleSet()
            .doInference()
            .results()
                .count(1)
                .param("num2").valid().modified().numeric(2).numericDomain().single(2).endDomain().endParam()
            .endResults();
    }

    @Test
    public void testOnValidNumeric() {
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
                        .param("num2").matchesNumeric(2)
                .endRule()
            .endRuleSet()
            .doInference()
            .results()
                .count(1)
                .param("num2").valid().numeric(2).numericDomain().single(2).endDomain().endParam()
            .endResults();
    }

    @Test
    public void testOnInvalidNumeric() {
        final int num2Val = 4;
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
                        .param("num2").matchesNumeric(2)
                .endRule()
            .endRuleSet()
            .doInference()
            .results()
                .count(1)
                .param("num2").valid().modified().numeric(2).numericDomain().single(2).endDomain().endParam()
            .endResults();
    }

    @Test
    public void testOnEmptyString() {
        tester
            .startModel()
                .id(1).category(1)
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
                        .param("str2").matchesString("cde")
                .endRule()
            .endRuleSet()
            .doInference()
            .results()
                .count(1)
                .param("str2").valid().modified().string("cde").stringDomain().match("cde").endDomain().endParam()
            .endResults();
    }

    @Test
    public void testOnValidString() {
        tester
            .startModel()
                .id(1).category(1)
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
                        .param("str2").matchesString("cde")
                .endRule()
            .endRuleSet()
            .doInference()
            .results()
                .count(1)
                .param("str2").valid().string("cde").stringDomain().match("cde").endDomain().endParam()
            .endResults();
    }

    @Test
    public void testOnInvalidString() {
        tester
            .startModel()
                .id(1).category(1)
                .param("str1").setString("abc")
                .param("str2").setString("rtd")
            .endModel()
            .startRuleSet()
                .id(1)
                .startRule()
                    .name("Rule 1").group("Test")
                    ._if()
                        .param("str1").matchesString("abc")
                    .then()
                        .param("str2").matchesString("cde")
                .endRule()
            .endRuleSet()
            .doInference()
            .results()
                .count(1)
                .param("str2").valid().modified().string("cde").stringDomain().match("cde").endDomain().endParam()
            .endResults();
    }

    @Test
    public void testOnEmptyEnum() {
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
                        .param("enum2").matchesEnum(1L, 2L)
                .endRule()
            .endRuleSet()
            .doInference()
            .results()
                .count(1)
                .iterationCount(2)
                .param("enum2").valid().shouldNull().enumDomain().options(1L, 2L).endDomain().endParam()
            .endResults();
    }

    @Test
    public void testOnValidEnum() {
        tester
            .startModel()
                .id(1).category(1)
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
                        .param("enum2").matchesEnum(1L, 2L)
                .endRule()
            .endRuleSet()
            .doInference()
            .results()
                .count(1)
                .iterationCount(2)
                .param("enum2").valid().shouldNull().enumDomain().options(1L, 2L).endDomain().endParam()
            .endResults();
    }

    @Test
    public void testOnInvalidEnum() {
        final long enum2Val = 3L;
        tester
            .startModel()
                .id(1).category(1)
                .param("enum1").setOption(1L)
                .param("enum2").setOption(enum2Val)
            .endModel()
            .startRuleSet()
                .id(1)
                .startRule()
                    .name("Rule 1").group("Test")
                    ._if()
                        .param("enum1").matchesEnum(1L)
                    .then()
                        .param("enum2").matchesEnum(1L, 2L)
                .endRule()
            .endRuleSet()
            .doInference()
            .results()
                .count(1)
                .iterationCount(2)
                .param("enum2").invalid().shouldNull().enumDomain().options(1L, 2L).endDomain().endParam()
            .endResults();
    }

    @Test
    public void testOnEmptyBoolean() {
        tester
            .startModel()
                .id(1).category(1)
                .param("bool1").setBoolean(true)
                .param("bool2").setEmpty()
            .endModel()
            .startRuleSet()
                .id(1)
                .startRule()
                    .name("Rule 1").group("Test")
                    ._if()
                        .param("bool1").matchesBoolean(true)
                    .then()
                        .param("bool2").matchesBoolean(true)
                .endRule()
            .endRuleSet()
            .doInference()
            .results()
                .count(1)
                .iterationCount(2)
                .param("bool2").valid().modified().bool(true).enumDomain().options(1L).endDomain().endParam()
            .endResults();
    }

    @Test
    public void testOnValidBoolean() {
        tester
            .startModel()
                .id(1).category(1)
                .param("bool1").setBoolean(true)
                .param("bool2").setBoolean(true)
            .endModel()
            .startRuleSet()
                .id(1)
                .startRule()
                    .name("Rule 1").group("Test")
                    ._if()
                        .param("bool1").matchesBoolean(true)
                    .then()
                        .param("bool2").matchesBoolean(true)
                .endRule()
            .endRuleSet()
            .doInference()
            .results()
                .count(1)
                .iterationCount(2)
                .param("bool2").valid().bool(true).enumDomain().options(1L).endDomain().endParam()
            .endResults();
    }

    @Test
    public void testOnInvalidBoolean() {
        tester
            .startModel()
                .id(1).category(1)
                .param("bool1").setBoolean(true)
                .param("bool2").setBoolean(true)
            .endModel()
            .startRuleSet()
                .id(1)
                .startRule()
                    .name("Rule 1").group("Test")
                    ._if()
                        .param("bool1").matchesBoolean(true)
                    .then()
                        .param("bool2").matchesBoolean(false)
                .endRule()
            .endRuleSet()
            .doInference()
            .results()
                .count(1)
                .iterationCount(2)
                .param("bool2").valid().modified().bool(false).enumDomain().options(2L).endDomain().endParam()
            .endResults();
    }
}
