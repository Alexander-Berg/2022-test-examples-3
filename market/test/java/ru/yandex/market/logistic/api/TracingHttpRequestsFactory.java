package ru.yandex.market.logistic.api;

import javax.annotation.Nonnull;

import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;

import ru.yandex.market.request.httpclient.trace.TraceHttpRequestInterceptor;
import ru.yandex.market.request.httpclient.trace.TraceHttpResponseInterceptor;
import ru.yandex.market.request.trace.Module;

public class TracingHttpRequestsFactory extends HttpComponentsClientHttpRequestFactory {

    public TracingHttpRequestsFactory(@Nonnull Module module) {
        super(HttpClientBuilder.create()
            .addInterceptorFirst(new TraceHttpRequestInterceptor(module))
            .addInterceptorFirst(new TraceHttpResponseInterceptor())
            .disableAutomaticRetries()
            .build());
    }
}
