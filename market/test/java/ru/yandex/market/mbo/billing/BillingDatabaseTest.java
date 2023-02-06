package ru.yandex.market.mbo.billing;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * @author amaslak
 */
public class BillingDatabaseTest {

    private JdbcTemplate jdbcTemplate;

    @Before
    public void setUp() throws Exception {
        jdbcTemplate = new JdbcTemplate(BillingDatabaseMock.getBillingDatasource());
    }

    @Test
    public void testTablesExist() {
        //очень странный тест, который просто поднимает h2, накатывает туда заранее подготовленный скрипт
        // и проверяет, что таблицы из скрипта накатились в h2
        Assert.assertNotNull(jdbcTemplate);
        jdbcTemplate.query("select 1", rs -> { /*do nothing */ });
        jdbcTemplate.query("select * from v_ng_billing_delta", rs -> { /*do nothing */ });
        jdbcTemplate.query("select * from v_ng_billing_balance", rs -> { /*do nothing */ });
        jdbcTemplate.query("select * from v_ng_billing_session", rs -> { /*do nothing */ });
        jdbcTemplate.query("select * from ng_suspended_operation_log", rs -> { /*do nothing */ });
    }

}
