package ru.yandex.market.abo.core.shop;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.EmptyTest;

/**
 * @author artemmz
 * @date 19.07.17.
 */
public class RefreshShopTest extends EmptyTest {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    public void testRefreshShop() throws Exception {
        jdbcTemplate.queryForObject("SELECT refresh_shop()", Object.class);
    }
}