package ru.yandex.market.abo.gen.common;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.EmptyTest;

/**
 * @author artemmz
 * @date 28/10/2019.
 */
class CommonGenQueryTest extends EmptyTest {
    @Autowired
    private JdbcTemplate pgJdbcTemplate;

    @Test
    void testQuery() {
        pgJdbcTemplate.queryForList("select view_query from common_gen", String.class).forEach(pgJdbcTemplate::queryForList);
    }
}
