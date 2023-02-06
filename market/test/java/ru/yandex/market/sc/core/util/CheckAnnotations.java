package ru.yandex.market.sc.core.util;

import java.util.Arrays;
import java.util.Set;

import lombok.experimental.UtilityClass;
import org.springframework.cache.annotation.Cacheable;

import static org.junit.jupiter.api.Assertions.assertTrue;

@UtilityClass
@SuppressWarnings("HideUtilityClassConstructor")
public class CheckAnnotations {

    public static <T> void checkCacheableAnnotations(Class<T> clazz, Set<String> exceptMethods) {
        var methods = clazz.getDeclaredMethods();
        for (var method : methods) {
            var methodName = method.getName();
            if (exceptMethods.contains(methodName)) {
                continue;
            }
            for (var annotation : method.getAnnotations()) {
                if (annotation instanceof Cacheable) {
                    assertTrue(Arrays.stream(((Cacheable) annotation).value())
                                    .anyMatch(value -> value.equals(method.getName())),
                            "Wrong @Cacheable value for " + method.getName());
                }
            }
        }
    }
}
