package ru.yandex.market.loyalty.back.other;

import java.lang.reflect.Method;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;

import ru.yandex.market.health.Utils;
import ru.yandex.market.loyalty.test.SourceScanner;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;

public class ControllerHealthCoverageTest {
    @Test
    public void checkAllRequestMethodsHasAllHealthAnnotations() {
        List<Method> methods = SourceScanner.requestMethods("ru.yandex.market.loyalty")
                .filter(m -> !Utils.hasAllHealthAnnotations(m))
                .collect(Collectors.toList());

        assertThat("Each web request method must have " +
                "health annotations @ErrorsPercent and @Timing. Or mark them @NoHealth", methods, is(empty()));
    }

}
