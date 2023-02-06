package ru.yandex.market.common.test.db;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Аннотация, которая обозначает, что надо отрефрешить мат.вьюхи перед тестом.
 * Рефреш матвьюх происходит после записи данных из {@link DbUnitDataSet}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@Inherited
public @interface DbUnitRefreshMatViews {

    /**
     * @return {@code id} бина в спринге, на котором нужно запускать DbUnit.
     */
    String dataSource() default "dataSource";

    /**
     * @return имя схемы по умолчанию. Применяется ближайшая к тесту аннотация.
     */
    String schema() default "";

    /**
     * @return список вида {@code schema.table} материализованных вьюх, которые не нужно очищать перед тестом.
     * Значения объединяются по всем указанным аннотациям.
     */
    String[] ignore() default {};
}
