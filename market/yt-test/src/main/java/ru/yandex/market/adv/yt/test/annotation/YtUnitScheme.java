package ru.yandex.market.adv.yt.test.annotation;

/**
 * Аннотация для описания схемы таблицы YT.
 * Date: 11.01.2022
 * Project: arcadia-market_adv_adv-shop
 *
 * @author alexminakov
 */
public @interface YtUnitScheme {

    /**
     * @return модель данных, представляющая таблицу YT
     */
    Class<?> model();

    /**
     * @return путь до таблицы YT
     */
    String path();

    /**
     * @return является ли таблица динамической, по умолчанию - да
     */
    boolean isDynamic() default true;

    /**
     * @return колонки, которые нужно игнорировать при проверке
     */
    String[] ignoreColumns() default {};
}
