package ru.yandex.market.logistics.lom.controller.order;

import java.util.Optional;
import java.util.stream.Stream;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.utils.LmsFactory;
import ru.yandex.market.logistics.management.client.LMSClient;

import static org.mockito.Mockito.when;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Связь заказов и отгрузок")
class OrderShipmentTest extends AbstractContextualTest {

    @Autowired
    private LMSClient lmsClient;

    @BeforeEach
    void mockLmsClient() {
        when(lmsClient.getPartner(102L)).thenReturn(Optional.of(
            LmsFactory.createPartnerResponse(102L, 101L)
        ));
    }

    @ParameterizedTest
    @MethodSource("insufficientShipmentDataProvider")
    @DisplayName("Создать черновик заказа, недостаточно данных отгрузки")
    @ExpectedDatabase(
        value = "/controller/order/after/order_without_shipment.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void createOrderNoShipment(String requestPath) throws Exception {
        OrderTestUtil.createOrder(mockMvc, requestPath)
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Создать черновик заказа, создание новой отгрузки для забора без склада назначения")
    @ExpectedDatabase(
        value = "/controller/order/after/withdraw_with_shipment.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void createOrderNewShipmentWithoutWarehouseTo() throws Exception {
        OrderTestUtil.createOrder(mockMvc, "controller/order/request/order_no_warehouse_to_id_withdraw.json")
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Создать черновик заказа, создание новой отгрузки")
    @ExpectedDatabase(
        value = "/controller/order/after/import_with_shipment.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void createOrderNewShipment() throws Exception {
        OrderTestUtil.createOrder(mockMvc, "controller/order/request/order_shipment.json")
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Создать черновик заказа, данные соответствуют существующей отгрузке")
    @DatabaseSetup("/controller/order/before/shipment_only.xml")
    @ExpectedDatabase(
        value = "/controller/order/after/import_with_shipment.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void createOrderExistingShipment() throws Exception {
        OrderTestUtil.createOrder(mockMvc, "controller/order/request/order_shipment.json")
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Создать черновик заказа, данные соответствуют существующей отгрузке c отправленными реестрами")
    @DatabaseSetup("/controller/order/before/shipment_with_no_new_orders_allowed.xml")
    @ExpectedDatabase(
        value = "/controller/order/before/shipment_with_no_new_orders_allowed.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void createOrderExistingSealedShipment() throws Exception {
        OrderTestUtil.createOrder(mockMvc, "controller/order/request/order_shipment.json")
            .andExpect(status().isBadRequest())
            .andExpect(jsonContent("controller/order/response/shipment_not_accepting_new_orders.json"));
    }

    @ParameterizedTest
    @MethodSource("insufficientShipmentDataProvider")
    @DisplayName("Обновить черновик заказа, удаление необходимых для отгрузки данных")
    @DatabaseSetup("/controller/order/before/order_with_shipment.xml")
    @ExpectedDatabase(
        value = "/controller/order/after/order_without_shipment.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void updateOrderRemoveShipment(String requestPath) throws Exception {
        OrderTestUtil.updateOrder(mockMvc, ORDER_ID, requestPath)
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Обновить черновик заказа, создание новой отгрузки для забора без склада назначения")
    @DatabaseSetup("/controller/order/before/order_without_shipment.xml")
    @ExpectedDatabase(
        value = "/controller/order/after/withdraw_with_shipment.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void updateOrderNewShipmentWithoutWarehouseTo() throws Exception {
        OrderTestUtil.updateOrder(mockMvc, ORDER_ID, "controller/order/request/order_no_warehouse_to_id_withdraw.json")
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Обновить черновик заказа, создание новой отгрузки")
    @DatabaseSetup("/controller/order/before/order_without_shipment.xml")
    @ExpectedDatabase(
        value = "/controller/order/after/import_with_shipment.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void updateOrderNewShipment() throws Exception {
        OrderTestUtil.updateOrder(mockMvc, ORDER_ID, "controller/order/request/order_shipment.json")
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Обновить черновик заказа, данные отгрузки неизменны")
    @DatabaseSetup("/controller/order/before/order_with_shipment.xml")
    @ExpectedDatabase(
        value = "/controller/order/after/import_with_shipment.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void updateOrderSameShipment() throws Exception {
        OrderTestUtil.updateOrder(mockMvc, ORDER_ID, "controller/order/request/order_shipment.json")
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Обновить черновик заказа, данные соответствуют другой отгрузке")
    @DatabaseSetup("/controller/order/before/order_with_different_shipment.xml")
    @ExpectedDatabase(
        value = "/controller/order/after/import_with_different_shipment.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void updateOrderOtherExistingShipment() throws Exception {
        OrderTestUtil.updateOrder(mockMvc, ORDER_ID, "controller/order/request/order_shipment.json")
            .andExpect(status().isOk());
    }

    private static Stream<Arguments> insufficientShipmentDataProvider() {
        return Stream.of(
            "controller/order/request/order_no_market_id_from.json",
            "controller/order/request/order_no_partner_id.json",
            "controller/order/request/order_no_shipment_date.json",
            "controller/order/request/order_no_shipment_type.json",
            "controller/order/request/order_no_warehouse_from_id.json",
            "controller/order/request/order_no_warehouse_to_id_import.json"
        ).map(Arguments::of);
    }

}
