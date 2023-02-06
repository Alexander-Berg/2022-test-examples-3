package ru.yandex.direct.tracing.real;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import ru.yandex.direct.tracing.TraceProfile;
import ru.yandex.direct.tracing.data.TraceDataProfile;
import ru.yandex.direct.tracing.util.MockedTraceClockProvider;

import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;

/**
 * Created by snaury on 25/04/16.
 */
public class RealTraceProfilerTest {
    @Test
    public void simpleProfile() {
        MockedTraceClockProvider clock = new MockedTraceClockProvider();
        RealTraceProfiler profiler = new RealTraceProfiler(clock);
        for (int i = 0; i < 3; ++i) {
            try (TraceProfile profile = profiler.profile("myfunc", "mytags", i + 1)) {
                clock.advance(500, TimeUnit.MILLISECONDS);
            }
        }
        List<TraceDataProfile> profiles = profiler.snapshot();
        List<TraceDataProfile> expected = Collections.singletonList(
                new TraceDataProfile("myfunc", "mytags", 1.5, 0.0, 3L, 1L + 2L + 3L));
        assertThat(profiles, beanDiffer(expected));
    }

    @Test
    public void nestedProfile() {
        MockedTraceClockProvider clock = new MockedTraceClockProvider();
        RealTraceProfiler profiler = new RealTraceProfiler(clock);
        for (int i = 0; i < 3; ++i) {
            try (TraceProfile profile1 = profiler.profile("myfunc1", "mytags1", i + 1)) {
                for (int j = 0; j < i + 1; ++j) {
                    try (TraceProfile profile2 = profiler.profile("myfunc2", "mytags2", 0)) {
                        clock.advance(500, TimeUnit.MILLISECONDS);
                    }
                }
                clock.advance(500, TimeUnit.MILLISECONDS);
            }
        }
        List<TraceDataProfile> profiles = profiler.snapshot();
        profiles.sort(Comparator.comparing(p -> p.getFunc()));
        List<TraceDataProfile> expected = new ArrayList<>();
        expected.add(new TraceDataProfile("myfunc1", "mytags1", 4.5, 3.0, 3L, 1L + 2L + 3L));
        expected.add(new TraceDataProfile("myfunc2", "mytags2", 3.0, 0.0, 1L + 2L + 3L, 0L));
        assertThat(profiles, beanDiffer(expected));
    }
}
