package ru.yandex.market.logistics.iris.utils.query;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Аннотация для проверки количество запросов, совершенных через JPA провайдер
 * в рамках вызова метода.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface JpaQueriesCount {

    /**
     * Ожидаемое значение количества запросов.
     */
    int value();

    boolean isThreshold() default false;
}
