package ru.yandex.market.api.test.infrastructure.prerequisites;

import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;
import ru.yandex.market.api.test.infrastructure.prerequisites.annotations.PrerequisiteEvaluation;
import ru.yandex.market.api.test.infrastructure.prerequisites.prerequisites.TestPrerequisite;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PrerequisiteStatementDecorator {
    public Statement decorate(Statement statement, Object targetTestClass, FrameworkMethod method) {
        Collection<Annotation> allAnnotations = findAllAnnotations(targetTestClass, method);
        TestPrerequisite prerequisites = compositePrerequisite(allAnnotations);
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                prerequisites.setUp(targetTestClass);
                try {
                    statement.evaluate();
                } finally {
                    prerequisites.tearDown();

                }
            }
        };
    }

    private TestPrerequisite compositePrerequisite(Collection<Annotation> allAnnotations) {
        List<TestPrerequisite> collect = allAnnotations.stream()
            .flatMap(x -> getRequisites(x))
            .collect(Collectors.toList());
        return new TestPrerequisite() {
            @Override
            public void setUp(Object testClass) {
                collect.forEach(x -> x.setUp(testClass));
            }

            @Override
            public void tearDown() {
                collect.forEach(x -> x.tearDown());
            }
        };
    }

    private Optional<TestPrerequisite> getPrerequisite(Annotation annotation) {

        Class<? extends Annotation> annotationType = annotation.annotationType();
        if (annotationType == PrerequisiteEvaluation.class) {
            Class<? extends TestPrerequisite> prerequisitesEvaluatorClass = ((PrerequisiteEvaluation) annotation).value();
            try {
                return Optional.of(instantiateClass(prerequisitesEvaluatorClass));
            } catch (Throwable t) {
                throw new RuntimeException(t);
            }
        }
        return Optional.empty();
    }

    private Stream<TestPrerequisite> getRequisites(Annotation annotation) {
        Optional<TestPrerequisite> singlePrerequisite = getPrerequisite(annotation);
        if (singlePrerequisite.isPresent()) {
            return Stream.of(singlePrerequisite.get());
        }
        return Arrays.stream(annotation.annotationType().getAnnotations())
            .map(x -> getPrerequisite(x))
            .filter(x -> x.isPresent())
            .map(x -> x.get());
    }

    private static Collection<Annotation> findAllAnnotations(
        final Object instance, final FrameworkMethod method
    ) {
        final Collection<Annotation> result = ReflexUtils.findAllAnnotations(instance.getClass());
        result.addAll(Arrays.asList(method.getAnnotations()));
        return result;
    }

    private static <T> T instantiateClass(final Class<T> clazz) {
        try {
            final Constructor<T> constructor = clazz.getDeclaredConstructor();
            return ReflexUtils.makeAccessible(constructor).newInstance();
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }
}
