package ru.yandex.market.loyalty.back.trace;

import com.google.common.collect.ImmutableSet;
import org.junit.Test;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.loyalty.test.SourceScanner;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;

public class TransactionalAdviceTest {

    private static final ImmutableSet<Propagation> WANTED_PROPAGATION = ImmutableSet.of(Propagation.MANDATORY,
            Propagation.REQUIRED, Propagation.NEVER);

    @Test
    public void checkOnlyWantedPropagationTypesUsed() {
        List<Method> methods = SourceScanner.findSpringBeans("ru.yandex.market.loyalty.back")
                .flatMap(bean -> Arrays.stream(bean.getDeclaredMethods())
                        .filter(method -> method.getDeclaredAnnotationsByType(Transactional.class).length > 0)
                )
                .collect(Collectors.toList());


        List<Method> unwanted = methods
                .stream()
                .filter(
                        m -> !WANTED_PROPAGATION.contains(m.getAnnotation(Transactional.class).propagation()))
                .collect(Collectors.toList());

        assertThat(unwanted, is(empty()));
    }
}
