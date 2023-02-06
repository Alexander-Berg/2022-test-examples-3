package ru.yandex.market.adv.incut.postgres;

import org.junit.jupiter.api.Test;

import ru.yandex.market.javaframework.postgres.test.AbstractJdbcRecipeTest;

public class DatabaseTest extends AbstractJdbcRecipeTest {

    @Test
    public void simpleTest() {
        jdbcTemplate.execute("SELECT 1");
    }
}
