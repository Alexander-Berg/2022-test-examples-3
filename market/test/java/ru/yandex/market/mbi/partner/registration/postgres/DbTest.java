package ru.yandex.market.mbi.partner.registration.postgres;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.market.mbi.partner.registration.AbstractFunctionalTest;

public class DbTest extends AbstractFunctionalTest {

    @Autowired
    public NamedParameterJdbcTemplate jdbcTemplate;

    @Test
    public void pgTest() {
        jdbcTemplate.getJdbcOperations().execute("select 1");
    }

}
