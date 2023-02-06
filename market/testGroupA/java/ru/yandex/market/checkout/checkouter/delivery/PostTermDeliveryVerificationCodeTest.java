package ru.yandex.market.checkout.checkouter.delivery;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.request.RequestClientInfo;
import ru.yandex.market.checkout.test.providers.ActualDeliveryProvider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.MOCK_POST_TERM_DELIVERY_SERVICE_ID;

public class PostTermDeliveryVerificationCodeTest extends DeliveryVerificationCodeTest {

    @BeforeEach
    @Override
    public void setUp() {
        order = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryType(DeliveryType.PICKUP)
                .withColor(Color.BLUE)
                .withDeliveryServiceId(MOCK_POST_TERM_DELIVERY_SERVICE_ID)
                .withActualDelivery(ActualDeliveryProvider.builder()
                        .addPostTerm(MOCK_POST_TERM_DELIVERY_SERVICE_ID)
                        .build())
                .build();
    }

    @Test
    @Override
    public void getDeliveryVerificationCodeData_shouldReturnVerificationCodeAndBarcodeData() {
        // Assign
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERY);
        final var requestClientInfo = new RequestClientInfo(ClientRole.SYSTEM, 0L);
        final var verificationCode = "12345";
        client.updateDeliveryVerificationCode(order.getId(), verificationCode, requestClientInfo);
        // Act
        var returnedVerificationCodeData = client.getDeliveryVerificationCodeData(order.getId(), requestClientInfo);
        // Assert
        assertEquals(verificationCode, returnedVerificationCodeData.getVerificationCode());
        assertNull(returnedVerificationCodeData.getBarcodeData());
    }
}
