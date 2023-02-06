package ru.yandex.market.adv.yt.test.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Аннотация для описания содержимого YT таблиц, для @Repeatable аннотации {@link YtUnitDataSet}.
 * Date: 11.01.2022
 * Project: arcadia-market_adv_adv-shop
 *
 * @author alexminakov
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({
        ElementType.METHOD,
        ElementType.TYPE
})
@Inherited
public @interface YtUnitDataSets {

    /**
     * @return список аннотаций {@link YtUnitDataSet}
     */
    YtUnitDataSet[] value();
}
