package ru.yandex.market.checkout.checkouter.delivery;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.event.HistoryEventType;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderNotFoundException;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.request.RequestClientInfo;
import ru.yandex.market.checkout.common.rest.ErrorCodeException;
import ru.yandex.market.checkout.helpers.EventsGetHelper;
import ru.yandex.market.checkout.helpers.YandexMarketDeliveryHelper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.MOCK_DELIVERY_SERVICE_ID;

public class DeliveryVerificationCodeTest extends AbstractWebTestBase {

    @Autowired
    protected YandexMarketDeliveryHelper yandexMarketDeliveryHelper;
    protected Order order;
    @Autowired
    private EventsGetHelper eventsGetHelper;

    @BeforeEach
    public void setUp() {
        order = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryType(DeliveryType.PICKUP)
                .withColor(Color.BLUE)
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .build();
    }

    @Test
    public void updateDeliveryVerificationCode_shouldUpdateVerificationCode() {
        // Assign
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERY);
        final var requestClientInfo = new RequestClientInfo(ClientRole.SYSTEM, 0L);
        final var verificationCode = "12345";
        // Act
        client.updateDeliveryVerificationCode(order.getId(), verificationCode, requestClientInfo);
        // Assert
        order = orderService.getOrder(order.getId());
        assertEquals(verificationCode, order.getDelivery().getVerificationCode());
    }

    @Test
    public void updateDeliveryVerificationCode_shouldRaiseOrderDeliveryUpdatedEvent() throws Exception {
        // Assign
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERY);
        var requestClientInfo = new RequestClientInfo(ClientRole.SYSTEM, 0L);
        final var verificationCode = "12345";
        // Act
        client.updateDeliveryVerificationCode(order.getId(), verificationCode, requestClientInfo);
        // Assert
        var events = eventsGetHelper.getOrderHistoryEvents(order.getId());
        var orderHistoryEvent = events.getItems().stream()
                .filter(ohe -> ohe.getType() == HistoryEventType.ORDER_DELIVERY_UPDATED)
                .findAny()
                .orElse(null);
        assertNotNull(orderHistoryEvent);
    }

    @ParameterizedTest
    @NullAndEmptySource
    public void updateDeliveryVerificationCode_forNullOrEmptyVerificationCode_shouldThrow400CodeException(
            String invalidVerificationCode) {
        // Assign
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERY);
        var requestClientInfo = new RequestClientInfo(ClientRole.SYSTEM, 0L);
        // Assert
        var e = assertThrows(ErrorCodeException.class, () ->
                // Act
                client.updateDeliveryVerificationCode(order.getId(), invalidVerificationCode, requestClientInfo));
        assertEquals("Missing verificationCode", e.getMessage());
        assertEquals(400, e.getStatusCode());
    }

    @Test
    public void updateDeliveryVerificationCode_forNotExistingOrderId_shouldThrowOrderNotFoundException() {
        // Assign
        var requestClientInfo = new RequestClientInfo(ClientRole.SYSTEM, 0L);
        final var verificationCode = "12345";
        final var orderId = 999;
        // Assert
        assertThrows(OrderNotFoundException.class, () ->
                // Act
                client.updateDeliveryVerificationCode(orderId, verificationCode, requestClientInfo));
    }

    @Test
    public void getDeliveryVerificationCode_shouldReturnVerificationCode() {
        // Assign
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERY);
        final var requestClientInfo = new RequestClientInfo(ClientRole.SYSTEM, 0L);
        final var verificationCode = "12345";
        client.updateDeliveryVerificationCode(order.getId(), verificationCode, requestClientInfo);
        // Act
        var returnedVerificationCode = client.getDeliveryVerificationCode(order.getId(), requestClientInfo);
        // Assert
        assertEquals(verificationCode, returnedVerificationCode);
    }

    @Test
    public void getDeliveryVerificationCode_forNotExistingOrderId_shouldThrowOrderNotFoundException() {
        // Assign
        var requestClientInfo = new RequestClientInfo(ClientRole.SYSTEM, 0L);
        final var orderId = 999;
        // Assert
        assertThrows(OrderNotFoundException.class, () ->
                // Act
                client.getDeliveryVerificationCode(orderId, requestClientInfo));
    }

    @Test
    public void getDeliveryVerificationCode_forOrderWithoutCode_shouldReturnNull() {
        // Assign
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERY);
        final var requestClientInfo = new RequestClientInfo(ClientRole.SYSTEM, 0L);
        // Act
        var returnedVerificationCode = client.getDeliveryVerificationCode(order.getId(), requestClientInfo);
        // Assert
        assertNull(returnedVerificationCode);
    }

    @Test
    public void getDeliveryVerificationCodeData_shouldReturnVerificationCodeAndBarcodeData() {
        // Assign
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERY);
        final var requestClientInfo = new RequestClientInfo(ClientRole.SYSTEM, 0L);
        final var verificationCode = "12345";
        final var verificationBarcodeData = order.getId() + "-" + verificationCode;
        client.updateDeliveryVerificationCode(order.getId(), verificationCode, requestClientInfo);
        // Act
        var returnedVerificationCodeData = client.getDeliveryVerificationCodeData(
                order.getId(), requestClientInfo);
        // Assert
        assertEquals(verificationCode, returnedVerificationCodeData.getVerificationCode());
        assertEquals(verificationBarcodeData, returnedVerificationCodeData.getBarcodeData());
    }

    @Test
    public void getDeliveryVerificationCodeData_forNotExistingOrderId_shouldThrowOrderNotFoundException() {
        // Assign
        var requestClientInfo = new RequestClientInfo(ClientRole.SYSTEM, 0L);
        final var orderId = 999;
        // Assert
        assertThrows(OrderNotFoundException.class, () ->
                // Act
                client.getDeliveryVerificationCodeData(orderId, requestClientInfo));
    }

    @Test
    public void getDeliveryVerificationCodeData_forOrderWithoutCode_shouldReturnResponseWithNulls() {
        // Assign
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERY);
        final var requestClientInfo = new RequestClientInfo(ClientRole.SYSTEM, 0L);
        // Act
        var returnedVerificationCodeData = client.getDeliveryVerificationCodeData(
                order.getId(), requestClientInfo);
        // Assert
        assertNotNull(returnedVerificationCodeData);
        assertNull(returnedVerificationCodeData.getVerificationCode());
        assertNull(returnedVerificationCodeData.getBarcodeData());
    }
}
