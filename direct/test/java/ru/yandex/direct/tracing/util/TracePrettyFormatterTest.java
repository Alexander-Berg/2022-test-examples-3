package ru.yandex.direct.tracing.util;

import java.util.ArrayList;

import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.tracing.Trace;
import ru.yandex.direct.tracing.data.TraceData;
import ru.yandex.direct.tracing.real.RealTrace;

import static org.junit.Assert.assertThat;

@RunWith(Parameterized.class)
public class TracePrettyFormatterTest {
    @Parameterized.Parameter
    public TraceData traceData;

    @Parameterized.Parameter(1)
    public TracePrettyFormatter formatter;

    @Parameterized.Parameters
    public static Object[][] params() {
        ArrayList<Object[]> ret = new ArrayList<>();

        TracePrettyFormatter[] formatters = new TracePrettyFormatter[]{
                TracePrettyFormatter.defaultFormatter(),
                new TracePrettyFormatter(5, false),
                new TracePrettyFormatter(5, true),
                new TracePrettyFormatter(0, true),
        };

        for (TracePrettyFormatter formatter : formatters) {
            for (boolean partial : new boolean[]{false, true}) {
                for (Trace trace : new Trace[]{
                        RealTrace.builder().build(),
                        RealTrace.builder().withInfo("asdfadsf", "gsdhfgdjdhj", "rqwerqw").build(),
                        RealTrace.builder().withIds(353245245, 1234, 634574567).build(),
                }) {
                    trace.profile("asdf").close();
                    trace.annotate("asdf", "daf");
                    trace.child("asdf", "weqr").close();
                    ret.add(new Object[]{
                            trace.snapshot(partial),
                            formatter
                    });
                }
            }
        }
        return ret.toArray(new Object[][]{});
    }

    @Test
    public void formatDontFailsAndContainsCertainInformation() throws Exception {
        String str = formatter.format(traceData);
        assertThat(str, Matchers.containsString(String.valueOf(traceData.getSpanId())));
    }
}
