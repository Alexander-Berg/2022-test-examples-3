package ru.yandex.market.replenishment.autoorder.service;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.replenishment.autoorder.config.FunctionalTest;
public class DbDeleteServiceTest extends FunctionalTest {

    @Autowired
    private DbDeleteService dbDeleteService;

    @Test
    @DbUnitDataSet(
            before = "DbDeleteServiceTest_orders.before.csv",
            after = "DbDeleteServiceTest_orders.after.csv"
    )
    public void deleteTest_cascadeOrders() {
        dbDeleteService.delete(DeleteJobConfiguration.builder()
                .setTableName("order_info")
                .setColumnDateName("created_at")
                .setDaysBeforeDeletion(80).build());
    }

    @Test
    @DbUnitDataSet(
            before = "DbDeleteServiceTest_setActiveColumn.before.csv",
            after = "DbDeleteServiceTest_setActiveColumn.after.csv"
    )
    public void deleteTest_setActiveColumn() {
        dbDeleteService.delete(DeleteJobConfiguration.builder()
                .setTableName("supplier_schedule")
                .setColumnDateName("updated_ts")
                .setActiveColumn("active")
                .setDaysBeforeDeletion(30).build());
    }
}
