package ru.yandex.market.vendors.analytics.tms.jobs.offer;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.vendors.analytics.tms.BalanceFunctionalTest;

/**
 * @author antipov93.
 */
class ImportDatasourcesBilledDatesExecutorTest extends BalanceFunctionalTest {
    @Autowired
    private ImportDatasourcesBilledDatesExecutor importDatasourcesBilledDatesExecutor;

    @Test
    @DbUnitDataSet(
            before = "ImportDatasourcesBilledDatesExecutorTest.before.csv",
            after = "ImportDatasourcesBilledDatesExecutorTest.after.csv"
    )
    void doImport() {
        mockBalance(1001, 0, LocalDateTime.of(2020, 6, 16, 10, 30));
        mockBalance(1002, 0, LocalDateTime.of(2020, 6, 15, 10, 30));
        mockBalance(1003, 0, LocalDateTime.of(2020, 6, 1, 0, 0));
        mockBalance(1004, 100, null);
        importDatasourcesBilledDatesExecutor.doJob(null);
    }
}