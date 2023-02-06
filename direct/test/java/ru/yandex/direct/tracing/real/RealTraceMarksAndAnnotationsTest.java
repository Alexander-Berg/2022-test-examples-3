package ru.yandex.direct.tracing.real;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.direct.tracing.data.TraceData;
import ru.yandex.direct.tracing.data.TraceDataAnnotation;
import ru.yandex.direct.tracing.data.TraceDataMark;
import ru.yandex.direct.tracing.util.TraceClockProvider;

import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.tracing.Trace.FULL;
import static ru.yandex.direct.tracing.util.TraceUtil.secondsFromNanoseconds;

public class RealTraceMarksAndAnnotationsTest {

    private static final String MARK_MSG_1 = "some message 1";
    private static final String MARK_MSG_2 = "some message 2";
    private static final String ANN_KEY_1 = "some annotation key 1";
    private static final String ANN_VALUE_1 = "some annotation value 1";
    private static final String ANN_KEY_2 = "some annotation key 2";
    private static final String ANN_VALUE_2 = "some annotation value 2";
    private static final long TIME_0 = 1000L;
    private static final long TIME_1 = 2000L;
    private static final long TIME_2 = 7000L;

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
    public void testMark() {
        trace.mark(MARK_MSG_1);
        TraceData traceData = trace.snapshot(FULL);

        TraceDataMark expectedMark = new TraceDataMark(secondsFromNanoseconds(TIME_1 - TIME_0), MARK_MSG_1);
        assertThat("mark is invalid", traceData.getMarks().get(0), beanDiffer(expectedMark));
    }

    @Test
    public void testMultipleMarks() {
        trace.mark(MARK_MSG_1);
        when(traceClockMock.nanoTime()).thenReturn(TIME_2);
        trace.mark(MARK_MSG_2);

        TraceData traceData = trace.snapshot(FULL);

        TraceDataMark expectedMark1 = new TraceDataMark(secondsFromNanoseconds(TIME_1 - TIME_0), MARK_MSG_1);
        TraceDataMark expectedMark2 = new TraceDataMark(secondsFromNanoseconds(TIME_2 - TIME_0), MARK_MSG_2);
        assertThat("marks list is invalid", traceData.getMarks(),
                beanDiffer(Arrays.asList(expectedMark1, expectedMark2)));
    }

    @Test
    public void testAnnotation() {
        trace.annotate(ANN_KEY_1, ANN_VALUE_1);
        TraceData traceData = trace.snapshot(FULL);

        TraceDataAnnotation expectedAnnotation = new TraceDataAnnotation(ANN_KEY_1, ANN_VALUE_1);
        assertThat("annotation is invalid", traceData.getAnnotations().get(0), beanDiffer(expectedAnnotation));
    }

    @Test
    public void testMultipleAnnotations() {
        trace.annotate(ANN_KEY_1, ANN_VALUE_1);
        trace.annotate(ANN_KEY_2, ANN_VALUE_2);
        TraceData traceData = trace.snapshot(FULL);

        TraceDataAnnotation expectedAnnotation1 = new TraceDataAnnotation(ANN_KEY_1, ANN_VALUE_1);
        TraceDataAnnotation expectedAnnotation2 = new TraceDataAnnotation(ANN_KEY_2, ANN_VALUE_2);
        assertThat("annotations list is invalid", traceData.getAnnotations(),
                beanDiffer(Arrays.asList(expectedAnnotation1, expectedAnnotation2)));
    }
}
