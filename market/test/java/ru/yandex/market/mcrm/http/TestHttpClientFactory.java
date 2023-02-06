package ru.yandex.market.mcrm.http;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;

/**
 * @author apershukov
 */
public class TestHttpClientFactory implements HttpClientFactory {

    private final HttpEnvironment environment;
    private final ConfigurableBeanFactory beanFactory;

    public TestHttpClientFactory(HttpEnvironment environment, ConfigurableBeanFactory beanFactory) {
        this.environment = environment;
        this.beanFactory = beanFactory;
    }

    @Override
    public HttpClient create(String name) {
        return new TestHttpClientImpl(environment, beanFactory.resolveEmbeddedValue("${external." + name + ".url}"));
    }
}
