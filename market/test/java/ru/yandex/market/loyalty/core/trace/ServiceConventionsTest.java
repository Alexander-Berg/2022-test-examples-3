package ru.yandex.market.loyalty.core.trace;

import com.google.common.annotations.VisibleForTesting;
import org.junit.Test;

import ru.yandex.market.loyalty.core.dao.query.Filter;
import ru.yandex.market.loyalty.test.SourceScanner;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;

public class ServiceConventionsTest {

    @Test
    public void checkFilterInterfaceNotExposedByServices() {
        Stream<? extends Class<?>> beans = SourceScanner.findSpringBeans("ru.yandex.market.loyalty.back");
        List<Method> methods = beans
                .filter(bean -> bean.getName().endsWith("Service"))
                .flatMap(bean -> Arrays.stream(bean.getMethods()))
                .collect(Collectors.toList());

        List<Method> unwanted = methods
                .stream()
                .filter(m -> Arrays.stream(m.getParameterTypes()).anyMatch(Filter.class::isAssignableFrom) && m.getAnnotation(VisibleForTesting.class) == null)
                .collect(Collectors.toList());

        assertThat(unwanted, is(empty()));
    }
}
