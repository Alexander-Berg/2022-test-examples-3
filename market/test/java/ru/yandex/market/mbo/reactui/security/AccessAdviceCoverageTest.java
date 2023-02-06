package ru.yandex.market.mbo.reactui.security;

import org.junit.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.junit.Assert.fail;

/**
 * @author kravchenko-aa
 * @date 27/02/2019
 */
public class AccessAdviceCoverageTest {

    private static final Set<Class<? extends Annotation>> ENDPOINT_METHOD_ANNOTATIONS = Set.of(
        PostMapping.class, GetMapping.class, DeleteMapping.class, PutMapping.class, RequestMapping.class
    );

    @Test
    public void testAllControllersAreEitherCoveredOrIgnored() throws ClassNotFoundException {
        ClassPathScanningCandidateComponentProvider scanner =
            new ClassPathScanningCandidateComponentProvider(false);

        scanner.addIncludeFilter(new AnnotationTypeFilter(RestController.class));
        scanner.addIncludeFilter(new AnnotationTypeFilter(Controller.class));

        List<String> fails = new ArrayList<>();
        for (BeanDefinition bd : scanner.findCandidateComponents("ru.yandex.market.mbo")) {

            Class<?> cls = Class.forName(bd.getBeanClassName());

            boolean classAnnotatedByWithAccess = cls.getAnnotation(AccessIgnore.class) != null
                || cls.getAnnotation(UiAccess.class) != null;

            List<Method> methods = filterMethods(cls.getDeclaredMethods());

            Predicate<Method> methodFailPredicate = method -> method.getAnnotation(AccessIgnore.class) == null
                && method.getAnnotation(UiAccess.class) == null;

            boolean allEndpointMethodsAnnotatedByWithAccess = methods.stream()
                .noneMatch(methodFailPredicate);

            List<String> failedMethods = Collections.emptyList();
            if (!allEndpointMethodsAnnotatedByWithAccess) {
                failedMethods = methods.stream().filter(methodFailPredicate)
                    .map(Method::getName)
                    .collect(Collectors.toList());
            }

            if (!allEndpointMethodsAnnotatedByWithAccess & !classAnnotatedByWithAccess) {
                fails.add("Either @AccessIgnore or @UiAccess is required for controller or all endpoint methods "
                    + cls.getCanonicalName() + " (" + failedMethods + ")");
            }
        }

        if (!fails.isEmpty()) {
            fail(String.join("\n", fails));
        }
    }

    private List<Method> filterMethods(Method[] methods) {
        return Arrays.stream(methods)
            .filter(method -> ENDPOINT_METHOD_ANNOTATIONS.stream()
                .anyMatch(annotationClass ->
                    method.getAnnotation(annotationClass) != null)
            )
            .collect(Collectors.toList());

    }

}
