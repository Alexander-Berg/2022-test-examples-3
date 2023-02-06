package ru.yandex.chemodan.app.djfs.migrator.integration;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.chemodan.app.djfs.core.test.TestContextConfiguration;
import ru.yandex.chemodan.app.djfs.migrator.test.TestContextConfiguration2;


/**
 * @author yappo
 */
@Configuration
@Import({
        TestContextConfiguration.class,
        TestContextConfiguration2.class,
})
public class DjfsMigratorIntegrationTestConfig {
}
