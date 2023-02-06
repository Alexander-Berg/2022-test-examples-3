package ru.yandex.market.mbi.bpmn.process.migration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.camunda.bpm.engine.runtime.Incident;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ru.yandex.market.business.migration.BusinessMigration;
import ru.yandex.market.core.asyncreport.ReportState;
import ru.yandex.market.core.asyncreport.model.ReportRequest;
import ru.yandex.market.core.asyncreport.model.ReportsType;
import ru.yandex.market.mbi.api.client.entity.business.CanMigrateVerdictDTO;
import ru.yandex.market.mbi.api.client.entity.operation.ExternalOperationResult;
import ru.yandex.market.mbi.api.client.entity.operation.OperationStatistics;
import ru.yandex.market.mbi.api.client.entity.operation.OperationStatus;
import ru.yandex.market.mbi.api.client.entity.supplier.SetUnitedCatalogStatusRequest;
import ru.yandex.market.mbi.bpmn.exception.SupplierMigrationNotPossibleException;
import ru.yandex.market.mbi.bpmn.model.enums.ProcessType;
import ru.yandex.market.mbi.bpmn.util.AsyncReportTestUtil;
import ru.yandex.market.mbi.bpmn.util.CamundaTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Mockito.times;
import static ru.yandex.market.mbi.bpmn.util.AsyncReportTestUtil.TEST_REPORT_ID;
import static ru.yandex.market.mbi.bpmn.util.AsyncReportTestUtil.getReportInfoDTO;

class MigrateToUcatProcessTest extends AbstractMigrationTest {

    private static final int BUSINESS_ID = SRC_BUSINESS_ID;

    private static final Map<String, Object> PARAMS_MAP = Map.of(
            "operationId", OPERATION_ID,
            "businessId", BUSINESS_ID,
            "serviceId", SERVICE_ID
    );

    @AfterEach
    void checkMocks() {
        then(mbiApiClient).should().canMigrateToUCat(eq(Long.valueOf(SERVICE_ID)), eq(Long.valueOf(BUSINESS_ID)));
    }

    @Test
    @DisplayName("Если миграция не возможна, процесс не запускается.")
    void canMigrate_no_processNotStarted() {
        willAnswer(invocation -> CanMigrateVerdictDTO.no(REASON))
                .given(mbiApiClient).canMigrateToUCat(anyLong(), anyLong());

        assertThrows(
                SupplierMigrationNotPossibleException.class,
                () -> runWithVariables(ProcessType.MIGRATE_TO_UCAT, PARAMS_MAP),
                REASON
        );

        then(mbiApiClient).should(times(0)).lockBusiness(any());
    }

    @Test
    @DisplayName("Проверка, что падает кубик блокировки МБИ")
    void testMbiLockFail() {
        //given
        willReturn(failMbiLockResponse()).given(mbiOpenApiClient).lockBusiness(any());

        runWithVariables(ProcessType.MIGRATE_TO_UCAT, PARAMS_MAP);

        //then
        then(mbiOpenApiClient).should().lockBusiness(any());
        then(dataCampShopClient).should(times(0)).lock(any());
        then(mbiApiClient).should(times(0)).updateOperationStatus(any());
    }

    @Test
    @DisplayName("Проверка, что падает кубик блокировки Хранилища")
    void testDataCampFail() throws InterruptedException {
        //given
        willAnswer(invocation -> failLockResponse()).given(dataCampShopClient).lock(any());

        //when
        ProcessInstance processInstance = runWithVariables(ProcessType.MIGRATE_TO_UCAT, PARAMS_MAP);
        assertNotNull(processInstance);

        //then
        assertTrue(CamundaTestUtil.waitUntilNoActiveJobs(
                processEngine, processInstance.getProcessInstanceId()));
        then(mbiOpenApiClient).should().lockBusiness(any());
        then(mboService).should().lock(eq(getLockRequest(processInstance.getId())), any());
        then(dataCampShopClient).should().lock(eq(getLockRequest(processInstance.getId())));
        then(mbiOpenApiClient).should().unlockBusiness(any());
        then(mboService).should().unlock(eq(getUnlockRequest(processInstance.getId(), true)), any());
        then(dataCampShopClient).should(times(0)).unlock(any());
        then(mbiApiClient).should().updateOperationStatus(Mockito.refEq(
                new ExternalOperationResult(processInstance.getId(), OperationStatus.ERROR,
                        new OperationStatistics(new HashMap<>()))
        ));
    }

    @Test
    @DisplayName("Проверка, что падает кубик блокировки МДМ")
    void testMdmFail() throws InterruptedException {
        //given
        willAnswer(this::failLockAnswer).given(mdmService).lock(any(), any());

        //when
        ProcessInstance processInstance = runWithVariables(ProcessType.MIGRATE_TO_UCAT, PARAMS_MAP);
        assertNotNull(processInstance);

        //then
        assertTrue(CamundaTestUtil.waitUntilNoActiveJobs(
                processEngine, processInstance.getProcessInstanceId()));
        //lock
        then(mbiOpenApiClient).should().lockBusiness(any());
        then(mboService).should().lock(eq(getLockRequest(processInstance.getId())), any());
        then(dataCampShopClient).should().lock(eq(getLockRequest(processInstance.getId())));
        then(mdmService).should().lock(eq(getLockRequest(processInstance.getId())), any());
        then(pppService).should(times(0)).lock(any(), any());
        //unlock
        then(mdmService).should(times(0)).unlock(any(), any());
        then(mboService).should().unlock(eq(getUnlockRequest(processInstance.getId(), true)), any());
        then(dataCampShopClient).should().unlock(eq(getUnlockRequest(processInstance.getId(), true)));
        then(mbiOpenApiClient).should().unlockBusiness(any());
        then(mbiApiClient).should().updateOperationStatus(Mockito.refEq(
                new ExternalOperationResult(processInstance.getId(), OperationStatus.ERROR,
                        new OperationStatistics(new HashMap<>()))
        ));
    }

    @Test
    @DisplayName("Проверка, что падает кубик блокировки МБО")
    void testMboFail() throws InterruptedException {
        //given
        willAnswer(this::failLockAnswer).given(mboService).lock(any(), any());

        //when
        ProcessInstance processInstance = runWithVariables(ProcessType.MIGRATE_TO_UCAT, PARAMS_MAP);
        assertNotNull(processInstance);

        //then
        assertTrue(CamundaTestUtil.waitUntilNoActiveJobs(
                processEngine, processInstance.getProcessInstanceId()));
        //lock
        then(mbiOpenApiClient).should().lockBusiness(any());
        then(mboService).should().lock(eq(getLockRequest(processInstance.getId())), any());
        then(mdmService).should(times(0)).lock(any(), any());
        then(dataCampShopClient).should(times(0)).lock(any());
        //unlock
        then(mbiOpenApiClient).should().unlockBusiness(any());
        then(dataCampShopClient).should(times(0)).unlock(any());
        then(mdmService).should(times(0)).unlock(any(), any());
        //mbi-api
        then(mbiApiClient).should().updateOperationStatus(Mockito.refEq(
                new ExternalOperationResult(processInstance.getId(), OperationStatus.ERROR,
                        new OperationStatistics(new HashMap<>()))
        ));
    }

    @Test
    @DisplayName("Проверка, что падает кубик блокировки PPP")
    void testPPPFail() throws InterruptedException {
        //given
        willAnswer(this::failLockAnswer).given(pppService).lock(any(), any());

        //when
        ProcessInstance processInstance = runWithVariables(ProcessType.MIGRATE_TO_UCAT, PARAMS_MAP);
        assertNotNull(processInstance);

        //then
        assertTrue(CamundaTestUtil.waitUntilNoActiveJobs(
                processEngine, processInstance.getProcessInstanceId()));
        //lock
        then(mbiOpenApiClient).should().lockBusiness(any());
        then(mboService).should().lock(eq(getLockRequest(processInstance.getId())), any());
        then(dataCampShopClient).should().lock(eq(getLockRequest(processInstance.getId())));
        then(mdmService).should().lock(eq(getLockRequest(processInstance.getId())), any());
        then(pppService).should().lock(eq(getLockRequest(processInstance.getId())), any());
        //unlock
        then(pppService).should(times(0)).unlock(any(), any());
        then(mdmService).should().unlock(eq(getUnlockRequest(processInstance.getId(), true)), any());
        then(dataCampShopClient).should().unlock(eq(getUnlockRequest(processInstance.getId(), true)));
        then(mboService).should().unlock(eq(getUnlockRequest(processInstance.getId(), true)), any());
        then(mbiOpenApiClient).should().unlockBusiness(any());
        //mbi-api
        then(mbiApiClient).should().updateOperationStatus(Mockito.refEq(
                new ExternalOperationResult(processInstance.getId(), OperationStatus.ERROR,
                        new OperationStatistics(new HashMap<>()))
        ));
    }


    @Test
    @DisplayName("Падает миграция категорий. ручной разбор")
    void testCatsMigrationFail() throws InterruptedException {
        //given
        willAnswer(invocation -> getReportInfoDTO(ENTITY_ID, ReportState.FAILED))
                .given(mbiApiClient).getReportInfo(eq(TEST_REPORT_ID));

        //when
        ProcessInstance processInstance = runWithVariables(ProcessType.MIGRATE_TO_UCAT, PARAMS_MAP);
        assertNotNull(processInstance);

        //then
        assertTrue(CamundaTestUtil.waitUntilNoActiveJobs(
                processEngine, processInstance.getProcessInstanceId()));
        then(mbiOpenApiClient).should().lockBusiness(any());
        then(mboService).should().lock(eq(getLockRequest(processInstance.getId())), any());
        then(dataCampShopClient).should().lock(eq(getLockRequest(processInstance.getId())));
        then(mdmService).should().lock(eq(getLockRequest(processInstance.getId())), any());
        then(pppService).should().lock(eq(getLockRequest(processInstance.getId())), any());

        ReportRequest catsMigrationReportRequest = AsyncReportTestUtil.getReportRequest(SERVICE_ID,
                ReportsType.MIGRATION_CATEGORIES,
                Map.of("sourceBusinessId", BUSINESS_ID,
                        "targetBusinessId", BUSINESS_ID,
                        "sourcePartnerId", SERVICE_ID,
                        "fromMbo", true)
        );
        then(mbiApiClient).should(times(6)).requestReportGeneration(eq(catsMigrationReportRequest));
        then(mbiApiClient).should(times(6)).getReportInfo(eq(TEST_REPORT_ID));

        then(pppService).should(times(0)).unlock(any(), any());
        then(mdmService).should(times(0)).unlock(any(), any());
        then(dataCampShopClient).should(times(0)).unlock(any());
        then(mboService).should(times(0)).unlock(any(), any());
    }

    @Test
    @DisplayName("Создаем инцидент, если падает установка признака ЕКат.")
    void testEnableUCatStatusFails() throws InterruptedException {
        //given
        willAnswer(invocation -> failEnableUCatStatus())
                .given(mbiApiClient).enableUnitedCatalog(eq(Long.valueOf(SERVICE_ID)), any());

        //when
        ProcessInstance processInstance = runWithVariables(ProcessType.MIGRATE_TO_UCAT, PARAMS_MAP);
        assertNotNull(processInstance);

        //then
        assertTrue(CamundaTestUtil.waitUntilNoActiveJobs(
                processEngine, processInstance.getProcessInstanceId()));
        then(mbiOpenApiClient).should().lockBusiness(any());
        then(mboService).should().lock(eq(getLockRequest(processInstance.getId())), any());
        then(dataCampShopClient).should().lock(eq(getLockRequest(processInstance.getId())));
        then(mdmService).should().lock(eq(getLockRequest(processInstance.getId())), any());
        then(pppService).should().lock(eq(getLockRequest(processInstance.getId())), any());

        ReportRequest catsMigrationReportRequest = AsyncReportTestUtil.getReportRequest(SERVICE_ID,
                ReportsType.MIGRATION_CATEGORIES,
                Map.of("sourceBusinessId", BUSINESS_ID,
                        "targetBusinessId", BUSINESS_ID,
                        "sourcePartnerId", SERVICE_ID,
                        "fromMbo", true)
        );
        then(mbiApiClient).should().requestReportGeneration(eq(catsMigrationReportRequest));
        ReportRequest offersMigrationReportRequest = AsyncReportTestUtil.getReportRequest(SERVICE_ID,
                ReportsType.MIGRATE_OFFERS_TO_UCAT,
                Map.of("sourceBusinessId", BUSINESS_ID,
                        "targetBusinessId", BUSINESS_ID,
                        "sourcePartnerId", SERVICE_ID,
                        "fromMbo", true)
        );
        then(mbiApiClient).should().requestReportGeneration(eq(offersMigrationReportRequest));
        then(mbiApiClient).should(times(3)).getReportInfo(eq(TEST_REPORT_ID));
        then(mbiApiClient).should(times(5))
                .enableUnitedCatalog(
                        eq(Long.valueOf(SERVICE_ID)),
                        eq(new SetUnitedCatalogStatusRequest(processInstance.getProcessInstanceId()))
                );

        List<Incident> incidents = CamundaTestUtil.getListOfIncidents(processEngine, processInstance.getId());
        assertEquals(1, incidents.size());
    }

    @DisplayName("Проверка, что падает кубик проверки статуса индексации")
    @Test
    void testIsSupplierIndexedWithUCat_fail() throws InterruptedException {
        willAnswer(invocation -> failIsSupplierIndexedWithUCatResponse(eq(Long.valueOf(SERVICE_ID))))
                .willAnswer(invocation -> successIsSupplierIndexedWithUCatResponse(eq(Long.valueOf(SERVICE_ID))))
                .given(mbiApiClient).isSupplierIndexedWithUCat(eq(Long.valueOf(SERVICE_ID)));

        ProcessInstance processInstance = runWithVariables(ProcessType.MIGRATE_TO_UCAT, PARAMS_MAP);
        assertNotNull(processInstance);

        assertTrue(CamundaTestUtil.waitUntilNoActiveJobs(
                processEngine, processInstance.getProcessInstanceId()));

        then(mbiOpenApiClient).should().lockBusiness(any());
        then(mboService).should().lock(any(), any());
        then(dataCampShopClient).should().lock(any());
        then(mdmService).should().lock(any(), any());
        then(pppService).should().lock(any(), any());
        then(mbiApiClient).should().enableUnitedCatalog(eq(Long.valueOf(SERVICE_ID)), any());
        then(pppService).should().unlock(any(), any());
        then(mdmService).should().unlock(any(), any());
        then(dataCampShopClient).should().unlock(any());
        then(mboService).should().unlock(any(), any());
        then(mbiOpenApiClient).should().unlockBusiness(any());
        then(mbiApiClient).should(times(2)).isSupplierIndexedWithUCat(eq(Long.valueOf(SERVICE_ID)));
        then(mbiApiClient).should(times(3)).getReportInfo(eq(TEST_REPORT_ID));
        then(mbiApiClient).should(times(3)).requestReportGeneration(any());
        then(mbiApiClient).should().updateOperationStatus(any());
        then(mboService).should(times(2)).asyncFinish(refEq(
                BusinessMigration.AsyncFinishBusinessRequest.newBuilder()
                        .setShopId(SERVICE_ID)
                        .setSrcBusinessId(BUSINESS_ID)
                        .setDstBusinessId(BUSINESS_ID)
                        .build()), any());
    }

    @Test
    @DisplayName("Проверка, что все работает")
    void testProcessIsSuccessful() throws InterruptedException {
        willAnswer(invocation -> successIsSupplierIndexedWithUCatResponse(eq(Long.valueOf(SERVICE_ID))))
                .given(mbiApiClient).isSupplierIndexedWithUCat(eq(Long.valueOf(SERVICE_ID)));

        //when
        ProcessInstance processInstance = runWithVariables(ProcessType.MIGRATE_TO_UCAT, PARAMS_MAP);
        assertNotNull(processInstance);

        //then
        assertTrue(CamundaTestUtil.waitUntilNoActiveJobs(
                processEngine, processInstance.getProcessInstanceId()));
        then(mbiOpenApiClient).should().lockBusiness(any());
        then(mboService).should().lock(eq(getLockRequest(processInstance.getId())), any());
        then(dataCampShopClient).should().lock(eq(getLockRequest(processInstance.getId())));
        then(mdmService).should().lock(eq(getLockRequest(processInstance.getId())), any());
        then(pppService).should().lock(eq(getLockRequest(processInstance.getId())), any());

        ReportRequest catsMigrationReportRequest = AsyncReportTestUtil.getReportRequest(SERVICE_ID,
                ReportsType.MIGRATION_CATEGORIES,
                Map.of("sourceBusinessId", BUSINESS_ID,
                        "targetBusinessId", BUSINESS_ID,
                        "sourcePartnerId", SERVICE_ID,
                        "fromMbo", true)
        );
        ReportRequest offersMigrationReportRequest = AsyncReportTestUtil.getReportRequest(SERVICE_ID,
                ReportsType.MIGRATE_OFFERS_TO_UCAT,
                Map.of("sourceBusinessId", BUSINESS_ID,
                        "targetBusinessId", BUSINESS_ID,
                        "sourcePartnerId", SERVICE_ID,
                        "fromMbo", true)
        );
        then(mbiApiClient).should().requestReportGeneration(eq(catsMigrationReportRequest));
        then(mbiApiClient).should().requestReportGeneration(eq(offersMigrationReportRequest));
        then(mbiApiClient).should(times(3)).getReportInfo(eq(TEST_REPORT_ID));

        then(pppService).should().unlock(eq(getUnlockRequest(processInstance.getId())), any());
        then(mdmService).should().unlock(eq(getUnlockRequest(processInstance.getId())), any());
        then(dataCampShopClient).should().unlock(eq(getUnlockRequest(processInstance.getId())));
        then(mboService).should().unlock(eq(getUnlockRequest(processInstance.getId())), any());
        then(mbiApiClient).should().enableUnitedCatalog(eq(Long.valueOf(SERVICE_ID)), any());
        then(mbiApiClient).should().isSupplierIndexedWithUCat(eq(Long.valueOf(SERVICE_ID)));
        then(mbiOpenApiClient).should().unlockBusiness(any());
        then(mbiApiClient).should().updateOperationStatus(Mockito.refEq(
                new ExternalOperationResult(processInstance.getId(), OperationStatus.OK,
                        new OperationStatistics(Map.of(
                                "catsMigration", Map.of("retriesLeft", 5L, "reportStatus", "DONE"),
                                "migrateToUcatReport", Map.of("retriesLeft", 100L, "reportStatus", "DONE"),
                                "migrateToUcatCleanStocks", Map.of("retriesLeft", 10L, "reportStatus", "DONE"))))));

        then(mboService).should(times(2)).asyncFinish(refEq(
                BusinessMigration.AsyncFinishBusinessRequest.newBuilder()
                        .setShopId(SERVICE_ID)
                        .setSrcBusinessId(BUSINESS_ID)
                        .setDstBusinessId(BUSINESS_ID)
                        .build()), any());
    }

    private BusinessMigration.LockBusinessRequest getLockRequest(String processId) {
        return BusinessMigration.LockBusinessRequest.newBuilder()
                .setProcessId(processId)
                .setDstBusinessId(BUSINESS_ID)
                .setSrcBusinessId(BUSINESS_ID)
                .setShopId(SERVICE_ID)
                .build();
    }

    private BusinessMigration.UnlockBusinessRequest getUnlockRequest(String processId) {
        return getUnlockRequest(processId, false);
    }

    private BusinessMigration.UnlockBusinessRequest getUnlockRequest(
            String processId,
            boolean isCancelled
    ) {
        return BusinessMigration.UnlockBusinessRequest.newBuilder()
                .setCancel(isCancelled)
                .setProcessId(processId)
                .setDstBusinessId(BUSINESS_ID)
                .setSrcBusinessId(BUSINESS_ID)
                .setShopId(SERVICE_ID)
                .build();
    }
}
