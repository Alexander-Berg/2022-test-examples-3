package ru.yandex.market.billing.tasks.view.v_mstat_dictionary_flags;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;

import static org.junit.Assert.assertEquals;

/**
 * Тестируем v_mstat_dictionary_flags.sql.
 *
 * @author samodurov-d@yandex-team.ru
 */
class GoodsAdBudgetTest extends FunctionalTest {
    private static final String GOODS_AD_BUDGET_QUERY =
            "select last_update_date_time from market_billing.v_mstat_dictionary_flags where name = 'GOODS_AD_BUDGET'";

    @Autowired
    private JdbcTemplate shopJdbcTemplate;

    @Test
    @DbUnitDataSet(before = "GoodsAdBudgetTest.service.before.csv")
    void testLastUpdateInGoodsAdBudget() {
        shopJdbcTemplate.query(GOODS_AD_BUDGET_QUERY, rs -> {
            assertEquals(rs.getTimestamp("last_update_date_time").toString(),
                    "2018-05-02 00:00:00.0");
        });
    }

    @Test
    @DbUnitDataSet(before = "GoodsAdBudgetTest.environment.before.csv")
    void testLastUpdateInEnvironmentManuallyUpdateTime() {
        shopJdbcTemplate.query(GOODS_AD_BUDGET_QUERY, rs -> {
            assertEquals(rs.getTimestamp("last_update_date_time").toString(),
                    "2018-07-06 17:16:14.0");
        });
    }
}
