package ru.yandex.market.mbi.bpmn.process.warehouse.group;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.camunda.bpm.engine.RuntimeService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.mbi.bpmn.FunctionalTest;
import ru.yandex.market.mbi.bpmn.util.CamundaTestUtil;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.mbi.bpmn.model.enums.ProcessType.STOCKS_WAREHOUSE_GROUP_DELETE;

public class StocksWarehouseGroupDeleteTest extends FunctionalTest {

    private static final Long UID = 100500L;
    private static final Long WAREHOUSE_GROUP_ID = 3L;

    @Test
    @DisplayName("Проверяет успешный запуск процесса")
    public void testSuccessProcess() throws InterruptedException {
        mockDeleteGroup();
        RuntimeService runtimeService = processEngine.getRuntimeService();
        var processInstance = runtimeService.startProcessInstanceByKey(
                STOCKS_WAREHOUSE_GROUP_DELETE.getId(),
                TEST_BUSINESS_KEY,
                Map.of("uid", UID,
                        "groupId", WAREHOUSE_GROUP_ID)
        );

        assertTrue(CamundaTestUtil.waitUntilNoActiveJobs(
                processEngine,
                processInstance.getProcessInstanceId()
        ));

        CamundaTestUtil.checkIncidents(processEngine, processInstance);
        var incidents = CamundaTestUtil.getListOfIncidents(processEngine,
                processInstance.getProcessInstanceId());
        verifyAll();
    }

    @Test
    @DisplayName("Процесс падает на первом кубике")
    public void failFirstTask() {
        mockDeleteGroupFail();
        RuntimeService runtimeService = processEngine.getRuntimeService();
        assertThrows(Exception.class, () -> runtimeService.startProcessInstanceByKey(
                STOCKS_WAREHOUSE_GROUP_DELETE.getId(),
                TEST_BUSINESS_KEY,
                Map.of("uid", UID,
                        "groupId", WAREHOUSE_GROUP_ID)
        ));

        verify(ff4shopsClient, times(1))
                .deleteStocksWarehouseGroup(UID, WAREHOUSE_GROUP_ID);
        verifyNoMoreInteractions(ff4shopsClient, mbiOpenApiClient, stockStorageWarehouseGroupClient);
    }

    private void mockDeleteGroup() {
        when(ff4shopsClient.deleteStocksWarehouseGroup(UID, WAREHOUSE_GROUP_ID))
                .thenReturn(CompletableFuture.completedFuture(null));
    }

    private void mockDeleteGroupFail() {
        doThrow(new RuntimeException())
                .when(ff4shopsClient).deleteStocksWarehouseGroup(anyLong(), anyLong());
    }

    private void verifyAll() {
        verify(ff4shopsClient, times(1))
                .deleteStocksWarehouseGroup(UID, WAREHOUSE_GROUP_ID);
        verify(stockStorageWarehouseGroupClient, times(1))
                .deleteGroup(WAREHOUSE_GROUP_ID);
        verifyNoMoreInteractions(ff4shopsClient, mbiOpenApiClient, stockStorageWarehouseGroupClient);
    }
}
