package ru.yandex.autotests.innerpochta.atlas;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author kurau (Yuri Kalinin)
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface FindByCss {

    String value();

    String mobile() default "";

}