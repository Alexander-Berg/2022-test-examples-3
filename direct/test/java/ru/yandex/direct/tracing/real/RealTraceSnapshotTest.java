package ru.yandex.direct.tracing.real;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.direct.tracing.data.TraceData;

import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.tracing.Trace.FULL;
import static ru.yandex.direct.tracing.Trace.PARTIAL;

public class RealTraceSnapshotTest {

    private RealTrace trace;

    @Before
    public void before() {
        trace = RealTrace.builder().build();
        trace.activate();
        trace.deactivate();
    }

    @Test
    public void snapshot_WhenFullSnapshotRetrieved_ReturnsNull() {
        trace.snapshot(FULL);
        TraceData traceData = trace.snapshot(FULL);
        assertThat("snapshot() must return null after full snapshot retrieved", traceData, nullValue());
    }

    @Test
    public void snapshot_WhenOnlyPartialSnapshotRetrieved_ReturnsNotNull() {
        trace.snapshot(PARTIAL);
        TraceData traceData = trace.snapshot(FULL);
        assertThat("snapshot() must return data after partial snapshot retrieved", traceData, notNullValue());
    }
}
