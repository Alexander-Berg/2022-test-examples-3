package ru.yandex.market.crm.platform.api.test.net;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ru.yandex.market.mcrm.http.HttpClientFactory;
import ru.yandex.market.mcrm.http.HttpEnvironment;
import ru.yandex.market.mcrm.http.TestHttpClientFactory;

/**
 * @author apershukov
 */
@Configuration
public class TestNetConfiguration {

    @Bean
    public HttpClientFactory httpClientFactory(HttpEnvironment environment, ConfigurableBeanFactory beanFactory) {
        return new TestHttpClientFactory(environment, beanFactory);
    }

    @Bean
    public HttpEnvironment httpEnvironment() {
        return new HttpEnvironment();
    }
}
