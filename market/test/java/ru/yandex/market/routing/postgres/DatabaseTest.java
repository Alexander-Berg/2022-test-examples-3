package ru.yandex.market.routing.postgres;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.javaframework.postgres.test.AbstractJdbcRecipeTest;
import ru.yandex.market.routing.AbstractFunctionalTest;

public class DatabaseTest extends AbstractFunctionalTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    public void simpleTest() {
        jdbcTemplate.execute("SELECT 1");
    }
}
