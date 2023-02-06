package ru.yandex.direct.tracing.real;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.direct.tracing.data.TraceData;
import ru.yandex.direct.tracing.util.TraceClockProvider;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.tracing.Trace.FULL;
import static ru.yandex.direct.tracing.Trace.PARTIAL;
import static ru.yandex.direct.tracing.util.TraceUtil.secondsFromNanoseconds;

public class RealTraceElapsedTimeTest {

    private static final long TIME_0 = 1000L;
    private static final long TIME_1 = 2000L;
    private static final long TIME_2 = 4000L;

    private TraceClockProvider traceClockMock;
    private RealTrace trace;

    @Before
    public void before() {
        traceClockMock = mock(TraceClockProvider.class);

        when(traceClockMock.nanoTime()).thenReturn(TIME_0);
        trace = RealTrace.builder().withClock(traceClockMock).build();

        when(traceClockMock.nanoTime()).thenReturn(TIME_1);
    }

    @Test
    public void computesAllElapsedTimeForOneFullSnapshot() {
        TraceData traceData = trace.snapshot(FULL);
        assertThat("TraceData.allEla  for one full snapshot is invalid",
                traceData.getAllEla(), is(secondsFromNanoseconds(TIME_1 - TIME_0)));
    }

    @Test
    public void computesAllElapsedTimeForOnePartialSnapshot() {
        TraceData traceData = trace.snapshot(PARTIAL);
        assertThat("TraceData.allEla for one partial snapshot is invalid",
                traceData.getAllEla(), is(secondsFromNanoseconds(TIME_1 - TIME_0)));
    }

    @Test
    public void computesCurSnapshotElapsedTimeForOneFullSnapshot() {
        TraceData traceData = trace.snapshot(FULL);
        assertThat("TraceData.times.ela for one full snapshot is invalid",
                traceData.getTimes().getEla(), is(secondsFromNanoseconds(TIME_1 - TIME_0)));
    }

    @Test
    public void computesCurSnapshotElapsedTimeForOnePartialSnapshot() {
        TraceData traceData = trace.snapshot(PARTIAL);
        assertThat("TraceData.times.ela for one partial snapshot is invalid",
                traceData.getTimes().getEla(), is(secondsFromNanoseconds(TIME_1 - TIME_0)));
    }

    @Test
    public void computesAllElapsedTimeForSecondPartialSnapshot() {
        trace.snapshot(PARTIAL);

        when(traceClockMock.nanoTime()).thenReturn(TIME_2);
        TraceData traceData = trace.snapshot(PARTIAL);

        assertThat("TraceData.allEla for second partial snapshot is invalid",
                traceData.getAllEla(), is(secondsFromNanoseconds(TIME_2 - TIME_0)));
    }

    @Test
    public void computesCurSpanshotElapsedTimeForSecondPartialSnapshot() {
        trace.snapshot(PARTIAL);

        when(traceClockMock.nanoTime()).thenReturn(TIME_2);
        TraceData traceData = trace.snapshot(PARTIAL);

        assertThat("TraceData.times.ela for second partial snapshot is invalid",
                traceData.getTimes().getEla(), is(secondsFromNanoseconds(TIME_2 - TIME_1)));
    }
}
