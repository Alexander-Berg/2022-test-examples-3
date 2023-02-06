package ru.yandex.market.arbiter.test.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ru.yandex.businesschat.provider.api.client.BusinesschatProviderApi;
import ru.yandex.market.arbiter.api.client.ArbiterApi;
import ru.yandex.market.arbiter.api.client.ServiceApi;


/**
 * @author moskovkin@yandex-team.ru
 * @since 14.05.2020
 */
@Configuration
public class ApiClientConfig {
    @Bean
    public ru.yandex.market.arbiter.api.ApiClient arbiterApiClient() {
        return new ru.yandex.market.arbiter.api.ApiClient();
    }

    @Bean
    public ru.yandex.businesschat.provider.api.ApiClient busiesschatApiClient() {
        return new ru.yandex.businesschat.provider.api.ApiClient();
    }

    @Bean
    ServiceApi serviceApi(ru.yandex.market.arbiter.api.ApiClient arbiterApiClient) {
        return new ServiceApi(arbiterApiClient);
    }

    @Bean
    ArbiterApi arbiterApi(ru.yandex.market.arbiter.api.ApiClient arbiterApiClient) {
        return new ArbiterApi(arbiterApiClient);
    }

    @Bean
    BusinesschatProviderApi providerApi(ru.yandex.businesschat.provider.api.ApiClient busiesschatApiClient) {
        return new BusinesschatProviderApi(busiesschatApiClient);
    }
}
