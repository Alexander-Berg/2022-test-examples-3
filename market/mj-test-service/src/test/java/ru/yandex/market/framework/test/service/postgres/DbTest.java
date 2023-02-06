package ru.yandex.market.framework.test.service.postgres;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

public class DbTest extends AbstractPostgresTest {

    @Autowired
    public NamedParameterJdbcTemplate jdbcTemplate;

    @Test
    public void pgTest() {
        jdbcTemplate.getJdbcOperations().execute("select 1");
    }

}
