package ru.yandex.market.wms.scheduler.service.orderstatus;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.wms.scheduler.config.SchedulerIntegrationTest;
import ru.yandex.market.wms.scheduler.order.status.calculate.service.UnbatchedOrderStatusFixer;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT;

public class UnbatchedOrderStatusFixerTest extends SchedulerIntegrationTest {

    @Autowired
    private UnbatchedOrderStatusFixer unbatchedOrderStatusFixer;

    @Test
    @DatabaseSetup(value = "/db/dao/unbatched-order-status-fixer/before.xml")
    @ExpectedDatabase(value = "/db/dao/unbatched-order-status-fixer/after.xml", assertionMode = NON_STRICT)
    void updateStatusForUnbatchedOrders() throws InterruptedException {
        unbatchedOrderStatusFixer.updateStatusForUnbatchedOrders();
    }

}
