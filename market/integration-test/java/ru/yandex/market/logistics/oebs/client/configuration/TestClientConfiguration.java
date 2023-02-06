package ru.yandex.market.logistics.oebs.client.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.logistics.oebs.client.OebsClient;
import ru.yandex.market.logistics.oebs.client.OebsClientImpl;
import ru.yandex.market.logistics.util.client.ClientUtilsFactory;
import ru.yandex.market.logistics.util.client.HttpTemplateImpl;
import ru.yandex.market.logistics.util.client.SpringClientUtilsFactory;
import ru.yandex.market.logistics.util.client.TvmTicketProvider;
import ru.yandex.market.request.trace.Module;

@Configuration
@MockBean({
    TvmTicketProvider.class,
})
public class TestClientConfiguration {
    @Bean
    public RestTemplate clientRestTemplate() {
        return SpringClientUtilsFactory.createRestTemplate(
            0,
            0,
            Module.OEBS
        );
    }

    @Bean
    public MockRestServiceServer mockServer(RestTemplate clientRestTemplate) {
        return MockRestServiceServer.createServer(clientRestTemplate);
    }

    @Bean
    public ObjectMapper objectMapper() {
        return ClientUtilsFactory.getObjectMapper()
            .registerModule(new JavaTimeModule())
            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    }

    @Bean
    public OebsClient oebsClient(
        @Value("${oebs.api.url}") String host,
        @Value("${oebs.api.token}") String token,
        RestTemplate clientRestTemplate,
        TvmTicketProvider tvmTicketProvider,
        ObjectMapper objectMapper
    ) {
        return new OebsClientImpl(
            new HttpTemplateImpl(
                host,
                clientRestTemplate,
                MediaType.APPLICATION_JSON,
                tvmTicketProvider
            ),
            token,
            objectMapper
        );
    }
}
