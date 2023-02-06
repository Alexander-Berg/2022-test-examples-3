package ru.yandex.market.checkout.checkouter.checkerx.client;

import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.common.web.CheckoutHttpParameters;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static ru.yandex.market.checkout.util.checkerx.ResourceUtils.readResourceFile;

public class CheckErxClientTest extends AbstractWebTestBase {

    @Autowired
    private WireMockServer checkErxMock;
    @Autowired
    private CheckErxClient checkErxClient;

    @AfterEach
    public void resetMocks() {
        checkErxMock.resetAll();
    }

    @Test
    public void prescriptionStatusResponseShouldBeValid() {
        final String offerId = "-_40VqaS9BpXO1qaTtweBA";
        String responseJson = readResourceFile("/json/prescriptionStatusResponse_1.json");
        checkErxMock.stubFor(
                post(urlPathEqualTo("/getPrescriptionsForRegion"))
                        .willReturn(okJson(responseJson)));

        checkErxMock.addMockServiceRequestListener((request, response) -> {
            String bodyAsString = request.getBodyAsString();
            String expectedJson = readResourceFile("/json/prescriptionStatusRequest_1.json");
            JSONAssert.assertEquals(expectedJson, bodyAsString, JSONCompareMode.NON_EXTENSIBLE);

            var serviceTicketHeader = request.getHeaders()
                    .getHeader(CheckoutHttpParameters.SERVICE_TICKET_HEADER);
            assertThat(serviceTicketHeader.isPresent(), equalTo(true));
            var contentTypeHeader = request.getHeaders()
                    .getHeader(HttpHeaders.CONTENT_TYPE);
            assertThat(contentTypeHeader.isPresent(), equalTo(true));
            assertThat(contentTypeHeader.containsValue(MediaType.APPLICATION_JSON_UTF8_VALUE), equalTo(true));
        });

        Set<String> offerIds = Set.of(offerId);
        Map<String, Boolean> offersMap = checkErxClient.checkPrescriptionStatuses(offerIds, 111222333444L);
        assertEquals(offersMap.size(), 1);
        assertEquals(offersMap.get(offerId), true);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("allowedErxErrors")
    public void restTemplateExceptionHandlerTest(ExceptionInfo exceptionInfo) {
        final Set<String> offerIds = Set.of("ware_md5");
        final long uid = 111222333444L;

        checkErxMock.stubFor(
                post(urlPathEqualTo("/getPrescriptionsForRegion"))
                        .willReturn(aResponse()
                                .withHeader("Content-Type", "application/json")
                                .withStatus(exceptionInfo.inputStatus)
                                .withBody(createResponse(exceptionInfo))
                        )
        );
        CheckErxException ex = assertThrows(CheckErxException.class,
                () -> checkErxClient.checkPrescriptionStatuses(offerIds, uid));
        assertEquals(ex.getCode(), exceptionInfo.outputCode);
        assertEquals(ex.getStatusCode(), exceptionInfo.outputStatus);
    }

    public static Stream<Arguments> allowedErxErrors() {
        return Stream.of(
                new ExceptionInfo(424, "UNKNOWN_UID", 424),
                new ExceptionInfo(422, "INVALID_REQUEST_PARAMETER", 422),
                new ExceptionInfo(401, "UNKNOWN_ESIA_TOKEN", 401),
                new ExceptionInfo(408, "MEDICATA_CONNECTION_PROBLEM", 408),
                new ExceptionInfo(500, "ERX_SERVICE_ERROR", 400)
        ).map(Arguments::of);
    }

    private static class ExceptionInfo {

        final int inputStatus;
        final String outputCode;
        final int outputStatus;

        ExceptionInfo(int inputStatus, String outputCode, int outputStatus) {
            this.inputStatus = inputStatus;
            this.outputCode = outputCode;
            this.outputStatus = outputStatus;
        }

        @Override
        public String toString() {
            return "ExceptionInfo{" +
                    "inputStatus=" + inputStatus +
                    ", outputCode='" + outputCode + '\'' +
                    ", outputStatus=" + outputStatus +
                    '}';
        }
    }

    private static String createResponse(ExceptionInfo exceptionInfo) {
        String template = "{\"timestamp\":1635871966386,\"status\":@STATUS@,\"error\":\"ERROR_CODE\"," +
                "\"message\":\"some error message\"}";
        return template.replace("@STATUS@", String.valueOf(exceptionInfo.inputStatus));
    }
}
