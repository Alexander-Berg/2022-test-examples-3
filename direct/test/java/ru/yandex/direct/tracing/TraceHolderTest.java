package ru.yandex.direct.tracing;

import org.junit.After;
import org.junit.Test;

import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.theInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class TraceHolderTest {

    @After
    public void after() {
        clearTraceHolder();
    }

    @Test
    public void pushTraceActivatesTrace() {
        Trace trace = mock(Trace.class);
        TraceHolder.pushTrace(trace);
        verify(trace).activate();
    }

    @Test(expected = NullPointerException.class)
    public void pushNullTraceThrowsException() {
        TraceHolder.pushTrace(null);
    }

    @Test
    public void popTraceDeactivatesTrace() {
        Trace trace = mock(Trace.class);
        TraceHolder.pushTrace(trace);
        TraceHolder.popTrace();
        verify(trace).deactivate();
    }

    @Test
    public void popTraceReturnsLastTraceAfterOneTracePushed() {
        Trace trace = mock(Trace.class);
        TraceHolder.pushTrace(trace);
        Trace returnedTrace = TraceHolder.popTrace();
        assertThat("pop() must return the last trace when pushed one trace", returnedTrace, theInstance(trace));
    }

    @Test
    public void popTraceReturnsLastTraceAfterTwoTracesPushed() {
        Trace trace1 = mock(Trace.class);
        Trace trace2 = mock(Trace.class);
        TraceHolder.pushTrace(trace1);
        TraceHolder.pushTrace(trace2);
        Trace returnedTrace = TraceHolder.popTrace();
        assertThat("pop() must return the last trace when pushed two traces", returnedTrace, theInstance(trace2));
    }

    @Test
    public void popTraceReturnsFirstTraceAfterTwoTracesPushedAndLastTraceRemoved() {
        Trace trace1 = mock(Trace.class);
        Trace trace2 = mock(Trace.class);
        TraceHolder.pushTrace(trace1);
        TraceHolder.pushTrace(trace2);
        TraceHolder.popTrace();

        Trace returnedTrace = TraceHolder.popTrace();
        assertThat("pop() must return the first pushed trace after pushed two traces and the last trace is removed",
                returnedTrace, theInstance(trace1));
    }

    @Test
    public void popTraceReturnsNullBeforePushedAnyTrace() {
        Trace returnedTrace = TraceHolder.popTrace();
        assertThat("pop() must return null before pushed any trace", returnedTrace, nullValue());
    }

    @Test
    public void popTraceReturnsNullAfterTracePushedAndRemoved() {
        Trace trace = mock(Trace.class);
        TraceHolder.pushTrace(trace);
        TraceHolder.popTrace();
        Trace returnedTrace = TraceHolder.popTrace();
        assertThat("pop() must return null after some trace was pushed and removed", returnedTrace, nullValue());
    }

    private void clearTraceHolder() {
        // do not use conditional loop to avoid infinite loop on broken unit
        TraceHolder.popTrace();
        TraceHolder.popTrace();
        TraceHolder.popTrace();
    }
}
