package ru.yandex.direct.tracing.real;

import org.junit.Test;

import ru.yandex.direct.tracing.Trace;
import ru.yandex.direct.tracing.TraceProfile;
import ru.yandex.direct.tracing.util.MockedTraceClockProvider;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class RealTraceChangeServiceAndMethodTest {
    @Test
    public void profileServiceAndMethod_EqualsTraceServiceAndMethod() {
        MockedTraceClockProvider clock = new MockedTraceClockProvider(1462454680);
        Trace trace = RealTrace.builder()
                .withClock(clock)
                .withInfo("firstservice", "firstmethod", "currenttags")
                .build();

        try (TraceProfile profile = trace.profile("myfunc")) {
            assertThat("Profile service not equals Trace service", profile.getService(), is("firstservice"));
            assertThat("Profile method not equals Trace method", profile.getMethod(), is("firstmethod"));
        }
    }

    @Test
    public void profileServiceAndMethod_EqualsTraceServiceAndMethodAfterChange() {
        MockedTraceClockProvider clock = new MockedTraceClockProvider(1462454680);
        Trace trace = RealTrace.builder()
                .withClock(clock)
                .withInfo("firstservice", "firstmethod", "currenttags")
                .build();

        trace.setService("anotherservice");
        trace.setMethod("anothermethod");

        try (TraceProfile profile = trace.profile("myfunc")) {
            assertThat("Profile service not equals Trace service", profile.getService(), is("anotherservice"));
            assertThat("Profile method not equals Trace method", profile.getMethod(), is("anothermethod"));
        }
    }

    @Test
    public void nestedProfileServiceAndMethod_EqualsTraceServiceAndMethodAfterChange() {
        MockedTraceClockProvider clock = new MockedTraceClockProvider(1462454680);
        Trace trace = RealTrace.builder()
                .withClock(clock)
                .withInfo("firstservice", "firstmethod", "currenttags")
                .build();

        try (TraceProfile profile = trace.profile("myfunc")) {
            trace.setService("nestedservice");
            trace.setMethod("nestedmethod");

            try (TraceProfile nestedProfile = trace.profile("mynestedfunc")) {
                assertThat("Profile service not equals Trace service", nestedProfile.getService(), is("nestedservice"));
                assertThat("Profile method not equals Trace method", nestedProfile.getMethod(), is("nestedmethod"));
            }
        }
    }
}
