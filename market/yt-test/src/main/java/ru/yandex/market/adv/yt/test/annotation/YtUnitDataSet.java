package ru.yandex.market.adv.yt.test.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Аннотация для описания содержимого YT таблицы.
 * <p>
 * Если будет навешено на тест более одной аннотации на одну и ту же таблицу,
 * то она создастся только на основе первой попавшейся аннотации.
 * <p>
 * Чтобы данные в таблицах корректно сравнивались, необходимо перегрузить у модели данных - equals, hashCode и toString.
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
@Repeatable(YtUnitDataSets.class)
public @interface YtUnitDataSet {

    /**
     * @return описание схемы таблицы YT
     */
    YtUnitScheme scheme();

    /**
     * @return json-файл со списком данных в таблице до запуска теста, по умолчанию - игнорируем
     */
    String before() default "";

    /**
     * @return json-файл со списком данных в таблице после запуска теста, по умолчанию - равен before
     */
    String after() default "";

    /**
     * @return нужно ли создать таблицы при запуске теста, по умолчанию - создаем
     */
    boolean create() default true;

    /**
     * @return существует ли таблица после запуска теста, по умолчанию - существует
     */
    boolean exist() default true;
}
