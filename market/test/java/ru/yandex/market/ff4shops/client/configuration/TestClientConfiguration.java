package ru.yandex.market.ff4shops.client.configuration;

import javax.annotation.ParametersAreNonnullByDefault;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.yandex.market.ff4shops.client.FF4ShopsClient;
import ru.yandex.market.ff4shops.client.FF4ShopsClientImpl;
import ru.yandex.market.logistics.util.client.ExternalServiceProperties;
import ru.yandex.market.logistics.util.client.HttpTemplate;
import ru.yandex.market.logistics.util.client.HttpTemplateBuilder;
import ru.yandex.market.logistics.util.client.StatelessTvmTicketProvider;
import ru.yandex.market.request.trace.Module;

@Configuration
public class TestClientConfiguration {
    @ParametersAreNonnullByDefault
    private static final StatelessTvmTicketProvider TICKET_PROVIDER = new StatelessTvmTicketProvider() {

        @Override
        public String provideServiceTicket(Integer tvmServiceId) {
            return "test-service-ticket";
        }

        @Override
        public String provideUserTicket(Integer tvmServiceId) {
            return "test-user-ticket";
        }
    };

    @Autowired
    private ObjectMapper objectMapper;

    @Bean
    @ConfigurationProperties("ff4shops.api")
    public ExternalServiceProperties testFF4ShopsProperties() {
        return new ExternalServiceProperties();
    }

    @Bean
    public FF4ShopsClient internalClient(HttpTemplate httpTemplate) {
        return new FF4ShopsClientImpl(httpTemplate, objectMapper);
    }

    @Bean
    public HttpTemplate httpTemplate(
            @Qualifier("testFF4ShopsProperties") ExternalServiceProperties ff4shopsProperties
    ) {
        return HttpTemplateBuilder.create(ff4shopsProperties, Module.FF4SHOPS)
                .withTicketProvider(TICKET_PROVIDER)
                .build();
    }
}
