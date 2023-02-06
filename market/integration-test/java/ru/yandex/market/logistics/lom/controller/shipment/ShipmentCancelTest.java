package ru.yandex.market.logistics.lom.controller.shipment;

import java.util.Optional;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultMatcher;

import ru.yandex.market.logistic.gateway.client.DeliveryClient;
import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.jobs.model.QueueType;
import ru.yandex.market.logistics.lom.jobs.processor.DeliveryServiceShipmentProcessingService;
import ru.yandex.market.logistics.lom.utils.LmsFactory;
import ru.yandex.market.logistics.management.client.LMSClient;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory.createShipmentApplicationIdPayload;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

class ShipmentCancelTest extends AbstractContextualTest {

    @Autowired
    private DeliveryServiceShipmentProcessingService processingService;

    @Autowired
    private DeliveryClient deliveryClient;

    @Autowired
    private LMSClient lmsClient;

    @Test
    @DisplayName("Отмена заявки")
    @DatabaseSetup("/controller/shipment/before/create_shipment_application_that_already_exists.xml")
    @ExpectedDatabase(
        value = "/controller/shipment/after/shipment_application_cancelled.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void testCancel() throws Exception {
        ShipmentTestUtil.cancelShipment(mockMvc, 1L, status().isOk(), 1L);

        softly.assertThat(backLogCaptor.getResults())
            .filteredOn(line -> line.contains("tags=BUSINESS_SHIPMENT_EVENT"))
            .isNotEmpty()
            .allMatch(line -> line.contains("level=INFO"
                + "\tformat=plain"
                + "\tcode=SHIPMENT_APPLICATION_CANCELLATION_SUCCESS"
                + "\tpayload=Shipment application cancelled"
                + "\trequest_id=1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd"
                + "\ttags=BUSINESS_SHIPMENT_EVENT"
                + "\tentity_types=platform,shipmentApplication"
                + "\tentity_values=platform:YANDEX_DELIVERY,shipmentApplication:1"
            ));
    }

    @Test
    @DisplayName("Не найдена заявка по идентификатору")
    @DatabaseSetup("/controller/shipment/before/create_shipment_application_that_already_exists.xml")
    void testCancelApplicationNotFound() throws Exception {
        cancelShipment(
            2L,
            status().isNotFound(),
            Matchers.equalToIgnoringCase("Failed to find [SHIPMENT_APPLICATION] with id [2]")
        );
    }

    @Test
    @DisplayName("Не найдена заявка по market-id отправителя")
    @DatabaseSetup("/controller/shipment/before/create_shipment_application_that_already_exists.xml")
    void testCancelApplicationNotFoundByMarketId() throws Exception {
        ShipmentTestUtil.cancelShipment(mockMvc, 1L, status().isNotFound(), 2L)
            .andExpect(
                jsonPath("message").value("Failed to find [SHIPMENT_APPLICATION] with id [1]")
            );
    }

    @Test
    @DisplayName("Неподдерживаемый статус для самопривоза")
    @DatabaseSetup("/controller/shipment/before/shipment_import_registry_sent.xml")
    void testCancelImportErrorStatus() throws Exception {
        cancelShipment(
            1L,
            status().isBadRequest(),
            Matchers.startsWith(
                "Cancellation of shipment application id=1 with status REGISTRY_SENT and type IMPORT is not supported."
            )
        );

        softly.assertThat(backLogCaptor.getResults())
            .filteredOn(line -> line.contains("tags=BUSINESS_SHIPMENT_EVENT"))
            .isNotEmpty()
            .allMatch(line -> line.contains("level=ERROR"
                + "\tformat=json-exception"
                + "\tcode=SHIPMENT_APPLICATION_CANCELLATION_ERROR"
            ));
    }

    @Test
    @DisplayName("Неподдерживаемый статус для забора")
    @DatabaseSetup("/controller/shipment/before/shipment_withdraw_registry_sent.xml")
    void testCancelWithdrawErrorStatus() throws Exception {
        cancelShipment(
            1L,
            status().isBadRequest(),
            Matchers.startsWith("Cancellation of shipment application id=1 with status REGISTRY_SENT "
                + "and type WITHDRAW is not supported.")
        );
    }

    @Test
    @DisplayName("Отправитель не задан")
    void testCancelNullMarketFrom() throws Exception {
        mockMvc.perform(
            delete("/shipments/1")
        )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("message").value("Required Long parameter 'marketIdFrom' is not present"));
    }

    @Test
    @DisplayName("Отмена заказа отменяет фоновую задачу отправки в СД")
    void testProcessingCancelledApplication() throws Exception {
        when(lmsClient.getPartner(3L)).thenReturn(Optional.of(LmsFactory.createPartnerResponse(3)));

        ShipmentTestUtil.createShipment(
            mockMvc,
            "create_shipment.json",
            status().isOk()
        );

        queueTaskChecker.assertQueueTaskCreated(
            QueueType.DELIVERY_SERVICE_SHIPMENT_CREATION,
            createShipmentApplicationIdPayload(1L, 1L)
        );

        ShipmentTestUtil.cancelShipment(mockMvc, 1L, status().isOk(), 1L);

        processingService.processPayload(createShipmentApplicationIdPayload(1L, 1L));

        verify(deliveryClient, never()).createIntake(any(), any(), any());
    }

    private void cancelShipment(long applicationId, ResultMatcher status, Matcher<String> message) throws Exception {
        ShipmentTestUtil.cancelShipment(mockMvc, applicationId, status, 1L)
            .andExpect(jsonPath("message").value(message));
    }

}
