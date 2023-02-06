package ru.yandex.market.loyalty.back.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.validation.constraints.Size;

import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static ru.yandex.market.loyalty.lightweight.ExceptionUtils.makeExceptionsUnchecked;

@Ignore
public class SoxProblemsTest {

    @Test
    public void checkNoUnlimitedCollectionsInControllerMethods() {
        List<String> methodsWithBadParameters = new ArrayList<>();
        ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(true);
        Set<Class<?>> controllerClasses = provider
                .findCandidateComponents("ru.yandex.market.loyalty.back")
                .stream()
                .map(BeanDefinition::getBeanClassName)
                .filter(Objects::nonNull)
                .filter(beanClassName -> beanClassName.contains("Controller"))
                .map(makeExceptionsUnchecked(Class::forName))
                .collect(Collectors.toSet());
        for (Class<?> controllerClass : controllerClasses) {
            for (Method method : controllerClass.getDeclaredMethods()) {
                if (!Modifier.isPublic(method.getModifiers())) {
                    continue;
                }
                for (Parameter parameter : method.getParameters()) {
                    Class<?> parameterClass = parameter.getType();
                    if (Collection.class.isAssignableFrom(parameterClass) && parameter.getAnnotation(Size.class) == null) {
                        methodsWithBadParameters.add(
                                controllerClass.getSimpleName()
                                        + "." + method.getName() + "->"
                                        + parameterClass.getSimpleName()
                        );
                    }
                    checkForNotLimitedCollections(
                            parameterClass,
                            methodsWithBadParameters,
                            controllerClass.getSimpleName() + "." + method.getName()
                    );
                }
            }
        }
        assertThat(methodsWithBadParameters, empty());
    }

    private static void checkForNotLimitedCollections(Class<?> clazz, Collection<String> methodsWithBadParameters,
                                                      String path) {
        if (clazz.isPrimitive()) {
            return;
        }
        for (Field field : clazz.getDeclaredFields()) {
            Class<?> fieldType = field.getType();
            if (Collection.class.isAssignableFrom(fieldType) && field.getAnnotation(Size.class) == null) {
                methodsWithBadParameters.add(path
                        + "->" + clazz.getSimpleName() + "->" + fieldType.getSimpleName());
            }
        }
    }

}
