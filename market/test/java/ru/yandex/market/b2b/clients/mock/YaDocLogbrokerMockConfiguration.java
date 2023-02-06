package ru.yandex.market.b2b.clients.mock;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import ru.yandex.market.b2b.logbroker.yadoc.YaDocDocumentLogbrokerEventPublisher;

@Configuration
public class YaDocLogbrokerMockConfiguration {

    @Bean
    @Primary
    public YaDocDocumentLogbrokerEventPublisher yaDocDocumentLogbrokerEventPublisherMock() {
        return Mockito.mock(YaDocDocumentLogbrokerEventPublisher.class);
    }
}
