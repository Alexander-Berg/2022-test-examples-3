package ru.yandex.market.crm.campaign.services;

import java.util.Collections;
import java.util.Map;

import javax.inject.Inject;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import ru.yandex.market.crm.campaign.IntegrationTestBase;
import ru.yandex.market.crm.templates.TemplateParameters;
import ru.yandex.market.crm.templates.TemplateService;

@Ignore
public class TemplateServiceTest extends IntegrationTestBase {

    @Inject
    TemplateService templateService;

    /**
     * Проверяем возможность использовать формулы для вычисления дат
     * <p>
     * 1501179992000 - Thu, 27 Jul 2017 18:26:32 GMT
     */
    @Test
    public void checkDateOperations() {
        // вызов системы
        String template = "${use (groovy.time.TimeCategory) {(new Date(1501179992000l) - 6.day).format" +
                "(\"yyyy-MM-dd\")}}";
        String result = templateService.process(template);
        // проверка утверждений
        Assert.assertEquals("2017-07-21", result);
    }

    /**
     * Проверка работы api.formatters
     */
    @Test
    public void checkDateFormatterApis() {
        // вызов системы
        String template = "${use (groovy.time.TimeCategory) { " +
                "   api.formatters.date.asIsoDate(new Date(1501179992000l) - 6.day) " +
                "}}";
        String result = templateService.process(template);
        // проверка утверждений
        Assert.assertEquals("2017-07-21", result);
    }

    @Test
    public void checkDefaultValueWithoutVar() {
        // вызов системы
        String template = "${ var.getOrDefault('notExistedVarName', 'defValue') }";
        String result = templateService.process(template);
        // проверка утверждений
        Assert.assertEquals("defValue", result);
    }

    @Test
    public void checkDefaultValueWithVar() {
        // вызов системы
        Map<String, Object> parameters = TemplateParameters.empty()
                .put("propName", "value")
                .build();

        String template = "${ var.getOrDefault('propName', 'defValue') }";
        String result = templateService.process(template, parameters);
        // проверка утверждений
        Assert.assertEquals("value", result);
    }

    /**
     * Проверяем правильность замены переменных шаблона значениями определенными в конфигурационном файле.
     */
    @Test
    public void checkSystemTemplateVars() {
        // вызов системы
        String yql = "${var.test_variable}";
        String result = templateService.process(yql, Collections.emptyMap());
        // проверка утверждений
        Assert.assertEquals("Должно совпадать со значением из template-vars-integration_test.properties",
                "home/test/model_stat_table", result);
    }

    /**
     * Проверяем правильность замены переменных шаблона значениями переданными в параметре вызова.
     */
    @Test
    public void checkUserTemplateVars() {
        // вызов системы
        String expectedValue = "expected_value";

        String yql = "${var.test_variable}";

        Map<String, Object> params = TemplateParameters.empty()
                .put("test_variable", expectedValue)
                .build();

        String result = templateService.process(yql, params);
        // проверка утверждений
        Assert.assertEquals("Должно совпадать со значением определенном в vars",
                expectedValue, result);
    }

    @Test
    public void checkEnumAccess() {
        // вызов системы
        String expectedValue = "A";

        String template = "${enums.TestEnum.A}";
        TemplateParameters parameters = TemplateParameters.empty().withEnum(TestEnum.class);

        String result = templateService.process(template, parameters);
        // проверка утверждений
        Assert.assertEquals("Должно получить название элемента перечисления",
                expectedValue, result);
    }

    private enum TestEnum {
        A, B
    }
}
