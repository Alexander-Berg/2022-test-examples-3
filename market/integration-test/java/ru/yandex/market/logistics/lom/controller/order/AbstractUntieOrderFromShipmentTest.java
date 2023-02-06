package ru.yandex.market.logistics.lom.controller.order;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.util.client.tvm.client.TvmClientApi;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static ru.yandex.market.logistics.lom.controller.order.TvmClientApiTestUtil.USER_UID;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Отвязывание заказа от отгрузки")
public abstract class AbstractUntieOrderFromShipmentTest extends AbstractContextualTest {

    @Autowired
    private TvmClientApi tvmClientApi;

    @Test
    @DisplayName("Успешно отвязать заказ от отгрузки. Заказ в ошибочном статусе")
    @DatabaseSetup(
        value = "/controller/admin/order/before/untie-from-shipment-processing-error-status.xml",
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/controller/admin/order/after/untie-from-shipment-success.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void untieFromShipmentSuccessOrderStatus() throws Exception {
        TvmClientApiTestUtil.mockTvmClientApi(tvmClientApi);
        untieOrderFromShipment(1L)
            .andExpect(status().isOk())
            .andExpect(content().string(""));
        OrderHistoryTestUtil.assertOrderHistoryEvent(jdbcTemplate, ORDER_ID, USER_UID, null);
    }

    @Test
    @DisplayName("Успешно отвязать заказ от отгрузки. Заказ отменён")
    @DatabaseSetup(
        value = "/controller/admin/order/before/untie-from-shipment-cancelled-status.xml",
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/controller/admin/order/after/untie-from-shipment-success.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void untieFromShipmentSuccessCancelledOrderStatus() throws Exception {
        TvmClientApiTestUtil.mockTvmClientApi(tvmClientApi);
        untieOrderFromShipment(1L)
            .andExpect(status().isOk())
            .andExpect(content().string(""));
        OrderHistoryTestUtil.assertOrderHistoryEvent(jdbcTemplate, ORDER_ID, USER_UID, null);
    }

    @Test
    @DisplayName("Успешно отвязать заказ от отгрузки с юзер-тикетом")
    @DatabaseSetup(
        value = "/controller/admin/order/before/untie-from-shipment-processing-error-status.xml",
        type = DatabaseOperation.UPDATE
    )
    void untieFromShipmentWithUserTicket() throws Exception {
        TvmClientApiTestUtil.mockTvmClientApi(tvmClientApi);
        untieOrderFromShipment(1L).andExpect(status().isOk());
        OrderHistoryTestUtil.assertOrderHistoryEvent(jdbcTemplate, ORDER_ID, USER_UID, null);
    }

    @Test
    @DisplayName("Успешно отвязать заказ от отгрузки. У заказа нет идентификатора в трекере")
    @DatabaseSetup(
        value = "/controller/admin/order/before/untie-from-shipment-no-tracker-id.xml",
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/controller/admin/order/after/untie-from-shipment-success.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void untieFromShipmentSuccessNoTrackerId() throws Exception {
        TvmClientApiTestUtil.mockTvmClientApi(tvmClientApi);
        untieOrderFromShipment(1L)
            .andExpect(status().isOk())
            .andExpect(content().string(""));
        OrderHistoryTestUtil.assertOrderHistoryEvent(jdbcTemplate, ORDER_ID, USER_UID, null);
    }

    @Test
    @DisplayName("Ошибка отвязывания заказа от отгрузки")
    @ExpectedDatabase(
        value = "/controller/admin/order/after/untie-from-shipment-error.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void untieFromShipmentError() throws Exception {
        untieOrderFromShipment(1L)
            .andExpect(status().isBadRequest())
            .andExpect(errorMessage(
                "Order id = 1 cannot be untied from shipment. " +
                    "All of the following conditions must be satisfied:\n" +
                    "(order status is in [PROCESSING_ERROR, CANCELLED] [false]"
                    + " or order is not tracked by Delivery Tracker [false])"
                    + " and order shipment exists [true]"
                    + " and shipment application is not in REGISTRY_SENT status [true]"
            ));
    }

    @Test
    @DisplayName("Ошибка отвязывания заказа от отгрузки. Заявка в неподходящем статусе")
    @DatabaseSetup(
        value = "/controller/admin/order/before/untie-from-shipment-cancelled-status.xml",
        type = DatabaseOperation.UPDATE
    )
    @DatabaseSetup(
        value = "/controller/admin/order/before/untie-from-shipment-no-tracker-id.xml",
        type = DatabaseOperation.UPDATE
    )
    @DatabaseSetup(
        value = "/controller/admin/order/before/untie-from-shipment-application-invalid-status.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/controller/admin/order/after/untie-from-shipment-error.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void untieFromShipmentErrorShipmentApplicationInvalidStatus() throws Exception {
        untieOrderFromShipment(1L)
            .andExpect(status().isBadRequest())
            .andExpect(errorMessage(
                "Order id = 1 cannot be untied from shipment. " +
                    "All of the following conditions must be satisfied:\n" +
                    "(order status is in [PROCESSING_ERROR, CANCELLED] [true]"
                    + " or order is not tracked by Delivery Tracker [true])"
                    + " and order shipment exists [true]"
                    + " and shipment application is not in REGISTRY_SENT status [false]"
            ));
    }

    @Test
    @DisplayName("Ошибка отвязывания заказа от отгрузки. Заказ не найден")
    void untieFromShipmentErrorOrderNotFound() throws Exception {
        untieOrderFromShipment(2L)
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("message").value("Failed to find [ORDER] with id [2]"));
    }

    @Nonnull
    public abstract ResultActions untieOrderFromShipment(long orderId) throws Exception;
}
