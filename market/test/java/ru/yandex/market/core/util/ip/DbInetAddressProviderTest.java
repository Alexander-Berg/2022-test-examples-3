package ru.yandex.market.core.util.ip;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;

import static org.assertj.core.api.Assertions.assertThat;

class DbInetAddressProviderTest extends FunctionalTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DbUnitDataSet(before = {"ProvideAddress.before.csv"})
    void provideAddress() {
        assertThat("193.25.126.135").isEqualTo(jdbcTemplate.queryForObject(
                "select ip from shops_web.clicker_ip_pool where rowid = (select rowid " +
                        " from shops_web.clicker_ip_pool order by last_used nulls first" +
                        " fetch next 1 rows" +
                        " only )", String.class), "rowid/ctid test");
    }

    @Test
    @DbUnitDataSet(before = {"ProvideAddressWithNull.before.csv"})
    void provideAddressWithNull() {
        assertThat("15.34.157.189").isEqualTo(jdbcTemplate.queryForObject(
                "select ip from shops_web.clicker_ip_pool where rowid = (select rowid " +
                        " from shops_web.clicker_ip_pool order by last_used nulls first" +
                        " fetch next 1 rows" +
                        " only )", String.class), "rowid/ctid test");
    }
}
