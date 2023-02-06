package ru.yandex.autotests.innerpochta.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author oleshko
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface DoTestOnlyForEnvironment {
    String value(); //valid values: Phone, Tablet, Android, iOS
}
