package ru.yandex.market.crm.campaign;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.market.crm.campaign.placeholders.AppPropertiesConfiguration;
import ru.yandex.market.crm.core.services.external.yandexsender.YaSenderApiClient;
import ru.yandex.market.crm.core.services.jackson.JacksonConfig;
import ru.yandex.market.crm.core.services.yandexsender.HttpYaSenderApiClientImpl;
import ru.yandex.market.crm.core.services.yandexsender.YaSenderApiKeySupplier;
import ru.yandex.market.crm.core.test.TestEnvironmentResolver;
import ru.yandex.market.crm.json.serialization.JsonDeserializer;
import ru.yandex.market.crm.json.serialization.JsonSerializer;
import ru.yandex.market.mcrm.http.HttpClientConfiguration;
import ru.yandex.market.mcrm.http.HttpClientFactory;

/**
 * @author wanderer25
 */
@Configuration
@ContextConfiguration(classes = {
        AppPropertiesConfiguration.class,
        TestEnvironmentResolver.class,
        HttpClientConfiguration.class,
        JacksonConfig.class
})
public class TestExternalServicesConfig {

    @Bean
    public YaSenderApiClient yaSenderApiClient(@Value("${external.yasender.account.slug}") String senderAccount,
                                               @Value("${external.yasender.api.key}") String property,
                                               HttpClientFactory factory,
                                               JsonSerializer jsonSerializer,
                                               JsonDeserializer jsonDeserializer) {
        return new HttpYaSenderApiClientImpl(
                senderAccount,
                factory.create("yasender"),
                new YaSenderApiKeySupplier("YA_SENDER_API_KEY_GREEN", property),
                jsonSerializer,
                jsonDeserializer
        );
    }
}
