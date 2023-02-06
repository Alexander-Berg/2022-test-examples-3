package ru.yandex.direct.web.entity.grid.service;

import java.util.Arrays;
import java.util.Collection;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.MDC;

import ru.yandex.direct.tracing.Trace;
import ru.yandex.direct.tracing.real.RealTrace;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static ru.yandex.direct.tracing.TraceMdcAdapter.METHOD_KEY;

@ParametersAreNonnullByDefault
@RunWith(Parameterized.class)
public class GridApiControllerTraceTest {
    @Parameterized.Parameter
    public String operationName;
    @Parameterized.Parameter(1)
    public String expectedTraceMethod;

    @Parameterized.Parameters()
    public static Collection<Object[]> params() {
        return Arrays.asList(new Object[][]{
                {"HelloWorld", "grid.api.HelloWorld"},
                {"hello_world", "grid.api.hello_world"},
                {"w<script>", "grid.api"},
                {"../../../etc/shadow", "grid.api"},
        });
    }

    @Before
    public void setUp() {
        RealTrace trace = RealTrace.builder().withMethod("grid.api").build();
        Trace.push(trace);
    }

    @After
    public void tearDown() {
        Trace.pop();
    }

    @Test
    public void testGridOperationInTraceMethod() {
        GridService.replaceTraceMethod(operationName);
        assertThat(Trace.current().getMethod(), is(expectedTraceMethod));
        assertThat(MDC.get(METHOD_KEY), is(expectedTraceMethod));
    }
}
