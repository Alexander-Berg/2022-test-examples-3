package ru.yandex.market.fps.module.mbi.test;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.market.fps.module.mbi.MbiPartnerApiClient;
import ru.yandex.market.fps.module.mbi.ModuleMbiConfiguration;
import ru.yandex.market.mbi.api.client.MbiApiClient;
import ru.yandex.market.mbi.open.api.client.MbiOpenApiClient;

@Configuration
@Import(ModuleMbiConfiguration.class)
public class ModuleMbiTestConfiguration {
    @Bean
    public MbiOpenApiClient mbiOpenApiClient() {
        return Mockito.mock(MbiOpenApiClient.class);
    }

    @Bean
    public MbiApiClient mbiApiClient() {
        return Mockito.mock(MbiApiClient.class);
    }

    @Bean
    public MbiPartnerApiClient mbiPartnerApiClient() {
        return Mockito.mock(MbiPartnerApiClient.class);
    }
}
