package ru.yandex.market.abo.tms;

import java.lang.reflect.Constructor;
import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;

import ru.yandex.market.tms.quartz2.spring.CronTrigger;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Aleksei Neliubin (neliubin@yandex-team.ru)
 * @date 18.11.2019
 */
class EmptyConstructorForExecutorsTest {

    private static final String PACKAGE = "ru.yandex.market.abo.tms";

    @Test
    void emptyConstructorTest() throws Exception {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(CronTrigger.class));
        for (BeanDefinition controllerBean : scanner.findCandidateComponents(PACKAGE)) {
            checkExistingOfEmptyConstructor(Class.forName(controllerBean.getBeanClassName()).getConstructors());
        }
    }

    private static void checkExistingOfEmptyConstructor(Constructor[] constructors) {
        assertTrue(Arrays.stream(constructors).anyMatch(constructor -> constructor.getParameterCount() == 0),
                "Could not find empty constructor for executor: " + constructors[0].getDeclaringClass().toString());
    }
}
