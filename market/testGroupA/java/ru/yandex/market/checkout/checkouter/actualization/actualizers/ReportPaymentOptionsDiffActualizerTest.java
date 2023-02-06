package ru.yandex.market.checkout.checkouter.actualization.actualizers;

import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.market.checkout.checkouter.actualization.model.PushApiCartResponse;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.pushapi.client.entity.DeliveryResponse;
import ru.yandex.market.common.report.model.ActualDelivery;
import ru.yandex.market.common.report.model.ActualDeliveryOption;
import ru.yandex.market.common.report.model.ActualDeliveryResult;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * @author zagidullinri
 * @date 07.04.2021
 */
public class ReportPaymentOptionsDiffActualizerTest {

    @InjectMocks
    private ReportPaymentOptionsDiffActualizer actualizer;
    @Mock
    private Order buyerCart;
    @Mock
    private OrderItem orderItem;
    @Mock
    private PushApiCartResponse cartResponse;
    @Mock
    private DeliveryResponse deliveryResponse;
    @Mock
    private ActualDelivery actualDelivery;
    @Mock
    private ActualDeliveryResult actualDeliveryResult;
    @Mock
    private ActualDeliveryOption actualDeliveryOption;

    @BeforeEach
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        when(buyerCart.getItems()).thenReturn(Collections.singletonList(orderItem));
        when(cartResponse.getDeliveryOptions()).thenReturn(Collections.singletonList(deliveryResponse));
        when(deliveryResponse.getType()).thenReturn(DeliveryType.DELIVERY);
        when(actualDelivery.getResults()).thenReturn(Collections.singletonList(actualDeliveryResult));
        when(actualDeliveryResult.getDelivery()).thenReturn(Collections.singletonList(actualDeliveryOption));
        when(actualDeliveryOption.getPaymentMethods()).thenReturn(Collections.singleton(
                PaymentMethod.CARD_ON_DELIVERY.toString()));
    }

    @Test
    public void shouldLogWhenThereIsDifference() throws Exception {
        when(deliveryResponse.getPaymentOptions()).thenReturn(Collections.emptySet());
        assertTrue(actualizer.logIfThereIsDifference(buyerCart, cartResponse, actualDelivery, false));
    }

    @Test
    public void shouldNotLogWhenThereIsNoDifference() throws Exception {
        when(deliveryResponse.getPaymentOptions()).thenReturn(Collections.singleton(PaymentMethod.CARD_ON_DELIVERY));
        assertFalse(actualizer.logIfThereIsDifference(buyerCart, cartResponse, actualDelivery, false));
    }
}
