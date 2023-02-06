package ru.yandex.market.mbi.partner.registration.client;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.mbi.partner.registration.client.model.ApiError;
import ru.yandex.market.mbi.partner.registration.client.model.PartnerPlacementType;
import ru.yandex.market.mbi.partner.registration.client.model.PartnerRegistration;
import ru.yandex.market.mbi.partner.registration.client.model.PartnerRegistrationRequest;
import ru.yandex.market.mbi.partner.registration.client.model.PartnerRegistrationResponse;
import ru.yandex.market.mbi.partner.registration.client.model.ProcessInstance;
import ru.yandex.market.mbi.partner.registration.client.model.ProcessInstances;
import ru.yandex.market.mbi.partner.registration.client.model.ProcessSearchRequest;
import ru.yandex.market.mbi.partner.registration.client.model.ProcessStart;
import ru.yandex.market.mbi.partner.registration.client.model.ProcessStartRequest;
import ru.yandex.market.mbi.partner.registration.client.model.ProcessStartResponse;
import ru.yandex.market.mbi.partner.registration.client.model.ProcessState;
import ru.yandex.market.mbi.partner.registration.client.model.ProcessStateResponse;
import ru.yandex.market.mbi.partner.registration.client.model.ProcessStatus;
import ru.yandex.market.mbi.partner.registration.client.model.ProcessType;
import ru.yandex.market.mbi.partner.registration.exception.MbiPartnerRegistrationClientException;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class MbiPartnerRegistrationClientTest {

    private static WireMockServer wm;

    private final MbiPartnerRegistrationClient client = MbiPartnerRegistrationClient.newBuilder()
            .baseUrl("http://localhost:9000")
            .build();

    @BeforeAll
    public static void setup() {
        wm = new WireMockServer(
                options()
                        .port(9000)
                        .withRootDirectory(Objects.requireNonNull(getClassPathFile("wiremock")).getAbsolutePath())
        );
        wm.start();
    }

    @AfterAll
    public static void tearDown() {
        wm.shutdown();
    }

    @AfterEach
    void after() {
        wm.resetAll();
    }

    @BeforeEach
    void before() {
        wm.resetAll();
    }

    @Test
    void registerPartner() {
        PartnerRegistrationResponse response = client.registerPartner(
                10L,
                new PartnerRegistrationRequest()
                        .partnerName("Магазин")
                        .partnerPlacementType(PartnerPlacementType.FBY)
        );
        assertEquals(
                new PartnerRegistration().partnerId(1L).businessId(10L),
                response.getResult()
        );
    }

    @Test
    void startPartnerProcess() {
        ProcessStartResponse response = client.startPartnerProcess(
                new ProcessStartRequest()
                        .processType(ProcessType.SIMPLE_PROCESS)
                        .businessKey("KEY")
        );
        assertEquals(
                new ProcessStart()
                        .started(true)
                        .status(ProcessStatus.ACTIVE)
                        .businessKey("KEY")
                        .processInstanceId("id"),
                response.getResult()
        );
    }

    @Test
    void getProcess() {
        ProcessStateResponse response = client.getProcess("id");
        assertEquals(
                new ProcessState()
                        .params(Map.of("one", 1.0))
                        .businessKey("KEY")
                        .processInstanceId("id")
                        .status(ProcessStatus.ACTIVE),
                response.getResult()
        );
    }

    @Test
    void searchProcess() {
        ProcessInstances processInstances = client.searchProcess(new ProcessSearchRequest().processInstanceId("id"));
        assertEquals(
                List.of(
                        new ProcessInstance()
                                .processInstanceId("id")
                                .businessKey("KEY")
                                .status(ProcessStatus.ACTIVE)
                ),
                processInstances.getResult()
        );
    }

    @Test
    void exception() {
        var exception = assertThrows(
                MbiPartnerRegistrationClientException.class,
                () -> client.getProcess("exception")
        );
        assertEquals(HttpStatus.SC_NOT_FOUND, exception.getHttpErrorCode());
        assertEquals(
                List.of(
                        new ApiError()
                                .code(ApiError.CodeEnum.PROCESS_NOT_FOUND)
                                .message("no way")
                ),
                exception.getErrors()
        );
    }

    private static File getClassPathFile(String path) {
        ClassLoader classLoader = MbiPartnerRegistrationClient.class.getClassLoader();
        URL url = classLoader.getResource(path);
        if (url == null) {
            return null;
        } else {
            return new File(url.getFile());
        }

    }
}
