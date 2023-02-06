package ru.yandex.market.mbi.bpmn.process.feed;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.stubbing.OngoingStubbing;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.core.asyncreport.ReportState;
import ru.yandex.market.core.asyncreport.model.ReportRequest;
import ru.yandex.market.core.asyncreport.model.ReportsType;
import ru.yandex.market.mbi.api.client.MbiApiClient;
import ru.yandex.market.mbi.api.client.entity.GenericCallResponse;
import ru.yandex.market.mbi.api.client.entity.bpmn.BpmnLockRequest;
import ru.yandex.market.mbi.api.client.entity.bpmn.BpmnLockType;
import ru.yandex.market.mbi.api.client.entity.operation.OperationStatus;
import ru.yandex.market.mbi.asyncreport.ReportInfoDTO;
import ru.yandex.market.mbi.bpmn.FunctionalTest;
import ru.yandex.market.mbi.bpmn.model.enums.ProcessType;
import ru.yandex.market.mbi.bpmn.util.CamundaTestUtil;

import static org.mockito.ArgumentMatchers.any;
import static ru.yandex.market.mbi.bpmn.util.AsyncReportTestUtil.getReportInfoDTO;
import static ru.yandex.market.mbi.bpmn.util.AsyncReportTestUtil.getReportRequest;
import static ru.yandex.market.mbi.bpmn.util.MbiApiTestUtil.checkOperationStatus;
import static ru.yandex.market.mbi.bpmn.util.MbiApiTestUtil.mockMbiNotifierResponse;

/**
 * Тесты на {@link ProcessType#PARTNER_FEED_OFFER_MIGRATION}.
 *
 * @author Kirill Batalin (batalin@yandex-team.ru)
 */
class PartnerFeedMigrationProcessTest extends FunctionalTest {

    private static final long PARTNER_ID = 1001L;
    private static final long FEED_ID = 2001L;
    private static final Instant TIMESTAMP = Instant.now();

    private static final String HIDING_REPORT_ID = "hiding_report_id";
    private static final Map<String, Object> HIDING_REPORT_PARAMS = Map.of(
            "partnerId", PARTNER_ID,
            "feedId", FEED_ID,
            "timestamp", TIMESTAMP
    );
    private static final ReportRequest<ReportsType> HIDING_REPORT_REQUEST = getReportRequest(
            PARTNER_ID, ReportsType.FEED_OFFER_HIDING, HIDING_REPORT_PARAMS);

    private static final String MIGRATION_REPORT_ID = "migration_report_id";
    private static final Map<String, Object> MIGRATION_REPORT_PARAMS = Map.of(
            "partnerId", PARTNER_ID,
            "feedId", FEED_ID
    );
    private static final ReportRequest<ReportsType> MIGRATION_REPORT_REQUEST = getReportRequest(
            PARTNER_ID, ReportsType.FEED_OFFER_MIGRATION, MIGRATION_REPORT_PARAMS);
    private static final int RETRIES_NUMBER = 3;
    private static final long OPERATION_ID = 5001L;

    private static final Map<String, Object> FAILED_STATS = Map.of(
            "retriesLeft", 0L,
            "reportStatus", "FAILED"
    );
    private static final Map<String, Object> OK_STATS = Map.of(
            "retriesLeft", 2L,
            "reportStatus", "DONE"
    );
    private static final Map<String, Object> OK_WITH_RETRY_STATS = Map.of(
            "retriesLeft", 1L,
            "reportStatus", "DONE"
    );

    @Autowired
    private MbiApiClient mbiApiClient;

    @BeforeEach
    void init() {
        Mockito.doReturn(getReportInfoDTO(ReportState.PROCESSING, HIDING_REPORT_ID, HIDING_REPORT_REQUEST))
                .when(mbiApiClient)
                .requestReportGeneration(HIDING_REPORT_REQUEST);

        Mockito.doReturn(getReportInfoDTO(ReportState.PROCESSING, MIGRATION_REPORT_ID, MIGRATION_REPORT_REQUEST))
                .when(mbiApiClient)
                .requestReportGeneration(MIGRATION_REPORT_REQUEST);
    }

    @AfterEach
    void checkMocks() {
        Mockito.verifyNoMoreInteractions(mbiApiClient);
    }

    @Test
    @DisplayName("В запросе есть скрытие и миграция. Упали на задаче скрытия. " +
            "Не должны делать миграцию. Отправляем в mbi ошибку")
    void hiding_errorInReport_stopOnHiding() throws InterruptedException {
        Map<String, Object> params = Map.of(
                "needHide", true,
                "needMigrate", true,
                "partnerId", PARTNER_ID,
                "feedId", FEED_ID,
                "timestamp", TIMESTAMP,
                "operationId", OPERATION_ID
        );

        mockMbiLock();
        mockMbiNotifierResponse(mbiApiClient);

        // Задача скрытия падает
        mockMbiHidingReport(ReportState.FAILED);

        ProcessInstance instance = invoke(params);

        // Было только скрытие
        checkHidingReport(RETRIES_NUMBER);

        // Захватили и отпустили лок
        checkLock(instance);

        // Сохранили фейл в mbi
        Map<String, Object> stats = Map.of(
                "hidingReport", FAILED_STATS
        );
        checkOperationStatus(mbiApiClient, instance, OperationStatus.ERROR, stats);

        // Не было инцидентов
        CamundaTestUtil.checkIncidents(processEngine, instance);
    }

    @Test
    @DisplayName("В запросе есть скрытие и миграция. Упали на задаче миграции. " +
            "Откатываем локи. Отправляем в mbi фейл")
    void migration_errorInReport_stopOnMigration() throws InterruptedException {
        Map<String, Object> params = Map.of(
                "needHide", true,
                "needMigrate", true,
                "partnerId", PARTNER_ID,
                "feedId", FEED_ID,
                "timestamp", TIMESTAMP,
                "operationId", OPERATION_ID
        );

        mockMbiLock();
        mockMbiNotifierResponse(mbiApiClient);

        // Задача скрытия отрабатывает успешно, миграции - падает
        mockMbiHidingReport(ReportState.DONE);
        mockMbiMigrationReport(ReportState.FAILED);

        ProcessInstance instance = invoke(params);

        // Было успешное скрытие
        checkHidingReport(1);

        // Была попытка миграции
        checkMigrationReport(RETRIES_NUMBER);

        // Захватили и отпустили лок
        checkLock(instance);

        // Сохранили фейл в mbi
        Map<String, Object> stats = Map.of(
                "hidingReport", OK_STATS,
                "migrationReport", FAILED_STATS
        );
        checkOperationStatus(mbiApiClient, instance, OperationStatus.ERROR, stats);

        // Не было инцидентов
        CamundaTestUtil.checkIncidents(processEngine, instance);
    }

    @Test
    @DisplayName("В запросе есть только скрытие. Выполняется успешно")
    void hiding_reportIsDone_success() throws InterruptedException {
        Map<String, Object> params = Map.of(
                "needHide", true,
                "needMigrate", false,
                "partnerId", PARTNER_ID,
                "feedId", FEED_ID,
                "timestamp", TIMESTAMP,
                "operationId", OPERATION_ID
        );

        mockMbiLock();
        mockMbiNotifierResponse(mbiApiClient);

        // Задача скрытия отрабатывают успешно
        mockMbiHidingReport(ReportState.DONE);

        ProcessInstance instance = invoke(params);

        // Было успешное скрытие
        checkHidingReport(1);

        // Захватили и отпустили лок
        checkLock(instance);

        // Сохранили успех в mbi
        Map<String, Object> stats = Map.of(
                "hidingReport", OK_STATS
        );
        checkOperationStatus(mbiApiClient, instance, OperationStatus.OK, stats);

        // Не было инцидентов
        CamundaTestUtil.checkIncidents(processEngine, instance);
    }

    @Test
    @DisplayName("В запросе есть только скрытие. Отчет залип. Перезапускаем")
    void hiding_stuck_restart() throws InterruptedException {
        Map<String, Object> params = Map.of(
                "needHide", true,
                "needMigrate", false,
                "partnerId", PARTNER_ID,
                "feedId", FEED_ID,
                "timestamp", TIMESTAMP,
                "operationId", OPERATION_ID
        );

        mockMbiLock();
        mockMbiNotifierResponse(mbiApiClient);

        // Задача скрытия залипла
        String stuckReportId = HIDING_REPORT_ID + "_stuck";
        Mockito.when(mbiApiClient.requestReportGeneration(HIDING_REPORT_REQUEST))
                .thenReturn(getReportInfoDTO(ReportState.PROCESSING, stuckReportId, HIDING_REPORT_REQUEST))
                .thenReturn(getReportInfoDTO(ReportState.PROCESSING, HIDING_REPORT_ID, HIDING_REPORT_REQUEST));

        Instant touchedAt = Instant.ofEpochMilli(System.currentTimeMillis() - 600001);
        Mockito.when(mbiApiClient.getReportInfo(stuckReportId))
                .thenReturn(getReportInfoDTO(ReportState.PROCESSING, stuckReportId, HIDING_REPORT_REQUEST, touchedAt));
        mockMbiHidingReport(ReportState.DONE);
        Mockito.when(mbiApiClient.cancelReport(stuckReportId))
                .thenReturn(GenericCallResponse.ok());

        ProcessInstance instance = invoke(params);

        // Был перезапуск
        Mockito.verify(mbiApiClient, Mockito.times(2))
                .requestReportGeneration(HIDING_REPORT_REQUEST);
        Mockito.verify(mbiApiClient)
                .getReportInfo(stuckReportId);
        Mockito.verify(mbiApiClient)
                .cancelReport(stuckReportId);
        Mockito.verify(mbiApiClient)
                .getReportInfo(HIDING_REPORT_ID);

        // Захватили и отпустили лок
        checkLock(instance);

        // Сохранили успех в mbi
        Map<String, Object> stats = Map.of(
                "hidingReport", OK_WITH_RETRY_STATS
        );
        checkOperationStatus(mbiApiClient, instance, OperationStatus.OK, stats);

        // Не было инцидентов
        CamundaTestUtil.checkIncidents(processEngine, instance);
    }

    @Test
    @DisplayName("В запросе есть только миграция. Выполняется успешно")
    void migration_reportIsDone_success() throws InterruptedException {
        Map<String, Object> params = Map.of(
                "needHide", false,
                "needMigrate", true,
                "partnerId", PARTNER_ID,
                "feedId", FEED_ID,
                "operationId", OPERATION_ID
        );

        mockMbiLock();
        mockMbiNotifierResponse(mbiApiClient);

        // Задача миграции отрабатывают успешно
        mockMbiMigrationReport(ReportState.DONE);

        ProcessInstance instance = invoke(params);

        // Была успешное миграция
        checkMigrationReport(1);

        // Захватили и отпустили лок
        checkLock(instance);

        // Сохранили успех в mbi
        Map<String, Object> stats = Map.of(
                "migrationReport", OK_STATS
        );
        checkOperationStatus(mbiApiClient, instance, OperationStatus.OK, stats);

        // Не было инцидентов
        CamundaTestUtil.checkIncidents(processEngine, instance);
    }

    @Test
    @DisplayName("В запросе есть только миграция. Залипла. Перезапускаем")
    void migration_stuck_restart() throws InterruptedException {
        Map<String, Object> params = Map.of(
                "needHide", false,
                "needMigrate", true,
                "partnerId", PARTNER_ID,
                "feedId", FEED_ID,
                "operationId", OPERATION_ID
        );

        mockMbiLock();
        mockMbiNotifierResponse(mbiApiClient);

        // Задача скрытия залипла
        String stuckReportId = MIGRATION_REPORT_ID + "_stuck";
        Mockito.when(mbiApiClient.requestReportGeneration(MIGRATION_REPORT_REQUEST))
                .thenReturn(getReportInfoDTO(ReportState.PROCESSING, stuckReportId, MIGRATION_REPORT_REQUEST))
                .thenReturn(getReportInfoDTO(ReportState.PROCESSING, MIGRATION_REPORT_ID, MIGRATION_REPORT_REQUEST));

        Instant touchedAt = Instant.ofEpochMilli(System.currentTimeMillis() - 600001);
        Mockito.when(mbiApiClient.getReportInfo(stuckReportId))
                .thenReturn(getReportInfoDTO(ReportState.PROCESSING,
                        stuckReportId, MIGRATION_REPORT_REQUEST, touchedAt));
        mockMbiMigrationReport(ReportState.DONE);
        Mockito.when(mbiApiClient.cancelReport(stuckReportId))
                .thenReturn(GenericCallResponse.ok());

        ProcessInstance instance = invoke(params);

        // Был перезапуск
        Mockito.verify(mbiApiClient, Mockito.times(2))
                .requestReportGeneration(MIGRATION_REPORT_REQUEST);
        Mockito.verify(mbiApiClient)
                .getReportInfo(stuckReportId);
        Mockito.verify(mbiApiClient)
                .cancelReport(stuckReportId);
        Mockito.verify(mbiApiClient)
                .getReportInfo(MIGRATION_REPORT_ID);

        // Захватили и отпустили лок
        checkLock(instance);

        // Сохранили успех в mbi
        Map<String, Object> stats = Map.of(
                "migrationReport", OK_WITH_RETRY_STATS
        );
        checkOperationStatus(mbiApiClient, instance, OperationStatus.OK, stats);

        // Не было инцидентов
        CamundaTestUtil.checkIncidents(processEngine, instance);
    }

    @Test
    @DisplayName("В запросе есть скрытие и миграция. Выполняется успешно")
    void hidingAndMigration_reportIsDone_success() throws InterruptedException {
        Map<String, Object> params = Map.of(
                "needHide", true,
                "needMigrate", true,
                "partnerId", PARTNER_ID,
                "feedId", FEED_ID,
                "timestamp", TIMESTAMP,
                "operationId", OPERATION_ID
        );

        mockMbiLock();
        mockMbiNotifierResponse(mbiApiClient);

        // Задачи отрабатывают успешно
        mockMbiHidingReport(ReportState.DONE);
        mockMbiMigrationReport(ReportState.DONE);

        ProcessInstance instance = invoke(params);

        // Было успешное скрытие
        checkHidingReport(1);

        // Была успешное миграция
        checkMigrationReport(1);

        // Захватили и отпустили лок
        checkLock(instance);

        // Сохранили успех в mbi
        Map<String, Object> stats = Map.of(
                "hidingReport", OK_STATS,
                "migrationReport", OK_STATS
        );
        checkOperationStatus(mbiApiClient, instance, OperationStatus.OK, stats);

        // Не было инцидентов
        CamundaTestUtil.checkIncidents(processEngine, instance);
    }

    @Test
    @DisplayName("В запросе есть скрытие и миграция. Во время каждого отчета случилась ошибка. " +
            "Случились ретраи. Успешное завершение")
    void hidingAndMigration_reportWithReties_success() throws InterruptedException {
        Map<String, Object> params = Map.of(
                "needHide", true,
                "needMigrate", true,
                "partnerId", PARTNER_ID,
                "feedId", FEED_ID,
                "timestamp", TIMESTAMP,
                "operationId", OPERATION_ID
        );

        mockMbiLock();
        mockMbiNotifierResponse(mbiApiClient);

        // Задачи фейлятся во время первой попытки, но отрабатывают корректно во время второй
        mockMbiHidingReport(ReportState.FAILED, ReportState.DONE);
        mockMbiMigrationReport(ReportState.FAILED, ReportState.DONE);

        ProcessInstance instance = invoke(params);

        // Было успешное скрытие
        checkHidingReport(2);

        // Была успешное миграция
        checkMigrationReport(2);

        // Захватили и отпустили лок
        checkLock(instance);

        // Сохранили успех в mbi
        Map<String, Object> stats = Map.of(
                "hidingReport", OK_WITH_RETRY_STATS,
                "migrationReport", OK_WITH_RETRY_STATS
        );
        checkOperationStatus(mbiApiClient, instance, OperationStatus.OK, stats);

        // Не было инцидентов
        CamundaTestUtil.checkIncidents(processEngine, instance);
    }

    /**
     * Замокать статусы задачи по скрытым.
     */
    private void mockMbiHidingReport(ReportState... states) {
        mockMbiReport(HIDING_REPORT_REQUEST, HIDING_REPORT_ID, states);
    }

    /**
     * Замокать статусы задачи по миграции.
     */
    private void mockMbiMigrationReport(ReportState... states) {
        mockMbiReport(MIGRATION_REPORT_REQUEST, MIGRATION_REPORT_ID, states);
    }

    private void mockMbiReport(ReportRequest<ReportsType> request, String reportId, ReportState... states) {
        OngoingStubbing<ReportInfoDTO> mockedClient = Mockito.when(mbiApiClient.getReportInfo(reportId));
        for (ReportState state : states) {
            mockedClient = mockedClient.thenReturn(getReportInfoDTO(state, reportId, request));
        }
    }

    /**
     * Проверить вызовы скрытий.
     */
    private void checkHidingReport(int requestsNumber) {
        checkReport(HIDING_REPORT_REQUEST, HIDING_REPORT_ID, requestsNumber);
    }

    /**
     * Проверить вызовы миграции.
     */
    private void checkMigrationReport(int requestsNumber) {
        checkReport(MIGRATION_REPORT_REQUEST, MIGRATION_REPORT_ID, requestsNumber);
    }

    private void checkReport(ReportRequest<ReportsType> request, String reportId, int requestsNumber) {
        Mockito.verify(mbiApiClient, Mockito.times(requestsNumber))
                .requestReportGeneration(request);
        Mockito.verify(mbiApiClient, Mockito.times(requestsNumber))
                .getReportInfo(reportId);
    }

    /**
     * Проверить, что захватили и отпустили лок для правильного процесса.
     */
    private void checkLock(ProcessInstance instance) {
        BpmnLockRequest request = getLockRequest(instance);

        // Захватили лок
        Mockito.verify(mbiApiClient)
                .lockBpmn(request);

        // Отпустили лок
        Mockito.verify(mbiApiClient)
                .unlockBpmn(request);
    }

    private BpmnLockRequest getLockRequest(ProcessInstance instance) {
        BpmnLockRequest request = new BpmnLockRequest();
        request.setLockType(BpmnLockType.PARTNER_FEED_OFFER_MIGRATION);
        request.setEntityIds(Set.of(PARTNER_ID));
        request.setProcessId(instance.getProcessInstanceId());
        return request;
    }

    /**
     * Замокать работу с локами через mbi-api.
     */
    private void mockMbiLock() {
        Mockito.when(mbiApiClient.lockBpmn(any()))
                .thenReturn(GenericCallResponse.ok());
        Mockito.when(mbiApiClient.unlockBpmn(any()))
                .thenReturn(GenericCallResponse.ok());
    }

    private ProcessInstance invoke(Map<String, Object> variables) throws InterruptedException {
        return CamundaTestUtil.invoke(processEngine, ProcessType.PARTNER_FEED_OFFER_MIGRATION.getId(), variables);
    }
}
