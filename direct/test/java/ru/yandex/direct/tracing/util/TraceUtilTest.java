package ru.yandex.direct.tracing.util;

import org.junit.Test;

import ru.yandex.direct.tracing.Trace;
import ru.yandex.direct.tracing.real.RealTrace;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertTrue;

public class TraceUtilTest {
    @Test
    public void withoutHeader() {
        Trace trace = TraceUtil.traceFromHeader(null, "myservice", "mymethod");
        assertThat(trace, instanceOf(RealTrace.class));
        assertThat(trace.getTraceId(), greaterThan(0L));
        assertThat(trace.getParentId(), is(0L));
        assertThat(trace.getSpanId(), is(trace.getTraceId()));
        assertThat(trace.getService(), is("myservice"));
        assertThat(trace.getMethod(), is("mymethod"));
        assertThat(trace.getSamplerate(), is(0));
        assertThat(trace.getTtl(), is(0));
    }

    @Test
    public void onlyTraceId() {
        Trace trace = TraceUtil.traceFromHeader("123");
        assertThat(trace.getTraceId(), is(123L));
        assertThat(trace.getParentId(), is(123L));
        assertThat(trace.getSpanId(), allOf(greaterThan(0L), not(is(123L))));
    }

    @Test
    public void allZeroWithTtl() {
        Trace trace = TraceUtil.traceFromHeader("0,0,0,123");
        assertThat(trace.getTraceId(), greaterThan(0L));
        assertThat(trace.getParentId(), is(0L));
        assertThat(trace.getSpanId(), is(trace.getTraceId()));
        assertThat(trace.getTtl(), is(123));
    }

    @Test
    public void missingParentIds() {
        Trace trace = TraceUtil.traceFromHeader("0,123,0");
        assertThat(trace.getTraceId(), is(123L));
        assertThat(trace.getParentId(), is(0L));
        assertThat(trace.getSpanId(), is(123L));
    }

    @Test
    public void missingTraceId() {
        Trace trace = TraceUtil.traceFromHeader("0,456,123");
        assertThat(trace.getTraceId(), is(123L));
        assertThat(trace.getParentId(), is(123L));
        assertThat(trace.getSpanId(), is(456L));
    }

    @Test
    public void traceToHeaderTest() {
        Trace trace = TraceUtil.traceFromHeader("1595152853173664079,1595152853173664080,0");
        String headerValue = TraceUtil.traceToHeader(trace.child("foo", "bar"));
        assertTrue(headerValue.matches("^1595152853173664079,[0-9]+,1595152853173664080,0$"));
    }

    @Test
    public void traceToHeaderWithTtlTest() {
        Trace trace = TraceUtil.traceFromHeader("1595152853173664079,1595152853173664080,0");
        String headerValue = TraceUtil.traceToHeader(trace.child("foo", "bar"), 42);
        assertTrue(headerValue.matches("^1595152853173664079,[0-9]+,1595152853173664080,42$"));
    }
}
