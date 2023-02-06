package ru.yandex.market.common.test.junit;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import org.assertj.core.api.Assertions;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;

import ru.yandex.market.common.test.db.DbUnitTestExecutionListener;
import ru.yandex.market.common.test.db.MemCachedServiceTestExecutionListener;
import ru.yandex.market.common.test.mockito.MockitoTestExecutionListener;
import ru.yandex.market.common.test.spring.LifecycleTestExecutionListener;

/**
 * Базовый класс для dbUnit-тестов, написанных с использованием Junit5.
 *
 * @author fbokovikov
 */
@TestExecutionListeners(listeners = {
        DirtiesContextTestExecutionListener.class,
        DependencyInjectionTestExecutionListener.class,
        DbUnitTestExecutionListener.class,
        MemCachedServiceTestExecutionListener.class,
        MockitoTestExecutionListener.class,
        LifecycleTestExecutionListener.class
}, mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
public abstract class JupiterDbUnitTest {

    private static final Set<String> CLASS_LEVEL_PROHIBITED_ANNOTATIONS = ImmutableSet.of(
            "org.springframework.test.context.ContextConfiguration",
            "org.springframework.test.annotation.DirtiesContext",
            "org.springframework.test.context.TestPropertySource"
    );

    private static final Set<String> METHOD_LEVEL_PROHIBITED_ANNOTATIONS = ImmutableSet.of(
            "org.junit.Test"
    );

    protected JupiterDbUnitTest() {
        checkProhibitedAnnotations();
    }

    /**
     * Проверить что класс с тестами не содержит запрещенных аннотаций.
     * Например специфичного Spring-контекста.
     */
    private void checkProhibitedAnnotations() {
        final Class<? extends JupiterDbUnitTest> clazz = getClass();
        final Annotation[] classAnnotations = clazz.getDeclaredAnnotations();

        for (final Annotation annotation : classAnnotations) {
            final String annotationType = annotation.annotationType().getCanonicalName();
            Assertions.assertThat(annotationType)
                    .isNotIn(CLASS_LEVEL_PROHIBITED_ANNOTATIONS);
        }

        final Method[] methods = clazz.getMethods();

        for (final Method method : methods) {
            Annotation[] methodAnnotations = method.getDeclaredAnnotations();
            for (final Annotation annotation : methodAnnotations) {
                final String annotationType = annotation.annotationType().getCanonicalName();
                Assertions.assertThat(annotationType)
                        .overridingErrorMessage("Не надо использовать аннотации junit 4")
                        .isNotIn(METHOD_LEVEL_PROHIBITED_ANNOTATIONS);
            }
        }
    }

}
