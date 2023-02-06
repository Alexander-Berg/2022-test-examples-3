package ru.yandex.market.mbi.bpmn.mvc;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.camunda.bpm.engine.BadUserRequestException;
import org.camunda.bpm.engine.ProcessEngine;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbi.api.client.entity.business.CanMigrateVerdictDTO;
import ru.yandex.market.mbi.bpmn.FunctionalTest;
import ru.yandex.market.mbi.bpmn.client.PartnerStatusServiceClient;
import ru.yandex.market.mbi.bpmn.model.ApiError;
import ru.yandex.market.mbi.bpmn.model.Error;
import ru.yandex.market.mbi.bpmn.model.ProcessInstance;
import ru.yandex.market.mbi.bpmn.model.ProcessInstanceRequest;
import ru.yandex.market.mbi.bpmn.model.ProcessInstancesResponse;
import ru.yandex.market.mbi.bpmn.model.ProcessSearchRequest;
import ru.yandex.market.mbi.bpmn.model.ProcessStartInstance;
import ru.yandex.market.mbi.bpmn.model.ProcessStartResponse;
import ru.yandex.market.mbi.bpmn.model.ProcessState;
import ru.yandex.market.mbi.bpmn.model.ProcessStatus;
import ru.yandex.market.mbi.bpmn.model.ProcessType;
import ru.yandex.market.mbi.bpmn.model.ProcessesStatesResponse;
import ru.yandex.market.mbi.bpmn.process.migration.MigrationProcessTest;
import ru.yandex.market.mbi.bpmn.process.replication.data.DbsReplicationForTestingData;
import ru.yandex.market.mbi.bpmn.process.replication.data.FbyReplicationForTestingData;
import ru.yandex.market.mbi.datacamp.saas.SaasService;
import ru.yandex.market.mbi.open.api.client.model.PartnerPlacementType;
import ru.yandex.market.mbi.open.api.exception.MbiOpenApiClientResponseException;

import static org.assertj.core.api.Assertions.as;
import static org.assertj.core.api.InstanceOfAssertFactories.MAP;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Mockito.when;

/**
 * Функциональные тесты на {@link ProcessController}
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ProcessControllerTest extends FunctionalTest {
    private static final String SRC_BUSINESS_ID = "1";
    private static final String DST_BUSINESS_ID = "2";
    private static final String SERVICE_ID = "777";

    @Autowired
    private ProcessEngine processEngine;

    @Autowired
    private SaasService saasService;

    @Autowired
    private PartnerStatusServiceClient partnerStatusServiceClient;

    private static String processInstanceMigrationId;

    @Test
    @Order(1)
    @DisplayName("Запустить процесс миграции, проверить ответ.")
    void processStart_ok() throws IOException {
        willReturn(CanMigrateVerdictDTO.yes()).given(mbiApiClient)
                .canMigrate(anyLong(), anyLong(), anyLong());

        var startProcessResponse = createProcess(ProcessType.MIGRATION_FULL,
                "123", Map.of("srcBusinessId", SRC_BUSINESS_ID,
                        "dstBusinessId", DST_BUSINESS_ID,
                        "serviceId", SERVICE_ID));

        processInstanceMigrationId = processEngine.getHistoryService()
                .createHistoricProcessInstanceQuery()
                .processDefinitionKey(
                        ru.yandex.market.mbi.bpmn.model.enums.ProcessType.MIGRATION_FULL.getId()
                )
                .singleResult()
                .getId();

        ProcessStartInstance expected = new ProcessStartInstance();
        expected.setProcessInstanceId(processInstanceMigrationId);
        expected.setBusinessKey("123");
        expected.setStatus(ProcessStatus.ACTIVE);
        expected.setStarted(true);
        assertEquals(expected, startProcessResponse);
    }

    @Test
    @DisplayName("Паралельный запуск двух процессов для заданного businessId")
    void twoParallelProcess() throws IOException {
        // мокаем все вызовы
        DbsReplicationForTestingData data = new DbsReplicationForTestingData(mbiOpenApiClient, mbiApiClient,
                dataCampShopClient, partnerStatusServiceClient, 998);
        data.mockAll();
        // запускаем процесс
        ProcessStartInstance processInstance1 = createProcess(ProcessType.DBS_TO_DBS_REPLICATION,
                String.valueOf(data.getBusinessId()), data.params());
        ProcessStartInstance processInstance2 = createProcess(ProcessType.DBS_TO_DBS_REPLICATION,
                String.valueOf(data.getBusinessId()), data.params());
        org.assertj.core.api.Assertions.assertThat(processInstance1)
                .returns(processInstance1.getStatus(), ProcessInstance::getStatus)
                .returns(processInstance1.getBusinessKey(), ProcessInstance::getBusinessKey)
                .returns(true, ProcessStartInstance::getStarted);
        org.assertj.core.api.Assertions.assertThat(processInstance2)
                .returns(processInstance2.getStatus(), ProcessInstance::getStatus)
                .returns(processInstance2.getBusinessKey(), ProcessInstance::getBusinessKey)
                .returns(true, ProcessStartInstance::getStarted);

        // проверяем статусы
        String processInstanceId1 = processInstance1.getProcessInstanceId();
        String processInstanceId2 = processInstance2.getProcessInstanceId();
        Assertions.assertNotEquals(processInstanceId1, processInstanceId2);

        // ждем завершения процесса
        waitEndProcess(processInstanceId1);
        waitEndProcess(processInstanceId2);
    }


    @Test
    @Order(2)
    @DisplayName("Проверить, что есть запущенный процесс миграции (по айдишнику) в статусе ACTIVE.")
    void processStatus_ok() {
        ProcessInstancesResponse expected = new ProcessInstancesResponse();
        ProcessInstance recordItem = new ProcessInstance();
        recordItem.setProcessInstanceId(processInstanceMigrationId);
        recordItem.setBusinessKey("123");
        recordItem.setStatus(ProcessStatus.ACTIVE);
        expected.addRecordsItem(recordItem);

        HttpGet getProcessStatus = prepareGetRequest(processInstanceMigrationId);
        ProcessInstancesResponse actualStatusResponse = sendHttpRequest(
                getProcessStatus,
                ProcessInstancesResponse.class
        );
        assertEquals(expected, actualStatusResponse);
    }

    @Test
    @DisplayName("Процесс не найдет.")
    void processStatus_notFound() throws IOException {
        HttpGet getProcessStatus = prepareGetRequest(UUID.randomUUID().toString());
        CloseableHttpResponse response = HttpClientBuilder.create().build().execute(getProcessStatus);
        assertEquals(HttpStatus.SC_NOT_FOUND, response.getStatusLine().getStatusCode());
    }

    @Test
    @DisplayName("Если миграция не возможна, возвращаем 400 (BAD_REQUEST) с причиной.")
    void migrationNotPossible_badRequest() throws IOException {
        willReturn(CanMigrateVerdictDTO.no(MigrationProcessTest.REASON))
                .given(mbiApiClient).canMigrate(anyLong(), anyLong(), anyLong());
        ProcessInstanceRequest processInstanceRequest = new ProcessInstanceRequest();
        processInstanceRequest.setProcessType(ProcessType.MIGRATION_FULL);
        processInstanceRequest.setBusinessKey("1234");
        processInstanceRequest.setParams(
                Map.of("srcBusinessId", SRC_BUSINESS_ID,
                        "dstBusinessId", DST_BUSINESS_ID,
                        "serviceId", SERVICE_ID));
        HttpPost httpRequest = prepareStartProcess(processInstanceRequest);
        CloseableHttpResponse response = HttpClientBuilder.create().build().execute(httpRequest);
        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusLine().getStatusCode());
        Error error = objectMapper.readValue(response.getEntity().getContent(), ApiError.class).getErrors().get(0);
        assertEquals("400", error.getCode());
        assertEquals(
                String.format("Migration possibility check: canMigrate=false, reason=%s.", MigrationProcessTest.REASON),
                error.getMessage());
    }

    @Timeout(value = 30)
    @Test
    @Order(3)
    @DisplayName("Запустить процесс создания склада для ДБС.")
    void processDsbsStart_ok() throws IOException {
        // мокаем все вызовы
        DbsReplicationForTestingData data = new DbsReplicationForTestingData(mbiOpenApiClient, mbiApiClient,
                dataCampShopClient, partnerStatusServiceClient);
        data.mockAll();

        // запускаем процесс
        ProcessInstance processInstance = createProcess(ProcessType.DBS_TO_DBS_REPLICATION,
                String.valueOf(data.getBusinessId()), data.params());
        // проверям статусы
        ProcessStartInstance expected = new ProcessStartInstance();
        expected.setProcessInstanceId(processInstance.getProcessInstanceId());
        expected.setStatus(ProcessStatus.ACTIVE);
        expected.businessKey(String.valueOf(data.getBusinessId()));
        expected.setStarted(true);
        assertEquals(expected, processInstance);

        // смотрим что статус процесса правильный
        ProcessInstance actualStatusResponse = getProcessStatus(processInstance.getProcessInstanceId());
        assertEquals(new ProcessInstance()
                        .processInstanceId(processInstance.getProcessInstanceId())
                        .status(ProcessStatus.ACTIVE)
                        .businessKey(String.valueOf(data.getBusinessId())),
                actualStatusResponse);

        // ждем завершения процесса
        waitEndProcess(processInstance.getProcessInstanceId());

        // смотрим по истории, что процесс успешно завершился
        var historicProcessInstance = processEngine.getHistoryService()
                .createHistoricProcessInstanceQuery()
                .processDefinitionKey(
                        ru.yandex.market.mbi.bpmn.model.enums.ProcessType.DBS_TO_DBS_REPLICATION.getId()
                )
                .processInstanceId(processInstance.getProcessInstanceId())
                .singleResult();
        assertEquals(historicProcessInstance.getState(), ProcessStatus.COMPLETED.toString());
        data.verifyAll();

        // Проверяем состояние процесса
        ProcessState processState = getProcessState(processInstance.getProcessInstanceId());
        org.assertj.core.api.Assertions.assertThat(processState)
                .returns(processInstance.getProcessInstanceId(), ProcessState::getProcessInstanceId)
                .returns(String.valueOf(data.getBusinessId()), ProcessState::getBusinessKey)
                .returns(ProcessStatus.COMPLETED, ProcessState::getStatus)
                .extracting(ProcessState::getParams, as(MAP))
                .containsAllEntriesOf(Map.of("campaignId", 1111, "partnerId", 111, "warehouseId", 5));
    }

    @Test
    @DisplayName("Получить состояние процесса. Процесс не найдет.")
    void processState_notFound() throws IOException {
        HttpResponse response = HttpClientBuilder.create().build().execute(prepareStateRequest("---"));
        assertEquals(HttpStatus.SC_NOT_FOUND, response.getStatusLine().getStatusCode());
    }

    @Timeout(value = 30)
    @Test
    @DisplayName("Запустить процесс создания склада для ДБС два раза. Проверить идемпотентность запуска.")
    void processDsbsStart_two() throws IOException {
        // мокаем все вызовы
        DbsReplicationForTestingData data = new DbsReplicationForTestingData(mbiOpenApiClient, mbiApiClient,
                dataCampShopClient, partnerStatusServiceClient);
        data.mockAll();

        // запускаем процесс
        ProcessStartInstance processInstance1 = createProcess(ProcessType.DBS_TO_DBS_REPLICATION,
                String.valueOf(data.getBusinessId()), data.params());
        ProcessStartInstance processInstance2 = createProcess(ProcessType.DBS_TO_DBS_REPLICATION,
                String.valueOf(data.getBusinessId()), data.params());
        org.assertj.core.api.Assertions.assertThat(processInstance1)
                .returns(processInstance2.getProcessInstanceId(), ProcessInstance::getProcessInstanceId)
                .returns(processInstance2.getStatus(), ProcessInstance::getStatus)
                .returns(processInstance2.getBusinessKey(), ProcessInstance::getBusinessKey)
                .returns(true, ProcessStartInstance::getStarted);
        org.assertj.core.api.Assertions.assertThat(processInstance2)
                .returns(false, ProcessStartInstance::getStarted);

        // проверяем статусы
        String processInstanceId = processInstance1.getProcessInstanceId();
        // ждем завершения процесса
        waitEndProcess(processInstanceId);

        // смотрим по истории, что процесс успешно завершился
        var processInstance = processEngine.getHistoryService()
                .createHistoricProcessInstanceQuery()
                .processDefinitionKey(
                        ru.yandex.market.mbi.bpmn.model.enums.ProcessType.DBS_TO_DBS_REPLICATION.getId()
                )
                .processInstanceId(processInstanceId)
                .singleResult();
        assertEquals(processInstance.getState(), ProcessStatus.COMPLETED.toString());
        data.verifyAll();
    }

    @Test
    @DisplayName("Не валидные входные параметры запроса для ДБС. Not null")
    void processDbsNotNull() throws IOException {
        String businessKey = UUID.randomUUID().toString();
        // запускаем процесс
        ProcessInstanceRequest processInstanceRequest = new ProcessInstanceRequest()
                .processType(ProcessType.DBS_TO_DBS_REPLICATION)
                .businessKey(String.valueOf(businessKey));
        HttpResponse response = HttpClientBuilder.create().build().execute(prepareStartProcess(processInstanceRequest));
        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusLine().getStatusCode());
        Error error = objectMapper.readValue(response.getEntity().getContent(), ApiError.class).getErrors().get(0);
        assertThat(error.getMessage(), containsString("warehouseName: may not be null"));
        assertThat(error.getMessage(), containsString("partnerDonorId: may not be null"));
        assertThat(error.getMessage(), containsString("regionId: may not be null"));
        assertThat(error.getMessage(), containsString("uid: may not be null"));
        assertThat(error.getMessage(), containsString("partnerWarehouseId: may not be null"));
    }

    @Test
    @DisplayName("Не валидные входные параметры запроса для FBY. Not null")
    void processFbyNotNull() throws IOException {
        String businessKey = UUID.randomUUID().toString();
        // запускаем процесс
        ProcessInstanceRequest processInstanceRequest = new ProcessInstanceRequest()
                .processType(ProcessType.FBS_TO_FBY_REPLICATION)
                .businessKey(String.valueOf(businessKey));
        HttpResponse response = HttpClientBuilder.create().build().execute(prepareStartProcess(processInstanceRequest));
        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusLine().getStatusCode());
        Error error = objectMapper.readValue(response.getEntity().getContent(), ApiError.class).getErrors().get(0);
        assertThat(error.getMessage(), containsString("partnerDonorId: may not be null"));
        assertThat(error.getMessage(), containsString("uid: may not be null"));
    }

    @Test
    @DisplayName("Не валидные входные параметры запроса для регистрации DBS. Mbi апи ответил 400")
    void processDbsMbiApi400() throws IOException {
        String businessKey = UUID.randomUUID().toString();
        // запускаем процесс
        ProcessInstanceRequest processInstanceRequest = new ProcessInstanceRequest()
                .processType(ProcessType.DBS_TO_DBS_REPLICATION)
                .businessKey(String.valueOf(businessKey))
                .params(Map.of("uid", 1,
                        "partnerDonorId", 1,
                        "regionId", 1,
                        "warehouseName", "warehouseName",
                        "partnerWarehouseId", "1",
                        "businessId", 1));
        when(mbiOpenApiClient.replicateDbsPartner(anyLong(), any()))
                .thenThrow(new MbiOpenApiClientResponseException("bad request", 400,
                        new ru.yandex.market.mbi.open.api.client.model.ApiError()
                                .code(1)
                                .message("very bad user")
                                .messageCode(ru.yandex.market.mbi.open.api.client.model.ApiError.MessageCodeEnum
                                        .APP_NOT_COMPLETED)));

        HttpResponse response = HttpClientBuilder.create().build().execute(prepareStartProcess(processInstanceRequest));
        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusLine().getStatusCode());
        Error error = objectMapper.readValue(response.getEntity().getContent(), ApiError.class).getErrors().get(0);
        assertThat(error.getMessage(), is("very bad user"));
        assertThat(error.getCode(), is("APP_NOT_COMPLETED"));
    }

    @Test
    @DisplayName("Не валидные входные параметры запроса для регистрации FBY. Mbi апи ответил 400")
    void processFbyMbiApi400() throws IOException {
        String businessKey = UUID.randomUUID().toString();
        // запускаем процесс
        ProcessInstanceRequest processInstanceRequest = new ProcessInstanceRequest()
                .processType(ProcessType.FBS_TO_FBY_REPLICATION)
                .businessKey(String.valueOf(businessKey))
                .params(Map.of("uid", 1,
                        "partnerDonorId", 1,
                        "acceptorPlacementType", PartnerPlacementType.FBY));
        when(mbiOpenApiClient.replicatePartner(anyLong(), any()))
                .thenThrow(new MbiOpenApiClientResponseException("bad request", 400,
                        new ru.yandex.market.mbi.open.api.client.model.ApiError().code(1).message("very bad user")));

        HttpResponse response = HttpClientBuilder.create().build().execute(prepareStartProcess(processInstanceRequest));
        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusLine().getStatusCode());
        Error error = objectMapper.readValue(response.getEntity().getContent(), ApiError.class).getErrors().get(0);
        assertThat(error.getMessage(), containsString("very bad user"));
    }

    @Test
    @DisplayName("Поиск процессов.")
    void searchByBusinessKey() throws IOException {
        final long[] businessIds = {200L, 300L, 400L, 500L, 600L};
        final long deletedBusinessId = 200L;
        final long searchBusinessId = 300L;
        final long donorPartnerId = 10001L;

        DbsReplicationForTestingData data = new DbsReplicationForTestingData(mbiOpenApiClient, mbiApiClient,
                dataCampShopClient, partnerStatusServiceClient);
        data.mockWaitBalance();
        data.setPartnerDonorId(donorPartnerId);

        // запускаем процессы
        final Map<Long, ProcessInstance> instancesByBusiness = new HashMap<>();
        final Map<String, ProcessInstance> instancesById = new HashMap<>();
        for (long businessId : businessIds) {
            data.setBusinessId(businessId);
            var pr = createProcess(ProcessType.DBS_TO_DBS_REPLICATION,
                    String.valueOf(businessId), data.params());
            instancesByBusiness.put(businessId, pr);
            instancesById.put(pr.getProcessInstanceId(), pr);
        }

        // процесс миграции
        willReturn(CanMigrateVerdictDTO.yes()).given(mbiApiClient)
                .canMigrate(anyLong(), anyLong(), anyLong());
        var migrationFullStartProcessResponse = createProcess(ProcessType.MIGRATION_FULL,
                "1234", Map.of("srcBusinessId", SRC_BUSINESS_ID,
                        "dstBusinessId", DST_BUSINESS_ID,
                        "serviceId", SERVICE_ID));

        // останавливаем 1 процесс
        ProcessInstance deleted = instancesByBusiness.remove(deletedBusinessId);
        deleted.setStatus(ProcessStatus.INTERNALLY_TERMINATED);
        instancesById.remove(deleted.getProcessInstanceId());
        deleteProcessInstance(deleted.getProcessInstanceId());

        // поиск активных процессов
        List<ProcessInstance> result = search(new ProcessSearchRequest()
                .processTypes(List.of(ProcessType.DBS_TO_DBS_REPLICATION))
                .status(ProcessStatus.ACTIVE));
        result.stream().filter(r -> !instancesById.containsKey(r.getProcessInstanceId()))
                .forEach(Assertions::assertNull);
        assertEquals(instancesByBusiness.size(), result.size());

        // поиск активных процессов с несколькими типами
        result = search(new ProcessSearchRequest()
                .processTypes(List.of(ProcessType.DBS_TO_DBS_REPLICATION, ProcessType.MIGRATION_FULL))
                .status(ProcessStatus.ACTIVE));
        // 4 dbs репликации и 1 полная миграция
        assertEquals(5, result.size());
        Set<String> activeProcessId = instancesByBusiness.values().stream()
                .map(ProcessInstance::getProcessInstanceId)
                .collect(Collectors.toSet());
        activeProcessId.add(migrationFullStartProcessResponse.getProcessInstanceId());
        assertEquals(activeProcessId,
                result.stream()
                        .map(ProcessInstance::getProcessInstanceId)
                        .collect(Collectors.toSet())
        );

        // поиск убитых процессов
        result = search(new ProcessSearchRequest()
                .processTypes(List.of(ProcessType.DBS_TO_DBS_REPLICATION))
                .status(ProcessStatus.INTERNALLY_TERMINATED));
        assertTrue(result.stream().anyMatch(p -> p.getProcessInstanceId().equals(deleted.getProcessInstanceId())));
        assertTrue(result.stream().noneMatch(p -> instancesById.containsKey(p.getProcessInstanceId())));

        // поиск процессов по бизнесу
        result = search(new ProcessSearchRequest()
                .status(ProcessStatus.ACTIVE)
                .businessKey(String.valueOf(searchBusinessId)));

        assertEquals(1, result.size());
        assertProcessInstance(instancesByBusiness.get(searchBusinessId), result.get(0));

        // поиск процессов по параметрам по businessId
        result = search(new ProcessSearchRequest()
                .processTypes(List.of(ProcessType.DBS_TO_DBS_REPLICATION))
                .params(Map.of("businessId", deletedBusinessId))
        );
        assertEquals(1, result.size());
        assertProcessInstance(deleted, result.get(0));

        // поиск по параметрам активных процессов по businessId
        result = search(new ProcessSearchRequest()
                .status(ProcessStatus.ACTIVE)
                .params(Map.of("businessId", searchBusinessId))
        );
        assertEquals(1, result.size());
        assertProcessInstance(instancesByBusiness.get(searchBusinessId), result.get(0));

        // поиск по partnerDonorId
        result = search(new ProcessSearchRequest().params(Map.of("partnerDonorId", donorPartnerId)));
        assertEquals(businessIds.length, result.size());

        instancesByBusiness.forEach((b, p) -> deleteProcessInstance(p.getProcessInstanceId()));
        deleteProcessInstance(migrationFullStartProcessResponse.getProcessInstanceId());
    }

    @Test
    void searchProcessesStates() throws Exception {
        final long[] businessIds = {123, 321};

        var data = new FbyReplicationForTestingData(
                mbiOpenApiClient,
                mbiApiClient,
                dataCampShopClient,
                saasService
        );
        data.mockAll();

        // запускаем процессы
        for (long businessId : businessIds) {
            data.setBusinessId(businessId);
            createProcess(
                    ProcessType.FBS_TO_FBY_REPLICATION,
                    String.valueOf(businessId),
                    data.params()
            );
        }

        List<ProcessState> states = searchStates(
                new ProcessSearchRequest().processTypes(List.of(ProcessType.FBS_TO_FBY_REPLICATION))
        );
        assertEquals(2, states.size());
        assertTrue(states.get(1).getStartTime().compareTo(states.get(0).getStartTime()) > 0);
    }

    private void deleteProcessInstance(String processInstanceId) {
        processEngine.getRuntimeService().deleteProcessInstance(processInstanceId, null);
    }

    private ProcessStartInstance createProcess(ProcessType processType,
                                               String businessKey,
                                               Map<String, Object> params)
            throws IOException {
        ProcessInstanceRequest processInstanceRequest = new ProcessInstanceRequest()
                .processType(processType)
                .businessKey(businessKey)
                .params(params);
        HttpResponse httpResponse =
                HttpClientBuilder.create().build().execute(prepareStartProcess(processInstanceRequest));
        ProcessStartResponse response = objectMapper.readValue(httpResponse.getEntity().getContent(),
                ProcessStartResponse.class);
        assertNotNull(response.getRecords());
        assertEquals(response.getRecords().size(), 1);
        return response.getRecords().get(0);
    }

    private ProcessInstance getProcessStatus(String processInstanceId) {
        ProcessInstancesResponse response = sendHttpRequest(
                prepareGetRequest(processInstanceId),
                ProcessInstancesResponse.class
        );
        assertNotNull(response.getRecords());
        assertEquals(response.getRecords().size(), 1);
        return response.getRecords().get(0);
    }

    private ProcessState getProcessState(String processInstanceId) throws IOException {
        HttpResponse response = HttpClientBuilder.create().build().execute(prepareStateRequest(processInstanceId));
        return objectMapper.readValue(response.getEntity().getContent(), ProcessState.class);
    }

    private List<ProcessInstance> search(ProcessSearchRequest processSearchRequest)
            throws IOException {
        HttpPost request = new HttpPost(getUri("/process/search", null));
        request.setHeader(HttpHeaders.CONTENT_TYPE, "application/json");
        request.setEntity(new StringEntity(new ObjectMapper().writeValueAsString(processSearchRequest),
                ContentType.APPLICATION_JSON));
        ProcessInstancesResponse response = sendHttpRequest(request, ProcessInstancesResponse.class);
        assertNotNull(response.getRecords());
        return response.getRecords();
    }

    private List<ProcessState> searchStates(ProcessSearchRequest processSearchRequest)
            throws IOException {
        HttpPost request = new HttpPost(getUri("/process/history-search", null));
        request.setHeader(HttpHeaders.CONTENT_TYPE, "application/json");
        request.setEntity(new StringEntity(new ObjectMapper().writeValueAsString(processSearchRequest),
                ContentType.APPLICATION_JSON));
        ProcessesStatesResponse response = sendHttpRequest(request, ProcessesStatesResponse.class);
        assertNotNull(response.getRecords());
        return response.getRecords();
    }

    private HttpGet prepareGetRequest(String processInstanceId) {
        HttpGet request = new HttpGet(getUri("/process/status", Map.of("process_instance_id", processInstanceId)));
        request.setHeader(HttpHeaders.CONTENT_TYPE, "application/json");
        return request;
    }

    private HttpPost prepareStartProcess(ProcessInstanceRequest processInstanceRequest)
            throws JsonProcessingException {
        HttpPost request = new HttpPost(getUri("/process", null));
        request.setHeader(HttpHeaders.CONTENT_TYPE, "application/json");
        request.setEntity(new StringEntity(new ObjectMapper().writeValueAsString(processInstanceRequest),
                ContentType.APPLICATION_JSON));
        return request;
    }

    private HttpGet prepareStateRequest(String processInstanceId) {
        HttpGet request = new HttpGet(getUri("/process/" + processInstanceId, Map.of()));
        request.setHeader(HttpHeaders.CONTENT_TYPE, "application/json");
        return request;
    }

    private void waitEndProcess(String processInstanceId) {
        ProcessInstance processInstance = getProcessStatus(processInstanceId);
        while (processInstance.getStatus() != ProcessStatus.COMPLETED) {
            processInstance = getProcessStatus(processInstanceId);
        }
    }

    private void assertProcessInstance(ProcessInstance expected, ProcessInstance actual) {
        org.assertj.core.api.Assertions.assertThat(actual)
                .returns(expected.getProcessInstanceId(), ProcessInstance::getProcessInstanceId)
                .returns(expected.getStatus(), ProcessInstance::getStatus)
                .returns(expected.getBusinessKey(), ProcessInstance::getBusinessKey);
    }

    @Test
    void removeAllProcesses() {
        try {
            processEngine.getRuntimeService().createProcessInstanceQuery().list()
                    .forEach(processInstance ->
                            processEngine.getRuntimeService()
                                    .deleteProcessInstance(processInstance.getProcessInstanceId(), "test"));
        } catch (BadUserRequestException be) {
            //skip
        }
    }
}
