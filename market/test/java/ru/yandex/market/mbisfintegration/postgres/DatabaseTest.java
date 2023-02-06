package ru.yandex.market.mbisfintegration.postgres;

import org.junit.jupiter.api.Test;

import ru.yandex.market.mbisfintegration.MbiSfAbstractJdbcRecipeTest;

public class DatabaseTest extends MbiSfAbstractJdbcRecipeTest {

    @Test
    public void simpleTest() {
        jdbcTemplate.execute("SELECT 1");
    }
}
