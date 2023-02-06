package ru.yandex.market.logistics.test.integration.db.listener;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Индикатор того, что тест требует очистки БД перед запуском.
 *
 * @see ResetDatabaseTestExecutionListener
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface CleanDatabase {
}
