package ru.yandex.market.reporting.generator.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.TestPropertySource;

/**
 * @author Aleksandr Kormushin &lt;kormushin@yandex-team.ru&gt;
 */
@Configuration
@TestPropertySource({"classpath:app-integration-tests.properties"})
public class IntegrationTestConfig {
}
