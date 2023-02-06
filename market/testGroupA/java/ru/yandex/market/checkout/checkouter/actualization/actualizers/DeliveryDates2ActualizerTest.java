package ru.yandex.market.checkout.checkouter.actualization.actualizers;

import java.time.Clock;
import java.util.Date;

import org.apache.commons.lang3.time.DateUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import ru.yandex.market.checkout.checkouter.delivery.DeliveryDates;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.common.time.TestableClock;
import ru.yandex.market.checkout.pushapi.client.entity.DeliveryResponse;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * @author artemmz
 * @date 29.08.17.
 */
public class DeliveryDates2ActualizerTest {

    @InjectMocks
    private BuyerDeliveryDatesActualizer deliveryActualizer;
    @Spy
    private Clock clock = TestableClock.getInstance();
    @Mock
    private Order buyerCart;
    @Mock
    private DeliveryResponse buyerDelivery;
    @Mock
    private DeliveryDates buyerDeliveryDates;

    @BeforeEach
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        when(buyerCart.getDelivery()).thenReturn(buyerDelivery);
        when(buyerDelivery.getDeliveryDates()).thenReturn(buyerDeliveryDates);
    }

    @Test
    public void actualizeDeliveryDates_actual() throws Exception {
        Date fromDate = DateUtils.addDays(new Date(), 1);
        when(buyerDeliveryDates.getFromDate()).thenReturn(fromDate);
        when(buyerDeliveryDates.getToDate()).thenReturn(fromDate);
        assertTrue(deliveryActualizer.actualizeDeliveryDates(buyerCart).isSuccess());
    }

    @Test
    public void actualizeDeliveryDates_old() throws Exception {
        Date fromDateOld = DateUtils.addDays(new Date(), -1);
        when(buyerDeliveryDates.getFromDate()).thenReturn(fromDateOld);
        when(buyerDeliveryDates.getToDate()).thenReturn(fromDateOld);
        assertFalse(deliveryActualizer.actualizeDeliveryDates(buyerCart).isSuccess());
    }

    @Test
    public void actualizeDeliveryDates_to_date_older() throws Exception {
        Date fromDate = DateUtils.addDays(new Date(), 1);
        when(buyerDeliveryDates.getFromDate()).thenReturn(fromDate);
        when(buyerDeliveryDates.getToDate()).thenReturn(DateUtils.addHours(fromDate, -1));
        assertFalse(deliveryActualizer.actualizeDeliveryDates(buyerCart).isSuccess());
    }

    @Test
    public void actualizeDeliveryDates_empty_buyer_dates() throws Exception {
        when(buyerDelivery.getDeliveryDates()).thenReturn(null);
        assertTrue(deliveryActualizer.actualizeDeliveryDates(buyerCart).isSuccess());
    }

}
