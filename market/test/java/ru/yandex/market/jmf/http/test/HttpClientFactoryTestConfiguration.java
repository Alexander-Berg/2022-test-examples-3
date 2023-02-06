package ru.yandex.market.jmf.http.test;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import ru.yandex.market.jmf.http.HttpClientFactory;
import ru.yandex.market.jmf.http.HttpClientFilterStrategy;
import ru.yandex.market.jmf.http.HttpConfiguration;
import ru.yandex.market.jmf.http.impl.HttpClientFactoryImpl;
import ru.yandex.market.jmf.module.metric.test.MetricsModuleTestConfiguration;
import ru.yandex.market.jmf.utils.serialize.SerializationService;

@Configuration
@Import({
        HttpConfiguration.class,
        MetricsModuleTestConfiguration.class
})
public class HttpClientFactoryTestConfiguration {
    @Bean
    @Primary
    public HttpClientFactory realHttpClientFactory(ConfigurableBeanFactory beanFactory,
                                                   List<HttpClientFilterStrategy> filterStrategies,
                                                   Map<String, SerializationService> serializationServices) {
        return new HttpClientFactoryImpl(beanFactory, filterStrategies, serializationServices);
    }
}
