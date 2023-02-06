package ru.yandex.market.core.util.db;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Тесты для класса {@link TokenPagingSqlHelper}.
 *
 * @author Vadim Lyalin
 */
public class TokenPagingSqlHelperTest {
    @Test
    void test() {
        TokenPagingSqlHelper tokenPagingSqlHelper = new TokenPagingSqlHelper(
                "select * from tbl where id # :id order by id # ", "#");

        assertEquals("select * from tbl where id > :id order by id asc ", tokenPagingSqlHelper.getQuery(false));
        assertEquals("select * from tbl where id < :id order by id desc ", tokenPagingSqlHelper.getQuery(true));
    }
}
