package ru.yandex.market.checkout.checkouter.actualization.actualizers;

import java.util.Collections;
import java.util.Date;

import org.apache.commons.lang3.time.DateUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryDates;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.pushapi.client.entity.DeliveryResponse;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * @author artemmz
 * @date 25.09.17.
 */
public class DeliveryDatesActualizerTest {

    private static final Date BUYER_DATE = new Date();

    @InjectMocks
    private DeliveryDatesActualizer deliveryDatesActualizer;
    @Mock
    private Order buyerCart;
    @Mock
    private DeliveryResponse shopDelivery;
    @Mock
    private Delivery buyerDelivery;
    @Mock
    private DeliveryDates buyerDates;
    @Mock
    private DeliveryDates shopDates;

    @BeforeEach
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        when(buyerCart.getDelivery()).thenReturn(buyerDelivery);
        when(buyerDelivery.getDeliveryDates()).thenReturn(buyerDates);
        when(buyerDates.getToDate()).thenReturn(BUYER_DATE);

        when(shopDelivery.getType()).thenReturn(DeliveryType.DELIVERY);
        when(shopDelivery.getDeliveryDates()).thenReturn(shopDates);
    }

    @Test
    public void actual() throws Exception {
        when(shopDates.getToDate()).thenReturn(BUYER_DATE);
        assertTrue(deliveryDatesActualizer.logIfNotActual(buyerCart, Collections.singletonList(shopDelivery), false));
    }

    @Test
    public void notActual() throws Exception {
        when(shopDates.getToDate()).thenReturn(DateUtils.addDays(BUYER_DATE, 1));
        assertFalse(deliveryDatesActualizer.logIfNotActual(buyerCart, Collections.singletonList(shopDelivery), false));
    }

}
