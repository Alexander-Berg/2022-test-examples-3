package ru.yandex.direct.tracing.real;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.direct.tracing.Trace;
import ru.yandex.direct.tracing.TraceChild;
import ru.yandex.direct.tracing.data.TraceDataChild;
import ru.yandex.direct.tracing.util.MockedTraceClockProvider;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;

public class RealTraceChildTest {
    private Trace trace;
    private TraceChild child;

    @Before
    public void initTrace() {
        MockedTraceClockProvider clock = new MockedTraceClockProvider(1462454680);
        trace = RealTrace.builder()
                .withClock(clock)
                .withTtl(5)
                .build();

        clock.advance(500, TimeUnit.MILLISECONDS);
        child = trace.child("someservice", "somemethod");
        clock.advance(1000, TimeUnit.MILLISECONDS);
        child.close();
    }

    @Test
    public void realTraceChildTtl() {
        assertThat(child.getTtl(), is(4));
    }

    @Test
    public void realTraceChildStartedAt() {
        assertThat(child.startedAt(), is(0.5));
    }

    @Test
    public void realTraceChildElapsed() {
        assertThat(child.elapsed(), is(1.0));
    }

    @Test
    public void realTraceChildSnapshot() {
        List<TraceDataChild> children = trace.snapshot(Trace.FULL).getChildren();
        List<TraceDataChild> expected =
                Collections.singletonList(new TraceDataChild("someservice", "somemethod", 0, 0.5, 1.0));
        assertThat("", children, beanDiffer(expected).useCompareStrategy(DefaultCompareStrategies
                .onlyExpectedFields()
                .forFields(BeanFieldPath.newPath("0", "spanId")).useMatcher(greaterThan(0L))));
    }
}
