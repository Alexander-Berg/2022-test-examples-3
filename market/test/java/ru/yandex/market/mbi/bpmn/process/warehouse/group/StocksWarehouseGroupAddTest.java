package ru.yandex.market.mbi.bpmn.process.warehouse.group;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.apache.commons.collections4.ListUtils;
import org.camunda.bpm.engine.RuntimeService;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.mbi.bpmn.FunctionalTest;
import ru.yandex.market.mbi.bpmn.util.CamundaTestUtil;
import ru.yandex.market.mbi.ff4shops.client.model.StocksWarehouseGroupDto;
import ru.yandex.market.mbi.ff4shops.client.model.WarehouseDto;
import ru.yandex.market.mbi.open.api.client.model.SyncWarehouseRolesRequest;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.mbi.bpmn.model.enums.ProcessType.STOCKS_WAREHOUSE_GROUP_ADD;

public class StocksWarehouseGroupAddTest extends FunctionalTest {

    private static final Long UID = 100500L;
    private static final List<Long> NEW_WAREHOUSE_IDS =  List.of(4L, 5L);
    private static final Long MAIN_WAREHOUSE_ID = 40L;
    private static final Long EXISTING_GROUP_ID = 10L;
    private static final List<Long> EXISTING_WAREHOUSE_IDS = List.of(40L, 41L);
    private static final List<Long> WAREHOUSE_IDS_IN_GROUP = ListUtils.union(NEW_WAREHOUSE_IDS, EXISTING_WAREHOUSE_IDS);

    @Test
    @Disabled
    @DisplayName("Проверяет успешный запуск процесса")
    public void testSuccessProcess() throws InterruptedException {
        mockUpdateGroup();
        RuntimeService runtimeService = processEngine.getRuntimeService();
        var processInstance = runtimeService.startProcessInstanceByKey(
                STOCKS_WAREHOUSE_GROUP_ADD.getId(),
                TEST_BUSINESS_KEY,
                Map.of("uid", UID,
                       "groupId", EXISTING_GROUP_ID,
                       "newWarehouseIds", NEW_WAREHOUSE_IDS)
        );

        assertTrue(CamundaTestUtil.waitUntilNoActiveJobs(
                processEngine,
                processInstance.getProcessInstanceId()
        ));

        CamundaTestUtil.checkIncidents(processEngine, processInstance);
        verifyAll();
    }

    private void verifyAll() {
        //Добавление складов в ff4shops
        verify(ff4shopsClient).addWarehousesToGroup(EXISTING_GROUP_ID, NEW_WAREHOUSE_IDS);
        // Обнуление стоков
        NEW_WAREHOUSE_IDS.forEach(id -> verify(ff4shopsClient).resetStocks(id));
        // Добавление склада в ss
        verify(stockStorageWarehouseGroupClient, times(2))
                .addWarehouseToGroup(anyLong(), any());
        // Синхронизировать роли
        verify(mbiOpenApiClient).syncWarehouseRoles(UID, createSyncWarehouseRolesRequest());
        // Включить синк стоков для DBS
        verify(mbiOpenApiClient).useStocks(UID, NEW_WAREHOUSE_IDS);
        verifyNoMoreInteractions(ff4shopsClient, mbiOpenApiClient, mbiApiClient, stockStorageWarehouseGroupClient);
    }

    private void mockUpdateGroup() {
        when(ff4shopsClient.addWarehousesToGroup(EXISTING_GROUP_ID, NEW_WAREHOUSE_IDS))
                .thenReturn(CompletableFuture.completedFuture(
                        new StocksWarehouseGroupDto()
                                .mainWarehouseId(MAIN_WAREHOUSE_ID)
                                .id(EXISTING_GROUP_ID)
                                .warehouses(createWarehouseDto(WAREHOUSE_IDS_IN_GROUP))));
    }

    private List<WarehouseDto> createWarehouseDto(List<Long> warehouseIds) {
        return warehouseIds.stream()
                .map(id -> new WarehouseDto().warehouseId(id).name("test"))
                .collect(Collectors.toList());
    }

    private SyncWarehouseRolesRequest createSyncWarehouseRolesRequest() {
        return new SyncWarehouseRolesRequest().warehouseIds(WAREHOUSE_IDS_IN_GROUP);
    }
}
