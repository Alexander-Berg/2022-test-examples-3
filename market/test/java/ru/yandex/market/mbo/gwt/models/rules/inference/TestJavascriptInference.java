package ru.yandex.market.mbo.gwt.models.rules.inference;

import org.junit.Test;
import ru.yandex.market.mbo.gwt.models.rules.ModelRuleException;

/**
 * @author gilmulla
 */
public class TestJavascriptInference extends TestInferenceBase {

    private static final int NUMERIC10 = 10;
    private static final int NUMERIC20 = 20;

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
                        .param("num2").javascript("return 10")
                .endRule()
            .endRuleSet()
            .doInference()
            .results()
                .count(1)
                .iterationCount(2)
                .param("num2").valid().modified()
                    .numeric(NUMERIC10).numericDomain().single(NUMERIC10).endDomain()
                .endParam()
            .endResults();
    }

    @Test
    public void testOnValidNumeric() {
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
                        .param("num2").javascript("return 10")
                .endRule()
            .endRuleSet()
            .doInference()
            .results()
                .count(1)
                .iterationCount(2)
                .param("num2").valid().numeric(NUMERIC10).numericDomain().single(NUMERIC10).endDomain().endParam()
            .endResults();
    }

    @Test
    public void testOnInvalidNumeric() {
        tester
            .startModel()
                .id(1).category(1)
                .param("num1").setNumeric(1)
                .param("num2").setNumeric(NUMERIC20)
            .endModel()
            .startRuleSet()
                .id(1)
                .startRule()
                    .name("Rule 1").group("Test")
                    ._if()
                        .param("num1").matchesNumeric(1)
                    .then()
                        .param("num2").javascript("return 10")
                .endRule()
            .endRuleSet()
            .doInference()
            .results()
                .count(1)
                .iterationCount(2)
                .param("num2").valid().modified().numeric(NUMERIC10)
                    .numericDomain().single(NUMERIC10).endDomain()
                .endParam()
            .endResults();
    }

    @Test
    public void testOnEmptyStringSingleString() {
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
                        .param("str2").javascript("return 'cba'")
                .endRule()
            .endRuleSet()
            .doInference()
            .results()
                .count(1)
                .iterationCount(2)
                .param("str2").valid().modified().string("cba").stringDomain().match("cba").endDomain().endParam()
            .endResults();
    }

    @Test
    public void testOnEmptyStringStringArray() {
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
                        .param("str2").javascript("return ['cba', 'cde', 'abc']")
                .endRule()
            .endRuleSet()
            .doInference()
            .results()
                .count(1)
                .iterationCount(2)
                .param("str2").valid().modified().string("cba", "cde", "abc")
                    .stringDomain().match("cba", "cde", "abc").endDomain()
                .endParam()
            .endResults();
    }

    @Test
    public void testOnEmptyEnumAndValidRetValue() {
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
                        .param("enum2").javascript("return 'Option2'")
                .endRule()
            .endRuleSet()
            .doInference()
            .results()
                .count(1)
                .iterationCount(2)
                .param("enum2").valid().modified().optionId(2L).enumDomain().options(2L).endDomain().endParam()
            .endResults();
    }

    @Test(expected = ModelRuleException.class)
    public void testOnEmptyEnumAndEmptyRetValue() {
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
                        .param("enum2").javascript("return ''")
                .endRule()
            .endRuleSet()
            .doInference();
    }

    @Test(expected = ModelRuleException.class)
    public void testOnEmptyEnumAndIllegalRetValue() {
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
                        .param("enum2").javascript("return 'Not existing option'")
                .endRule()
            .endRuleSet()
            .doInference();
    }

    @Test
    public void testOnEmptyBooleanAndRetValueTrue1() {
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
                        .param("bool2").javascript("return true;")
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
    public void testOnEmptyBooleanAndRetValueTrue2() {
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
                        .param("bool2").javascript("return 'true';")
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
    public void testOnEmptyBooleanAndRetValueTrue3() {
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
                        .param("bool2").javascript("return 'True';")
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
    public void testOnEmptyBooleanAndRetValueFalse1() {
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
                        .param("bool2").javascript("return false;")
                .endRule()
            .endRuleSet()
            .doInference()
            .results()
                .count(1)
                .iterationCount(2)
                .param("bool2").valid().modified().bool(false).enumDomain().options(2L).endDomain().endParam()
            .endResults();
    }

    @Test
    public void testOnEmptyBooleanAndRetValueFalse2() {
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
                        .param("bool2").javascript("return 'false';")
                .endRule()
            .endRuleSet()
            .doInference()
            .results()
                .count(1)
                .iterationCount(2)
                .param("bool2").valid().modified().bool(false).enumDomain().options(2L).endDomain().endParam()
            .endResults();
    }

    @Test
    public void testOnEmptyBooleanAndRetValueFalse3() {
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
                        .param("bool2").javascript("return 'False';")
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
