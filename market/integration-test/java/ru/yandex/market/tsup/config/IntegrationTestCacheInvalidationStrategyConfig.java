package ru.yandex.market.tsup.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.tpl.common.data_provider.provider.DataProvider;
import ru.yandex.market.tsup.core.cache.sly_cacher.service.TestInvalidationStrategy;
import ru.yandex.market.tsup.core.data_provider.provider.TestDataProvider;

@Configuration
public class IntegrationTestCacheInvalidationStrategyConfig {
    @Bean
    public TestInvalidationStrategy testInvalidationStrategy() {
        return new TestInvalidationStrategy();
    }

    @Bean
    public DataProvider<?, ?> testDataProvider(LMSClient lmsClient) {
        return new TestDataProvider(lmsClient);
    }

}
