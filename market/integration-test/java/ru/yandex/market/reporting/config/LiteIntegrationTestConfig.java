package ru.yandex.market.reporting.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.test.context.TestPropertySource;
import ru.yandex.market.reporting.generator.config.CacheConfig;
import ru.yandex.market.reporting.generator.config.MdsConfig;
import ru.yandex.market.reporting.generator.config.SlideShowRendererConfig;

/**
 * @author Aleksandr Kormushin &lt;kormushin@yandex-team.ru&gt;
 */
@Configuration
@Import({
        MdsConfig.class,
        CacheConfig.class,
        SlideShowRendererConfig.class
})
@ComponentScan(basePackages = {
/*
        "ru.yandex.market.reporting.generator.service",
        "ru.yandex.market.reporting.generator.dao",
*/
        "ru.yandex.market.reporting.generator.presentation",
        "ru.yandex.market.reporting.generator.workbook",
        "ru.yandex.market.reporting.generator.aspect",
        "ru.yandex.market.reporting.generator.indexer"
})
@EnableAspectJAutoProxy
@EnableScheduling
@TestPropertySource({"classpath:app-integration-tests.properties"})
public class LiteIntegrationTestConfig {
}
