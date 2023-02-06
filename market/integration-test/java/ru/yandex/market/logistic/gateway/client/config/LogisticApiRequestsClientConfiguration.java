package ru.yandex.market.logistic.gateway.client.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.market.logistic.api.model.common.request.Token;
import ru.yandex.market.logistic.api.utils.UniqService;
import ru.yandex.market.logistic.gateway.client.LogisticApiRequestsClient;
import ru.yandex.market.logistic.gateway.client.LogisticApiRequestsClientFactory;
import ru.yandex.market.logistics.util.client.HttpTemplate;

@Configuration
@Import(RestTemplateConfig.class)
public class LogisticApiRequestsClientConfiguration {

    @Bean
    public LogisticApiRequestsClient getLogisticApiRequestsClient(@Value("${lgw.token}") Token token,
                                                                  HttpTemplate xmlHttpTemplate,
                                                                  UniqService uniqService) {
        return LogisticApiRequestsClientFactory.create(token, xmlHttpTemplate, uniqService);
    }
}
