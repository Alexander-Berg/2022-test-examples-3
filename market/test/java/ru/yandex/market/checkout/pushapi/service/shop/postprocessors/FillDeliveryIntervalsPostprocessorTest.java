package ru.yandex.market.checkout.pushapi.service.shop.postprocessors;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.common.util.date.DateUtil;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryDates;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType;
import ru.yandex.market.checkout.checkouter.delivery.RawDeliveryInterval;
import ru.yandex.market.checkout.checkouter.delivery.RawDeliveryIntervalsCollection;
import ru.yandex.market.checkout.pushapi.client.entity.Cart;
import ru.yandex.market.checkout.pushapi.client.entity.CartResponse;
import ru.yandex.market.checkout.pushapi.client.entity.DeliveryResponse;
import ru.yandex.market.checkout.pushapi.service.EnvironmentService;
import ru.yandex.market.checkout.pushapi.service.shop.CartContext;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.market.checkout.pushapi.service.shop.postprocessors.FillDeliveryIntervalsPostprocessor.FILL_DBS_DELIVERY_INTERVALS_FLAG;

public class FillDeliveryIntervalsPostprocessorTest {

    private static final LocalTime AM_9 = LocalTime.of(9, 0);
    private static final LocalTime PM_6 = LocalTime.of(18, 0);
    private static final LocalTime PM_9 = LocalTime.of(21, 0);

    @Test
    void testFromDateLessToDate() {
        EnvironmentService environmentService = mock(EnvironmentService.class);
        when(environmentService.getBooleanValueOrDefault(FILL_DBS_DELIVERY_INTERVALS_FLAG, false)).thenReturn(true);

        FillDeliveryIntervalsPostprocessor postprocessor = new FillDeliveryIntervalsPostprocessor(environmentService);

        Delivery delivery = new Delivery();
        delivery.setDeliveryPartnerType(DeliveryPartnerType.SHOP);

        Cart cart = new Cart();
        cart.setDelivery(delivery);
        CartContext cartContext = new CartContext(123);

        CartResponse cartResponse = new CartResponse();

        LocalDate fromDate = LocalDate.of(2022, 6, 1);
        LocalDate toDate = LocalDate.of(2022, 6, 2);

        DeliveryResponse firstDeliveryOption = new DeliveryResponse();
        firstDeliveryOption.setDeliveryDates(new DeliveryDates(DateUtil.asDate(fromDate), DateUtil.asDate(toDate)));

        fromDate = LocalDate.of(2022, 6, 10);
        toDate = LocalDate.of(2022, 6, 15);

        DeliveryResponse secondDeliveryOption = new DeliveryResponse();
        secondDeliveryOption.setDeliveryDates(new DeliveryDates(DateUtil.asDate(fromDate), DateUtil.asDate(toDate)));

        cartResponse.setDeliveryOptions(List.of(firstDeliveryOption, secondDeliveryOption));

        postprocessor.process(cart, cartResponse, cartContext);

        Assertions.assertEquals(2, cartResponse.getDeliveryOptions().size());

        RawDeliveryIntervalsCollection firstExpectedIntervals = new RawDeliveryIntervalsCollection();
        LocalDate expectedFromDate = LocalDate.of(2022, 6, 1);
        firstExpectedIntervals.add(new RawDeliveryInterval(DateUtil.asDate(expectedFromDate), AM_9, PM_9));
        firstExpectedIntervals.add(new RawDeliveryInterval(DateUtil.asDate(expectedFromDate.plusDays(1)), AM_9, PM_9));

        RawDeliveryIntervalsCollection firstDeliveryIntervals = cartResponse
                .getDeliveryOptions().get(0)
                .getRawDeliveryIntervals();

        Assertions.assertEquals(firstExpectedIntervals, firstDeliveryIntervals);

        RawDeliveryIntervalsCollection secondExpectedIntervals = new RawDeliveryIntervalsCollection();
        expectedFromDate = LocalDate.of(2022, 6, 10);
        secondExpectedIntervals.add(new RawDeliveryInterval(DateUtil.asDate(expectedFromDate), AM_9, PM_9));
        secondExpectedIntervals.add(new RawDeliveryInterval(DateUtil.asDate(expectedFromDate.plusDays(1)), AM_9,PM_9));
        secondExpectedIntervals.add(new RawDeliveryInterval(DateUtil.asDate(expectedFromDate.plusDays(2)), AM_9,PM_9));
        secondExpectedIntervals.add(new RawDeliveryInterval(DateUtil.asDate(expectedFromDate.plusDays(3)), AM_9,PM_9));
        secondExpectedIntervals.add(new RawDeliveryInterval(DateUtil.asDate(expectedFromDate.plusDays(4)), AM_9,PM_9));
        secondExpectedIntervals.add(new RawDeliveryInterval(DateUtil.asDate(expectedFromDate.plusDays(5)), AM_9,PM_9));

        RawDeliveryIntervalsCollection secondDeliveryIntervals = cartResponse
                .getDeliveryOptions().get(1)
                .getRawDeliveryIntervals();

        Assertions.assertEquals(secondExpectedIntervals, secondDeliveryIntervals);
    }

    @Test
    void testFromDateGreaterToDate() {
        EnvironmentService environmentService = mock(EnvironmentService.class);
        when(environmentService.getBooleanValueOrDefault(FILL_DBS_DELIVERY_INTERVALS_FLAG, false)).thenReturn(true);

        FillDeliveryIntervalsPostprocessor postprocessor = new FillDeliveryIntervalsPostprocessor(environmentService);

        Delivery delivery = new Delivery();
        delivery.setDeliveryPartnerType(DeliveryPartnerType.SHOP);

        Cart cart = new Cart();
        cart.setDelivery(delivery);
        CartContext cartContext = new CartContext(123);

        CartResponse cartResponse = new CartResponse();

        LocalDate fromDate = LocalDate.of(2022, 6, 10);
        LocalDate toDate = LocalDate.of(2022, 6, 2);

        DeliveryResponse deliveryOption = new DeliveryResponse();
        deliveryOption.setDeliveryDates(new DeliveryDates(DateUtil.asDate(fromDate), DateUtil.asDate(toDate)));

        cartResponse.setDeliveryOptions(List.of(deliveryOption));

        postprocessor.process(cart, cartResponse, cartContext);

        Assertions.assertEquals(1, cartResponse.getDeliveryOptions().size());
        Assertions.assertTrue(cartResponse.getDeliveryOptions().get(0).getRawDeliveryIntervals().isEmpty());
    }

    @Test
    void testFromDateEqualsToDate() {
        EnvironmentService environmentService = mock(EnvironmentService.class);
        when(environmentService.getBooleanValueOrDefault(FILL_DBS_DELIVERY_INTERVALS_FLAG, false)).thenReturn(true);

        FillDeliveryIntervalsPostprocessor postprocessor = new FillDeliveryIntervalsPostprocessor(environmentService);

        Delivery delivery = new Delivery();
        delivery.setDeliveryPartnerType(DeliveryPartnerType.SHOP);

        Cart cart = new Cart();
        cart.setDelivery(delivery);
        CartContext cartContext = new CartContext(123);

        CartResponse cartResponse = new CartResponse();

        LocalDate fromDate = LocalDate.of(2022, 6, 1);
        LocalDate toDate = LocalDate.of(2022, 6, 1);

        DeliveryResponse deliveryOption = new DeliveryResponse();
        deliveryOption.setDeliveryDates(new DeliveryDates(DateUtil.asDate(fromDate), DateUtil.asDate(toDate)));

        cartResponse.setDeliveryOptions(List.of(deliveryOption));

        postprocessor.process(cart, cartResponse, cartContext);

        Assertions.assertEquals(1, cartResponse.getDeliveryOptions().size());
        Assertions.assertTrue(cartResponse.getDeliveryOptions().get(0).getRawDeliveryIntervals().isEmpty());
    }

    @Test
    void testHasIntervals() {
        EnvironmentService environmentService = mock(EnvironmentService.class);
        when(environmentService.getBooleanValueOrDefault(FILL_DBS_DELIVERY_INTERVALS_FLAG, false)).thenReturn(true);

        FillDeliveryIntervalsPostprocessor postprocessor = new FillDeliveryIntervalsPostprocessor(environmentService);

        Delivery delivery = new Delivery();
        delivery.setDeliveryPartnerType(DeliveryPartnerType.SHOP);

        Cart cart = new Cart();
        cart.setDelivery(delivery);
        CartContext cartContext = new CartContext(123);

        CartResponse cartResponse = new CartResponse();

        LocalDate fromDate = LocalDate.of(2022, 6, 1);
        LocalDate toDate = LocalDate.of(2022, 6, 4);

        DeliveryResponse deliveryOption = new DeliveryResponse();
        deliveryOption.setDeliveryDates(new DeliveryDates(DateUtil.asDate(fromDate), DateUtil.asDate(toDate)));

        RawDeliveryIntervalsCollection intervals = new RawDeliveryIntervalsCollection();
        LocalDate expectedFromDate = LocalDate.of(2022, 6, 1);
        intervals.add(new RawDeliveryInterval(DateUtil.asDate(expectedFromDate), AM_9, PM_6));
        intervals.add(new RawDeliveryInterval(DateUtil.asDate(expectedFromDate.plusDays(2)), PM_6, PM_9));

        deliveryOption.setRawDeliveryIntervals(intervals);

        cartResponse.setDeliveryOptions(List.of(deliveryOption));

        postprocessor.process(cart, cartResponse, cartContext);

        Assertions.assertEquals(1, cartResponse.getDeliveryOptions().size());
        Assertions.assertEquals(intervals, cartResponse.getDeliveryOptions().get(0).getRawDeliveryIntervals());
    }

    @Test
    void testEmptyFbs() {
        EnvironmentService environmentService = mock(EnvironmentService.class);
        when(environmentService.getBooleanValueOrDefault(FILL_DBS_DELIVERY_INTERVALS_FLAG, false)).thenReturn(true);

        FillDeliveryIntervalsPostprocessor postprocessor = new FillDeliveryIntervalsPostprocessor(environmentService);

        Delivery delivery = new Delivery();
        delivery.setDeliveryPartnerType(DeliveryPartnerType.YANDEX_MARKET);

        Cart cart = new Cart();
        cart.setDelivery(delivery);
        CartContext cartContext = new CartContext(123);

        CartResponse cartResponse = new CartResponse();

        LocalDate fromDate = LocalDate.of(2022, 6, 1);
        LocalDate toDate = LocalDate.of(2022, 6, 10);

        DeliveryResponse deliveryOption = new DeliveryResponse();
        deliveryOption.setDeliveryDates(new DeliveryDates(DateUtil.asDate(fromDate), DateUtil.asDate(toDate)));

        cartResponse.setDeliveryOptions(List.of(deliveryOption));

        postprocessor.process(cart, cartResponse, cartContext);

        Assertions.assertEquals(1, cartResponse.getDeliveryOptions().size());
        Assertions.assertTrue(cartResponse.getDeliveryOptions().get(0).getRawDeliveryIntervals().isEmpty());
    }

    @Test
    void testCheckFlag() {
        EnvironmentService environmentService = mock(EnvironmentService.class);
        when(environmentService.getBooleanValueOrDefault(FILL_DBS_DELIVERY_INTERVALS_FLAG, false)).thenReturn(false);

        FillDeliveryIntervalsPostprocessor postprocessor = new FillDeliveryIntervalsPostprocessor(environmentService);

        Delivery delivery = new Delivery();
        delivery.setDeliveryPartnerType(DeliveryPartnerType.SHOP);

        Cart cart = new Cart();
        cart.setDelivery(delivery);
        CartContext cartContext = new CartContext(123);

        CartResponse cartResponse = new CartResponse();

        LocalDate fromDate = LocalDate.of(2022, 6, 1);
        LocalDate toDate = LocalDate.of(2022, 6, 10);

        DeliveryResponse deliveryOption = new DeliveryResponse();
        deliveryOption.setDeliveryDates(new DeliveryDates(DateUtil.asDate(fromDate), DateUtil.asDate(toDate)));

        cartResponse.setDeliveryOptions(List.of(deliveryOption));

        postprocessor.process(cart, cartResponse, cartContext);

        Assertions.assertEquals(1, cartResponse.getDeliveryOptions().size());
        Assertions.assertTrue(cartResponse.getDeliveryOptions().get(0).getRawDeliveryIntervals().isEmpty());
    }
}
