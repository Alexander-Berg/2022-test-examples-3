package ru.yandex.market.mbo.test;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * @author Alexander Kramarev (pochemuto@yandex-team.ru)
 * @date 09.02.2018
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface InjectResource {
    String value();
}
