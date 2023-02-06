package ru.yandex.market.promo.taskprocessor.postgres;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;

import ru.yandex.market.javaframework.postgres.test.AbstractJdbcRecipeTest;

@ActiveProfiles("functionalTest")
public class DatabaseTest extends AbstractJdbcRecipeTest {

    @Test
    public void simpleTest() {
        jdbcTemplate.execute("SELECT 1");
    }
}
