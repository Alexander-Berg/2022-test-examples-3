package ru.yandex.market.b2b.clients.mock;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import ru.yandex.mj.generated.client.yadoc.api.DocumentsApiClient;

@Configuration
public class DocumentsApiClientMockConfiguration {

    @Bean
    @Primary
    public DocumentsApiClient documentsApiClientMock() {
        return Mockito.mock(DocumentsApiClient.class);
    }
}
