package ru.yandex.market.sqb.db;

import org.junit.jupiter.api.Test;

import ru.yandex.market.sqb.test.db.DbUtils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.not;
import static ru.yandex.market.sqb.test.db.DbUtils.runWithDb;

/**
 * Smoke-тест на корректное подключение к реальной БД.
 *
 * @author Vladislav Bauer
 */
class SimpleDbIntegrationTest {

    private static final String SQL_CHECK_DATABASE = "select 1 from dual";


    @Test
    void testSimpleConnection() throws Exception {
        runWithDb(() -> {
            assertThat(DbUtils.execQuery(SQL_CHECK_DATABASE), not(empty()));
        });
    }

}
