package ru.yandex.market.logistics.lom.admin;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.jobs.model.QueueType;
import ru.yandex.market.logistics.lom.jobs.model.ShipmentApplicationIdPayload;
import ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.noContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

class ShipmentSendApplicationTest extends AbstractContextualTest {
    private static final ShipmentApplicationIdPayload PAYLOAD =
        PayloadFactory.createShipmentApplicationIdPayload(1L, 1L);

    @Test
    @DisplayName("Отгрузка не существует")
    void shipmentDoesNotExist() throws Exception {
        sendApplication()
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [SHIPMENT] with id [1]"));
    }

    @Test
    @DisplayName("У отгрузки нет заявки")
    @DatabaseSetup("/controller/admin/shipment/sendapplications/before/shipment.xml")
    void shipmentWithoutApplication() throws Exception {
        sendApplication()
            .andExpect(status().isBadRequest())
            .andExpect(errorMessage("Shipment 1 doesn't have application to send"));
    }

    @Test
    @DisplayName("Заявка отгрузки находится в необрабатываемом статусе")
    @DatabaseSetup("/controller/admin/shipment/sendapplications/before/shipment.xml")
    @DatabaseSetup("/controller/admin/shipment/sendapplications/before/shipment_application.xml")
    @DatabaseSetup(
        value = "/controller/admin/shipment/sendapplications/before/shipment_application_already_sent.xml",
        type = DatabaseOperation.REFRESH
    )
    void shipmentWithApplicationInWrongStatus() throws Exception {
        sendApplication()
            .andExpect(status().isBadRequest())
            .andExpect(errorMessage("Application in wrong status: CREATED. Expected status: NEW."));
    }

    @Test
    @DisplayName("Успешная отправка заявки в СД")
    @DatabaseSetup("/controller/admin/shipment/sendapplications/before/shipment.xml")
    @DatabaseSetup("/controller/admin/shipment/sendapplications/before/shipment_application.xml")
    void sendApplicationForDeliveryPartnerSuccess() throws Exception {
        sendApplication()
            .andExpect(status().isOk())
            .andExpect(noContent());
        queueTaskChecker.assertQueueTaskCreated(QueueType.DELIVERY_SERVICE_SHIPMENT_CREATION, PAYLOAD);
    }

    @Test
    @DisplayName("Успешная отправка заявки в СЦ")
    @DatabaseSetup("/controller/admin/shipment/sendapplications/before/shipment.xml")
    @DatabaseSetup("/controller/admin/shipment/sendapplications/before/shipment_application.xml")
    @DatabaseSetup(
        value = "/controller/admin/shipment/sendapplications/before/sorting_center_shipment.xml",
        type = DatabaseOperation.REFRESH
    )
    void sendApplicationForSortingCenterPartnerSuccess() throws Exception {
        sendApplication()
            .andExpect(status().isOk())
            .andExpect(noContent());
        queueTaskChecker.assertQueueTaskCreated(QueueType.FULFILLMENT_SHIPMENT_CREATION, PAYLOAD);
    }

    @Nonnull
    private ResultActions sendApplication() throws Exception {
        return mockMvc.perform(
            post("/admin/shipments/sendApplication")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"id\":1}")
        );
    }
}
