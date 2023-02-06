package ru.yandex.market.wms.api.config;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ru.yandex.market.logistics.iris.client.api.PushApiClient;

@Configuration
public class IrisClientTestConfiguration {

    @Bean
    public PushApiClient pushApiClient() {
        return Mockito.mock(PushApiClient.class);
    }
}
