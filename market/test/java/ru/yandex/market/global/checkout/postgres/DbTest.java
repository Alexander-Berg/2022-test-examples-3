package ru.yandex.market.global.checkout.postgres;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.market.global.checkout.BaseFunctionalTest;

public class DbTest extends BaseFunctionalTest {

    @Autowired
    public NamedParameterJdbcTemplate jdbcTemplate;

    @Test
    public void pgTest() {
        jdbcTemplate.getJdbcOperations().execute("select 1");
    }

}
