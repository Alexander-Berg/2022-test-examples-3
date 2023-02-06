package ru.yandex.market.api.util.httpclient.clients;

import com.google.common.base.Strings;
import org.springframework.core.env.PropertySourcesPropertyResolver;
import ru.yandex.market.api.config.AppPropertySourcesPlaceholderConfigurer;
import ru.yandex.market.api.util.httpclient.spi.HttpExpectations;
import ru.yandex.market.api.util.httpclient.spi.HttpRequestExpectationBuilder;
import ru.yandex.market.api.util.httpclient.spi.HttpResponseConfigurer;

import javax.inject.Inject;
import java.util.function.Function;

public abstract class AbstractTestClient {

    @Inject
    protected HttpExpectations httpExpectations;

    private PropertySourcesPropertyResolver properties;

    protected HttpResponseConfigurer configure(Function<HttpRequestExpectationBuilder, HttpRequestExpectationBuilder> fn) {
        return httpExpectations.configure(fn.apply(getBuilderByConfigurationName(resolveConfigurationName())));
    }

    private HttpRequestExpectationBuilder getBuilderByConfigurationName(String configurationName) {
        String property = "external." + configurationName + ".url";
        String url = properties.getProperty(property);
        if (Strings.isNullOrEmpty(url)) {
            throw new IllegalArgumentException(String.format("cant find property '%s'", property));
        }
        return HttpRequestExpectationBuilder.url(url);
    }

    @Inject
    private void setProperties(AppPropertySourcesPlaceholderConfigurer configurer) {
        this.properties = new PropertySourcesPropertyResolver(configurer.getAppliedPropertySources());
    }

    protected abstract String resolveConfigurationName();
}
