package ru.yandex.direct.core.testing.repository;

import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Table;
import org.jooq.impl.DSL;

import static ru.yandex.direct.utils.CommonUtils.nvl;

/**
 * Вспомогательные методы для тестовых репозиториев
 */
public class UtilRepository {

    /**
     * Получение следующего значения на основне максимального в БД по заданному полю таблицы
     *
     * @param context контекст для подключения к базе
     * @param table   таблица
     * @param idField поле
     * @return следующее значение
     */
    public static Long getNextId(DSLContext context, Table table, Field<Long> idField) {
        Long maxId = context
                .select(DSL.max(idField))
                .from(table)
                .fetchOne()
                .value1();

        return nvl(maxId, 0L) + 1;
    }
}
