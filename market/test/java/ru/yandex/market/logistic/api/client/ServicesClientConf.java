package ru.yandex.market.logistic.api.client;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import ru.yandex.market.logistic.api.RestTemplateConfiguration;
import ru.yandex.market.logistic.api.utils.HttpTemplate;

@Configuration
@Import(RestTemplateConfiguration.class)
@TestPropertySource("classpath:test.properties")
public class ServicesClientConf {

    public static final String UNIQ_FIXED = "6ea161f870ba6574d3bd9bdd19e1e9d8";

    @Value("${logistic.api.yado.host}")
    protected String yadoHost;

    @Bean
    public DeliveryServiceClient deliveryServiceClient(
        @Qualifier("xmlHttpTemplate") HttpTemplate xmlHttpTemplate) {
        return LogisticApiClientFactory.createDeliveryServiceClient(
            xmlHttpTemplate,
            () -> UNIQ_FIXED);
    }

    @Bean
    public FulfillmentClient fulfillmentClient(
        @Qualifier("xmlHttpTemplate") HttpTemplate xmlHttpTemplate) {
        return LogisticApiClientFactory.createFulfillmentClient(
            xmlHttpTemplate,
            () -> UNIQ_FIXED);
    }
}
