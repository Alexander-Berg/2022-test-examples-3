package ru.yandex.direct.tracing.real;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.direct.tracing.Trace;
import ru.yandex.direct.tracing.TraceChild;
import ru.yandex.direct.tracing.TraceProfile;
import ru.yandex.direct.tracing.data.TraceData;
import ru.yandex.direct.tracing.data.TraceDataAnnotation;
import ru.yandex.direct.tracing.data.TraceDataChild;
import ru.yandex.direct.tracing.data.TraceDataMark;
import ru.yandex.direct.tracing.data.TraceDataProfile;
import ru.yandex.direct.tracing.util.MockedTraceClockProvider;

import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;

/**
 * Created by snaury on 25/04/16.
 */
public class RealTraceTest {
    @Test
    public void simpleTrace() {
        MockedTraceClockProvider clock = new MockedTraceClockProvider(1462454680);
        Trace trace = RealTrace.builder()
                .withClock(clock)
                .withInfo("currentservice", "currentmethod", "currenttags")
                .build();

        clock.advance(500, TimeUnit.MILLISECONDS);
        try (TraceChild child = trace.child("otherservice", "othermethod")) {
            clock.advance(500, TimeUnit.MILLISECONDS);
        }
        try (TraceProfile profile = trace.profile("myfunc")) {
            clock.advance(500, TimeUnit.MILLISECONDS);
        }
        clock.advance(500, TimeUnit.MILLISECONDS);
        trace.mark("mymark");
        clock.advance(500, TimeUnit.MILLISECONDS);
        trace.annotate("mykey", "myvalue");

        TraceData data = trace.snapshot(false);
        TraceData expected = new TraceData();
        expected.setLogTime(Instant.parse("2016-05-05T13:24:42.500Z"));
        expected.setService("currentservice");
        expected.setMethod("currentmethod");
        expected.setTags("currenttags");
        expected.setChunkIndex(1);
        expected.setChunkFinal(true);
        expected.setAllEla(2.5);
        expected.setSamplerate(1);
        expected.getTimes().setEla(2.5);
        expected.getProfiles().add(new TraceDataProfile("myfunc", "", 0.5, 0.0, 1, 0));
        expected.getChildren().add(new TraceDataChild("otherservice", "othermethod", 0, 0.5, 0.5));
        expected.getMarks().add(new TraceDataMark(2.0, "mymark"));
        expected.getAnnotations().add(new TraceDataAnnotation("mykey", "myvalue"));

        assertThat(data, beanDiffer(expected).useCompareStrategy(DefaultCompareStrategies.allFieldsExcept(
                newPath("host"),
                newPath("traceId"),
                newPath("parentId"),
                newPath("spanId"),
                newPath("times", "mem"),
                newPath("children", "0", "spanId")
        )));
    }
}
