package ru.yandex.market.adv.yt.test.model;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

/**
 * Содержимое таблицы.
 * Date: 12.01.2022
 * Project: arcadia-market_adv_adv-shop
 *
 * @author alexminakov
 */
@Builder
@Getter
public class TableContent {
    /**
     * Нужно ли создавать таблицу. Если null - нет информации.
     */
    private final Boolean create;
    /**
     * Существует ли таблица. Если null - нет информации.
     */
    private final Boolean exist;
    /**
     * Список строк в таблице.
     */
    private final List<?> rows;
}
