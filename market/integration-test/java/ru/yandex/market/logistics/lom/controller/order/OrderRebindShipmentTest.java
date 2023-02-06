package ru.yandex.market.logistics.lom.controller.order;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logistics.lom.AbstractContextualTest;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

class OrderRebindShipmentTest extends AbstractContextualTest {

    @Test
    @DatabaseSetup("/controller/order/before/order_with_shipment_and_other_one.xml")
    @DisplayName("Успешная перепривязка (отмененные заказы отвязываются от отгрузки)")
    @ExpectedDatabase(
        value = "/controller/admin/order/after/order_with_shipment_all_rebound.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void rebindShipment() throws Exception {
        OrderTestUtil
            .rebindOrder(mockMvc, "controller/order/request/rebind_order.json")
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Неуспешная перепривязка: не найдена отгрузка")
    @DatabaseSetup("/controller/order/before/order_with_shipment_and_other_one.xml")
    @ExpectedDatabase(
        value = "/controller/order/before/order_with_shipment_and_other_one.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void rebindShipmentFailNotFoundShipment() throws Exception {
        OrderTestUtil.rebindOrder(
            mockMvc,
            "controller/order/request/rebind_order_fail_404.json"
        )
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("message").value(
                "Failed to find [SHIPMENT] with id [4]"));
    }

    @Test
    @DatabaseSetup("/controller/order/before/order_with_shipment_with_sent_application.xml")
    @DisplayName("Неуспешная перепривязка: заказ нельзя отвязать от отгрузки")
    @ExpectedDatabase(
        value = "/controller/order/before/order_with_shipment_with_sent_application.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void rebindShipmentFailRebindError() throws Exception {
        OrderTestUtil.rebindOrder(
            mockMvc,
            "controller/order/request/rebind_order.json"
        )
            .andExpect(status().isBadRequest())
            .andExpect(errorMessage("Failed to process shipment request change"
                + " ShipmentChangeRequestDto(sourceShipments=[1], targetShipment=2),"
                + " following errors ocurred:"
                + " [ShipmentId=1, error=ru.yandex.market.logistics.lom.exception.http.base.BadRequestException:"
                + " Cannot change shipment of order id = 1. All of the following conditions must be satisfied:\n"
                + "order shipment exists [true]"
                + " and shipment application is not in REGISTRY_SENT status [false],"
                + " ShipmentId=1, error=ru.yandex.market.logistics.lom.exception.http.base.BadRequestException:"
                + " Cannot change shipment of order id = 2. All of the following conditions must be satisfied:\n"
                + "order shipment exists [true]"
                + " and shipment application is not in REGISTRY_SENT status [false]]"
            ));
    }

    @Test
    @DatabaseSetup("/controller/order/before/order_with_shipment_different_partners.xml")
    @DisplayName("Неуспешная перепривязка: разные партнёры")
    @ExpectedDatabase(
        value = "/controller/order/before/order_with_shipment_different_partners.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void rebindShipmentFailRebindErrorDifferentPartners() throws Exception {
        OrderTestUtil.rebindOrder(
            mockMvc,
            "controller/order/request/rebind_order.json"
        )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("message").value(
                "Cannot rebind orders from shipment 1 to 2. All following conditions have to be satisfied:\n" +
                    "Shipments belong to same partner relation (false)\n" +
                    "Target shipment application is not in REGISTRY_SENT status (true)"));

    }

    @Test
    @DatabaseSetup("/controller/order/before/order_with_shipment_and_other_one.xml")
    @DisplayName("Неуспешная перепривязка: неполный запрос")
    @ExpectedDatabase(
        value = "/controller/order/before/order_with_shipment_and_other_one.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void rebindShipmentFailRebindErrorFaultyDto() throws Exception {
        OrderTestUtil.rebindOrder(
            mockMvc,
            "controller/order/request/rebind_order_faulty_dto.json"
        )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("message").value(
                "Following validation errors occurred:\n" +
                    "Field: 'sourceShipments', message: 'must not be empty'\n" +
                    "Field: 'targetShipment', message: 'must not be null'"));

    }
}
