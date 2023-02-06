package ru.yandex.market.checkout.checkouter.health;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.reflections.Reflections;
import org.reflections.scanners.MethodAnnotationsScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import ru.yandex.market.health.annotations.BusinessEndPoint;
import ru.yandex.market.health.annotations.ErrorPercent;
import ru.yandex.market.health.annotations.NoHealth;
import ru.yandex.market.health.annotations.Timing;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;

public abstract class AlertAnnotationsTestBase {

    private static final Logger LOG = LoggerFactory.getLogger(AlertAnnotationsTestBase.class);
    private static final List<Class<? extends Annotation>> ANNOTATIONS = Arrays.asList(
            RequestMapping.class,
            GetMapping.class,
            PostMapping.class,
            PutMapping.class,
            DeleteMapping.class,
            PatchMapping.class
    );
    private final Set<Class<?>> classesToSkip;
    private final String packagePrefix;

    public AlertAnnotationsTestBase(Set<Class<?>> classesToSkip, String packagePrefix) {
        this.classesToSkip = classesToSkip;
        this.packagePrefix = packagePrefix;
    }

    @Test
    public void requestMappingMethodsShouldBeAnnotatedWithErrorPercentAnnotation() {
        doCheckIfMethodsAreAnnotatedWithAnnotation(
                ErrorPercent.class,
                BusinessEndPoint.class,
                NoHealth.class);
    }

    @Test
    public void requestMappingMethodsShouldBeAnnotatedWithTimings() {
        doCheckIfMethodsAreAnnotatedWithAnnotation(
                Timing.class,
                BusinessEndPoint.class,
                NoHealth.class);
    }

    @SafeVarargs
    private final void doCheckIfMethodsAreAnnotatedWithAnnotation(Class<? extends Annotation>... annotationClasses) {
        Reflections reflections = new Reflections(
                packagePrefix,
                new MethodAnnotationsScanner()
        );

        List<Method> methods = ANNOTATIONS.stream()
                .map(reflections::getMethodsAnnotatedWith)
                .flatMap(Set::stream)
                .filter(method -> !classesToSkip.contains(method.getDeclaringClass()))
                .filter(method -> hasAnnotation(method, annotationClasses))
                .sorted(Comparator.comparing(m -> m.getDeclaringClass().getCanonicalName()))
                .collect(Collectors.toList());

        methods
                .forEach(m -> LOG.error("Found method without @" + annotationClasses[0].getSimpleName() + ": {}", m));

        assertThat(methods, empty());
    }

    private boolean hasAnnotation(Method method, Class<? extends Annotation>[] annotationClasses) {
        return Stream.of(annotationClasses)
                .noneMatch(a -> method.getAnnotation(a) != null || method.getDeclaringClass().getAnnotation(a) != null);
    }
}
