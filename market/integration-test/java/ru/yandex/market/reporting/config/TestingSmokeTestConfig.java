package ru.yandex.market.reporting.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.TestPropertySource;

/**
 * @author Aleksandr Kormushin &lt;kormushin@yandex-team.ru&gt;
 */
@Configuration
@ComponentScan(basePackages = "ru.yandex.market.reporting.generator.dao", lazyInit = true)
@TestPropertySource({"classpath:app-testing-smoke-tests.properties"})
public class TestingSmokeTestConfig {
}
