package ru.yandex.market.logistics.delivery.calculator.client.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.logistics.delivery.calculator.client.DeliveryCalculatorSearchEngineClient;
import ru.yandex.market.logistics.delivery.calculator.client.DeliveryCalculatorSearchEngineClientImpl;
import ru.yandex.market.logistics.util.client.HttpTemplateImpl;
import ru.yandex.market.logistics.util.client.SpringClientUtilsFactory;
import ru.yandex.market.logistics.util.client.TvmTicketProvider;
import ru.yandex.market.request.trace.Module;

@Configuration
@MockBean({
    TvmTicketProvider.class,
})
public class TestClientConfig {

    @Bean
    public RestTemplate clientRestTemplate() {
        return SpringClientUtilsFactory.createRestTemplate(
            0,
            0,
            Module.MBI_DELIVERY_CALCULATOR_SEARCH_ENGINE
        );
    }

    @Bean
    public MockRestServiceServer mockServer(RestTemplate clientRestTemplate) {
        return MockRestServiceServer.createServer(clientRestTemplate);
    }

    @Bean
    public DeliveryCalculatorSearchEngineClient searchEngineClient(
        @Value("${delivery-calculator.api.url}") String host,
        RestTemplate clientRestTemplate,
        TvmTicketProvider tvmTicketProvider
    ) {
        return new DeliveryCalculatorSearchEngineClientImpl(new HttpTemplateImpl(
            host,
            clientRestTemplate,
            MediaType.APPLICATION_JSON,
            tvmTicketProvider
        ));
    }

}
