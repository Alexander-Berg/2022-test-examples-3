package ru.yandex.market.antifraud.yql.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import ru.yandex.market.stat.conf.YtConfig;
import ru.yandex.market.stats.test.config.PropertiesITestConfig;

@Lazy(false)
@Configuration
@Profile("integration-tests")
@Import({PropertiesITestConfig.class, YtConfig.class})
@ImportResource({"classpath:test-beans.xml"})
@ComponentScan("ru.yandex.market.antifraud.yql.validate")
public class IntegrationTestAntifraudYqlConfig {
}
