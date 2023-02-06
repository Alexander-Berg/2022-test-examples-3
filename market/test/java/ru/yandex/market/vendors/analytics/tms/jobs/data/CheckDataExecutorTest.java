package ru.yandex.market.vendors.analytics.tms.jobs.data;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.vendors.analytics.core.utils.dbunit.ClickhouseDbUnitDataSet;
import ru.yandex.market.vendors.analytics.tms.FunctionalTest;

/**
 * Функциональный тест для джобы {@link CheckDataExecutor}.
 *
 * @author ogonek
 */
@DbUnitDataSet(
        before = "CheckDataExecutorTest.before.csv"
)
public class CheckDataExecutorTest extends FunctionalTest {

    @Autowired
    private CheckDataExecutor checkDataExecutor;

    @Test
    @DbUnitDataSet(
            after = "AllOkTest.after.csv"
    )
    @ClickhouseDbUnitDataSet(before = "AllOkTest.clickhouse.csv")
    void allOkTest() {
        checkDataExecutor.doJob(null);
    }

    @Test
    @DbUnitDataSet(
            after = "BadSalesTableTest.after.csv"
    )
    @ClickhouseDbUnitDataSet(before = "BadSalesTableTest.clickhouse.csv")
    void badSalesTableTest() {
        checkDataExecutor.doJob(null);
    }

    @Test
    @DbUnitDataSet(
            after = "BadWhitelistTableTest.after.csv"
    )
    @ClickhouseDbUnitDataSet(before = "BadWhitelistTableTest.clickhouse.csv")
    void badWhitelistTableTest() {
        checkDataExecutor.doJob(null);
    }

    @Test
    @DbUnitDataSet(
            after = "BadSearchTableTest.after.csv"
    )
    @ClickhouseDbUnitDataSet(before = "BadSearchTableTest.clickhouse.csv")
    void badSearchTableTest() {
        checkDataExecutor.doJob(null);
    }

    @Test
    @DbUnitDataSet(
            after = "BadPrivateLabelsTableTest.after.csv"
    )
    @ClickhouseDbUnitDataSet(before = "BadPrivateLabelsTableTest.clickhouse.csv")
    void badPrivateLabelsTableTest() {
        checkDataExecutor.doJob(null);
    }

    @Test
    @DbUnitDataSet(
            after = "BadExclusiveVendorsTable.after.csv"
    )
    @ClickhouseDbUnitDataSet(before = "BadExclusiveVendorsTable.clickhouse.csv")
    void badExclusiveVendorsTable() {
        checkDataExecutor.doJob(null);
    }

}
