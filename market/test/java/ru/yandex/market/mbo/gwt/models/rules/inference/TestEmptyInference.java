package ru.yandex.market.mbo.gwt.models.rules.inference;

import org.junit.Test;
import ru.yandex.market.mbo.gwt.models.rules.EnumDomain;
import ru.yandex.market.mbo.gwt.models.rules.StringDomain;

/**
 * @author gilmulla
 *
 */
@SuppressWarnings("checkstyle:magicNumber")
public class TestEmptyInference extends TestInferenceBase {

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
                        .param("num2").isEmpty()
                .endRule()
            .endRuleSet()
            .doInference()
            .results()
                .count(1)
                .iterationCount(2)
                .param("num2").valid().isEmpty().numericDomain().singleEmpty().endDomain().endParam()
            .endResults();
    }

    @Test
    public void testOnNotEmptyNumeric() {
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
                        .param("num2").isEmpty()
                .endRule()
            .endRuleSet()
            .doInference()
            .results()
                .count(1)
                .iterationCount(2)
                .param("num2")
                .valid().modified().isEmpty()
                .numericDomain()
                    .singleEmpty()
                .endDomain()
                .endParam()
            .endResults();
    }

    @Test
    public void testOnEmptyString() {
        tester
            .startModel()
                .id(1).category(1)
                .param("str1").setString("aaa")
                .param("str2").setEmpty()
            .endModel()
            .startRuleSet()
                .id(1)
                .startRule()
                    .name("Rule 1").group("Test")
                    ._if()
                        .param("str1").matchesString("aaa")
                    .then()
                        .param("str2").isEmpty()
                .endRule()
            .endRuleSet()
            .doInference()
            .results()
                .count(1)
                .iterationCount(2)
                .param("str2").valid().isEmpty().stringDomain().match(StringDomain.EMPTY).endDomain().endParam()
            .endResults();
    }

    @Test
    public void testOnNotEmptyString() {
        tester
            .startModel()
                .id(1).category(1)
                .param("str1").setString("aaa")
                .param("str2").setString("bbb")
            .endModel()
            .startRuleSet()
                .id(1)
                .startRule()
                    .name("Rule 1").group("Test")
                    ._if()
                        .param("str1").matchesString("aaa")
                    .then()
                        .param("str2").isEmpty()
                .endRule()
            .endRuleSet()
            .doInference()
            .results()
                .count(1)
                .iterationCount(2)
                .param("str2").valid().modified().isEmpty()
                .stringDomain()
                    .match(StringDomain.EMPTY)
                .endDomain()
                .endParam()
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
                        .param("enum2").isEmpty()
                .endRule()
            .endRuleSet()
            .doInference()
            .results()
                .count(1)
                .iterationCount(2)
                .param("enum2").valid().isEmpty().enumDomain().options(EnumDomain.EMPTY).endDomain().endParam()
            .endResults();
    }

    @Test
    public void testOnNotEmptyEnum() {
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
                        .param("enum2").isEmpty()
                .endRule()
            .endRuleSet()
            .doInference()
            .results()
                .count(1)
                .iterationCount(2)
                .param("enum2").valid().modified().isEmpty()
                .enumDomain()
                    .options(EnumDomain.EMPTY)
                .endDomain()
            .endParam()
            .endResults();
    }
}
