package ru.yandex.market.delivery.tracker.configuration;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.delivery.tracker.configuration.lgw.LgwRestTemplateConfiguration;

@Configuration
public class LgwRestTemplateMockConfiguration {
    private static final int CONNECT_TIMEOUT = 40000;
    private static final int READ_TIMEOUT = 3000;
    private static final int DEFAULT_MAX_POOL_SIZE = 10;
    private static final int DEFAULT_ROUTE_POOL_SIZE = 5;

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
    public RestTemplate lgwRestTemplate() {
        return LgwRestTemplateConfiguration.initializeLgwRestTemplate(
            CONNECT_TIMEOUT, READ_TIMEOUT, DEFAULT_MAX_POOL_SIZE, DEFAULT_ROUTE_POOL_SIZE);
    }
}
