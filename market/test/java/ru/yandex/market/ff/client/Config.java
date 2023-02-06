package ru.yandex.market.ff.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.test.web.client.MockRestServiceServer;

import ru.yandex.market.ff.client.tvm.config.FulfillmentWorkflowAsyncClientConfiguration;
import ru.yandex.market.logistics.util.client.ExternalServiceProperties;
import ru.yandex.market.logistics.util.client.HttpTemplate;
import ru.yandex.market.logistics.util.client.HttpTemplateImpl;
import ru.yandex.passport.tvmauth.CheckedServiceTicket;
import ru.yandex.passport.tvmauth.CheckedUserTicket;
import ru.yandex.passport.tvmauth.ClientStatus;
import ru.yandex.passport.tvmauth.NativeTvmClient;
import ru.yandex.passport.tvmauth.TvmClient;

@Configuration
@Import(FulfillmentWorkflowAsyncClientConfiguration.class)
@PropertySource("classpath:test.properties")
public class Config {

    @Bean
    public ExternalServiceProperties fulfillmentWorkflowProperties(
            @Value("${fulfillment.workflow.api.host}") String host
    ) {
        ExternalServiceProperties properties = new ExternalServiceProperties();
        properties.setUrl(host);
        return properties;
    }

    @Bean
    public MockRestServiceServer mockRestServiceServer(HttpTemplate fulfillmentWorkflowTemplate) {
        return MockRestServiceServer.createServer(((HttpTemplateImpl) fulfillmentWorkflowTemplate).getRestTemplate());
    }

    @Bean
    public TvmClient tvmClientMock() {
        return new NativeTvmClient() {

            @Override
            public ClientStatus getStatus() {
                return null;
            }

            @Override
            public String getServiceTicketFor(String alias) {
                return null;
            }

            @Override
            public String getServiceTicketFor(int clientId) {
                return null;
            }

            @Override
            public CheckedServiceTicket checkServiceTicket(String ticketBody) {
                return null;
            }

            @Override
            public CheckedUserTicket checkUserTicket(String ticketBody) {
                return null;
            }

            @Override
            public void close() {

            }
        };
    }
}
