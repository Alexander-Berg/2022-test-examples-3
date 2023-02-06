package ru.yandex.market.logistics.test.integration.db;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Import;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Shortcut для конфигурации embedded postgres
 */
@Import(EmbeddedPostgresConfiguration.class)
@Retention(RUNTIME)
@Target(ElementType.TYPE)
public @interface EnableEmbeddedPostgres {
}
