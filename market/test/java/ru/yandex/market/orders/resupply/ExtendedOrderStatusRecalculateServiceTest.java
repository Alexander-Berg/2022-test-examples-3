package ru.yandex.market.orders.resupply;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.order.model.SupplierOrderId;
import ru.yandex.market.shop.FunctionalTest;

class ExtendedOrderStatusRecalculateServiceTest extends FunctionalTest {

    @Autowired
    ExtendedOrderStatusRecalculateService statusRecalculateService;

    @Test
    @DbUnitDataSet(
            before = "ExtendedOrderStatusRecalculateServiceTest.before.csv",
            after = "ExtendedOrderStatusRecalculateServiceTest.recalculate.after.csv"
    )
    void recalculate() {
        statusRecalculateService.recalculateSupplierOrdersExtendedStatus(Set.of(
                new SupplierOrderId(1,91),
                new SupplierOrderId(1,92),
                new SupplierOrderId(2,92),
                new SupplierOrderId(2,93),
                new SupplierOrderId(2,94)
        ));
    }

    @Test
    @DbUnitDataSet(
            before = "ExtendedOrderStatusRecalculateServiceTest.before.csv",
            after = "ExtendedOrderStatusRecalculateServiceTest.recalculateForAllReturns.after.csv"
    )
    void recalculateForAllReturns() {
        statusRecalculateService.recalculateSupplierOrdersExtendedStatusForAllReturns();
    }

}
