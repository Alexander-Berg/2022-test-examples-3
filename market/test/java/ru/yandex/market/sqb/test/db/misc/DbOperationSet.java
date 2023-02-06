package ru.yandex.market.sqb.test.db.misc;

/**
 * Функциональный интерфейс, описывающий операцию в БД.
 *
 * @author Vladislav Bauer
 */
@FunctionalInterface
public interface DbOperationSet {

    void process() throws Exception;

}
