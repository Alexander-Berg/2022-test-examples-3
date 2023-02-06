package ru.yandex.market.mboc.app.util;

import java.lang.reflect.Method;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.mboc.http.MboCategory;
import ru.yandex.market.mboc.http.MboCategoryService;
import ru.yandex.market.mboc.http.MboCategoryServiceHandler;

/**
 * @author yuramalinov
 * @created 10.02.2020
 */
public class ProtoMetricsTest {
    private static final Method PROTO_METHOD;
    private static final Method PING_METHOD;

    private ProtoMetricsContext context;
    private MboCategoryService serviceMock;
    private MboCategoryService service;

    private ProtoMetricsStatistics statistics;

    static {
        try {
            PROTO_METHOD = MboCategoryService.class.getMethod(
                "getContentCommentTypes", MboCategory.ContentCommentTypes.Request.class);
            PING_METHOD = MboCategoryService.class.getMethod("ping");
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    @Before
    public void setup() {
        context = new ProtoMetricsContext()
            .setRegistry(new SimpleMeterRegistry());
        serviceMock = Mockito.mock(MboCategoryService.class);
        var handlerWrapper = ProtoMetrics.wrap(serviceMock, MboCategoryServiceHandler::new, context);
        service = handlerWrapper.getService();
        statistics = handlerWrapper.getStatistics();
    }

    @Test
    public void testWrapProto() {
        service.getContentCommentTypes(MboCategory.ContentCommentTypes.Request.newBuilder().build());

        var timer = statistics.getTimer(PROTO_METHOD, null);
        var error = statistics.getCounter(PROTO_METHOD, null);

        Assertions.assertThat(error).extracting(Counter::count).isEqualTo(0d);
        Assertions.assertThat(timer).extracting(Timer::count).isEqualTo(1L);
    }

    @Test
    public void testErrors() {
        Mockito.when(serviceMock.getContentCommentTypes(Mockito.any())).thenThrow(new RuntimeException("Failed!"));

        Assertions.assertThatThrownBy(
            () -> service.getContentCommentTypes(MboCategory.ContentCommentTypes.Request.newBuilder().build())
        ).hasMessage("Failed!");

        var timer = statistics.getTimer(PROTO_METHOD, null);
        var error = statistics.getCounter(PROTO_METHOD, null);

        Assertions.assertThat(error).extracting(Counter::count).isEqualTo(1d);
        Assertions.assertThat(timer).extracting(Timer::count).isEqualTo(1L);
    }

    @Test
    public void testNotInterfaceMethod() {
        service.ping();

        var timer = statistics.getTimer(PING_METHOD, null);
        var error = statistics.getCounter(PING_METHOD, null);

        Assertions.assertThat(error).isNull();
        Assertions.assertThat(timer).isNull();
    }

    @Test
    public void testWrapByDifferentUserAgents() {
        // no agent
        service.getContentCommentTypes(MboCategory.ContentCommentTypes.Request.newBuilder().build());

        // empty
        context.setUserAgent("");
        service.getContentCommentTypes(MboCategory.ContentCommentTypes.Request.newBuilder().build());

        // normal
        var userAgent3 = "normal";
        context.setUserAgent(userAgent3);
        service.getContentCommentTypes(MboCategory.ContentCommentTypes.Request.newBuilder().build());
        service.getContentCommentTypes(MboCategory.ContentCommentTypes.Request.newBuilder().build());

        // extra-long
        var userAgent4 = "extra-long-extra-long-extra-long-extra-long-extra-long-extra-long-extra-long\n" +
            "extra-long-extra-long-extra-long-extra-long-extra-long-extra-long-extra-long";
        context.setUserAgent(userAgent4);
        service.getContentCommentTypes(MboCategory.ContentCommentTypes.Request.newBuilder().build());

        var timer1 = statistics.getTimer(PROTO_METHOD, null);
        var timer2 = statistics.getTimer(PROTO_METHOD, "");
        var timer3 = statistics.getTimer(PROTO_METHOD, userAgent3);
        var timer4 = statistics.getTimer(PROTO_METHOD, userAgent4);
        var timer5 = statistics.getTimer(PROTO_METHOD, "not-exists");

        Assertions.assertThat(timer1).extracting(Timer::count).isEqualTo(1L);
        Assertions.assertThat(timer2).extracting(Timer::count).isEqualTo(1L);
        Assertions.assertThat(timer3).extracting(Timer::count).isEqualTo(2L);
        Assertions.assertThat(timer4).extracting(Timer::count).isEqualTo(1L);
        Assertions.assertThat(timer5).isNull();
    }
}
