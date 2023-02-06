package ru.yandex.market.pricelabs.tms.programs;

import java.io.IOException;
import java.util.List;

import lombok.extern.slf4j.Slf4j;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import ru.yandex.market.pricelabs.CoreConfigurationForTests;
import ru.yandex.market.pricelabs.model.program.AdvProgramActivationRequest;
import ru.yandex.market.pricelabs.model.program.AdvProgramActivationRequestStatus;
import ru.yandex.market.pricelabs.model.program.MonetizationService;
import ru.yandex.market.pricelabs.tms.ConfigurationForTests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.yandex.market.pricelabs.tms.ConfigurationForTests.MockWebServerControls;

//CHECKSTYLE:OFF
@Slf4j
@ExtendWith(MockitoExtension.class)
public class ProgramSendActivationsTmsTest extends AbstractMonetizationTests {

    @Value("${monetization.client.http.url}")
    private String monetizationServerUrl;

    @Autowired
    private MonetizationService monetizationService;

    private MockWebServerControls monetizationMockServer;

    @BeforeEach
    private void before() throws IOException {
        monetizationMockServer = monetizationMockServer();
        monetizationMockServer.cleanup();
    }

    @AfterEach
    private void after() {
        monetizationMockServer.close();
    }

    @Test
    public void testSendActivatedAndResetToMonetization() throws Exception {
        List<AdvProgramActivationRequest> activationRequests = getActivationAndResetRequests();
        advProgramActivationRequestYtScenarioExecutor.insert(activationRequests);

        setServerResponse(200, "ok");
        monetizationService.sendActivatedAndResetToMonetization();

        for (int i : List.of(1, 2, 3)) {
            RecordedRequest message = monetizationMockServer.getMessage();
            assertEquals(
                    "PUT /v1/program/status HTTP/1.1",
                    message.toString());

            assertEquals(loadJson(String.format("/program/testSendActivatedAndResetToMonetization_%s.request", i)),
                    prettify(message.getBody().readUtf8()));
        }

        Assertions.assertTrue(getRequest(10).isSent());
        Assertions.assertFalse(getRequest(20).isSent());
        Assertions.assertTrue(getRequest(30).isSent());
        Assertions.assertFalse(getRequest(40).isSent());
        Assertions.assertTrue(getRequest(50).isSent());
    }

    @Test
    public void testSendActivatedAndResetToMonetizationRequestFailedNothingWasChanged() {
        List<AdvProgramActivationRequest> activationRequests = getActivationAndResetRequests();
        advProgramActivationRequestYtScenarioExecutor.insert(activationRequests);

        setServerResponse(500, "some error");
        try {
            monetizationService.sendActivatedAndResetToMonetization();
        } catch (Throwable expected) {
            log.info("Expecting this exception {}", expected.getMessage());
        }
        RecordedRequest message = monetizationMockServer.getMessage();
        assertEquals(
                "PUT /v1/program/status HTTP/1.1",
                message.toString());

        Assertions.assertFalse(getRequest(10).isSent());
        Assertions.assertFalse(getRequest(20).isSent());
        Assertions.assertFalse(getRequest(30).isSent());
        Assertions.assertFalse(getRequest(40).isSent());
        Assertions.assertFalse(getRequest(50).isSent());
    }

    private List<AdvProgramActivationRequest> getActivationAndResetRequests() {
        return List.of(
                newActivationRequest(10, false, AdvProgramActivationRequestStatus.READY),
                newActivationRequest(20, true, AdvProgramActivationRequestStatus.NEW),
                newActivationRequest(30, true, AdvProgramActivationRequestStatus.READY_RESET),
                newActivationRequest(40),
                newActivationRequest(50, false, AdvProgramActivationRequestStatus.READY)
        );
    }

    private MockWebServerControls monetizationMockServer() throws IOException {
        int lastColonIdx = monetizationServerUrl.lastIndexOf(":");
        String host = monetizationServerUrl.substring(0, lastColonIdx);
        int port = Integer.parseInt(monetizationServerUrl.substring(lastColonIdx + 1));

        MockWebServer monetizationMockServer = CoreConfigurationForTests.Basic.mockWebServer();
        monetizationMockServer.start(port);
        monetizationMockServer.url(host);
        return ConfigurationForTests.MockWebServerControls.wrap(monetizationMockServer,
                CoreConfigurationForTests.Basic.getAgnosticDispatcher());
    }

    public void setServerResponse(int status, String body) {
        String requestBidding = "PUT /v1/program/status";
        monetizationMockServer.addRequestMatcher(request -> {
            if (request.toString().startsWith(requestBidding)) {
                MockResponse response = new MockResponse();
                response.setResponseCode(status);
                response.setBody(String.format("{\"status\": \"%s\"}", body));
                return response;
            }

            return new MockResponse();
        });
    }
}
//CHECKSTYLE:ON
