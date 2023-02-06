package ru.yandex.market.mdm.integration.test;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.market.mdm.integration.test.config.HttpIntegrationTestConfig;
import ru.yandex.market.mdm.integration.test.config.TestRestTemplateConfig;

/**
 * @author s-ermakov
 */
@Configuration
@Import({TestRestTemplateConfig.class, HttpIntegrationTestConfig.class})
public class CommonConfiguration {
}
