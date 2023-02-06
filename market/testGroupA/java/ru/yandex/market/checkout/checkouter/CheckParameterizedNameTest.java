package ru.yandex.market.checkout.checkouter;

import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.reflections.Reflections;
import org.reflections.scanners.MethodAnnotationsScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.lessThan;

public class CheckParameterizedNameTest {

    private static final Logger log = LoggerFactory.getLogger(CheckParameterizedNameTest.class);

    @Test
    public void jsonPropertyShouldBeMarkedWithApiModelProperty() {
        Reflections reflections = new Reflections(
                "ru.yandex.market.checkout.checkouter",
                new MethodAnnotationsScanner()
        );

        List<Method> parameterizedTestWithoutName = reflections.getMethodsAnnotatedWith(ParameterizedTest.class)
                .stream()
                .filter(method -> method.getDeclaringClass().getSimpleName().endsWith("Test"))
                .filter(method -> method.getAnnotation(MethodSource.class) != null)
                .sorted(Comparator.comparing(m -> m.getDeclaringClass().getCanonicalName()))
                .map(method -> Pair.of(method, method.getAnnotation(ParameterizedTest.class)))
                .filter(pair -> "[{index}] {arguments}".equals(pair.getRight().name()))
                .map(Pair::getLeft)
                .collect(Collectors.toList());

        parameterizedTestWithoutName
                .forEach(m -> log.error("Found @ParameterizedTest without name: {}. You should provide generated " +
                        "stable name to defense from flaky tests", m));

        String message = "Found errors in following methods: " + parameterizedTestWithoutName.stream()
                .map(method -> method.getDeclaringClass().getSimpleName() + "::" + method.getName())
                .collect(Collectors.toList());
        // Не готов сразу выправить 126 теста, где это нарушается - защита от написания новых
        // На самом деле проблема не такая страшная - не в каждом из этих тестов проблема есть
        // 1. Проблема возникает только тогда, когда в методе указанном в @MethodSource порядок элементов не
        // стабилен
        // 2. В имени используется индекс теста и если в середину вставили элемента, то все имена изменятся
        assertThat(message, parameterizedTestWithoutName.size(), lessThan(128));
    }
}
