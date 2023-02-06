package ru.yandex.market.logistics.cte.client;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.kotlin.KotlinModule;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.client.MockRestServiceServer;

import ru.yandex.market.logistics.util.client.ClientUtilsFactory;
import ru.yandex.market.logistics.util.client.ExternalServiceProperties;
import ru.yandex.market.logistics.util.client.HttpTemplate;
import ru.yandex.market.logistics.util.client.HttpTemplateBuilder;
import ru.yandex.market.logistics.util.client.HttpTemplateImpl;
import ru.yandex.market.logistics.util.client.tvm.client.MockTvmClient;
import ru.yandex.market.request.trace.Module;
import ru.yandex.passport.tvmauth.TvmClient;

@Configuration
@PropertySource("classpath:test.properties")
public class Config {

    @Bean
    public ExternalServiceProperties fulfillmentWorkflowProperties(
        @Value("${fulfillment.cte.api.host}") String host
    ) {
        ExternalServiceProperties properties = new ExternalServiceProperties();
        properties.setUrl(host);
        return properties;
    }

    @Bean
    public MockRestServiceServer mockRestServiceServer(HttpTemplate fulfillmentCteTemplate) {
        return MockRestServiceServer.createServer(((HttpTemplateImpl) fulfillmentCteTemplate).getRestTemplate());
    }

    @Bean
    public TvmClient tvmClientMock() {
        return new MockTvmClient();
    }

    @Bean
    public FulfillmentCteClientApi fulfillmentWorkflowClient(
        @Qualifier("fulfillmentHttpTemplate") HttpTemplate fulfillmentHttpTemplate) {
        return new FulfillmentCteClientHttpTemplate(fulfillmentHttpTemplate);
    }

    @Bean(name = "fulfillmentHttpTemplate")
    public HttpTemplate fulfillmentHttpTemplate(
        ExternalServiceProperties fulfillmentCteProperties,
        TvmClient tvmClient
    ) {
        ObjectMapper objectMapper = ClientUtilsFactory.getObjectMapper();
        objectMapper.registerModule(new KotlinModule());
        return HttpTemplateBuilder.create(fulfillmentCteProperties, Module.FULFILLMENT_CTE)
            .withTicketProvider(tvmClient::getServiceTicketFor)
            .withObjectMapper(objectMapper)
            .withConverters(
                Arrays.asList(new MappingJackson2HttpMessageConverter(objectMapper),
                    new ByteArrayHttpMessageConverter(),
                    new StringHttpMessageConverter(StandardCharsets.UTF_8)))
            .build();
    }
}
