package ru.yandex.market.tpl.integration.tests.configuration;

import java.time.Clock;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;

import ru.yandex.market.tpl.common.util.DateTimeUtil;

@Configuration
@ComponentScan(basePackages = {"ru.yandex.market.tpl.integration.tests.stress",
        "ru.yandex.market.tpl.integration.tests.client", "ru.yandex.market.tpl.integration.tests.facade",
        "ru.yandex.market.tpl.integration.tests.service"})
@EnableConfigurationProperties(CourierProperties.class)
@Import(TvmConfiguration.class)
public class AutoTestsConfiguration {

    @Profile({"local", "testing"})
    @Bean
    Clock clock() {
        return Clock.system(DateTimeUtil.DEFAULT_ZONE_ID);
    }
}
