package ru.yandex.market.adv.yt.test.service;

import javax.annotation.ParametersAreNonnullByDefault;

import ru.yandex.market.adv.yt.test.model.TableInfo;

/**
 * Интерфейс для работы с YT.
 * Date: 12.01.2022
 * Project: arcadia-market_adv_adv-shop
 *
 * @author alexminakov
 */
@ParametersAreNonnullByDefault
public interface YtService {

    /**
     * Создание таблицы в YT.
     *
     * @param tableInfo информация по создаваемой таблице
     */
    void createTable(TableInfo tableInfo);

    /**
     * Проверка соответствия таблицы в YT с переданными данными и ее последующая очистка.
     *
     * @param tableInfo информация по проверяемой и удаляемой таблице
     */
    void checkAndCleanTable(TableInfo tableInfo);
}
