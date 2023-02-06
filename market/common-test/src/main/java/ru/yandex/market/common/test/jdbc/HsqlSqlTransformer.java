package ru.yandex.market.common.test.jdbc;

import ru.yandex.market.common.test.transformer.CompositeStringTransformer;

/**
 * Преобразует sql-запрос из Оракл-диалекта в HSQL- совместмый диалект.
 *
 * @author zoom
 */
public class HsqlSqlTransformer extends CompositeStringTransformer {
    @Override
    public String transform(String string) {
        return super.transform(string).replace("SET REFERENTIAL_INTEGRITY ", "SET DATABASE REFERENTIAL INTEGRITY ");
    }
}
