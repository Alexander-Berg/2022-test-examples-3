package ru.yandex.market.crm.campaign.test;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ru.yandex.market.mcrm.http.HttpClientFactory;
import ru.yandex.market.mcrm.http.HttpEnvironment;
import ru.yandex.market.mcrm.http.TestHttpClientFactory;
import ru.yandex.market.mcrm.http.tvm.TvmApplicationDescriptorHolder;

import static org.mockito.Mockito.mock;

/**
 * @author apershukov
 */
@Configuration
public class TestNetConfig {

    @Bean
    public HttpClientFactory httpClientFactory(HttpEnvironment environment, ConfigurableBeanFactory beanFactory) {
        return new TestHttpClientFactory(environment, beanFactory);
    }

    @Bean
    public HttpEnvironment httpEnvironment() {
        return new HttpEnvironment();
    }

    @Bean
    public TvmApplicationDescriptorHolder tvmApplicationDescriptorHolder() {
        return mock(TvmApplicationDescriptorHolder.class);
    }
}
