package ru.yandex.market.loyalty.test;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static ru.yandex.market.loyalty.lightweight.ExceptionUtils.makeExceptionsUnchecked;

/**
 * Проверяет, что если запустить все тесты, проаннатированные @TestFor, то все публичные методы класса, указанного в
 * аннотации будут выполнены
 */
public class TestCoverageRule extends TestWatcher {
    /**
     * Список тестов для класса упомянутого в @TestFor. Ключ - класс, который должен быть протестирован,
     * значение - список тестов, которые тестируют этот класс
     */
    final Map<Class<?>, List<Pair<? extends Class<?>, String>>> testsToRun = new HashMap<>();
    /**
     * Учёт вызванных методов у классов, которые должны быть протестированы. Ключ - класс, который должен быть
     * протестирован,
     * значение - список вызыванных методов
     */
    final Map<Class<?>, Set<Method>> calledMethods = new ConcurrentHashMap<>();

    private final Set<Method> exclusions;

    @SafeVarargs
    public TestCoverageRule(String rootPackage, Set<Method>... exclusions) {
        this.exclusions = Arrays.stream(exclusions).flatMap(Collection::stream).collect(Collectors.toSet());
        ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(false);
        provider.addIncludeFilter(new AnnotationTypeFilter(TestFor.class));
        provider.findCandidateComponents(rootPackage).stream()
                .map(BeanDefinition::getBeanClassName)
                .map(makeExceptionsUnchecked(Class::forName))
                .distinct()
                .forEach(testClass -> {
                    List<Pair<? extends Class<?>, String>> toRun = Arrays.stream(testClass.getDeclaredMethods())
                            .filter(m -> Modifier.isPublic(m.getModifiers()))
                            .filter(m -> m.isAnnotationPresent(Test.class))
                            .filter(m -> !m.isAnnotationPresent(Ignore.class))
                            .map(m -> Pair.of(testClass, m.getName()))
                            .collect(Collectors.toList());
                    Arrays.stream(testClass.getAnnotationsByType(TestFor.class)[0].value())
                            .map(testFor -> testsToRun.computeIfAbsent(testFor, k -> new ArrayList<>()))
                            .forEach(
                                    testFor -> testFor.addAll(toRun)
                            );
                });
    }

    public static Set<Method> exclude(Class<?> aClass, String... methods) {
        return Arrays.stream(methods)
                .map(methodName -> {
                    Set<Method> methodsWithThisName = Arrays.stream(aClass.getDeclaredMethods())
                            .filter(m -> m.getName().equals(methodName)).collect(Collectors.toSet());
                    assertThat(methodsWithThisName, hasSize(1));
                    return methodsWithThisName.iterator().next();
                })
                .collect(Collectors.toSet());
    }

    @Override
    protected void finished(Description description) {
        TestFor[] annotations = description.getTestClass().getAnnotationsByType(TestFor.class);
        if (annotations.length > 0) {
            for (Class<?> testFor : annotations[0].value()) {
                testsToRun.get(testFor).remove(Pair.of(description.getTestClass(), description.getMethodName()));
                // последний тест, который проверяет testFor, надо проверить, все ли методы были вызваны
                // если запущена только часть тестов, то тогда это условие никогда не выполнится
                if (testsToRun.get(testFor).isEmpty()) {
                    Set<Method> wasCalled = calledMethods.getOrDefault(testFor, Collections.emptySet());
                    Set<Method> missedCalls = Arrays.stream(testFor.getDeclaredMethods())
                            .filter(m -> Modifier.isPublic(m.getModifiers()))
                            .filter(m -> !m.isAnnotationPresent(Deprecated.class))
                            .filter(m -> !wasCalled.contains(m))
                            .filter(m -> !exclusions.contains(m))
                            .filter(m -> !"onServerStart".equals(m.getName()))
                            .collect(Collectors.toSet());
                    assertThat("not all public methods was call", missedCalls, is(empty()));

                    Set<Method> unnecessaryExclusions = wasCalled.stream()
                            .filter(exclusions::contains)
                            .collect(Collectors.toSet());
                    assertThat("please, remove them from exclusions", unnecessaryExclusions, is(empty()));
                }
            }
        }
    }
}
