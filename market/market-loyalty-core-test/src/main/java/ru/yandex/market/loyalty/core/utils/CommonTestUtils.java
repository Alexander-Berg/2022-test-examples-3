package ru.yandex.market.loyalty.core.utils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;

import org.apache.commons.codec.digest.DigestUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.annotation.AnnotatedElementUtils;

import ru.yandex.market.loyalty.core.config.DatabaseUsage;
import ru.yandex.market.loyalty.lightweight.WrappingExecutorService;
import ru.yandex.market.loyalty.spring.retry.spring.PgaasNoRetry;
import ru.yandex.market.loyalty.spring.retry.spring.PgaasRetryable;
import ru.yandex.market.loyalty.test.SourceScanner;
import ru.yandex.market.loyalty.test.TestFor;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;

public class CommonTestUtils {
    public static final Pattern NEW_LINE_CRNL = Pattern.compile("\r\n", Pattern.LITERAL);

    private CommonTestUtils() {
    }

    public static String md5(String key) {
        return DigestUtils.md5Hex(key);
    }

    public static String randomString() {
        return md5(UUID.randomUUID().toString());
    }

    public static int randomInt() {
        return ThreadLocalRandom.current().nextInt();
    }

    public static long randomLong() {
        return ThreadLocalRandom.current().nextLong();
    }

    public static void checkAllExecutorServicesIsWrapped(String rootPackage) {
        Set<Field> notWrappedExecutors = SourceScanner.findAllClasses(rootPackage)
                .flatMap(aClass -> Arrays.stream(aClass.getDeclaredFields()))
                .filter(field -> ExecutorService.class.isAssignableFrom(field.getType()))
                .filter(field -> !WrappingExecutorService.class.isAssignableFrom(field.getType()))
                .collect(Collectors.toSet());

        assertThat(notWrappedExecutors, is(empty()));
    }

    public static void checkAllWebMethodsSecured(String rootPackage, Set<Class<?>> classesToExclude) {
        List<Method> missingAnnotation = SourceScanner.requestMethods(rootPackage)
                .filter(m -> m.getAnnotation(RolesAllowed.class) == null && m.getAnnotation(PermitAll.class) == null)
                .filter(m -> !classesToExclude.contains(m.getDeclaringClass()))
                .collect(Collectors.toList());

        assertThat(missingAnnotation, is(empty()));
    }

    public static void checkControllers(String rootPackage, Class<?>... exclusions) {
        Set<Class<?>> exclusionsSet = Arrays.stream(exclusions).collect(Collectors.toSet());
        List<Method> missingPgaasAnnotation = SourceScanner.requestMethods(rootPackage)
                .filter(m -> m.getAnnotation(PgaasRetryable.class) == null && m.getAnnotation(PgaasNoRetry.class) == null)
                .filter(bean -> exclusionsSet.contains(bean))
                .collect(Collectors.toList());

        assertThat(missingPgaasAnnotation, is(empty()));
    }

    public static void checkAllDatabaseUsageTested(String rootPackage, Class<?>... exclusions) {
        Set<Class<?>> exclusionsSet = Arrays.stream(exclusions).collect(Collectors.toSet());

        Set<Class<?>> classesWithCoverage = SourceScanner.findClassesByAnnotation(rootPackage, TestFor.class)
                .flatMap(c -> Arrays.stream(c.getAnnotationsByType(TestFor.class)))
                .flatMap(annotation -> Arrays.stream(annotation.value()))
                .collect(Collectors.toSet());

        List<? extends Class<?>> classesWithoutCoverage = SourceScanner.findSpringBeans(rootPackage)
                .filter(bean -> Arrays.stream(bean.getDeclaredMethods())
                        .anyMatch(method -> method.getDeclaredAnnotationsByType(DatabaseUsage.class).length != 0)
                )
                .filter(bean -> !classesWithCoverage.contains(bean))
                .filter(bean -> !exclusionsSet.contains(bean))
                .collect(Collectors.toList());

        assertThat("Each class with @DatabaseUsage mast have @TestFor in tests",
                classesWithoutCoverage, is(empty())
        );
    }

    @NotNull
    public static Stream<String> allRolesAllowed(String rootPackage) {
        return SourceScanner.requestMethods(rootPackage)
                .filter(method -> AnnotatedElementUtils.hasAnnotation(method, RolesAllowed.class))
                .map(method -> method.getAnnotation(RolesAllowed.class))
                .map(RolesAllowed::value)
                .flatMap(Arrays::stream);
    }
}
