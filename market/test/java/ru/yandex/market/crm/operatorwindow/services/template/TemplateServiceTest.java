package ru.yandex.market.crm.operatorwindow.services.template;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import groovy.lang.MissingPropertyException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.crm.operatorwindow.domain.template.TemplateService;
import ru.yandex.market.crm.util.Exceptions;
import ru.yandex.market.crm.util.Result;

public class TemplateServiceTest {

    private final TemplateService templateService = new TemplateService();

    @Test
    public void missingParameterValue() {
        assertError(
                "test ${param}",
                x -> {
                },
                error -> Assertions.assertTrue(Exceptions.hasNestedException(error, MissingPropertyException.class)));
    }

    @Test
    public void emptyParameterConvertsToNull() {
        assertProcessed(
                "test null",
                "test ${}",
                x -> {
                });
    }

    @Test
    public void simple() {
        assertProcessed(
                "prefix paramValue suffix",
                "prefix ${paramName} suffix",
                x -> x.put("paramName", "paramValue"));
    }

    private void assertProcessed(String expected,
                                 String template,
                                 Consumer<Map<String, String>> bindingModifier) {
        Result<String, Throwable> actual = templateService.tryRender(template, getBinding(bindingModifier));
        Assertions.assertTrue(actual.isOk());
        Assertions.assertEquals(expected, actual.getValue());
    }

    private void assertError(String template,
                             Consumer<Map<String, String>> bindingModifier,
                             Consumer<Throwable> assertError) {
        final Result<String, Throwable> result = templateService.tryRender(template, getBinding(bindingModifier));
        Assertions.assertTrue(result.hasError());
        assertError.accept(result.getError());
    }

    private Map<String, String> getBinding(
            Consumer<Map<String, String>> modifier) {
        Map<String, String> binding = new HashMap<>();
        modifier.accept(binding);
        return binding;
    }
}
