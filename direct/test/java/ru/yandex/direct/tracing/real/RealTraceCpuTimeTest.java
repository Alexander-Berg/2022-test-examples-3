package ru.yandex.direct.tracing.real;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.direct.tracing.data.TraceData;
import ru.yandex.direct.tracing.util.ThreadUsedResources;
import ru.yandex.direct.tracing.util.ThreadUsedResourcesProvider;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.tracing.Trace.FULL;
import static ru.yandex.direct.tracing.util.TraceUtil.secondsFromNanoseconds;

public class RealTraceCpuTimeTest {

    private static final long TRACE_ID = 1L;

    private ThreadUsedResources startTimeMock;
    private ThreadUsedResources finishTimeMock;
    private RealTrace trace;

    @Before
    public void before() {
        ThreadUsedResourcesProvider timeProviderMock = mock(ThreadUsedResourcesProvider.class);

        trace = RealTrace.builder()
                .withTraceId(TRACE_ID)
                .withThreadCpuTimeProvider(timeProviderMock)
                .build();

        startTimeMock = createThreadCpuTimeMock(100L, 80L);
        finishTimeMock = createThreadCpuTimeMock(200L, 140L);

        when(timeProviderMock.getCurrentThreadCpuTime()).thenReturn(startTimeMock);
        trace.activate();

        when(timeProviderMock.getCurrentThreadCpuTime()).thenReturn(finishTimeMock);
        trace.deactivate();
    }

    @Test
    public void computesCpuUserTimeForOneThread() {
        TraceData traceData = trace.snapshot(FULL);

        long userDelta = finishTimeMock.getUserTime() - startTimeMock.getUserTime();
        assertThat("cpu user time for one-thread-usage is invalid",
                traceData.getTimes().getCpuUserTime(), is(secondsFromNanoseconds(userDelta)));
    }

    @Test
    public void computesCpuSystemTimeForOneThread() {
        TraceData traceData = trace.snapshot(FULL);

        long systemDelta = finishTimeMock.getSystemTime() - startTimeMock.getSystemTime();
        assertThat("cpu system time for one-thread-usage is invalid",
                traceData.getTimes().getCpuSystemTime(), is(secondsFromNanoseconds(systemDelta)));
    }

    private ThreadUsedResources createThreadCpuTimeMock(long cpuTime, long userTime) {
        ThreadUsedResources cpuTimeMock = mock(ThreadUsedResources.class);
        when(cpuTimeMock.getCpuTime()).thenReturn(cpuTime);
        when(cpuTimeMock.getUserTime()).thenReturn(userTime);
        when(cpuTimeMock.getSystemTime()).thenReturn(cpuTime - userTime);
        return cpuTimeMock;
    }
}
