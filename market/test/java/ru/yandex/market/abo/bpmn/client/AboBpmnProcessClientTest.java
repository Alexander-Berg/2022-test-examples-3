package ru.yandex.market.abo.bpmn.client;

import java.util.Map;
import java.util.Objects;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.abo.bpmn.client.model.ApiError;
import ru.yandex.market.abo.bpmn.client.model.ModerationCreateRequest;
import ru.yandex.market.abo.bpmn.client.model.ModerationCreateResponse;
import ru.yandex.market.abo.bpmn.client.model.ModerationInfo;
import ru.yandex.market.abo.bpmn.client.model.ModerationReadyRequest;
import ru.yandex.market.abo.bpmn.client.model.ModerationType;
import ru.yandex.market.abo.bpmn.client.model.ProcessStart;
import ru.yandex.market.abo.bpmn.client.model.ProcessStartRequest;
import ru.yandex.market.abo.bpmn.client.model.ProcessStartResponse;
import ru.yandex.market.abo.bpmn.client.model.ProcessStatus;
import ru.yandex.market.abo.bpmn.client.model.ProcessType;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Aleksei Neliubin (neliubin@yandex-team.ru)
 * @date 17.03.2022
 */
public class AboBpmnProcessClientTest {
    private static WireMockServer wm;
    private final AboBpmnProcessClient processClient = AboBpmnProcessClient.newBuilder()
            .baseUrl("http://localhost:9000").build();
    private final AboBpmnModerationClient moderationClient = AboBpmnModerationClient.newBuilder()
            .baseUrl("http://localhost:9000").build();
    private final AboBpmnClient aboBpmnClient = new AboBpmnClient(processClient, moderationClient);

    @BeforeAll
    public static void setup() {
        wm = new WireMockServer(
                options()
                        .port(9000)
                        .withRootDirectory(Objects.requireNonNull(Util.getClassPathFile("wiremock")).getAbsolutePath())
        );
        wm.start();
    }

    @AfterAll
    public static void tearDown() {
        wm.shutdown();
    }

    @DisplayName("Запрос выполнился с ошибкой. Сервер вернул код 400")
    @Test
    public void get400ErrorTest() {
        AboBpmnClientException thrown = assertThrows(AboBpmnClientException.class, () -> {
            String processInstanceId = "592637c1-a61a-11ec-bd48-16e4c444965b";
            aboBpmnClient.processClient().getProcessState(processInstanceId);
        });
        assertEquals(400, thrown.getHttpErrorCode());
        assertNotNull(thrown.getErrors());
        assertEquals(1, thrown.getErrors().size());
        ApiError error = thrown.getErrors().get(0);
        assertEquals(ApiError.CodeEnum.BAD_REQUEST, error.getCode());
        assertEquals("You are wrong", error.getMessage());
    }

    @DisplayName("Запрос выполнился с ошибкой. Сервер вернул код 500")
    @Test
    public void get500ErrorTest() {
        AboBpmnClientException thrown = assertThrows(AboBpmnClientException.class, () -> {
            String processInstanceId = "692637c1-a61a-11ec-bd48-16e4c444965b";
            aboBpmnClient.processClient().getProcessState(processInstanceId);
        });
        assertEquals(500, thrown.getHttpErrorCode());
        assertNotNull(thrown.getErrors());
        assertEquals(1, thrown.getErrors().size());
        ApiError error = thrown.getErrors().get(0);
        assertEquals(ApiError.CodeEnum.INTERNAL_SERVER_ERROR, error.getCode());
        assertEquals("We are wrong", error.getMessage());
    }

    @DisplayName("Запрос на создание модерации")
    @Test
    public void createModerationTest() {
        ModerationCreateRequest moderationStartRequest = new ModerationCreateRequest();
        moderationStartRequest.setPartnerId(123L);
        moderationStartRequest.setModerationType(ModerationType.LITE_MODERATION);
        ModerationCreateResponse response = aboBpmnClient.moderationClient().createModeration(moderationStartRequest);
        ModerationInfo moderationInfo = response.getResult();

        assertNotNull(moderationInfo);
        assertEquals(123, moderationInfo.getPartnerId());
        assertEquals(ModerationType.LITE_MODERATION, moderationInfo.getModerationType());
    }

    @DisplayName("Запрос на запуск модерации")
    @Test
    public void pushReadyForModerationTest() {
        ModerationReadyRequest moderationReadyRequest = new ModerationReadyRequest();
        moderationReadyRequest.setPartnerId(123L);
        moderationReadyRequest.setModerationType(ModerationType.LITE_MODERATION);
        ProcessStartResponse response = aboBpmnClient.moderationClient()
                .pushReadyForModeration(moderationReadyRequest);
        ProcessStart processStart = response.getResult();

        assertNotNull(processStart);
        assertEquals("12", processStart.getBusinessKey());
        assertEquals(ProcessStatus.ACTIVE, processStart.getStatus());
        assertNotNull(processStart.getProcessInstanceId());
    }

    @DisplayName("Запрос процесса")
    @Test
    public void startProcessTest() {
        ProcessStartRequest processStartRequest = new ProcessStartRequest();
        processStartRequest.setBusinessKey("12");
        processStartRequest.processType(ProcessType.PARTNER_MODERATION);
        processStartRequest.params(Map.of("partnerId", 123));

        ProcessStartResponse response = aboBpmnClient.processClient().startProcess(processStartRequest);
        ProcessStart processStart = response.getResult();

        assertNotNull(processStart);
        assertEquals("12", processStart.getBusinessKey());
        assertEquals(ProcessStatus.ACTIVE, processStart.getStatus());
        assertNotNull(processStart.getProcessInstanceId());
    }
}
