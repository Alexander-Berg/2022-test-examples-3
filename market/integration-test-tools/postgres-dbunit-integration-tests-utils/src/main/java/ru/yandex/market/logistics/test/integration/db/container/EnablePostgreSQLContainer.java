package ru.yandex.market.logistics.test.integration.db.container;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Import;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Shortcut для конфигурации postgresql-test-containers
 */
@Import({
    PostgreSQLContainerConfiguration.class,
    TestDatasourceConfiguration.class
})
@Retention(RUNTIME)
@Target(ElementType.TYPE)
public @interface EnablePostgreSQLContainer {
}
