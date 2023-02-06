package ru.yandex.market.common.test.db;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Аннотация, которая регулирует правила транкейта данных.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@Inherited
public @interface DbUnitTruncatePolicy {

    /**
     * @return {@code id} бина в спринге, на котором нужно запускать DbUnit.
     */
    String dataSource() default "dataSource";

    /**
     * @return имя схемы по умолчанию. Применяется ближайшая к тесту аннотация.
     */
    String schema() default "";

    /**
     * @return Тип транкейта таблиц.
     */
    TruncateType truncateType() default TruncateType.INHERITANCE;
}
