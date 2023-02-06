package ru.yandex.direct.tracing.real;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.direct.tracing.Trace;
import ru.yandex.direct.tracing.TraceProfile;
import ru.yandex.direct.tracing.data.TraceData;
import ru.yandex.direct.tracing.data.TraceDataProfile;
import ru.yandex.direct.tracing.util.MockedThreadUsedResourcesProvider;
import ru.yandex.direct.tracing.util.MockedTraceClockProvider;

import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;

public class RealTraceSnapshotDataTest {
    private Trace trace;
    private TraceData partialData;
    private TraceData finalData;

    @Before
    public void prepareData() {
        MockedTraceClockProvider clock = new MockedTraceClockProvider(1462454680);
        MockedThreadUsedResourcesProvider cpuTimeProvider =
                new MockedThreadUsedResourcesProvider(Duration.ofSeconds(1));
        trace = RealTrace.builder()
                .withClock(clock)
                .withThreadCpuTimeProvider(cpuTimeProvider)
                .build();
        trace.activate();
        try {
            clock.advance(1000, TimeUnit.MILLISECONDS);

            try (TraceProfile profile1 = trace.profile("myfunc1")) {
                clock.advance(500, TimeUnit.MILLISECONDS);

                partialData = trace.snapshot(true);

                try (TraceProfile profile2 = trace.profile("myfunc2")) {
                    clock.advance(500, TimeUnit.MILLISECONDS);
                }

                clock.advance(500, TimeUnit.MILLISECONDS);
            }

            clock.advance(500, TimeUnit.MILLISECONDS);

            finalData = trace.snapshot(false);
        } finally {
            trace.deactivate();
        }

        partialData.getProfiles().sort(Comparator.comparing(p -> p.getFunc()));
        finalData.getProfiles().sort(Comparator.comparing(p -> p.getFunc()));
    }

    @Test
    public void partialTraceData() {
        TraceData expected = new TraceData();
        expected.setLogTime(Instant.parse("2016-05-05T13:24:41.500Z"));
        expected.setSamplerate(1);
        expected.setChunkIndex(1);
        expected.setChunkFinal(false);
        expected.setAllEla(1.5);
        expected.getTimes().setEla(1.5);
        expected.getTimes().setCpuUserTime(1);
        expected.getTimes().setCpuSystemTime(2);
        expected.getProfiles().add(new TraceDataProfile("myfunc1", "", 0.5, 0.0, 0, 0));
        expected.getProfiles().add(new TraceDataProfile("rest", "", 1.0, 0.0, 1, 0));

        assertThat(partialData, beanDiffer(expected).useCompareStrategy(DefaultCompareStrategies.allFieldsExcept(
                newPath("host"),
                newPath("traceId"),
                newPath("parentId"),
                newPath("spanId"),
                newPath("times", "mem")
        )));
    }

    @Test
    public void finalTestData() {
        TraceData expected = new TraceData();
        expected.setLogTime(Instant.parse("2016-05-05T13:24:43Z"));
        expected.setSamplerate(1);
        expected.setChunkIndex(2);
        expected.setChunkFinal(true);
        expected.setAllEla(3.0);
        expected.getTimes().setEla(1.5);
        expected.getTimes().setCpuUserTime(1);
        expected.getTimes().setCpuSystemTime(2);
        expected.getProfiles().add(new TraceDataProfile("myfunc1", "", 1.0, 0.5, 1, 0));
        expected.getProfiles().add(new TraceDataProfile("myfunc2", "", 0.5, 0.0, 1, 0));
        expected.getProfiles().add(new TraceDataProfile("rest", "", 0.5, 0.0, 1, 0));
        assertThat(finalData, beanDiffer(expected).useCompareStrategy(DefaultCompareStrategies.allFieldsExcept(
                newPath("host"),
                newPath("traceId"),
                newPath("parentId"),
                newPath("spanId"),
                newPath("times", "mem")
        )));

    }
}
