package ru.yandex.market.replenishment.autoorder.repository;

import org.junit.Test;

import ru.yandex.market.common.test.db.DbUnitDataBaseConfig;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.replenishment.autoorder.config.FunctionalTest;

@DbUnitDataBaseConfig({@DbUnitDataBaseConfig.Entry(
        name = "tableType",
        value = "TABLE,VIEW"
)})
public class ViewTests extends FunctionalTest {

    @Test
    @DbUnitDataSet(before = "ViewTests.testVYtTenderVirtualTransits.before.csv",
            after = "ViewTests.testVYtTenderVirtualTransits.after.csv")
    public void testVYtTenderVirtualTransits() {}

    @Test
    @DbUnitDataSet(before = "ViewTests.testVYtTenderAssortment.before.csv",
            after = "ViewTests.testVYtTenderAssortment.after.csv")
    public void testVYtTenderAssortment() {}

    @Test
    @DbUnitDataSet(before = "ViewTests.testVYtExportedDemands.before.csv",
            after = "ViewTests.testVYtExportedDemands.after.csv")
    public void testVYtExportedDemands() {}
}
