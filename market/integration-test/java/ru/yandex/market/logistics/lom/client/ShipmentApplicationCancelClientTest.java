package ru.yandex.market.logistics.lom.client;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.ResponseActions;

import ru.yandex.market.logistics.util.client.exception.HttpTemplateException;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;

class ShipmentApplicationCancelClientTest extends AbstractClientTest {

    @Autowired
    private LomClient lomClient;

    @Test
    @DisplayName("Успешная отмена заявки на отгрузку")
    void successCancel() {
        cancelShipment()
            .andRespond(withSuccess());

        lomClient.cancelShipmentApplication(1L, 2L);
    }

    @Test
    @DisplayName("Заявка на отгрузку не найдена")
    void notFoundApplication() throws Exception {
        cancelShipment()
            .andRespond(withStatus(HttpStatus.NOT_FOUND)
                .body(extractFileContent("response/shipment/cancel/shipment_application_not_found.json"))
                .contentType(MediaType.APPLICATION_JSON)
            );

        softly.assertThatThrownBy(() -> lomClient.cancelShipmentApplication(1L, 2L))
            .isInstanceOf(HttpTemplateException.class)
            .hasFieldOrPropertyWithValue("statusCode", HttpStatus.NOT_FOUND.value())
            .hasFieldOrPropertyWithValue("body", "{\n"
                + "  \"message\": \"Failed to find [SHIPMENT_APPLICATION] with id [1]\",\n"
                + "  \"resourceType\": \"SHIPMENT_APPLICATION\",\n"
                + "  \"identifier\": \"1\"\n"
                + "}\n")
            .hasMessage(
                "Http request exception: status <404>, response body <{\n"
                    + "  \"message\": \"Failed to find [SHIPMENT_APPLICATION] with id [1]\",\n"
                    + "  \"resourceType\": \"SHIPMENT_APPLICATION\",\n"
                    + "  \"identifier\": \"1\"\n"
                    + "}\n"
                    + ">."
            );
    }

    @Test
    @DisplayName("Невалидный статус заявки на отгрузку")
    void wrongApplicationStatus() throws Exception {
        cancelShipment()
            .andRespond(withStatus(HttpStatus.BAD_REQUEST)
                .body(extractFileContent("response/shipment/cancel/shipment_application_wrong_status.json"))
                .contentType(MediaType.APPLICATION_JSON)
            );

        softly.assertThatThrownBy(() -> lomClient.cancelShipmentApplication(1L, 2L))
            .isInstanceOf(HttpTemplateException.class)
            .hasFieldOrPropertyWithValue("statusCode", HttpStatus.BAD_REQUEST.value())
            .hasFieldOrPropertyWithValue("body", "{\n"
                + "  \"message\": \"Cancellation of shipment application id=1 with status REGISTRY_SENT "
                + "and type WITHDRAW is not supported. Supported statuses: [NEW, CANCELLED].\"\n"
                + "}\n")
            .hasMessage(
                "Http request exception: status <400>, response body <{\n"
                    + "  \"message\": \"Cancellation of shipment application id=1 with status REGISTRY_SENT"
                    + " and type WITHDRAW is not supported. Supported statuses: [NEW, CANCELLED].\"\n"
                    + "}\n"
                    + ">."
            );
    }

    private ResponseActions cancelShipment() {
        return mock.expect(method(HttpMethod.DELETE))
            .andExpect(queryParam("marketIdFrom", "2"))
            .andExpect(requestTo(uri + "/shipments/1?marketIdFrom=2"));
    }
}
