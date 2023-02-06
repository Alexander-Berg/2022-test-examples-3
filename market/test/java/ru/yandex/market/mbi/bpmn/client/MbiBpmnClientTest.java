package ru.yandex.market.mbi.bpmn.client;

import java.net.SocketTimeoutException;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.mbi.bpmn.client.model.Error;
import ru.yandex.market.mbi.bpmn.client.model.ProcessInstanceRequest;
import ru.yandex.market.mbi.bpmn.client.model.ProcessInstancesResponse;
import ru.yandex.market.mbi.bpmn.client.model.ProcessStartResponse;
import ru.yandex.market.mbi.bpmn.client.model.ProcessType;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;


public class MbiBpmnClientTest {

    private static WireMockServer wm;
    private MbiBpmnClient client = new MbiBpmnClient.Builder().baseUrl("http://localhost:9000").build();

    @BeforeAll
    public static void setup() {
        wm = new WireMockServer(options().port(9000).withRootDirectory(Objects.requireNonNull(Util.getClassPathFile(
                "wiremock")).getAbsolutePath()));
        wm.start();
    }

    @AfterAll
    public static void tearDown() {
        wm.shutdown();
    }

    @DisplayName("Запрос выполнился с ошибкой. Сервер вернул код 400")
    @Test
    public void  get400ErrorTest() {
        MbiBpmnClientException thrown = assertThrows(MbiBpmnClientException.class, () -> {
            String processInstanceId = "f7b55ada-e261-4935-89c2-56b67d9dc6a3";
            client.getState(processInstanceId);
        });
        assertEquals(400, thrown.getHttpErrorCode());
        assertEquals(1, thrown.getErrors().size());
        Error error = thrown.getErrors().get(0);
        assertEquals("Bad Request", error.getCode());
        assertEquals("You are wrong", error.getMessage());
    }

    @DisplayName("Запрос выполнился с ошибкой. Сервер вернул код 500")
    @Test
    public void  get500ErrorTest() {
        MbiBpmnClientException thrown = assertThrows(MbiBpmnClientException.class, () -> {
            String processInstanceId = "f7b55adb-e261-4935-89c2-56b67d9dc6a3";
            client.getState(processInstanceId);
        });
        assertEquals(500, thrown.getHttpErrorCode());
        assertEquals(1, thrown.getErrors().size());
        Error error = thrown.getErrors().get(0);
        assertEquals("Internal Server Error", error.getCode());
        assertEquals("We are wrong", error.getMessage());
    }

    @DisplayName("Получение информации о запущенном процессе")
    @Test
    public void  getProcessStatusTest() {
        String processInstanceId = "f7b55adc-e261-4935-89c2-56b67d9dc6a3";
        ProcessInstancesResponse response = client.getState(processInstanceId);
        Assertions.assertNotNull(response.getApplication());
        Assertions.assertNotNull(response.getHost());
        Assertions.assertNotNull(response.getTimestamp());
        Assertions.assertNotNull(response.getPager());
        Assertions.assertNotNull(response.getPager().getPageSize());
        Assertions.assertNotNull(response.getPager().getCurrentPage());
        Assertions.assertNotNull(response.getPager().getTotalCount());
        Assertions.assertNotNull(response.getPager().getHasMorePages());
        Assertions.assertNotNull(response.getPager().getHasLessPages());
        Assertions.assertNotNull(response.getRecords());
        Assertions.assertTrue(response.getRecords().size() > 0);
        Assertions.assertNotNull(response.getRecords().get(0).getProcessInstanceId());
        Assertions.assertNotNull(response.getRecords().get(0).getBusinessKey());
        Assertions.assertNotNull(response.getRecords().get(0).getStatus());
    }

    @DisplayName("Сервер не ответил во время.")
    @Test
    public void timeoutTest() {
        // given
        MbiBpmnClient newClient = new MbiBpmnClient.Builder()
                .baseUrl("http://localhost:9000")
                .readTimeout(5, TimeUnit.MILLISECONDS)
                .build();

        // then
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> {
            String processInstanceId = "f7b55adc-e261-4935-89c2-56b67d9dc6a3";
            newClient.getState(processInstanceId);
        });
        assertEquals(SocketTimeoutException.class, thrown.getCause().getClass());
        assertEquals("timeout", thrown.getCause().getMessage());
    }

    @DisplayName("Получение информации о запущенном процессе")
    @Test
    public void  postProcessTest() {
        ProcessInstanceRequest processInstanceRequest = new ProcessInstanceRequest();
        processInstanceRequest.setBusinessKey("businessKey");
        processInstanceRequest.setProcessType(ProcessType.BUSINESS_MIGRATION);
        processInstanceRequest.setParams(Map.of("name", "value"));
        ProcessStartResponse response = client.postProcess(processInstanceRequest);

        Assertions.assertNotNull(response.getApplication());
        Assertions.assertNotNull(response.getHost());
        Assertions.assertNotNull(response.getTimestamp());
        Assertions.assertNotNull(response.getPager());
        Assertions.assertNotNull(response.getPager().getPageSize());
        Assertions.assertNotNull(response.getPager().getCurrentPage());
        Assertions.assertNotNull(response.getPager().getTotalCount());
        Assertions.assertNotNull(response.getPager().getHasMorePages());
        Assertions.assertNotNull(response.getPager().getHasLessPages());
        Assertions.assertNotNull(response.getRecords());
        Assertions.assertTrue(response.getRecords().size() > 0);
        Assertions.assertNotNull(response.getRecords().get(0).getProcessInstanceId());
        Assertions.assertNotNull(response.getRecords().get(0).getBusinessKey());
        Assertions.assertNotNull(response.getRecords().get(0).getStatus());
    }
}
