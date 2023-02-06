package ru.yandex.market.logistics.test.integration.db.zonky;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Import;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Shortcut для конфигурации zonky-embedded-postgres
 * Поддерживает Hikari пул
 */
@Import(ZonkyPooledEmbeddedPostgresConfiguration.class)
@Retention(RUNTIME)
@Target(ElementType.TYPE)
public @interface EnablePooledZonkyEmbeddedPostgres {
}
