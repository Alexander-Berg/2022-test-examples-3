package ru.yandex.market.jmf.utils.test.time;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.time.OffsetDateTime;

import org.springframework.core.annotation.AliasFor;

@Target({ElementType.TYPE, ElementType.METHOD})
@Inherited
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface FixedTime {
    /**
     * Строковое представление {@link OffsetDateTime}. Возможные значения можно посмотреть в
     * {@link ru.yandex.market.crm.util.Dates#parseDateTime(String)}
     */
    @AliasFor("time")
    String value() default "";

    @AliasFor("value")
    String time() default "";
}
