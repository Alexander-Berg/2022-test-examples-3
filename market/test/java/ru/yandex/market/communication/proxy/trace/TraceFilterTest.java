package ru.yandex.market.communication.proxy.trace;

import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClientConfig;
import org.asynchttpclient.RequestBuilder;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import ru.yandex.market.request.trace.Module;
import ru.yandex.market.request.trace.RequestTraceUtil;

/**
 * @author imelnikov
 * @since 14.12.2021
 */
public class TraceFilterTest {

    @Test
    public void testTraceLogging() {
        var traceFilter = new TraceResponseFilter(Module.TELEPHONY_PLATFORM);
        DefaultAsyncHttpClientConfig config = new DefaultAsyncHttpClientConfig
                .Builder()
                .addRequestFilter(new TraceRequestFilter())
                .addResponseFilter(traceFilter)
                .build();
        var client = new DefaultAsyncHttpClient(config);

        try (MockedStatic<RequestTraceUtil> mocked = Mockito.mockStatic(RequestTraceUtil.class)) {

            String url = "http://sandbox.yandex-team.ru"; // any url available from CI env
            client.executeRequest(new RequestBuilder().setUrl(url).build());

            mocked.verify(RequestTraceUtil::generateRequestId);
        }
    }
}
