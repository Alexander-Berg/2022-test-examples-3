package ru.yandex.market.logistics.utilizer.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import ru.yandex.market.logistics.test.integration.db.DbUnitTestConfiguration;
import ru.yandex.market.logistics.test.integration.db.LiquibaseTestConfiguration;
import ru.yandex.market.logistics.utilizer.config.cache.LmsCacheConfiguration;
import ru.yandex.market.logistics.utilizer.config.jpa.JpaConfig;
import ru.yandex.misc.db.embedded.ImportEmbeddedPg;

import static ru.yandex.market.logistics.utilizer.config.profiles.Profiles.INTEGRATION_TEST;

@Configuration
@ImportEmbeddedPg
@Import({
        TestDataSourceConfiguration.class,
        LiquibaseTestConfiguration.class,
        DbUnitTestConfiguration.class,
        JpaConfig.class,
        ClockTestConfig.class,
        MockConfig.class,
        LmsCacheConfiguration.class,
        SecurityTestConfig.class
})
@ComponentScan({
        "ru.yandex.market.logistics.utilizer.controller",
        "ru.yandex.market.logistics.utilizer.repo",
        "ru.yandex.market.logistics.utilizer.service",
        "ru.yandex.market.logistics.utilizer.converter",
        "ru.yandex.market.logistics.utilizer.solomon.repository"
})
@ActiveProfiles(INTEGRATION_TEST)
public class IntegrationTestConfig {

}
