package ru.yandex.market.logistics.logging.rest.transactional;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.logistics.logging.rest.transactional.json.EnableRestInTransactionMonitoring;

@EnableAspectJAutoProxy(proxyTargetClass = true)
@EnableRestInTransactionMonitoring
@Configuration
public class TestConfiguration {
    @Bean
    public ServiceWithRestCall service(RestTemplate restTemplate) {
        return new ServiceWithRestCall(restTemplate);
    }

    @Bean
    public RestTemplate restTemplate() {
        return Mockito.mock(RestTemplate.class);
    }
}
