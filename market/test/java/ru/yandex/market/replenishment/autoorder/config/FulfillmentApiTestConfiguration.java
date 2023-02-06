package ru.yandex.market.replenishment.autoorder.config;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ru.yandex.market.ff.client.FulfillmentWorkflowAsyncClientApi;
import ru.yandex.market.ff.client.FulfillmentWorkflowClientApi;

@Configuration
public class FulfillmentApiTestConfiguration {

    @Bean
    public FulfillmentWorkflowClientApi fulfillmentWorkflowClient() {
        return Mockito.mock(FulfillmentWorkflowClientApi.class);
    }

    @Bean
    public FulfillmentWorkflowAsyncClientApi fulfillmentWorkflowAsyncClient() {
        return Mockito.mock(FulfillmentWorkflowAsyncClientApi.class);
    }
}
