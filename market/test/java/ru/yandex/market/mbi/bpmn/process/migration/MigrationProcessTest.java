package ru.yandex.market.mbi.bpmn.process.migration;

import java.util.List;
import java.util.Map;

import org.camunda.bpm.engine.runtime.Incident;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.task.Task;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ru.yandex.market.business.migration.BusinessMigration;
import ru.yandex.market.core.asyncreport.ReportState;
import ru.yandex.market.mbi.api.client.entity.business.CanMigrateVerdictDTO;
import ru.yandex.market.mbi.api.client.entity.operation.ExternalOperationResult;
import ru.yandex.market.mbi.api.client.entity.operation.OperationStatistics;
import ru.yandex.market.mbi.api.client.entity.operation.OperationStatus;
import ru.yandex.market.mbi.bpmn.model.enums.ProcessType;
import ru.yandex.market.mbi.bpmn.util.CamundaTestUtil;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.Mockito.times;
import static ru.yandex.market.mbi.bpmn.util.AsyncReportTestUtil.TEST_REPORT_ID;
import static ru.yandex.market.mbi.bpmn.util.AsyncReportTestUtil.getReportInfoDTO;


public class MigrationProcessTest extends AbstractMigrationTest {
    @Test
    @DisplayName("Если миграция не возможна, процесс не запускается.")
    void canMigrate_no_processNotStarted() {
        willAnswer(invocation -> CanMigrateVerdictDTO.no(REASON)).given(mbiApiClient)
                .canMigrate(anyLong(), anyLong(), anyLong());

        assertThrows(RuntimeException.class, () -> runMigrationProcessInstance(ProcessType.MIGRATION_FULL),
                REASON);

        then(mbiApiClient).should(times(0)).lockBusiness(any());
    }


    @Test
    @DisplayName("Проверка, что  кубик блокировки ММБО отвечает 10 раз IN_PROGRESS")
    void testRetriesOnLock() throws InterruptedException {
        //given
        willAnswer(this::pendingLockAnswer).given(mboService).lock(any(), any());

        //when
        ProcessInstance processInstance = runMigrationProcessInstance(ProcessType.MIGRATION_FULL);
        assertNotNull(processInstance);

        //then
        assertTrue(CamundaTestUtil.waitUntilNoActiveJobs(
                processEngine, processInstance.getProcessInstanceId()));
        //lock
        then(mbiOpenApiClient).should().lockBusiness(any());
        then(mboService).should(times(11)).lock(any(), any());
        then(mdmService).should(times(0)).lock(any(), any());
        then(dataCampShopClient).should(times(0)).lock(any());
        //unlock
        then(mbiOpenApiClient).should().unlockBusiness(any());
        then(dataCampShopClient).should(times(0)).unlock(any());
        then(mdmService).should(times(0)).unlock(any(), any());
        //mbi-api
        then(mbiApiClient).should().updateOperationStatus(Mockito.refEq(
                new ExternalOperationResult(processInstance.getId(), OperationStatus.ERROR,
                        new OperationStatistics(Map.of("lockProcess", Map.of(
                                "MBO", Map.of(
                                        "status", "FAIL",
                                        "message", "Lock retries expired"
                                )
                        ))))
        ));
        then(mbiApiClient).should(times(0)).requestReportGeneration(any());
    }

    @Test
    @DisplayName("Проверка, что падает кубик блокировки Хранилища")
    void testDataCampFail() throws InterruptedException {
        //given
        willAnswer(invocation -> failLockResponse()).given(dataCampShopClient).lock(any());

        //when
        ProcessInstance processInstance = runMigrationProcessInstance(ProcessType.MIGRATION_FULL);
        assertNotNull(processInstance);

        //then
        assertTrue(CamundaTestUtil.waitUntilNoActiveJobs(
                processEngine, processInstance.getProcessInstanceId()));
        then(mbiOpenApiClient).should().lockBusiness(any());
        then(mboService).should().lock(any(), any());
        then(dataCampShopClient).should().lock(any());
        then(mbiOpenApiClient).should().unlockBusiness(any());
        then(mboService).should().unlock(
                eq(BusinessMigration.UnlockBusinessRequest.newBuilder()
                        .setCancel(true)
                        .setProcessId(processInstance.getId())
                        .setDstBusinessId(DST_BUSINES_ID)
                        .setSrcBusinessId(SRC_BUSINESS_ID)
                        .setShopId(SERVICE_ID)
                        .build()), any());
        then(dataCampShopClient).should(times(0)).unlock(any());

        then(mbiApiClient).should(times(0)).requestReportGeneration(any());
        then(mbiOpenApiClient).should(times(0))
                .requestReportGeneration(any(), anyLong(), any());
        then(mbiApiClient).should().updateOperationStatus(Mockito.refEq(
                new ExternalOperationResult(processInstance.getId(), OperationStatus.ERROR,
                        new OperationStatistics(Map.of("lockProcess", Map.of(
                                "DATACAMP", Map.of(
                                        "status", "FAIL",
                                        "message", "ErrorMessage"
                                )
                        ))))
        ));
    }

    @Test
    @DisplayName("Проверка, что падает кубик блокировки МДМ")
    void testMdmFail() throws InterruptedException {
        //given
        willAnswer(this::failLockAnswer).given(mdmService).lock(any(), any());

        //when
        ProcessInstance processInstance = runMigrationProcessInstance(ProcessType.MIGRATION_FULL);
        assertNotNull(processInstance);

        //then
        assertTrue(CamundaTestUtil.waitUntilNoActiveJobs(
                processEngine, processInstance.getProcessInstanceId()));
        //lock
        then(mbiOpenApiClient).should().lockBusiness(any());
        then(dataCampShopClient).should().lock(any());
        then(mdmService).should().lock(any(), any());
        then(pppService).should(times(0)).lock(any(), any());
        //unlock
        then(mdmService).should(times(0)).unlock(any(), any());
        then(dataCampShopClient).should().unlock(any());
        then(mbiOpenApiClient).should().unlockBusiness(any());
        then(mbiApiClient).should().updateOperationStatus(Mockito.refEq(
                new ExternalOperationResult(processInstance.getId(), OperationStatus.ERROR,
                        new OperationStatistics(Map.of("lockProcess", Map.of(
                                "MDM", Map.of(
                                        "status", "FAIL",
                                        "message", "ErrorMessage"
                                )
                        ))))
        ));
        then(mbiApiClient).should(times(0)).requestReportGeneration(any());
    }

    @Test
    @DisplayName("Проверка, что падает кубик блокировки МБО")
    void testMboFail() throws InterruptedException {
        //given
        willAnswer(this::failLockAnswer).given(mboService).lock(any(), any());

        //when
        ProcessInstance processInstance = runMigrationProcessInstance(ProcessType.MIGRATION_FULL);
        assertNotNull(processInstance);

        //then
        assertTrue(CamundaTestUtil.waitUntilNoActiveJobs(
                processEngine, processInstance.getProcessInstanceId()));
        //lock
        then(mbiOpenApiClient).should().lockBusiness(any());
        then(mboService).should().lock(any(), any());
        then(mdmService).should(times(0)).lock(any(), any());
        then(dataCampShopClient).should(times(0)).lock(any());
        //unlock
        then(mbiOpenApiClient).should().unlockBusiness(any());
        then(dataCampShopClient).should(times(0)).unlock(any());
        then(mdmService).should(times(0)).unlock(any(), any());
        //mbi-api
        then(mbiApiClient).should().updateOperationStatus(Mockito.refEq(
                new ExternalOperationResult(processInstance.getId(), OperationStatus.ERROR,
                        new OperationStatistics(Map.of("lockProcess", Map.of(
                                "MBO", Map.of(
                                        "status", "FAIL",
                                        "message", "ErrorMessage"
                                )
                        ))))
        ));
        then(mbiApiClient).should(times(0)).requestReportGeneration(any());
    }

    @Test
    @DisplayName("Проверка, что падает кубик блокировки PPP")
    void testPPPFail() throws InterruptedException {
        //given
        willAnswer(this::failLockAnswer).given(pppService).lock(any(), any());

        //when
        ProcessInstance processInstance = runMigrationProcessInstance(ProcessType.MIGRATION_FULL);
        assertNotNull(processInstance);

        //then
        assertTrue(CamundaTestUtil.waitUntilNoActiveJobs(
                processEngine, processInstance.getProcessInstanceId()));
        //lock
        then(mbiOpenApiClient).should().lockBusiness(any());
        then(mboService).should().lock(any(), any());
        then(dataCampShopClient).should().lock(any());
        then(mdmService).should().lock(any(), any());
        then(pppService).should().lock(any(), any());
        //unlock
        then(mdmService).should().unlock(any(), any());
        then(dataCampShopClient).should().unlock(any());
        then(mboService).should().unlock(any(), any());
        then(mbiOpenApiClient).should().unlockBusiness(any());
        then(pppService).should(times(0)).unlock(any(), any());
        //mbi-api
        then(mbiApiClient).should().updateOperationStatus(Mockito.refEq(
                new ExternalOperationResult(processInstance.getId(), OperationStatus.ERROR,
                        new OperationStatistics(Map.of("lockProcess", Map.of(
                                "PPP", Map.of(
                                        "status", "FAIL",
                                        "message", "ErrorMessage"
                                )
                        ))))
        ));
        then(mbiApiClient).should(times(0)).requestReportGeneration(any());
    }

    @DisplayName("Проверка, что падает кубик проверки статуса индексации")
    @Test
    void testIsPartnerIndexedWithBusinessId_fail() throws InterruptedException {
        //given 1й раз кубик индексации возвращает false, 2й раз true
        willAnswer(invocation -> failMbiCheckPartnerBusinessIdResponse(SERVICE_ID, DST_BUSINES_ID))
                .willAnswer(invocation -> successMbiCheckPartnerBusinessIdResponse(SERVICE_ID, DST_BUSINES_ID))
                .given(mbiApiClient).isPartnerIndexedWithBusiness(anyLong(), anyLong());

        //when
        ProcessInstance processInstance = runMigrationProcessInstance(ProcessType.MIGRATION_FULL);
        assertNotNull(processInstance);

        //then
        assertTrue(CamundaTestUtil.waitUntilNoActiveJobs(
                processEngine, processInstance.getProcessInstanceId()));

        //lock
        then(mbiOpenApiClient).should().lockBusiness(any());
        then(mboService).should().lock(any(), any());
        then(dataCampShopClient).should().lock(any());
        then(mdmService).should().lock(any(), any());
        then(pppService).should().lock(any(), any());
        //unlock
        then(pppService).should().unlock(any(), any());
        then(mdmService).should().unlock(any(), any());
        then(dataCampShopClient).should().unlock(any());
        then(mboService).should().unlock(any(), any());
        then(mbiOpenApiClient).should().unlockBusiness(any());
        // здесь проверяем что кубик индексации сработал два раза (1й раз -> false, 2й -> true)
        then(mbiApiClient).should(times(2)).isPartnerIndexedWithBusiness(anyLong(), anyLong());
        then(mbiApiClient).should(times(5)).requestReportGeneration(any());
    }


    @Test
    @DisplayName("Создание инцидента, если не удачно сменили businessId")
    void testChangeBusinessIdFail() throws InterruptedException {
        //given
        willAnswer(invocation -> failBusinessIdChange()).given(mbiApiClient).changeBusiness(any());
        //when
        ProcessInstance processInstance = runMigrationProcessInstance(ProcessType.MIGRATION_FULL);
        assertNotNull(processInstance);
        //then
        assertTrue(CamundaTestUtil.waitUntilNoActiveJobs(
                processEngine, processInstance.getProcessInstanceId()));
        List<Incident> incidents = CamundaTestUtil.getListOfIncidents(processEngine, processInstance.getId());
        assertEquals(1, incidents.size());
        then(mbiOpenApiClient).should().lockBusiness(any());
        then(dataCampShopClient).should().lock(any());
        then(mdmService).should().lock(any(), any());
        then(mboService).should().lock(any(), any());
    }

    @Test
    @DisplayName("Создание ручного задания, если не смогли выполнить отчет")
    void testMigrationReportFail() throws InterruptedException {
        //given
        willAnswer(invocation -> getReportInfoDTO(ENTITY_ID, ReportState.DONE))
                .willAnswer(invocation -> getReportInfoDTO(ENTITY_ID, ReportState.FAILED))
                .given(mbiApiClient).getReportInfo(eq(TEST_REPORT_ID));
        //when
        ProcessInstance processInstance = runMigrationProcessInstance(ProcessType.MIGRATION_FULL);
        assertNotNull(processInstance);
        //then
        assertTrue(CamundaTestUtil.waitUntilNoActiveJobs(
                processEngine, processInstance.getProcessInstanceId()));
        List<Incident> incidents = CamundaTestUtil.getListOfIncidents(processEngine, processInstance.getId());
        assertEquals("Should have no incidents", 0, incidents.size());
        List<Task> tasks = CamundaTestUtil.getListOfUserTasksByRoot(processEngine, processInstance.getId());
        assertEquals("Should have user task", 1, tasks.size());
        then(mbiOpenApiClient).should().lockBusiness(any());
        then(dataCampShopClient).should().lock(any());
        then(mdmService).should().lock(any(), any());
        then(mboService).should().lock(any(), any());
        //не должны завершать операцию
        then(mbiApiClient).should(times(0)).updateOperationStatus(any());
    }

    @Test
    @DisplayName("Проверка, что блокировки работают")
    void testLocksSuccess() throws InterruptedException {
        //given
        willAnswer(invocation -> successMbiCheckPartnerBusinessIdResponse(11L, 2222L))
                .given(mbiApiClient).isPartnerIndexedWithBusiness(anyLong(), anyLong());

        //when
        ProcessInstance processInstance = runMigrationProcessInstance(ProcessType.MIGRATION_FULL);
        assertNotNull(processInstance);

        //then
        assertTrue(CamundaTestUtil.waitUntilNoActiveJobs(
                processEngine, processInstance.getProcessInstanceId()));
        then(mbiOpenApiClient).should().lockBusiness(any());
        then(dataCampShopClient).should().lock(any());
        then(mdmService).should().lock(any(), any());
        then(mboService).should().lock(any(), any());
        then(mbiOpenApiClient).should().unlockBusiness(any());
        then(dataCampShopClient).should().unlock(any());
        then(mdmService).should().unlock(any(), any());
        then(mboService).should().unlock(any(), any());
        then(mbiApiClient).should().updateOperationStatus(Mockito.refEq(
                new ExternalOperationResult(processInstance.getId(), OperationStatus.OK,
                        new OperationStatistics(Map.of("asyncReport", Map.of(
                                "retriesLeft", 5L,
                                "reportStatus", "DONE"
                                ),
                                "offersMigrateReport", Map.of(
                                        "retriesLeft", 5L,
                                        "reportStatus", "DONE"
                                ), "offersDeleteReport", Map.of(
                                        "retriesLeft", 5L,
                                        "reportStatus", "DONE"
                                ), "partnerPromosMigrateReport", Map.of(
                                        "retriesLeft", 5L,
                                        "reportStatus", "DONE"
                                ), "partnerPromosDeleteReport", Map.of(
                                        "retriesLeft", 5L,
                                        "reportStatus", "DONE"
                                ))))
        ));
        then(mbiApiClient).should().isPartnerIndexedWithBusiness(anyLong(), anyLong());
        then(mboService).should(times(2)).asyncFinish(refEq(
                BusinessMigration.AsyncFinishBusinessRequest.newBuilder()
                        .setShopId(11L)
                        .setSrcBusinessId(1111L)
                        .setDstBusinessId(2222L)
                        .build()), any());
    }
}
