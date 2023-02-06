package ru.yandex.direct.tracing;

import org.junit.Test;

import ru.yandex.direct.tracing.data.TraceData;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.tracing.Trace.FULL;
import static ru.yandex.direct.tracing.Trace.PARTIAL;

public class TraceLoggerTest {

    private TraceLogger logger = new TraceLogger();

    @Test
    public void dumpGetsFullTraceSnapshotByDefault() {
        Trace trace = mock(Trace.class);
        logger.dump(trace);
        verify(trace).snapshot(FULL);
    }

    @Test
    public void partialDumpGetsPartialTraceSnapshot() {
        Trace trace = mock(Trace.class);
        logger.dump(trace, PARTIAL);
        verify(trace).snapshot(PARTIAL);
    }

    @Test
    public void dumpReturnsTrueWhenTraceReturnsData() throws Exception {
        Trace trace = mock(Trace.class);
        TraceData traceData = mock(TraceData.class);
        when(traceData.toJson()).thenReturn("{}");
        when(trace.snapshot(false)).thenReturn(traceData);
        boolean result = logger.dump(trace);
        assertThat("dump must return true when trace.spanshot() returns data", result, is(true));
    }

    @Test
    public void dumpReturnsFalseWhenTraceReturnsNull() {
        Trace trace = mock(Trace.class);
        boolean result = logger.dump(trace);
        assertThat("dump must return false when trace.spanshot() returns null", result, is(false));
    }
}
