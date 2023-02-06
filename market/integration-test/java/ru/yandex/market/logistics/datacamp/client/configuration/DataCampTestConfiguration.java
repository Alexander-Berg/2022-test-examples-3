package ru.yandex.market.logistics.datacamp.client.configuration;

import javax.annotation.ParametersAreNonnullByDefault;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ru.yandex.market.logistics.datacamp.client.DataCampClient;
import ru.yandex.market.logistics.datacamp.client.DataCampClientImpl;
import ru.yandex.market.logistics.util.client.ExternalServiceProperties;
import ru.yandex.market.logistics.util.client.HttpTemplate;
import ru.yandex.market.logistics.util.client.HttpTemplateBuilder;
import ru.yandex.market.logistics.util.client.StatelessTvmTicketProvider;
import ru.yandex.market.request.trace.Module;

@Configuration
public class DataCampTestConfiguration {

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

    @Bean
    public ExternalServiceProperties dataCampProperties(@Value("${data-camp.url}") String url) {
        ExternalServiceProperties properties = new ExternalServiceProperties();
        properties.setUrl(url);
        return properties;
    }

    @Bean
    public HttpTemplate dataCampHttpTemplate(
        ExternalServiceProperties dataCampProperties
    ) {
        return HttpTemplateBuilder.create(dataCampProperties, Module.DATACAMP_STROLLER)
            .withTicketProvider(TICKET_PROVIDER)
            .build();
    }

    @Bean
    public DataCampClient dataCampClient(HttpTemplate dataCampHttpTemplate) {
        return new DataCampClientImpl(dataCampHttpTemplate);
    }
}
