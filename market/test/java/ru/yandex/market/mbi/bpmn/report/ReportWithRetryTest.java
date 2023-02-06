package ru.yandex.market.mbi.bpmn.report;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Map;

import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.core.asyncreport.ReportState;
import ru.yandex.market.core.asyncreport.model.ReportInfo;
import ru.yandex.market.core.asyncreport.model.ReportRequest;
import ru.yandex.market.core.asyncreport.model.ReportsType;
import ru.yandex.market.core.param.model.EntityName;
import ru.yandex.market.mbi.api.client.MbiApiClient;
import ru.yandex.market.mbi.api.client.entity.GenericCallResponse;
import ru.yandex.market.mbi.api.client.entity.operation.OperationStatus;
import ru.yandex.market.mbi.asyncreport.ReportInfoDTO;
import ru.yandex.market.mbi.bpmn.FunctionalTest;
import ru.yandex.market.mbi.bpmn.interceptor.TraceDelegateInterceptor;
import ru.yandex.market.mbi.bpmn.model.enums.ProcessType;
import ru.yandex.market.mbi.bpmn.util.AsyncReportTestUtil;
import ru.yandex.market.mbi.bpmn.util.CamundaTestUtil;
import ru.yandex.market.mbi.bpmn.util.MbiApiTestUtil;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.Mockito.times;
import static ru.yandex.market.mbi.bpmn.util.AsyncReportTestUtil.TEST_REPORT_ID;
import static ru.yandex.market.mbi.bpmn.util.MbiApiTestUtil.checkOperationStatus;

/**
 * Тестирование схемы запуска генератора отчетов с ретраями.
 */
class ReportWithRetryTest extends FunctionalTest {

    public static final String TEST_REPORT_ID_NEW = "test_report_2";

    public static final long ENTITY_ID = 11;

    private static final Map<String, Object> OK_STATS = Map.of(
            "asyncReport", Map.of(
                    "retriesLeft", 0,
                    "reportStatus", "DONE"
            ),
            TraceDelegateInterceptor.X_MARKET_REQUEST_ID, "mytrace"
    );
    private static final Map<String, Object> FAILED_STATS = Map.of(
            "asyncReport", Map.of(
                    "retriesLeft", 0,
                    "reportStatus", "FAILED"
            ),
            TraceDelegateInterceptor.X_MARKET_REQUEST_ID, "mytrace"
    );

    @Autowired
    private MbiApiClient mbiApiClient;

    @BeforeEach
    void init() {
        MbiApiTestUtil.mockMbiNotifierResponse(mbiApiClient);
    }

    @AfterEach
    void checkMbiApi() {
        Mockito.verifyNoMoreInteractions(mbiApiClient);
    }

    @Test
    @DisplayName("Без entityId процесс не создается")
    void testFail() {
        Map<String, Object> variables = Map.of(
                "partnerId", 11
        );
        RuntimeService runtimeService = processEngine.getRuntimeService();
        assertThrows(IllegalStateException.class, () -> runtimeService.startProcessInstanceByKey(
                ProcessType.REPORT_WITH_RETRY.getId(),
                TEST_BUSINESS_KEY,
                variables
        ));
    }

    @Test
    @DisplayName("Успешный запуск")
    void testRun() throws InterruptedException {
        //given
        willAnswer(invocation -> AsyncReportTestUtil.getReportInfoDTO(ENTITY_ID, ReportState.PROCESSING))
                .given(mbiApiClient).requestReportGeneration(any());
        willAnswer(invocation -> AsyncReportTestUtil.getReportInfoDTO(ENTITY_ID, ReportState.DONE))
                .given(mbiApiClient).getReportInfo(eq(TEST_REPORT_ID));
        //when
        ProcessInstance processInstance = runReport(0);

        //then
        CamundaTestUtil.waitUntilNoActiveJobs(processEngine, processInstance.getProcessInstanceId());
        then(mbiApiClient).should().requestReportGeneration(any());
        then(mbiApiClient).should().getReportInfo(eq(TEST_REPORT_ID));
        checkOperationStatus(mbiApiClient, processInstance, OperationStatus.OK, OK_STATS);
    }

    @Test
    @DisplayName("Успешный запуск с двумя запросами статуса")
    void testRunWith2Requests() throws InterruptedException {
        //given
        willAnswer(invocation -> AsyncReportTestUtil.getReportInfoDTO(ENTITY_ID, ReportState.PROCESSING))
                .given(mbiApiClient).requestReportGeneration(any());
        willAnswer(invocation -> AsyncReportTestUtil.getReportInfoDTO(ENTITY_ID, ReportState.PROCESSING)).
                willAnswer(invocation -> AsyncReportTestUtil.getReportInfoDTO(ENTITY_ID, ReportState.DONE))
                .given(mbiApiClient).getReportInfo(eq(TEST_REPORT_ID));
        //when
        ProcessInstance processInstance = runReport(0);

        //then
        CamundaTestUtil.waitUntilNoActiveJobs(processEngine, processInstance.getProcessInstanceId());
        then(mbiApiClient).should().requestReportGeneration(any());
        then(mbiApiClient).should(times(2)).getReportInfo(eq(TEST_REPORT_ID));
        checkOperationStatus(mbiApiClient, processInstance, OperationStatus.OK, OK_STATS);
    }

    @Test
    @DisplayName("Успешный запуск с ошибкой и ретраем")
    void testRunWithRetry() throws InterruptedException {
        //given
        willAnswer(invocation -> AsyncReportTestUtil.getReportInfoDTO(ENTITY_ID, ReportState.PROCESSING))
                .willAnswer(invocation -> AsyncReportTestUtil
                        .getReportInfoDTO(ENTITY_ID, ReportState.PROCESSING, TEST_REPORT_ID_NEW))
                .given(mbiApiClient).requestReportGeneration(any());
        willAnswer(invocation -> AsyncReportTestUtil.getReportInfoDTO(ENTITY_ID, ReportState.PROCESSING))
                .willAnswer(invocation -> AsyncReportTestUtil.getReportInfoDTO(ENTITY_ID, ReportState.FAILED))
                .given(mbiApiClient).getReportInfo(eq(TEST_REPORT_ID));
        willAnswer(invocation -> AsyncReportTestUtil.getReportInfoDTO(ENTITY_ID, ReportState.FAILED))
                .given(mbiApiClient).getReportInfo(eq(TEST_REPORT_ID_NEW));
        //when
        ProcessInstance processInstance = runReport(1);

        //then
        CamundaTestUtil.waitUntilNoActiveJobs(processEngine, processInstance.getProcessInstanceId());
        then(mbiApiClient).should(times(2)).requestReportGeneration(any());
        then(mbiApiClient).should(times(2)).getReportInfo(eq(TEST_REPORT_ID));
        then(mbiApiClient).should().getReportInfo(eq(TEST_REPORT_ID_NEW));
        then(mbiApiClient).should(times(1)).getReportInfo(eq(TEST_REPORT_ID_NEW));
        checkOperationStatus(mbiApiClient, processInstance, OperationStatus.ERROR, FAILED_STATS);
    }

    @Test
    @DisplayName("Успешный запуск с ретраем после таймаута")
    void testRunWithRestartViaCancel() throws InterruptedException {
        Instant reportStarted10MinutesAgo = Instant.now().minus(30, ChronoUnit.MINUTES);
        //given
        willAnswer(invocation -> AsyncReportTestUtil.getReportInfoDTO(ENTITY_ID, ReportState.PROCESSING))
                .willAnswer(invocation -> AsyncReportTestUtil
                        .getReportInfoDTO(ENTITY_ID, ReportState.PROCESSING, TEST_REPORT_ID_NEW))
                .given(mbiApiClient).requestReportGeneration(any());
        willAnswer(invocation -> ReportInfoDTO.fromReportRequest(ReportInfo.<ReportsType>builder()
                .setId(TEST_REPORT_ID)
                .setState(ReportState.PROCESSING)
                .setReportRequest(new ReportRequest.RequestReportInfoBuilder<ReportsType>()
                        .setEntityId(ENTITY_ID)
                        .setReportType(ReportsType.MIGRATE_OFFERS_TO_UCAT)
                        .setEntityName(EntityName.PARTNER)
                        .setParams(Collections.emptyMap())
                        .build())
                .setRequestCreatedAt(reportStarted10MinutesAgo)
                .setStateUpdateAt(reportStarted10MinutesAgo)
                .setTouchedAt(null) // здесь цимес - отчет начал работу, но не дошел до первого touch
                .build()))
                .given(mbiApiClient).getReportInfo(eq(TEST_REPORT_ID));

        willAnswer(invocation -> GenericCallResponse.ok())
                .given(mbiApiClient).cancelReport(eq(TEST_REPORT_ID));

        willAnswer(invocation -> AsyncReportTestUtil.getReportInfoDTO(ENTITY_ID, ReportState.DONE))
                .given(mbiApiClient).getReportInfo(eq(TEST_REPORT_ID_NEW));
        //when
        ProcessInstance processInstance = runReport(1);

        //then
        CamundaTestUtil.waitUntilNoActiveJobs(processEngine, processInstance.getProcessInstanceId());
        then(mbiApiClient).should(times(2)).requestReportGeneration(any());
        then(mbiApiClient).should().getReportInfo(eq(TEST_REPORT_ID));
        then(mbiApiClient).should().cancelReport(eq(TEST_REPORT_ID));
        then(mbiApiClient).should().getReportInfo(eq(TEST_REPORT_ID_NEW));
        checkOperationStatus(mbiApiClient, processInstance, OperationStatus.OK, OK_STATS);
    }

    @Test
    @DisplayName("Если все генерации отвалились по таймауту, то шлем FAILED")
    void testRunWithRestartViaCancelFailed() throws InterruptedException {
        Instant reportStarted10MinutesAgo = Instant.now().minus(60, ChronoUnit.MINUTES);
        //given
        willAnswer(invocation -> AsyncReportTestUtil.getReportInfoDTO(ENTITY_ID, ReportState.PROCESSING))
                .given(mbiApiClient).requestReportGeneration(any());
        willAnswer(invocation -> ReportInfoDTO.fromReportRequest(ReportInfo.<ReportsType>builder()
                .setId(TEST_REPORT_ID)
                .setState(ReportState.PROCESSING)
                .setReportRequest(new ReportRequest.RequestReportInfoBuilder<ReportsType>()
                        .setEntityId(ENTITY_ID)
                        .setReportType(ReportsType.MIGRATE_OFFERS_TO_UCAT)
                        .setEntityName(EntityName.PARTNER)
                        .setParams(Collections.emptyMap())
                        .build())
                .setRequestCreatedAt(reportStarted10MinutesAgo)
                .setStateUpdateAt(reportStarted10MinutesAgo)
                .setTouchedAt(reportStarted10MinutesAgo) // здесь цимес - отчет начал работу, но не дошел до первого
                // touch
                .build()))
                .given(mbiApiClient).getReportInfo(eq(TEST_REPORT_ID));

        willAnswer(invocation -> GenericCallResponse.ok())
                .given(mbiApiClient).cancelReport(eq(TEST_REPORT_ID));

        //when
        ProcessInstance processInstance = runReport(4);

        //then
        CamundaTestUtil.waitUntilNoActiveJobs(processEngine, processInstance.getProcessInstanceId());
        then(mbiApiClient).should(times(5)).requestReportGeneration(any());
        then(mbiApiClient).should(times(5)).getReportInfo(eq(TEST_REPORT_ID));
        then(mbiApiClient).should(times(5)).cancelReport(eq(TEST_REPORT_ID));
        checkOperationStatus(mbiApiClient, processInstance, OperationStatus.ERROR, FAILED_STATS);
    }

    @Test
    @DisplayName("Успешный запуск с ошибкой и без ретраев")
    void testRunFailWithoutRetry() throws InterruptedException {
        //given
        willAnswer(invocation -> AsyncReportTestUtil.getReportInfoDTO(ENTITY_ID, ReportState.PROCESSING))
                .given(mbiApiClient).requestReportGeneration(any());
        willAnswer(invocation -> AsyncReportTestUtil.getReportInfoDTO(ENTITY_ID, ReportState.PROCESSING)).
                willAnswer(invocation -> AsyncReportTestUtil.getReportInfoDTO(ENTITY_ID, ReportState.FAILED))
                .given(mbiApiClient).getReportInfo(eq(TEST_REPORT_ID));
        //when
        ProcessInstance processInstance = runReport(0);

        //then
        CamundaTestUtil.waitUntilNoActiveJobs(processEngine, processInstance.getProcessInstanceId());
        then(mbiApiClient).should(times(1)).requestReportGeneration(any());
        then(mbiApiClient).should(times(2)).getReportInfo(eq(TEST_REPORT_ID));
        checkOperationStatus(mbiApiClient, processInstance, OperationStatus.ERROR, FAILED_STATS);
    }

    private ProcessInstance runReport(int retries) {
        Map<String, Object> variables = Map.of(
                "entityId", ENTITY_ID,
                "reportType", "ASSORTMENT",
                "params", Map.of(
                        "param1", "value1",
                        "param2", "value2",
                        "param3", "value3"
                ),
                "retries", retries,
                "operationId", "operation123",
                TraceDelegateInterceptor.X_MARKET_REQUEST_ID, "mytrace"
        );
        RuntimeService runtimeService = processEngine.getRuntimeService();
        return runtimeService.startProcessInstanceByKey(
                ProcessType.REPORT_WITH_RETRY.getId(),
                TEST_BUSINESS_KEY,
                variables
        );
    }
}
