package ru.yandex.market.logistics.util.client.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.logistics.util.client.HttpTemplate;
import ru.yandex.market.logistics.util.client.HttpTemplateImpl;
import ru.yandex.market.logistics.util.client.SimpleClient;
import ru.yandex.market.logistics.util.client.SpringClientUtilsFactory;
import ru.yandex.market.logistics.util.client.TvmTicketProvider;
import ru.yandex.market.request.trace.Module;

@Configuration
public class TestClientConfig {

    @Value("${test.api.url}")
    private String host;

    @Bean
    public HttpTemplate httpTemplate() {
        return new HttpTemplateImpl(host, clientRestTemplate(), tvmTicketProvider());
    }

    @Bean
    public RestTemplate clientRestTemplate() {
        return SpringClientUtilsFactory.createRestTemplate(5000, 60000, Module.LOGISTICS_LOM);
    }

    @Bean
    public TvmTicketProvider tvmTicketProvider() {
        return new TvmTicketProvider() {
            @Override
            public String provideServiceTicket() {
                return "test-service-ticket";
            }

            @Override
            public String provideUserTicket() {
                return "test-user-ticket";
            }
        };
    }

    @Bean
    public SimpleClient simpleClient() {
        return new SimpleClient(httpTemplate());
    }

}
