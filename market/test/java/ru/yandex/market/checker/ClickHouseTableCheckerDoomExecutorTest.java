package ru.yandex.market.checker;

import java.time.Clock;
import java.time.ZoneId;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.util.DateTimes;

/**
 * Тесты для {@link ClickHouseDoomTableCheckerExecutor}
 */
class ClickHouseTableCheckerDoomExecutorTest extends FunctionalTest {

    @Autowired
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    private ClickHouseDoomTableCheckerExecutor clickHouseTableCheckerDoomExecutor;

    @BeforeEach
    void setUp() {
        clickHouseTableCheckerDoomExecutor = new ClickHouseDoomTableCheckerExecutor(
                namedParameterJdbcTemplate,
                Clock.fixed(DateTimes.toInstantAtDefaultTz(2020, 9, 3), ZoneId.systemDefault())
        );
    }

    @Test
    @DbUnitDataSet(
            before = "ClickHouseTableCheckerDoomExecutorTest.delete.before.csv",
            after = "ClickHouseTableCheckerDoomExecutorTest.delete.after.csv"
    )
    void checkDelete() {
        clickHouseTableCheckerDoomExecutor.doJob(null);
    }
}
