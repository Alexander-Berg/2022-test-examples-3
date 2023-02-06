package ru.yandex.direct.common.metrics;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import static org.junit.Assert.assertEquals;
import static ru.yandex.direct.common.metrics.MetricsFilter.toCleanMethodName;

@RunWith(Parameterized.class)
public class MetricsFilterTest {
    @Parameter
    public String methodName;

    @Parameter(1)
    public String metricMethodName;

    @Parameters(name = "{0} -> {1}")
    public static Collection parameters() {
        return Arrays.asList(new Object[][] {
                { "users.@me", "users_me" },
                { "a$b@c", "a_b_c" },
                { "%%a$$b@c@%", "_a_b_c_" },
                { "0aZ__-", "0aZ_-"}
        });
    }
    @Test
    public void toCleanMethodNameTest() {
        assertEquals(metricMethodName, toCleanMethodName(methodName));
    }
}
