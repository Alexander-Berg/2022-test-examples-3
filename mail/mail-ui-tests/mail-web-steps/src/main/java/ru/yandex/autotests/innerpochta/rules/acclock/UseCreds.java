package ru.yandex.autotests.innerpochta.rules.acclock;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * User: lanwen
 * Date: 09.09.13
 * Time: 10:47
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface UseCreds {
    String[] value();
}
