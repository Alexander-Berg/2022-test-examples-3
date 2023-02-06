package ru.yandex.market.checkout.pushapi.db;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import ru.yandex.market.checkout.pushapi.application.AbstractWebTestBase;

public class InitDbTest extends AbstractWebTestBase {
    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    @Test
    public void testConnectAndVersion() {
        var res = jdbcTemplate.getJdbcTemplate()
                .queryForObject("select version()", (rs, n) -> rs.getString("version"));
        Assertions.assertTrue(res.contains("PostgreSQL 12.5"));
    }

}
