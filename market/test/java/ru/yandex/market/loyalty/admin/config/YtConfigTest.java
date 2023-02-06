package ru.yandex.market.loyalty.admin.config;

import com.google.common.collect.ImmutableSet;
import org.junit.Test;

import ru.yandex.market.loyalty.core.config.YtArnold;
import ru.yandex.market.loyalty.core.config.YtHahn;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

public class YtConfigTest {
    private static final Set<Class<? extends Annotation>> YT_QUALIFIERS = ImmutableSet.of(
            YtHahn.class,
            YtArnold.class
    );

    @Test
    public void qualifiersShouldMatch() {
        for (Method method : AdminConfigInternal.YtServiceConfig.class.getDeclaredMethods()) {
            for (Class<? extends Annotation> qualifier : YT_QUALIFIERS) {
                if (method.getAnnotation(qualifier) != null) {
                    for (Annotation[] parameterAnnotations : method.getParameterAnnotations()) {
                        Set<Class<?>> ytQualifiersOfParameter = Arrays.stream(parameterAnnotations)
                                .map(Annotation::annotationType)
                                .filter(YT_QUALIFIERS::contains)
                                .collect(Collectors.toSet());

                        if (!ytQualifiersOfParameter.isEmpty()) {
                            assertEquals(
                                    "method " + method.getName(),
                                    Collections.singleton(qualifier), ytQualifiersOfParameter
                            );
                        }
                    }
                }
            }
        }
    }
}
