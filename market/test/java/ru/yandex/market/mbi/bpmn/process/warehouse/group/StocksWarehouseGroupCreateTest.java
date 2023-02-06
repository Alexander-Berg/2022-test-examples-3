package ru.yandex.market.mbi.bpmn.process.warehouse.group;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.camunda.bpm.engine.RuntimeService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.unitils.reflectionassert.ReflectionAssert;

import ru.yandex.market.core.asyncreport.ReportState;
import ru.yandex.market.core.asyncreport.model.ReportRequest;
import ru.yandex.market.core.asyncreport.model.ReportsType;
import ru.yandex.market.core.param.model.EntityName;
import ru.yandex.market.fulfillment.stockstorage.client.entity.request.GroupWarehouse;
import ru.yandex.market.fulfillment.stockstorage.client.entity.request.SaveStocksWarehouseGroupRequest;
import ru.yandex.market.mbi.api.client.entity.partner.PartnerBusinessDTO;
import ru.yandex.market.mbi.api.client.entity.partner.PartnerFulfillmentLinkDTO;
import ru.yandex.market.mbi.api.client.entity.partner.PartnerFulfillmentLinksDTO;
import ru.yandex.market.mbi.api.client.entity.partner.PartnersBusinessResponse;
import ru.yandex.market.mbi.bpmn.FunctionalTest;
import ru.yandex.market.mbi.bpmn.util.CamundaTestUtil;
import ru.yandex.market.mbi.ff4shops.client.model.StocksWarehouseGroupCreateResponse;
import ru.yandex.market.mbi.ff4shops.client.model.StocksWarehouseGroupRequest;
import ru.yandex.market.mbi.open.api.client.model.SyncWarehouseRolesRequest;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.core.delivery.DeliveryServiceType.DROPSHIP;
import static ru.yandex.market.mbi.bpmn.model.enums.ProcessType.STOCKS_WAREHOUSE_GROUP_CREATE;
import static ru.yandex.market.mbi.bpmn.util.AsyncReportTestUtil.TEST_REPORT_ID;
import static ru.yandex.market.mbi.bpmn.util.AsyncReportTestUtil.getReportInfoDTO;

public class StocksWarehouseGroupCreateTest extends FunctionalTest {

    private static final Long UID = 100500L;
    private static final List<Long> WAREHOUSE_IDS = List.of(1L, 2L, 3L);
    private static final Map<Long, Long> GROUP_PARTNER_IDS = Map.of(1L, 11L, 2L, 22L, 3L, 33L);
    private static final String WAREHOUSE_GROUP_NAME = "groupName";
    private static final Long CREATED_GROUP_ID = 3L;
    private static final Long MAIN_WAREHOUSE_GROUP_ID = WAREHOUSE_IDS.stream().min(Comparator.naturalOrder()).get();
    private static final long BUSINESS_ID = 10;

    @Test
    @DisplayName("Проверяет успешный запуск процесса")
    public void testSuccessProcess() throws InterruptedException {
        mockCreateGroup();
        RuntimeService runtimeService = processEngine.getRuntimeService();
        var processInstance = runtimeService.startProcessInstanceByKey(
                STOCKS_WAREHOUSE_GROUP_CREATE.getId(),
                TEST_BUSINESS_KEY,
                Map.of("uid", UID,
                        "warehousesIds", WAREHOUSE_IDS,
                        "groupName", WAREHOUSE_GROUP_NAME)
        );

        assertTrue(CamundaTestUtil.waitUntilNoActiveJobs(
                processEngine,
                processInstance.getProcessInstanceId()
        ));

        CamundaTestUtil.checkIncidents(processEngine, processInstance);
        verifyAll();
    }

    private void verifyAll() {
        ArgumentCaptor<SaveStocksWarehouseGroupRequest> captor =
                ArgumentCaptor.forClass(SaveStocksWarehouseGroupRequest.class);
        // создание группы в ff4shops
        verify(ff4shopsClient).createStocksWarehouseGroup(UID, createStocksWarehouseGroupRequest());
        // получить partnerId для группы
        verify(mbiApiClient).getPartnerLinks(WAREHOUSE_IDS);
        verify(mbiApiClient).getBusinessesForPartners(any());
        // сгенерировать файл с текущими остатками
        verify(mbiApiClient).requestReportGeneration(eq(ReportRequest.<ReportsType>builder()
                        .setEntityId(11)
                        .setEntityName(EntityName.PARTNER)
                        .setParams(Map.of("partnerWarehouseIds", "11,22,33", "businessId", BUSINESS_ID,
                                "mainWarehouseId", 1L))
                        .setReportType(ReportsType.SHARED_CURRENT_STOCKS)
                .build()));
        verify(mbiApiClient).getReportInfo(TEST_REPORT_ID);
        // Обнуление стоков
        verify(ff4shopsClient).resetStocks(1);
        verify(ff4shopsClient).resetStocks(2);
        verify(ff4shopsClient).resetStocks(3);
        // создание группы в SS
        verify(stockStorageWarehouseGroupClient).saveGroup(captor.capture());
        ReflectionAssert.assertReflectionEquals(new SaveStocksWarehouseGroupRequest(CREATED_GROUP_ID,
                        MAIN_WAREHOUSE_GROUP_ID, WAREHOUSE_IDS.stream()
                        .map(wId -> new GroupWarehouse(GROUP_PARTNER_IDS.get(wId), wId))
                        .collect(Collectors.toList())),
                captor.getValue());
        // Синхронизировать роли
        verify(mbiOpenApiClient).syncWarehouseRoles(UID, createSyncWarehouseRolesRequest());
        // Включить синк стоков для DBS
        verify(mbiOpenApiClient).useStocks(UID, WAREHOUSE_IDS);
        verifyNoMoreInteractions(ff4shopsClient, mbiOpenApiClient, mbiApiClient, stockStorageWarehouseGroupClient);
    }

    private void mockCreateGroup() {
        when(ff4shopsClient.createStocksWarehouseGroup(UID, createStocksWarehouseGroupRequest()))
                .thenReturn(CompletableFuture.completedFuture(
                        new StocksWarehouseGroupCreateResponse().id(CREATED_GROUP_ID)
                                .mainWarehouseId(MAIN_WAREHOUSE_GROUP_ID)));
        CompletableFuture<Void> resetStocksCf = new CompletableFuture<>();
        resetStocksCf.complete(null);
        when(ff4shopsClient.resetStocks(anyLong())).thenReturn(resetStocksCf);
        when(mbiApiClient.getPartnerLinks(WAREHOUSE_IDS)).thenReturn(
                new PartnerFulfillmentLinksDTO(WAREHOUSE_IDS.stream()
                        .map(whId ->
                                new PartnerFulfillmentLinkDTO(GROUP_PARTNER_IDS.get(whId), whId, 999L, DROPSHIP)
                        ).collect(Collectors.toList())));
        when(mbiApiClient.getBusinessesForPartners(any())).thenReturn(new PartnersBusinessResponse(List.of(
                new PartnerBusinessDTO(1, BUSINESS_ID))));

        when(mbiApiClient.requestReportGeneration(any())).thenReturn(getReportInfoDTO(1, ReportState.PROCESSING));
        when(mbiApiClient.getReportInfo(any())).thenReturn(getReportInfoDTO(1, ReportState.DONE));
    }

    private void mockCreateGroupFail() {
        doReturn(CompletableFuture.<StocksWarehouseGroupCreateResponse>failedFuture(
                new Exception()))
                .when(ff4shopsClient).createStocksWarehouseGroup(anyLong(), any());
    }

    private StocksWarehouseGroupRequest createStocksWarehouseGroupRequest() {
        return new StocksWarehouseGroupRequest()
                .warehouseIds(WAREHOUSE_IDS).name(WAREHOUSE_GROUP_NAME);
    }

    private SyncWarehouseRolesRequest createSyncWarehouseRolesRequest() {
        return new SyncWarehouseRolesRequest().warehouseIds(WAREHOUSE_IDS);
    }

    @Test
    @DisplayName("Процесс падает на первом кубике")
    public void failFirstTask() {
        mockCreateGroupFail();
        RuntimeService runtimeService = processEngine.getRuntimeService();
        assertThrows(Exception.class, () -> runtimeService.startProcessInstanceByKey(
                STOCKS_WAREHOUSE_GROUP_CREATE.getId(),
                TEST_BUSINESS_KEY,
                Map.of("uid", UID,
                        "warehousesIds", WAREHOUSE_IDS,
                        "groupName", WAREHOUSE_GROUP_NAME)
        ));

        verify(ff4shopsClient, times(1))
                .createStocksWarehouseGroup(anyLong(), any());
        verify(mbiOpenApiClient, never())
                .syncWarehouseRoles(anyLong(), any());
        verify(mbiOpenApiClient, never())
                .useStocks(anyLong(), any());
        verifyNoMoreInteractions(ff4shopsClient, mbiOpenApiClient, stockStorageWarehouseGroupClient);
    }
}
