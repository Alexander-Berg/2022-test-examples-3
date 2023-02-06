package ru.yandex.market.delivery.transport_manager.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;

import ru.yandex.market.delivery.transport_manager.config.ffwf.FulfillmentWorkflowProperties;

public class FulfillmentWorkflowConfiguration {
    @Bean
    @ConfigurationProperties("fulfillment.workflow.api")
    public FulfillmentWorkflowProperties fulfillmentWorkflowProperties() {
        return new FulfillmentWorkflowProperties();
    }
}
