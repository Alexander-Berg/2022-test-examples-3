package ru.yandex.market.fintech.fintechutils.postgres;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.market.fintech.fintechutils.AbstractFunctionalTest;

public class DatabaseTest extends AbstractFunctionalTest {

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    @Test
    public void simpleTest() {
        jdbcTemplate.getJdbcTemplate().execute("SELECT 1");
    }
}
