package ru.yandex.market.core.views;

import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;

/**
 * Тесты для вьюхи {@code shops_web.v_shops_alive}.
 */
@DbUnitDataSet(before = "data/VShopsAliveTest.before.csv")
class VShopsAliveTest extends FunctionalTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DbUnitDataSet(before = "data/testCpcAliveWithRecentCutoffShop.before.csv")
    void testCpcAliveWithRecentCutoffShop() {
        Assertions.assertThat(getAliveShops())
                .containsExactlyInAnyOrder(1000L);
    }

    @Test
    void testCpcAliveWithoutCutoffShop() {
        Assertions.assertThat(getAliveShops())
                .containsExactlyInAnyOrder(1000L);
    }

    @Test
    @DbUnitDataSet(before = "data/testCpcDeadShop.before.csv")
    void testCpcDeadShop() {
        Assertions.assertThat(getAliveShops())
                .isEmpty();
    }

    @Test
    @DbUnitDataSet(before = "data/testWhitelistIsAlwaysAlive.before.csv")
    void testWhitelistIsAlwaysAlive() {
        Assertions.assertThat(getAliveShops())
                .containsExactlyInAnyOrder(1000L);
    }

    private List<Long> getAliveShops() {
        return jdbcTemplate.queryForList("select datasource_id from shops_web.v_shops_alive", Long.class);
    }
}
