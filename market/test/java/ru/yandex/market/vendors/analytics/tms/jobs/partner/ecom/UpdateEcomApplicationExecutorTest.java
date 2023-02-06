package ru.yandex.market.vendors.analytics.tms.jobs.partner.ecom;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.vendors.analytics.core.utils.dbunit.ClickhouseDbUnitDataSet;
import ru.yandex.market.vendors.analytics.tms.FunctionalTest;

/**
 * Функциональный тест для джобы {@link UpdateEcomApplicationExecutor}.
 *
 * @author sergeymironov
 */
@DbUnitDataSet(before = "UpdateEcomApplicationExecutorTest.before.csv")
@ClickhouseDbUnitDataSet(before = "UpdateEcomApplicationExecutorTest.before.clickhouse.csv")
class UpdateEcomApplicationExecutorTest extends FunctionalTest {

    @Autowired
    private UpdateEcomApplicationExecutor updateEcomApplicationExecutor;

    @Test
    @DbUnitDataSet(after = "UpdateEcomApplicationExecutorTest.after.csv")
    void updateEcomApplicationExecutorTest() {
        updateEcomApplicationExecutor.doJob(null);
    }
}
