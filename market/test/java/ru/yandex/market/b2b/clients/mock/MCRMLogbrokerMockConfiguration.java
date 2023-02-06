package ru.yandex.market.b2b.clients.mock;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import ru.yandex.market.b2b.logbroker.mcrm.MultiOrderPaymentInvoiceLogbrokerEventPublisher;

@Configuration
public class MCRMLogbrokerMockConfiguration {

    @Bean
    @Primary
    public MultiOrderPaymentInvoiceLogbrokerEventPublisher multiOrderPaymentInvoiceLogbrokerEventPublisherMock() {
        return Mockito.mock(MultiOrderPaymentInvoiceLogbrokerEventPublisher.class);
    }
}
