package ru.yandex.market.api.internal.djviewer;

import java.util.function.Function;

import org.springframework.stereotype.Service;

import ru.yandex.market.api.util.httpclient.clients.AbstractFixedConfigurationTestClient;
import ru.yandex.market.api.util.httpclient.spi.HttpRequestExpectationBuilder;
import ru.yandex.market.api.util.httpclient.spi.HttpResponseConfigurer;

@Service
public class DjViewerTestClient extends AbstractFixedConfigurationTestClient {

    protected DjViewerTestClient() {
        super("DjViewer");
    }

    public HttpResponseConfigurer doRequest(
            Function<HttpRequestExpectationBuilder, HttpRequestExpectationBuilder> fn
    ) {
        return configure(fn);
    }

}
