package ru.yandex.market.vendors.analytics.tms.jobs.partner.ga;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.vendors.analytics.core.utils.dbunit.ClickhouseDbUnitDataSet;
import ru.yandex.market.vendors.analytics.tms.FunctionalTest;

/**
 * Функциональный тест для джобы {@link UpdateNoGaApplicationExecutor}.
 *
 * @author petrgiloian
 */
@DbUnitDataSet(before = "UpdateNoGaApplicationExecutorTest.before.csv")
@ClickhouseDbUnitDataSet(before = "UpdateNoGaApplicationExecutorTest.before.clickhouse.csv")
class UpdateNoGaApplicationExecutorTest extends FunctionalTest {

    @Autowired
    private UpdateNoGaApplicationExecutor updateNoGaApplicationExecutor;

    @Test
    @DbUnitDataSet(after = "UpdateNoGaApplicationExecutorTest.after.csv")
    void updateNoGaApplicationExecutorTest() {
        updateNoGaApplicationExecutor.doJob(null);
    }
}
