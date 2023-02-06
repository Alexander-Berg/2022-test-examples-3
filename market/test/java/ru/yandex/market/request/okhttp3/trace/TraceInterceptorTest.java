package ru.yandex.market.request.okhttp3.trace;

import java.io.IOException;

import javax.annotation.Nullable;

import okhttp3.Interceptor;
import okhttp3.Request;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;

import ru.yandex.market.request.SourceModuleUtil;
import ru.yandex.market.request.trace.Module;
import ru.yandex.market.request.trace.RequestContextHolder;
import ru.yandex.market.request.trace.RequestTraceUtil;

/**
 * Тесты для {@link TraceInterceptor}.
 *
 * @author Kirill Batalin (batalin@yandex-team.ru)
 */
class TraceInterceptorTest {

    @BeforeEach
    void init() {
        RequestContextHolder.createNewContext();
    }

    @Test
    void testWithSourceModule() throws IOException {
        checkHttpHeaders(Module.MBI_API);
    }

    @Test
    void testWithoutSourceModule() throws IOException {
        checkHttpHeaders(null);
    }

    private void checkHttpHeaders(@Nullable Module sourceModule) throws IOException {
        TraceInterceptor traceInterceptor = new TraceInterceptor(sourceModule, Module.MBI_ADMIN);
        Interceptor.Chain chain = mockChain();
        traceInterceptor.intercept(chain);

        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        Mockito.verify(chain).proceed(requestCaptor.capture());
        Request actualRequest = requestCaptor.getValue();

        if (sourceModule != null) {
            Assertions.assertEquals(Module.MBI_API.name(), actualRequest.header(SourceModuleUtil.SOURCE_MODULE_HEADER));
        } else {
            Assertions.assertNull(actualRequest.header(SourceModuleUtil.SOURCE_MODULE_HEADER));
        }
        Assertions.assertEquals(
                RequestContextHolder.getContext().getRequestId() + "/1",
                actualRequest.header(RequestTraceUtil.REQUEST_ID_HEADER)
        );
    }

    private Interceptor.Chain mockChain() {
        Interceptor.Chain chain = Mockito.mock(Interceptor.Chain.class);
        Mockito.when(chain.request()).thenReturn(new Request.Builder().url("http://local.loc").build());
        return chain;
    }
}
