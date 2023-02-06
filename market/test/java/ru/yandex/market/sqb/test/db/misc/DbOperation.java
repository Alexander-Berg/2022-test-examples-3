package ru.yandex.market.sqb.test.db.misc;

import java.sql.Statement;

/**
 * Фунциональная операция над {@link Statement}.
 *
 * @param <T> тип возвращаемого значения
 * @author Vladislav Bauer
 */
@FunctionalInterface
public interface DbOperation<T> {

    T process(Statement statement) throws Exception;

}
