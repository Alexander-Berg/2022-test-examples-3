package ru.yandex.market.pers.address.services;


import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.Test;
import ru.yandex.market.pers.address.controllers.MonitoringController;
import ru.yandex.market.loyalty.test.SourceScanner;
import ru.yandex.market.pers.address.controllers.PingController;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;

public class AnnotationCoverageTest {
    @Test
    public void checkAllWebMethodsSecured() {
        checkAllWebMethodsSecured(
                "ru.yandex.market.pers.address",
                ImmutableSet.of(
                        PingController.class,
                        MonitoringController.class
                )
        );
    }

    public static void checkAllWebMethodsSecured(String rootPackage, Set<Class<?>> classesToExclude) {
        List<Method> missingAnnotation = SourceScanner.requestMethods(rootPackage)
                .filter(m -> m.getAnnotation(RolesAllowed.class) == null && m.getAnnotation(PermitAll.class) == null)
                .filter(m -> !classesToExclude.contains(m.getDeclaringClass()))
                .collect(Collectors.toList());

        assertThat(missingAnnotation, is(empty()));
    }
}
