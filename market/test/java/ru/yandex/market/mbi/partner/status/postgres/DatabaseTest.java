package ru.yandex.market.mbi.partner.status.postgres;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.mbi.partner.status.AbstractFunctionalTest;

public class DatabaseTest extends AbstractFunctionalTest {

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @Test
    public void simpleTest() {
        jdbcTemplate.execute("SELECT 1");
    }
}
