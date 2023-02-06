package ru.yandex.market.partner.order.limit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.shop.FunctionalTest;

class DailyOrderLimitResetExecutorTest extends FunctionalTest {
    @Autowired
    private DailyOrderLimitResetExecutor executor;

    @Test
    @DbUnitDataSet(
            before = "DailyOrderLimitResetExecutorTest.before.csv",
            after = "DailyOrderLimitResetExecutorTest.after.csv"
    )
    void testResetLimit() {
        executor.doJob(null);
    }
}
