package ru.yandex.market.jmf.http.test.impl;

import org.springframework.context.EmbeddedValueResolverAware;
import org.springframework.stereotype.Component;
import org.springframework.util.StringValueResolver;

import ru.yandex.market.jmf.http.HttpClient;
import ru.yandex.market.jmf.http.HttpClientFactory;
import ru.yandex.market.jmf.utils.serialize.ObjectSerializeService;
import ru.yandex.market.jmf.utils.serialize.SerializationService;

/**
 * @author apershukov
 */
@Component
public class TestHttpClientFactory implements HttpClientFactory, EmbeddedValueResolverAware {

    private final HttpEnvironment environment;
    private StringValueResolver resolver;

    private SerializationService serializationService;

    public TestHttpClientFactory(HttpEnvironment environment, ObjectSerializeService serializationService) {
        this.environment = environment;
        this.serializationService = serializationService;
    }

    @Override
    public HttpClient create(String name) {
        return new TestHttpClientImpl(
                environment,
                resolver.resolveStringValue("${external." + name + ".url}"),
                serializationService
        );
    }

    @Override
    public void setEmbeddedValueResolver(StringValueResolver resolver) {
        this.resolver = resolver;
    }
}
