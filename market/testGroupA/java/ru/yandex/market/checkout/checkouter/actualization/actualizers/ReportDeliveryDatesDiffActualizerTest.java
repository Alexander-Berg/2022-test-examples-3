package ru.yandex.market.checkout.checkouter.actualization.actualizers;

import java.util.Calendar;
import java.util.Collections;
import java.util.Date;

import org.apache.commons.lang3.time.DateUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.market.checkout.checkouter.actualization.model.PushApiCartResponse;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryDates;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * @author zagidullinri
 * @date 05.04.2021
 */
public class ReportDeliveryDatesDiffActualizerTest {
    private static final Date TODAY = DateUtils.truncate(new Date(), Calendar.DATE);
    private static final Date DELIVERY_TO_DATE = DateUtils.addDays(TODAY, 5);
    private static final Date DELIVERY_FROM_DATE = DateUtils.addDays(TODAY, 3);

    @InjectMocks
    private ReportDeliveryDatesDiffActualizer actualizer;
    @Mock
    private Order buyerCart;
    @Mock
    private OrderItem orderItem;
    @Mock
    private PushApiCartResponse cartResponse;
    @Mock
    private DeliveryResponse deliveryResponse;
    @Mock
    private DeliveryDates deliveryDates;
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
        when(deliveryResponse.getDeliveryServiceId()).thenReturn(100L);
        when(deliveryResponse.getType()).thenReturn(DeliveryType.DELIVERY);
        when(deliveryResponse.getDeliveryDates()).thenReturn(deliveryDates);
        when(actualDelivery.getResults()).thenReturn(Collections.singletonList(actualDeliveryResult));
        when(actualDeliveryResult.getDelivery()).thenReturn(Collections.singletonList(actualDeliveryOption));
        when(actualDeliveryOption.getDeliveryServiceId()).thenReturn(100L);
        when(actualDeliveryOption.getDayFrom()).thenReturn(3);
        when(actualDeliveryOption.getDayTo()).thenReturn(5);
        when(deliveryDates.getToDate()).thenReturn(DELIVERY_TO_DATE);
    }

    @Test
    public void findDiffTest() {
        when(deliveryDates.getFromDate()).thenReturn(DateUtils.addDays(DELIVERY_FROM_DATE, 1));
        assertEquals(
                new CartDiff(
                        CartLoggingEvent.REPORT_PUSHAPI_DELIVERY_DATES,
                        CartDiffDetails.builder()
                                .withDeliveryDateFrom(DELIVERY_FROM_DATE)
                                .withDeliveryDateTo(DELIVERY_TO_DATE)
                                .build(),
                        CartDiffDetails.builder()
                                .withDeliveryDateFrom(DateUtils.addDays(DELIVERY_FROM_DATE, 1))
                                .withDeliveryDateTo(DELIVERY_TO_DATE)
                                .build()
                ),
                actualizer.findDiff(buyerCart, cartResponse, actualDelivery, false).orElse(null)
        );
    }

    @Test
    public void shopsDateToIsGreaterThenReportsDateToTest() {
        when(deliveryDates.getFromDate()).thenReturn(DELIVERY_FROM_DATE);
        when(deliveryDates.getToDate()).thenReturn(DateUtils.addDays(DELIVERY_TO_DATE, 1));
        assertTrue(actualizer.findDiff(buyerCart, cartResponse, actualDelivery, false).isEmpty());
    }

    @Test
    public void diffEmptyTest() {
        when(deliveryDates.getFromDate()).thenReturn(DELIVERY_FROM_DATE);
        assertTrue(actualizer.findDiff(buyerCart, cartResponse, actualDelivery, false).isEmpty());
    }

    @Test
    public void dateFromIsNullTest() {
        assertTrue(actualizer.findDiff(buyerCart, cartResponse, actualDelivery, false).isEmpty());
    }
}
