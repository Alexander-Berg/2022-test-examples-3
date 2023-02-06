package ru.yandex.market.crm.campaign.test;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import ru.yandex.market.crm.campaign.placeholders.AppPropertiesConfiguration;
import ru.yandex.market.crm.campaign.services.ExecutorsConfig;
import ru.yandex.market.crm.campaign.yt.YtConfig;
import ru.yandex.market.crm.core.services.jackson.JacksonConfig;
import ru.yandex.market.crm.core.test.TestEnvironmentResolver;

/**
 * Конфигурация спринг контекста для интеграционного тестирования с YT
 */
@Configuration
@EnableTransactionManagement(proxyTargetClass = true)
@Import({
        YtConfig.class,
        AppPropertiesConfiguration.class,
        TestEnvironmentResolver.class,
        JacksonConfig.class,
        ExecutorsConfig.class
})
public class YtIntegrationTestConfig {
}
