package ru.yandex.market.supplier_events;

import java.time.LocalDateTime;
import java.util.Arrays;

import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.ff.client.FulfillmentWorkflowClientApi;
import ru.yandex.market.ff.client.dto.SupplierWithFirstFinishedSupplyInfo;
import ru.yandex.market.ff.client.dto.SuppliersWithFirstFinishedSupplyInfos;
import ru.yandex.market.shop.FunctionalTest;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SupplierFirstSupplyExecutorTest extends FunctionalTest {

    @Autowired
    private SupplierFirstSupplyExecutor executor;
    @Autowired
    private FulfillmentWorkflowClientApi clientApi;

    @Test
    @DbUnitDataSet(before = "SupplierFirstSupplyExecutorTest.before.csv", after = "SupplierFirstSupplyExecutorTest.after.csv")
    void doJobTest() {
        mockClientApi();
        executor.doJob(null);
        verify(clientApi).getSuppliersWithFirstFinishedSupplyInfo();
    }

    @Test
    @DbUnitDataSet(before = "SupplierFirstSupplyExecutorTest.before.csv", after = "SupplierFirstSupplyExecutorTest.after.csv")
    void doJobTwiceTest() {
        mockClientApi();
        executor.doJob(null);
        executor.doJob(null);
        verify(clientApi, times(2)).getSuppliersWithFirstFinishedSupplyInfo();
    }

    private void mockClientApi() {
        SuppliersWithFirstFinishedSupplyInfos suppliersWithFirstFinishedSupplyInfos =
                new SuppliersWithFirstFinishedSupplyInfos();
        suppliersWithFirstFinishedSupplyInfos.setSuppliersWithFirstFinishedSupplyInfos(Arrays.asList(
                getSupplierWithFirstSupplyInfo(101, 10, LocalDateTime.of(2020, 3, 15, 12, 34, 23)),
                getSupplierWithFirstSupplyInfo(102, 6, LocalDateTime.of(2020, 4, 14, 13, 23, 13)),
                getSupplierWithFirstSupplyInfo(105, 5, LocalDateTime.of(2020, 4, 13, 13, 23, 13))
        ));
        when(clientApi.getSuppliersWithFirstFinishedSupplyInfo()).thenReturn(suppliersWithFirstFinishedSupplyInfos);
    }

    private SupplierWithFirstFinishedSupplyInfo getSupplierWithFirstSupplyInfo(long supplierId,
                                                                               long supplyId,
                                                                               LocalDateTime updatedAt) {
        SupplierWithFirstFinishedSupplyInfo supplierWithFirstFinishedSupplyInfo =
                new SupplierWithFirstFinishedSupplyInfo();
        supplierWithFirstFinishedSupplyInfo.setSupplierId(supplierId);
        supplierWithFirstFinishedSupplyInfo.setFirstFinishedSupplyId(supplyId);
        supplierWithFirstFinishedSupplyInfo.setFirstFinishedSupplyUpdatedAt(updatedAt);
        return supplierWithFirstFinishedSupplyInfo;
    }
}
