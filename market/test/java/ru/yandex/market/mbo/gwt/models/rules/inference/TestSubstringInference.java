package ru.yandex.market.mbo.gwt.models.rules.inference;

import org.junit.Test;

/**
 * @author gilmulla
 */
public class TestSubstringInference extends TestInferenceBase {
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
                        .param("str2").substring("cde")
                .endRule()
            .endRuleSet()
            .doInference()
            .results()
                .count(1)
                .iterationCount(2)
                .param("str2").valid().shouldNull().stringDomain().substring("cde").endDomain().endParam()
            .endResults();
    }

    @Test
    public void testSingleSubstringOnValidSingleString() {
        tester
            .startModel()
                .id(1).category(1)
                .param("str1").setString("abc")
                .param("str2").setString("abcde1")
            .endModel()
            .startRuleSet()
                .id(1)
                .startRule()
                    .name("Rule 1").group("Test")
                    ._if()
                        .param("str1").matchesString("abc")
                    .then()
                        .param("str2").substring("cde")
                .endRule()
            .endRuleSet()
            .doInference()
            .results()
                .count(1)
                .iterationCount(2)
                .param("str2").valid().shouldNull().stringDomain().substring("cde").endDomain().endParam()
            .endResults();
    }

    @Test
    public void testSubstringOnValidMultipleString() {
        tester
            .startModel()
                .id(1).category(1)
                .param("str1").setString("abc")
                .param("str2").setString("abcde", "fhty")
            .endModel()
            .startRuleSet()
                .id(1)
                .startRule()
                    .name("Rule 1").group("Test")
                    ._if()
                        .param("str1").matchesString("abc")
                    .then()
                        .param("str2").substring("ht")
                .endRule()
            .endRuleSet()
            .doInference()
            .results()
                .count(1)
                .iterationCount(2)
                .param("str2").valid().shouldNull().stringDomain().substring("ht").endDomain().endParam()
            .endResults();
    }

    @Test
    public void testSubstringOnInvalidSingleString() {
        tester
            .startModel()
                .id(1).category(1)
                .param("str1").setString("abc")
                .param("str2").setString("abcde")
            .endModel()
            .startRuleSet()
                .id(1)
                .startRule()
                    .name("Rule 1").group("Test")
                    ._if()
                        .param("str1").matchesString("abc")
                    .then()
                        .param("str2").substring("rtf")
                .endRule()
            .endRuleSet()
            .doInference()
            .results()
                .count(1)
                .iterationCount(2)
                .param("str2").invalid().shouldNull().stringDomain().substring("rtf").endDomain().endParam()
            .endResults();
    }

    @Test
    public void testSubstringOnInvalidMultipleString() {
        tester
            .startModel()
                .id(1).category(1)
                .param("str1").setString("abc")
                .param("str2").setString("abcde", "fhty")
            .endModel()
            .startRuleSet()
                .id(1)
                .startRule()
                    .name("Rule 1").group("Test")
                    ._if()
                        .param("str1").matchesString("abc")
                    .then()
                        .param("str2").substring("rtf")
                .endRule()
            .endRuleSet()
            .doInference()
            .results()
                .count(1)
                .iterationCount(2)
                .param("str2").invalid().shouldNull().stringDomain().substring("rtf").endDomain().endParam()
            .endResults();
    }

    @Test
    public void testOnEnum1() {
        tester
            .startModel()
                .id(1).category(1)
                .param("enum1").setOption(1)
                .param("enum2").setEmpty()
            .endModel()
            .startRuleSet()
                .id(1)
                .startRule()
                    .name("Rule 1").group("Test")
                    ._if()
                        .param("enum1").substring("Opt")
                    .then()
                        .param("enum2").substring("Opt")
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
    public void testOnEnum2() {
        tester
            .startModel()
                .id(1).category(1)
                .param("enum1").setOption(1)
                .param("enum2").setOption(2)
            .endModel()
            .startRuleSet()
                .id(1)
                .startRule()
                    .name("Rule 1").group("Test")
                    ._if()
                        .param("enum1").substring("Opt")
                    .then()
                        .param("enum2").substring("Option2")
                .endRule()
            .endRuleSet()
            .doInference()
            .results()
                .count(1)
                .iterationCount(2)
                .param("enum2").valid().optionId(2L).enumDomain().options(2L).endDomain().endParam()
            .endResults();
    }
}
