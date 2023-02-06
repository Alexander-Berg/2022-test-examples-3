package ru.yandex.market.jmf.http.test;

import java.util.List;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import ru.yandex.market.jmf.http.HttpClientFactory;
import ru.yandex.market.jmf.http.HttpClientFilterStrategy;
import ru.yandex.market.jmf.http.HttpConfiguration;
import ru.yandex.market.jmf.http.impl.HttpClientFactoryImpl;
import ru.yandex.market.jmf.queue.retry.RetryServiceTestConfiguration;
import ru.yandex.market.jmf.queue.retry.RetryTaskService;

@Configuration
@Import({
        HttpConfiguration.class,
        RetryServiceTestConfiguration.class,
})
public class HttpClientFactoryTestConfiguration {
    @Bean
    @Primary
    public HttpClientFactory realHttpClientFactory(ConfigurableBeanFactory beanFactory,
                                                   RetryTaskService retryTasksService,
                                                   List<HttpClientFilterStrategy> filterStrategies) {
        return new HttpClientFactoryImpl(beanFactory, retryTasksService, filterStrategies);
    }
}
