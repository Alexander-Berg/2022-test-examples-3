package ru.yandex.market.adv.yt.test.model;

import lombok.Builder;
import lombok.Getter;

import ru.yandex.market.yt.client.YtClientProxy;

/**
 * Информация по таблице.
 * Date: 12.01.2022
 * Project: arcadia-market_adv_adv-shop
 *
 * @author alexminakov
 */
@Builder
@Getter
public class TableInfo {
    /**
     * Клиент, для доступа к таблице.
     */
    private final YtClientProxy ytClient;
    /**
     * Класс, в объекты которого будут преобразованы строки таблицы.
     */
    private final Class<?> model;
    /**
     * Таблица является динамической.
     */
    private final boolean dynamic;
    /**
     * Путь до таблицы.
     */
    private final String path;
    /**
     * Содержимое таблицы.
     */
    private final TableContent tableContent;
    /**
     * Колонки, которые нужно игнорировать при проверке.
     */
    private final String[] ignoreColumns;
}
