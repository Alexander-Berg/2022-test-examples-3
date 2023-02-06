package ru.yandex.market.mbi.banners;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;


public class DbTest extends FunctionalTest {

    @Autowired
    public NamedParameterJdbcTemplate jdbcTemplate;

    @Test
    public void pgTest() {
        jdbcTemplate.getJdbcOperations().execute("select 1");
    }
}
