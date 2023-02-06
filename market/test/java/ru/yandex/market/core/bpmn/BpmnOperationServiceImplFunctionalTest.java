package ru.yandex.market.core.bpmn;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.operations.model.params.BusinessMigrationParams;
import ru.yandex.market.core.operations.model.params.PartnerFeedOfferMigrationParams;
import ru.yandex.market.core.util.DateTimes;
import ru.yandex.market.mbi.bpmn.client.MbiBpmnClient;
import ru.yandex.market.mbi.bpmn.client.MbiBpmnClientException;
import ru.yandex.market.mbi.bpmn.client.model.Error;
import ru.yandex.market.mbi.bpmn.client.model.ProcessInstanceRequest;
import ru.yandex.market.mbi.bpmn.client.model.ProcessStartInstance;
import ru.yandex.market.mbi.bpmn.client.model.ProcessStartResponse;
import ru.yandex.market.mbi.bpmn.client.model.ProcessStatus;
import ru.yandex.market.mbi.bpmn.client.model.ProcessType;
import ru.yandex.market.mbi.datacamp.model.search.SearchBusinessOffersResult;
import ru.yandex.market.mbi.datacamp.saas.SaasService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

/**
 * Тесты для {@link BpmnOperationServiceImpl}.
 */
@DbUnitDataSet(before = "BpmnOperationServiceTest.before.csv")
class BpmnOperationServiceImplFunctionalTest extends FunctionalTest {
    private static final long SERVICE_ID = 777L;
    private static final long SRC_BUSINESS_ID = 666L;
    private static final long DST_BUSINESS_ID = 667L;

    private static final BusinessMigrationParams PARAMS = BusinessMigrationParams.builder()
            .setDstBusinessId(DST_BUSINESS_ID)
            .setSrcBusinessId(SRC_BUSINESS_ID)
            .setServiceId(SERVICE_ID)
            .build();

    @Autowired
    private BpmnOperationServiceImpl bpmnOperationDefaultService;

    @Autowired
    private MbiBpmnClient mbiBpmnClient;
    @Autowired
    private SaasService saasService;

    @BeforeEach
    void init() {
        doReturn(SearchBusinessOffersResult.builder()
                .setTotalCount(1)
                .build())
                .when(saasService).searchAndConvertBusinessOffers(any());
    }


    private void initAndRun(Function<ProcessInstanceRequest, Object> func) {
        ProcessInstanceRequest request = new ProcessInstanceRequest();
        request.setProcessType(ProcessType.MIGRATION_FULL);
        Map<String, Object> paramsMap = new HashMap<>(Map.of(
                "serviceId", String.valueOf(SERVICE_ID),
                "srcBusinessId", String.valueOf(SRC_BUSINESS_ID),
                "dstBusinessId", String.valueOf(DST_BUSINESS_ID)));
        request.setParams(paramsMap);
        func.apply(request);
    }

    @Test
    @DisplayName("Успешное начало операции миграции офферов между фидами партнера")
    @DbUnitDataSet(after = "BpmnOperationServiceTest.feedOfferMigration.after.success.csv")
    public void feedOfferMigration_goodRequest_success() {
        ProcessStartInstance instance = new ProcessStartInstance();
        instance.setStatus(ProcessStatus.ACTIVE);
        instance.setProcessInstanceId("camunda_process_id");

        ProcessStartResponse response = new ProcessStartResponse();
        response.addRecordsItem(instance);
        when(mbiBpmnClient.postProcess(any())).thenReturn(response);

        PartnerFeedOfferMigrationParams params = PartnerFeedOfferMigrationParams.builder()
                .setPartnerId(777L)
                .setFeedId(100L)
                .setNeedMigrate(true)
                .setNeedHide(true)
                .setTimestamp(DateTimes.toInstant(2020, 1, 1))
                .build();

        Assertions.assertTrue(bpmnOperationDefaultService.startPartnerFeedOffersMigration(params));
    }

    @Test
    @DisplayName("Успешный рестарт операции")
    @DbUnitDataSet(
            before = "BpmnOperationServiceTest.successRestart.before.success.csv",
            after = "BpmnOperationServiceTest.successRestart.after.success.csv"
    )
    public void successRestart() {
        ProcessStartInstance instance = new ProcessStartInstance();
        instance.setStatus(ProcessStatus.ACTIVE);
        instance.setProcessInstanceId("camunda_process_id_2");

        ProcessStartResponse response = new ProcessStartResponse();
        response.addRecordsItem(instance);
        when(mbiBpmnClient.postProcess(any())).thenReturn(response);

        Assertions.assertTrue(bpmnOperationDefaultService.restartOperation(1001L));
    }

    @Test
    @DisplayName("Не удалось перезапустить операцию. Операции нет в базе")
    @DbUnitDataSet(
            before = "BpmnOperationServiceTest.successRestart.before.success.csv",
            after = "BpmnOperationServiceTest.successRestart.before.success.csv"
    )
    public void restartForInvalidId() {
        Assertions.assertFalse(bpmnOperationDefaultService.restartOperation(2L));
        Mockito.verifyNoMoreInteractions(mbiBpmnClient);
    }

    @Test
    @DbUnitDataSet(before = "BpmnOperationServiceTest.testRunOperationSuccess.before.success.csv",
            after = "BpmnOperationServiceTest.after.success.csv")
    public void testRunOperationSuccess() {
        initAndRun((request) -> {
            var response = new ProcessStartResponse();
            var processInstance = new ProcessStartInstance();
            processInstance.setStatus(ProcessStatus.ACTIVE);
            processInstance.processInstanceId("EXT");
            response.setRecords(List.of(processInstance));
            ArgumentCaptor<ProcessInstanceRequest> captor = ArgumentCaptor.forClass(ProcessInstanceRequest.class);
            when(mbiBpmnClient.postProcess(captor.capture())).thenReturn(response);
            Assertions.assertTrue(bpmnOperationDefaultService.startBusinessMigration(PARAMS));
            Assertions.assertEquals(ProcessType.MIGRATION_FULL, captor.getValue().getProcessType());
            return null;
        });
    }

    @Test
    @DbUnitDataSet(after = "BpmnOperationServiceOperationServiceTest.after.fail.csv")
    public void testRunOperationFailOnStatus() {
        initAndRun((request) -> {
            var response = new ProcessStartResponse();
            var processInstance = new ProcessStartInstance();
            processInstance.setStatus(ProcessStatus.INTERNALLY_TERMINATED);
            processInstance.processInstanceId("EXT");
            response.setRecords(List.of(processInstance));
            doReturn(response)
                    .when(mbiBpmnClient).postProcess(any());
            Assertions.assertFalse(bpmnOperationDefaultService.startBusinessMigration(PARAMS));
            return null;
        });
    }

    @Test
    @DbUnitDataSet(after = "BpmnOperationServiceOperationServiceTest.after.fail.csv")
    public void testRunOperationFailOnEmptyResponse() {
        initAndRun((request) -> {
            var response = new ProcessStartResponse();
            var processInstance = new ProcessStartInstance();
            processInstance.setStatus(ProcessStatus.INTERNALLY_TERMINATED);
            processInstance.processInstanceId("EXT");
            response.setRecords(List.of(processInstance));
            when(mbiBpmnClient.postProcess(any())).thenReturn(response);
            Assertions.assertFalse(bpmnOperationDefaultService.startBusinessMigration(PARAMS));
            return null;
        });
    }

    @Test
    @DbUnitDataSet(after = "BpmnOperationServiceOperationServiceTest.after.fail.csv")
    public void testRunOperationFailOnBadRequest() {
        initAndRun((request) -> {
            String reason = "There are mappings in MBO for shopId: 10746219!";
            MbiBpmnClientException exception = new MbiBpmnClientException(
                    "Bad Request",
                    400,
                    List.of(new Error().message(reason).code("400"))
            );
            doThrow(exception)
                    .when(mbiBpmnClient).postProcess(any());
            try {
                bpmnOperationDefaultService.startBusinessMigration(PARAMS);
                Assertions.fail("Exception wasn't thrown!");
            } catch (IllegalStateException ex) {
                Assertions.assertEquals(String.format("reason=\"%s\"", reason), ex.getMessage());
            }
            return null;
        });
    }
}
