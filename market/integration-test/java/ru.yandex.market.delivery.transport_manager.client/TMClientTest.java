package ru.yandex.market.delivery.transport_manager.client;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

import ru.yandex.market.delivery.transport_manager.model.dto.MovementCourierDto;
import ru.yandex.market.delivery.transport_manager.model.dto.StockKeepingUnitDto;
import ru.yandex.market.delivery.transport_manager.model.dto.route.RouteStatusDto;
import ru.yandex.market.delivery.transport_manager.model.enums.CountType;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonRequestContent;

public class TMClientTest extends AbstractClientTest {

    private static final long SEQUENCE_ID = 14L;
    private static final String PROCESS_ID = "14";
    private static final String INTAKE_ID = "TM66";
    private static final long PARTNER_ID = 119L;
    private static final String ERROR_MSG = "error message";
    private static final String REGISTER_ID = "regId1";
    private static final String MOVEMENT_ID = "TMM123";

    @Autowired
    TransportManagerClient client;

    @Test
    void createIntakeSuccess() {
        mockServer.expect(method(HttpMethod.PUT))
            .andExpect(requestTo(tmApiProperties.getUrl() + "/shipment/createIntakeSuccess"))
            .andExpect(jsonRequestContent("request/shipment/create_shipment_success.json"))
            .andRespond(withSuccess());

        client.setCreateIntakeSuccess(SEQUENCE_ID, INTAKE_ID, "3123312", PARTNER_ID);
    }

    @Test
    void createIntakeError() {
        mockServer.expect(method(HttpMethod.PUT))
            .andExpect(requestTo(tmApiProperties.getUrl() + "/shipment/createIntakeError"))
            .andExpect(jsonRequestContent("request/shipment/create_shipment_error.json"))
            .andRespond(withSuccess());

        client.setCreateIntakeError(SEQUENCE_ID, INTAKE_ID, PARTNER_ID, ERROR_MSG);
    }

    @Test
    void createSelfExportSuccess() {
        mockServer.expect(method(HttpMethod.PUT))
            .andExpect(requestTo(tmApiProperties.getUrl() + "/shipment/createSelfExportSuccess"))
            .andExpect(jsonRequestContent("request/shipment/create_shipment_success.json"))
            .andRespond(withSuccess());

        client.setCreateSelfExportSuccess(SEQUENCE_ID, INTAKE_ID, "3123312", PARTNER_ID);
    }

    @Test
    void createSelfExportError() {
        mockServer.expect(method(HttpMethod.PUT))
            .andExpect(requestTo(tmApiProperties.getUrl() + "/shipment/createSelfExportError"))
            .andExpect(jsonRequestContent("request/shipment/create_shipment_error.json"))
            .andRespond(withSuccess());

        client.setCreateSelfExportError(SEQUENCE_ID, INTAKE_ID, PARTNER_ID, ERROR_MSG);
    }

    @Test
    void createRegistrySuccess() {
        mockServer.expect(method(HttpMethod.PUT))
            .andExpect(requestTo(tmApiProperties.getUrl() + "/register/createSuccess"))
            .andExpect(jsonRequestContent("request/register/create_register_success.json"))
            .andRespond(withSuccess());

        client.setCreateRegisterSuccess(PROCESS_ID, REGISTER_ID, "e1");
    }

    @Test
    void createRegistryError() {
        mockServer.expect(method(HttpMethod.PUT))
            .andExpect(requestTo(tmApiProperties.getUrl() + "/register/createError"))
            .andExpect(jsonRequestContent("request/register/create_register_error.json"))
            .andRespond(withSuccess());

        client.setCreateRegisterError(PROCESS_ID, REGISTER_ID, ERROR_MSG);
    }

    @Test
    void createMovementSuccess() {
        mockServer.expect(method(HttpMethod.PUT))
            .andExpect(requestTo(tmApiProperties.getUrl() + "/lgw/movement/" + MOVEMENT_ID + "/success"))
            .andExpect(jsonRequestContent("request/movement/create_movement_success.json"))
            .andRespond(withSuccess());

        client.setPutMovementSuccess(SEQUENCE_ID, MOVEMENT_ID, "666", PARTNER_ID);
    }

    @Test
    void createMovementError() {
        mockServer.expect(method(HttpMethod.PUT))
            .andExpect(requestTo(tmApiProperties.getUrl() + "/lgw/movement/" + MOVEMENT_ID + "/error"))
            .andExpect(jsonRequestContent("request/movement/create_movement_error.json"))
            .andRespond(withSuccess());

        client.setPutMovementError(
            SEQUENCE_ID,
            MOVEMENT_ID,
            "666",
            PARTNER_ID,
            ERROR_MSG
        );
    }

    @Test
    void getMovementSuccess() {
        mockServer.expect(method(HttpMethod.PUT))
            .andExpect(requestTo(tmApiProperties.getUrl() + "/lgw/movement/" + MOVEMENT_ID + "/courier/success"))
            .andExpect(jsonRequestContent("request/movement/get_movement_success.json"))
            .andRespond(withSuccess());

        client.setGetMovementSuccess(SEQUENCE_ID, MOVEMENT_ID, "666", PARTNER_ID, MovementCourierDto.builder()
            .carModel("старая добрая буханка")
            .carNumber("Р173НО199")
            .name("Иван")
            .surname("Доставкин")
            .patronymic("Михайлович")
            .yandexUid(-1L)
            .build()
        );
    }

    @Test
    void getMovementError() {
        mockServer.expect(method(HttpMethod.PUT))
            .andExpect(requestTo(tmApiProperties.getUrl() + "/lgw/movement/" + MOVEMENT_ID + "/courier/error"))
            .andExpect(jsonRequestContent("request/movement/get_movement_error.json"))
            .andRespond(withSuccess());

        client.setGetMovementError(
            SEQUENCE_ID,
            MOVEMENT_ID,
            "666",
            PARTNER_ID,
            ERROR_MSG
        );
    }

    @Test
    void createTransportationTask() {
        mockServer.expect(method(HttpMethod.POST))
            .andExpect(requestTo(tmApiProperties.getUrl() + "/transportation-task"))
            .andExpect(jsonRequestContent("request/transportation_task/create_task.json"))
            .andRespond(withSuccess());

        client.createTransportationTask(
            1L,
            2L,
            List.of(
                new StockKeepingUnitDto("abc", "sup_id1", null, 5, null, null),
                new StockKeepingUnitDto("abcds", "sup_id2", "real_id", 5, null, CountType.FIT)
            ),
            1L
        );
    }

    @Test
    void validateTransportationTask() {
        mockServer.expect(method(HttpMethod.POST))
            .andExpect(requestTo(tmApiProperties.getUrl() + "/transportation-task/validate"))
            .andExpect(jsonRequestContent("request/transportation_task/id.json"))
            .andRespond(withSuccess());

        client.revalidateTransportationTask(1L);
    }

    @Test
    void searchRouteById() {
        mockServer.expect(method(HttpMethod.GET))
            .andExpect(requestTo(tmApiProperties.getUrl() + "/routes?routeId=1"))
            .andRespond(withSuccess(
                extractFileContent("response/routes/search.json"),
                MediaType.APPLICATION_JSON
            ));
        client.searchRouteById(1);
    }

    @Test
    void changeRouteStatus() {
        mockServer.expect(method(HttpMethod.POST))
            .andExpect(requestTo(tmApiProperties.getUrl() + "/routes/1/changeStatus?status=ARCHIVE"))
            .andRespond(withSuccess(
                extractFileContent("response/routes/search.json"),
                MediaType.APPLICATION_JSON
            ));
        client.changeRouteStatus(1, RouteStatusDto.ARCHIVE);
    }

    @Test
    void getLogisticPointsAddress() {
        mockServer.expect(method(HttpMethod.GET))
            .andExpect(requestTo(tmApiProperties.getUrl() + "/logisticPoint?ids=101,202"))
            .andRespond(
                withSuccess(
                    extractFileContent("request/emptyArray.json"),
                    MediaType.APPLICATION_JSON
                ));

        client.getLogisticPointsAddress(List.of(101L, 202L));
    }

    @Test
    void setPutOutboundError() {
        mockServer.expect(method(HttpMethod.PUT))
            .andExpect(requestTo(tmApiProperties.getUrl() + "/lgw/outbound/TMU1/error"))
            .andExpect(jsonRequestContent("request/unit/put_outbound_error.json"))
            .andRespond(withSuccess());

        client.setPutOutboundError(null, "TMU1", null, 2L, "error");
    }

    @Test
    void setGetOutboundError() {
        mockServer.expect(method(HttpMethod.PUT))
            .andExpect(requestTo(tmApiProperties.getUrl() + "/lgw/outbound/get/error"))
            .andExpect(jsonRequestContent("request/unit/get_unit_error.json"))
            .andRespond(withSuccess());

        client.setGetOutboundError("TMU2", "EXT_123", 5L, null, "error");
    }

    @Test
    void setGetInboundError() {
        mockServer.expect(method(HttpMethod.PUT))
            .andExpect(requestTo(tmApiProperties.getUrl() + "/lgw/inbound/get/error"))
            .andExpect(jsonRequestContent("request/unit/get_unit_error.json"))
            .andRespond(withSuccess());

        client.setGetInboundError("TMU2", "EXT_123", 5L, null, "error");
    }

    @Test
    void setPutOutboundDocumentsSuccess() {
        mockServer.expect(method(HttpMethod.PUT))
            .andExpect(requestTo(tmApiProperties.getUrl() + "/lgw/unit-documents/TMU1/success"))
            .andExpect(jsonRequestContent("request/unit/put_outbound_documents_success.json"))
            .andRespond(withSuccess());

        client.setPutOutboundDocumentsSuccess(4L, "TMU1", "666", 2L);
    }

    @Test
    void setPutOutboundDocumentsError() {
        mockServer.expect(method(HttpMethod.PUT))
            .andExpect(requestTo(tmApiProperties.getUrl() + "/lgw/unit-documents/TMU1/error"))
            .andExpect(jsonRequestContent("request/unit/put_outbound_documents_error.json"))
            .andRespond(withSuccess());

        client.setPutOutboundDocumentsError(1L, null, "TMU1", null, 2L, "error");
    }

    @Test
    void cancelUnitSuccess() {
        mockServer.expect(method(HttpMethod.PUT))
            .andExpect(requestTo(tmApiProperties.getUrl() + "/lgw/inbound/cancel/TMU1/success"))
            .andRespond(withSuccess());

        client.cancelInboundSuccess("TMU1");
    }

    @Test
    void cancelUnitError() {
        mockServer.expect(method(HttpMethod.PUT))
            .andExpect(requestTo(tmApiProperties.getUrl() + "/lgw/inbound/cancel/TMU1/error"))
            .andRespond(withSuccess());

        client.cancelInboundError("TMU1");
    }
}
