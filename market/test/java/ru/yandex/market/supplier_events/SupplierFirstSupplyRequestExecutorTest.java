package ru.yandex.market.supplier_events;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.ff.client.FulfillmentWorkflowClientApi;
import ru.yandex.market.ff.client.dto.SuppliersList;
import ru.yandex.market.shop.FunctionalTest;

import static org.mockito.Mockito.when;

class SupplierFirstSupplyRequestExecutorTest extends FunctionalTest {

    @Autowired
    private SupplierFirstSupplyRequestExecutor executor;

    @Autowired
    private FulfillmentWorkflowClientApi clientApi;

    @Test
    @DbUnitDataSet(
            before = "SupplierFirstSupplyRequestExecutorTest.before.csv",
            after = "SupplierFirstSupplyRequestExecutorTest.after.csv"
    )
    void test() {
        var suppliersWithRequests = new SuppliersList();
        suppliersWithRequests.setSupplierIds(List.of(1L, 2L, 3L));
        when(clientApi.getSuppliersHavingAtLeastOneSupply()).thenReturn(
                suppliersWithRequests
        );
        executor.doJob(null);
    }
}
