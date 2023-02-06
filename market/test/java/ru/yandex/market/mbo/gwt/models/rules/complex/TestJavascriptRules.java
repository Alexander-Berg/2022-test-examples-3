package ru.yandex.market.mbo.gwt.models.rules.complex;

import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.mbo.gwt.models.rules.EvalJavascriptException;
import ru.yandex.market.mbo.gwt.models.rules.conditions.TestConditionBase;

import static org.hamcrest.Matchers.containsString;

/**
 * Тесты, которые проверяют множественное применение js правил.
 *
 * @author s-ermakov
 */
@SuppressWarnings("checkstyle:MagicNumber")
public class TestJavascriptRules extends TestConditionBase {

    @Test
    public void test2JavascriptRules() {
        tester
            .startModel()
                .id(1).category(1)
                .param("num1").setNumeric(1)
                .param("num2").setNumeric(2)
                .param("num3").setNumeric(3)
            .endModel()
            .startRuleSet()
                .id(1)
                .startRule()
                    .name("Rule 1").group("Test")
                    ._if()
                        .param("num1").javascript("return true;")
                    .then()
                        .param("num2").matchesNumeric(-2)
                .endRule()
                .startRule()
                    .name("Rule 2").group("Test")
                    ._if()
                        .param("num2").matchesNumeric(-2)
                    .then()
                        .param("num3").javascript("return -3;")
                .endRule()
            .endRuleSet()
            .doInference()
            .results()
                .count(2)
                .iterationCount(2)
                .param("num2").numeric(-2).endParam()
                .param("num3").numeric(-3).endParam()
            .endResults();
    }

    @Test
    public void test2JavaScriptRulesWithDependentParameters() throws Exception {
        // тест удаляет из str2 все значения по одному, пока они там есть
        // как только количество значений в str2 становится равным 2, то выставляется флаг bool1 == true
        // как только bool1 == true, str3 = val('str2')[0]
        tester
            .startModel()
                .id(1).category(1)
                .param("bool1").setBoolean(false)
                .param("str2").setString("aaa", "bbb", "ccc")
                .param("str3").setEmpty()
            .endModel()
            .startRuleSet()
                .id(1)
                .startRule()
                    .name("Rule 1").group("Test")
                    ._if()
                        .param("str2").javascript("var array = val('str2');" +
                                                            "return array.length > 0;")
                    .then()
                        .param("str2").javascript("var array = val('str2');" +
                                                            "array.shift();" +
                                                            "return array;")
                    .then()
                        .param("bool1").javascript("return val('str2').length === 2;")
                .endRule()
                .startRule()
                    .name("Rule 2").group("Test")
                    ._if()
                        .param("bool1").javascript("return val('bool1');")
                    .then()
                        .param("str3").javascript("return val('str2')[0]")
                .endRule()
            .endRuleSet()
            .doInference()
            .results()
                .count(3)
                .iterationCount(4)
                .param("str2").isEmpty().endParam()
                .param("str3").string("bbb").endParam()
                .param("bool1").bool(false).endParam()
            .endResults();
    }

    /**
     * Тест проверяет корректность работы ${@link ru.yandex.market.mbo.db.rules.NashornJsExecutor},
     * когда одним экзекьютором обновляются сразу несколько моделей c разными параметрами.
     *
     * @throws Exception
     */
    @Test
    public void testSeveralDifferentModelsTests() throws Exception {
        tester
            .startModel()
                .id(1).category(1)
                .param("num1").setNumeric(1)
                .param("str1").setString("aaa", "bbb", "ccc")
                .param("str2").setEmpty()
            .endModel()
            .startRuleSet()
                .id(1)
                .startRule()
                    .name("Rule 1").group("Test")
                    ._if()
                        .param("num1").javascript("return val('num1') < val('str1').length;")
                    .then()
                        .param("str2").javascript("return val('str1')[val('num1')];")
                .endRule()
            .endRuleSet()
            .doInference()
            .results()
                .count(1)
                .iterationCount(2)
                .param("str2").string("bbb").endParam()
                .noParam("num3")
            .endResults()
            .startModel()
                .id(100).category(100)
                .param("num1").setNumeric(2)
                .param("str1").setString("111", "222", "333")
                .param("num3").setNumeric(0) // тут важно, что 3 параметр отличался от 3 параметра предыдущей модели
            .endModel()
            .startRuleSet()
                .id(1)
                .startRule()
                    .name("Rule 1").group("Test")
                    ._if()
                        .param("num1").javascript("return val('num1') < val('str1').length;")
                    .then()
                        .param("num3").javascript("return val('num1') + val('str1').length;")
                .endRule()
                .startRule()
                    .name("Rule 2").group("Test")
                    ._if()
                        .param("num1").javascript("return val('num1') < val('str1').length;")
                    .then() // заведомо неверный скрипт, чтобы убедиться, что данные из предыдущей модели не сохранились
                        .param("str1").javascript("return val('str2')[val('num1')];")
                .endRule()
            .endRuleSet();

        try {
            tester.doInference();
        } catch (EvalJavascriptException e) {
            // отлавливаем ошибку, которая возникла, у модели #2 в правиле #2
            Assert.assertThat(e.getMessage(), containsString("Cannot get property"));
        }
    }
}
