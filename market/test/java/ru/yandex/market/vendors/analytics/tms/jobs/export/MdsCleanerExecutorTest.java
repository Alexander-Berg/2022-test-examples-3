package ru.yandex.market.vendors.analytics.tms.jobs.export;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.vendors.analytics.tms.FunctionalTest;

/**
 * Функциональный тест для джобы {@link MdsCleanerExecutor}.
 *
 * @author ogonek
 */
@DbUnitDataSet(
        before = "MdsCleanerExecutorTest.before.csv"
)
public class MdsCleanerExecutorTest extends FunctionalTest {

    @Autowired
    private MdsCleanerExecutor mdsCleanerExecutor;

    @Test
    @DbUnitDataSet(
            after = "MdsCleanerExecutorTest.after.csv"
    )
    void badSalesTableTest() {
        mdsCleanerExecutor.doJob(null);
    }
}
