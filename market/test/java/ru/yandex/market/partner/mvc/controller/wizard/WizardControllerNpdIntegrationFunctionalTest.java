package ru.yandex.market.partner.mvc.controller.wizard;

import java.time.OffsetDateTime;
import java.util.Map;

import com.github.tomakehurst.wiremock.matching.RequestPattern;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.program.partner.model.Status;
import ru.yandex.market.core.wizard.model.WizardStepType;
import ru.yandex.market.core.wizard.step.dto.PartnerNpdApplicationDto;
import ru.yandex.market.core.wizard.step.dto.PartnerNpdApplicationStatusDto;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;

@DbUnitDataSet(before = {"csv/commonBlueWizardData.before.csv", "csv/testNpdIntegration.csv"})
class WizardControllerNpdIntegrationFunctionalTest extends AbstractWizardControllerFunctionalTest {

    @BeforeEach
    private void beforeEach() {
        integrationNpdWireMockServer.resetMappings();
        integrationNpdWireMockServer.removeServeEventsMatching(RequestPattern.everything());
    }

    @Test
    void testNotSelfEmployed() {
        var ex = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> requestStep(DS_SUPPLIER_CAMPAIGN_ID, WizardStepType.NPD_INTEGRATION)
        );
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void testNoApplication() {
        String expectedPhone = "89164490000";
        mockNotFound(2501);
        var response = requestStep(DROPSHIP_SUPPLIER_CAMPAIGN_ID, WizardStepType.NPD_INTEGRATION);
        assertResponse(
                response,
                makeResponseStepStatus(
                        WizardStepType.NPD_INTEGRATION,
                        Status.RESTRICTED,
                        Map.of("phoneNumber", expectedPhone)
                )
        );
    }

    @Test
    void testNewApplication() {
        String expectedPhone = "89164490000";
        mockGetApplication(
                2501,
                "{\n" +
                        "  \"partnerId\": 2501,\n" +
                        "  \"phone\": \"89164490000\",\n" +
                        "  \"status\": \"NEW\",\n" +
                        "  \"inn\": \"1234567\",\n" +
                        "  \"fnsRequestId\": \"123456789\",\n" +
                        "  \"createdDate\":\"2022-05-16T21:00:00Z\"," +
                        "  \"updatedDate\":\"2022-05-16T21:00:00Z\"" +
                        "}"
        );
        var response = requestStep(DROPSHIP_SUPPLIER_CAMPAIGN_ID, WizardStepType.NPD_INTEGRATION);
        assertResponse(
                response,
                makeResponseStepStatus(
                        WizardStepType.NPD_INTEGRATION,
                        Status.RESTRICTED,
                        Map.of("phoneNumber",
                                expectedPhone,
                                "npdApplication",
                                PartnerNpdApplicationDto.newBuilder()
                                        .setPartnerId(2501L)
                                        .setInn("1234567")
                                        .setPhone(expectedPhone)
                                        .setStatus(PartnerNpdApplicationStatusDto.NEW)
                                        .setCreatedDate(OffsetDateTime.parse("2022-05-16T21:00:00Z"))
                                        .setUpdatedDate(OffsetDateTime.parse("2022-05-16T21:00:00Z"))
                                        .setFnsRequestId("123456789")
                                        .build()
                        )
                )
        );
    }

    @Test
    void testDoneApplication() {
        String expectedPhone = "89164490000";
        mockGetApplication(
                2501,
                "{\n" +
                        "  \"partnerId\": 2501,\n" +
                        "  \"phone\": \"89164490000\",\n" +
                        "  \"status\": \"DONE\",\n" +
                        "  \"inn\": \"1234567\",\n" +
                        "  \"fnsRequestId\": \"123456789\",\n" +
                        "  \"createdDate\":\"2022-05-16T21:00:00Z\"," +
                        "  \"updatedDate\":\"2022-05-16T21:00:00Z\"" +
                        "}"
        );
        var response = requestStep(DROPSHIP_SUPPLIER_CAMPAIGN_ID, WizardStepType.NPD_INTEGRATION);
        assertResponse(
                response,
                makeResponseStepStatus(
                        WizardStepType.NPD_INTEGRATION,
                        Status.FULL,
                        Map.of("phoneNumber",
                                expectedPhone,
                                "npdApplication",
                                PartnerNpdApplicationDto.newBuilder()
                                        .setPartnerId(2501L)
                                        .setInn("1234567")
                                        .setPhone(expectedPhone)
                                        .setStatus(PartnerNpdApplicationStatusDto.DONE)
                                        .setCreatedDate(OffsetDateTime.parse("2022-05-16T21:00:00Z"))
                                        .setUpdatedDate(OffsetDateTime.parse("2022-05-16T21:00:00Z"))
                                        .setFnsRequestId("123456789")
                                        .build()
                        )
                )
        );
    }

    @Test
    void testErrorApplication() {
        String expectedPhone = "89164490000";
        mockGetApplication(
                2501,
                "{\n" +
                        "  \"partnerId\": 2501,\n" +
                        "  \"phone\": \"89164490000\",\n" +
                        "  \"status\": \"ERROR\",\n" +
                        "  \"inn\": \"1234567\",\n" +
                        "  \"fnsRequestId\": \"123456789\",\n" +
                        "  \"createdDate\":\"2022-05-16T21:00:00Z\"," +
                        "  \"updatedDate\":\"2022-05-16T21:00:00Z\"" +
                        "}"
        );
        var response = requestStep(DROPSHIP_SUPPLIER_CAMPAIGN_ID, WizardStepType.NPD_INTEGRATION);
        assertResponse(
                response,
                makeResponseStepStatus(
                        WizardStepType.NPD_INTEGRATION,
                        Status.FAILED,
                        Map.of("phoneNumber",
                                expectedPhone,
                                "npdApplication",
                                PartnerNpdApplicationDto.newBuilder()
                                        .setPartnerId(2501L)
                                        .setInn("1234567")
                                        .setPhone(expectedPhone)
                                        .setStatus(PartnerNpdApplicationStatusDto.ERROR)
                                        .setCreatedDate(OffsetDateTime.parse("2022-05-16T21:00:00Z"))
                                        .setUpdatedDate(OffsetDateTime.parse("2022-05-16T21:00:00Z"))
                                        .setFnsRequestId("123456789")
                                        .build()
                        )
                )
        );
    }

    @Test
    void testInternalError() {
        String expectedPhone = "89164490000";
        mockInternalError(2501);
        var response = requestStep(DROPSHIP_SUPPLIER_CAMPAIGN_ID, WizardStepType.NPD_INTEGRATION);
        assertResponse(
                response,
                makeResponseStepStatus(
                        WizardStepType.NPD_INTEGRATION,
                        Status.INTERNAL_ERROR,
                        Map.of("error", "ru.yandex.market.core.npd.client.exception.NpdApplicationException: " +
                                "Exception occurred in request to integration-npd")
                )
        );
    }

    private void mockInternalError(long partnerId) {
        integrationNpdWireMockServer.stubFor(get("/api/v1/partners/" + partnerId + "/application")
                .willReturn(aResponse().withStatus(HttpStatus.INTERNAL_SERVER_ERROR.value())
                        .withBody(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase()))
        );
    }

    private void mockNotFound(long partnerId) {
        integrationNpdWireMockServer.stubFor(get("/api/v1/partners/" + partnerId + "/application")
                .willReturn(aResponse().withStatus(HttpStatus.NOT_FOUND.value())
                        .withBody( "{\n" +
                                "  \"errors\": [\n" +
                                "    {\n" +
                                "      \"message\": \"Partner " + partnerId + " was not found\",\n" +
                                "      \"details\": {\n" +
                                "        \"partnerId\": " + partnerId + "\n" +
                                "      },\n" +
                                "      \"code\": \"NOT_FOUND\"\n" +
                                "    }\n" +
                                "  ]\n" +
                                "}"))
        );
    }
}
