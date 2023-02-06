package ru.yandex.market.mbo.gwt.models.rules.inference;

import org.junit.Test;

/**
 * @author Alexander Kramarev (pochemuto@yandex-team.ru)
 * @date 21.05.2018
 */
@SuppressWarnings("checkstyle:magicNumber")
public class TestAddValuesInference extends TestInferenceBase {

    @Test
    public void testAddEnum() {
        tester
            .startModel()
                .id(1).category(1)
                .param("num1").setNumeric(1)
                .param("enum1").setOption(1)
            .endModel()
            .startRuleSet()
                .id(1)
                .startRule()
                .name("Rule 1").group("Test")
                    ._if()
                        .param("num1").matchesNumeric(1)
                    .then()
                        .param("enum1").addEnum(2L, 3L)
                .endRule()
            .endRuleSet()
            .doInference()
                .results()
                .count(1)
                .iterationCount(2)
                .param("enum1").valid().optionIds(1L, 2L, 3L).endParam()
            .endResults();
    }

    @Test
    public void testAddString() {
        tester
            .startModel()
                .id(1).category(1)
                .param("num1").setNumeric(1)
                .param("str1").setString("original-1", "original-2")
            .endModel()
            .startRuleSet()
                .id(1)
                .startRule()
                .name("Rule 1").group("Test")
                    ._if()
                        .param("num1").matchesNumeric(1)
                    .then()
                        .param("str1").addString("added-1")
                .endRule()
            .endRuleSet()
            .doInference()
                .results()
                .count(1)
                .iterationCount(2)
                .param("str1").valid().string("original-1", "original-2", "added-1").endParam()
            .endResults();
    }

}
