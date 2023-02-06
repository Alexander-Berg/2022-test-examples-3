package ru.yandex.market.checkout.checkouter.actualization.actualizers;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.market.checkout.checkouter.actualization.model.PushApiCartResponse;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.log.cart.CartDiff;
import ru.yandex.market.checkout.checkouter.log.cart.CartDiffDetails;
import ru.yandex.market.checkout.checkouter.log.cart.CartLoggingEvent;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.pushapi.client.entity.DeliveryResponse;
import ru.yandex.market.common.report.model.ActualDelivery;
import ru.yandex.market.common.report.model.ActualDeliveryOption;
import ru.yandex.market.common.report.model.ActualDeliveryResult;
import ru.yandex.market.common.report.model.PickupOption;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * @author zagidullinri
 * @date 06.04.2021
 */
public class ReportDeliveryOptionsDiffActualizerTest {

    @InjectMocks
    private ReportDeliveryOptionsDiffActualizer actualizer;
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
    @Mock
    private PickupOption pickupOption;

    @BeforeEach
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        when(buyerCart.getItems()).thenReturn(Collections.singletonList(orderItem));
        when(cartResponse.getDeliveryOptions()).thenReturn(Collections.singletonList(deliveryResponse));
        when(deliveryResponse.getType()).thenReturn(DeliveryType.DELIVERY);
        when(actualDelivery.getResults()).thenReturn(Collections.singletonList(actualDeliveryResult));
        when(actualDeliveryResult.getDelivery()).thenReturn(Collections.singletonList(actualDeliveryOption));
    }

    @Test
    public void pushApiLessOptionsTest() {
        when(actualDeliveryResult.getPost()).thenReturn(Collections.singletonList(pickupOption));
        assertEquals(
                new CartDiff(
                        CartLoggingEvent.REPORT_PUSHAPI_DELIVERY_OPTIONS,
                        CartDiffDetails.builder()
                                .withDeliveryTypes(List.of(DeliveryType.DELIVERY, DeliveryType.POST))
                                .build(),
                        CartDiffDetails.builder()
                                .withDeliveryTypes(List.of(DeliveryType.DELIVERY))
                                .build()
                ),
                actualizer.findDiff(buyerCart, cartResponse, actualDelivery, false).orElse(null)
        );
    }

    @Test
    public void optionsEqualsTest() {
        when(actualDeliveryResult.getPost()).thenReturn(Collections.emptyList());
        assertTrue(actualizer.findDiff(buyerCart, cartResponse, actualDelivery, false).isEmpty());
    }

    @Test
    public void pushApiMoreOptions() {
        when(actualDeliveryResult.getPost()).thenReturn(Collections.emptyList());
        when(actualDeliveryResult.getDelivery()).thenReturn(Collections.emptyList());
        assertTrue(actualizer.findDiff(buyerCart, cartResponse, actualDelivery, false).isEmpty());
    }
}
