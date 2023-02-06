package ru.yandex.market.jmf.http.test.impl;

import org.springframework.context.EmbeddedValueResolverAware;
import org.springframework.stereotype.Component;
import org.springframework.util.StringValueResolver;

import ru.yandex.market.jmf.http.HttpClient;
import ru.yandex.market.jmf.http.HttpClientFactory;

/**
 * @author apershukov
 */
@Component
public class TestHttpClientFactory implements HttpClientFactory, EmbeddedValueResolverAware {

    private final HttpEnvironment environment;
    private StringValueResolver resolver;

    public TestHttpClientFactory(HttpEnvironment environment) {
        this.environment = environment;
    }

    @Override
    public HttpClient create(String name) {
        return new TestHttpClientImpl(environment, resolver.resolveStringValue("${external." + name + ".url}"));
    }

    @Override
    public void setEmbeddedValueResolver(StringValueResolver resolver) {
        this.resolver = resolver;
    }
}
