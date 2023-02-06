package ru.yandex.market.logistics.nesu.controller.shipment;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistics.lom.client.LomClient;
import ru.yandex.market.logistics.nesu.AbstractContextualTest;
import ru.yandex.market.logistics.util.client.exception.HttpTemplateException;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Отмена заявки на отгрузку")
@DatabaseSetup("/repository/shipments/database_prepare.xml")
class ShipmentCancelTest extends AbstractContextualTest {

    @Autowired
    private LomClient lomClient;

    @Test
    @DisplayName("Успешная отмена заявки на отгрузку")
    void successShipmentApplicationCancel() throws Exception {
        cancelShipmentApplication(1L)
            .andExpect(status().isOk());

        verify(lomClient).cancelShipmentApplication(eq(1L), eq(100L));
    }

    @Test
    @DisplayName("Не найдена заявка")
    void shipmentApplicationNotFound() throws Exception {
        doThrow(new HttpTemplateException(HttpStatus.SC_NOT_FOUND, "{\n"
            + "  \"message\": \"Failed to find [SHIPMENT_APPLICATION] with ids [1]\"\n"
            + "}\n"))
            .when(lomClient).cancelShipmentApplication(eq(1L), eq(100L));

        cancelShipmentApplication(1L)
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [SHIPMENT_APPLICATION] with ids [1]"));
    }

    @Test
    @DisplayName("Заявка в неподходящем статусе")
    void shipmentApplicationWrongStatus() throws Exception {
        doThrow(new HttpTemplateException(HttpStatus.SC_BAD_REQUEST, "{\n"
            + "  \"message\": \"Cancellation of shipment application id=1 with status REGISTRY_SENT "
            + "and type WITHDRAW is not supported. Supported statuses: [NEW, CANCELLED].\"\n"
            + "}\n"))
            .when(lomClient).cancelShipmentApplication(eq(1L), eq(100L));

        cancelShipmentApplication(1L)
            .andExpect(status().isBadRequest())
            .andExpect(errorMessage("Cancellation of shipment application id=1 with"
                + " status REGISTRY_SENT and type WITHDRAW is not supported. Supported statuses: [NEW, CANCELLED]."));
    }

    private ResultActions cancelShipmentApplication(long applicationId) throws Exception {
        return mockMvc.perform(
            delete("/back-office/shipments/" + applicationId)
                .param("shopId", "1")
                .param("userId", "1")
        );
    }

}
