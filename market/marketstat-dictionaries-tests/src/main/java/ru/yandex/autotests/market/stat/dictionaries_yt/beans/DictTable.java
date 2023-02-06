package ru.yandex.autotests.market.stat.dictionaries_yt.beans;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Created by kateleb on 29.09.17
 */

@Target(TYPE)
@Retention(RUNTIME)
@Inherited
public @interface DictTable {
    String name();
}
