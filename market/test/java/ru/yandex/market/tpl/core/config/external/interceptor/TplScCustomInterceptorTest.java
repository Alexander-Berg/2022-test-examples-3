package ru.yandex.market.tpl.core.config.external.interceptor;

import lombok.SneakyThrows;
import org.apache.http.HttpRequest;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.message.BasicHttpRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.yandex.market.tpl.core.config.external.interceptor.TplScCustomInterceptor.SC_INCREASED_SOCKET_TIMEOUT_MS;
import static ru.yandex.market.tpl.core.config.external.interceptor.TplScCustomInterceptor.SC_INTERNAL_ORDERS_ROUTING_RESULTS_PATH;

class TplScCustomInterceptorTest {

    public static final int ORIGINAL_SOCKET_TIMEOUT_MS = 10_000;
    public static final int ORIGINAL_CONNECT_TIMEOUT_MS = 15_000;
    public static final String INAPPROPRIATE_PATH = "INAPPROPRIATE_PATH";

    private final TplScCustomInterceptor scCustomInterceptor = new TplScCustomInterceptor();

    private final RequestConfig originalRequestConfig = RequestConfig.custom()
            .setSocketTimeout(ORIGINAL_SOCKET_TIMEOUT_MS)
            .setConnectTimeout(ORIGINAL_CONNECT_TIMEOUT_MS)
            .build();

    @Test
    @SneakyThrows
    void updateTimeout_success_whenAppropriateRequest() {
        //given
        HttpClientContext context = new HttpClientContext();
        context.setRequestConfig(originalRequestConfig);

        HttpRequest httpRequest = new BasicHttpRequest("GET", SC_INTERNAL_ORDERS_ROUTING_RESULTS_PATH);
        //when
        scCustomInterceptor.process(httpRequest, context);

        //then
        assertEquals(SC_INCREASED_SOCKET_TIMEOUT_MS, context.getRequestConfig().getSocketTimeout());
        assertEquals(ORIGINAL_CONNECT_TIMEOUT_MS, context.getRequestConfig().getConnectTimeout());
    }

    @Test
    @SneakyThrows
    void skipUpdateTimeout_whenInappropriateRequest() {
        //given
        HttpClientContext context = new HttpClientContext();
        context.setRequestConfig(originalRequestConfig);

        HttpRequest httpRequest = new BasicHttpRequest("GET", INAPPROPRIATE_PATH);
        //when
        scCustomInterceptor.process(httpRequest, context);

        //then
        assertEquals(ORIGINAL_SOCKET_TIMEOUT_MS, context.getRequestConfig().getSocketTimeout());
        assertEquals(ORIGINAL_CONNECT_TIMEOUT_MS, context.getRequestConfig().getConnectTimeout());
    }

    @Test
    void skipUpdateTimeout_whenNull() {
        //when
        assertDoesNotThrow( () -> scCustomInterceptor.process(null, null));
    }
}
