package ru.yandex.market.mboc.integration.test;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.market.mboc.integration.test.config.HttpIntegrationTestConfig;
import ru.yandex.market.mboc.integration.test.config.TestRestTemplateConfig;

/**
 * @author s-ermakov
 */
@Configuration
@Import({TestRestTemplateConfig.class, HttpIntegrationTestConfig.class})
public class CommonConfiguration {
}
