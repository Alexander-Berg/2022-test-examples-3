package ru.yandex.market.abo.tms.cpa.returned;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.EmptyTest;

/**
 * @author imelnikov
 */

public class AxaptaConfigTest extends EmptyTest {

    @Autowired
    private JdbcTemplate axaptaJdbcTemplate;

    @Test
    @Disabled
    public void select() {
        axaptaJdbcTemplate.query("select * from ABOWarehouseReturn", rs -> {});
    }
}
