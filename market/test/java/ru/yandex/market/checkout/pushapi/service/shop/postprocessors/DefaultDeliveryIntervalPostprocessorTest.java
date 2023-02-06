package ru.yandex.market.checkout.pushapi.service.shop.postprocessors;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.LinkedList;

import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.checkouter.delivery.DeliveryDates;
import ru.yandex.market.checkout.checkouter.delivery.RawDeliveryInterval;
import ru.yandex.market.checkout.checkouter.delivery.RawDeliveryIntervalsCollection;
import ru.yandex.market.checkout.pushapi.client.entity.Cart;
import ru.yandex.market.checkout.pushapi.client.entity.CartResponse;
import ru.yandex.market.checkout.pushapi.client.entity.DeliveryResponse;
import ru.yandex.market.checkout.pushapi.service.shop.CartContext;

import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

/**
 * @author gelvy
 * Created on: 24.11.2020
 **/
public class DefaultDeliveryIntervalPostprocessorTest {

    private final DefaultDeliveryIntervalPostprocessor postprocessor = new DefaultDeliveryIntervalPostprocessor();

    private final Cart cart = new Cart();
    private final CartContext cartContext = new CartContext(123);

    @Test
    public void shouldSetDefaultIntervalWhenDoesntContainAny() {
        CartResponse cartResponse = new CartResponse();
        DeliveryResponse deliveryOption = new DeliveryResponse();

        Date date = new Date();
        deliveryOption.setDeliveryDates(new DeliveryDates(date, date));
        RawDeliveryIntervalsCollection deliveryIntervals = deliveryOption.getRawDeliveryIntervals();
        LocalTime Am9 = LocalTime.of(9, 0);
        LocalTime Am10 = LocalTime.of(10, 0);
        LocalTime Am11 = LocalTime.of(11, 0);
        LocalTime Am12 = LocalTime.of(12, 0);
        deliveryIntervals.add(new RawDeliveryInterval(date, Am9, Am10));
        deliveryIntervals.add(new RawDeliveryInterval(date, Am10, Am11));
        deliveryIntervals.add(new RawDeliveryInterval(date, Am11, Am12));

        cartResponse.setDeliveryOptions(singletonList(deliveryOption));

        postprocessor.process(cart, cartResponse, cartContext);

        var actualDeliveryOptions = cartResponse.getDeliveryOptions();
        var rawDeliveryIntervals = actualDeliveryOptions.get(0).getRawDeliveryIntervals();
        var intervals = new LinkedList<>(rawDeliveryIntervals.getIntervalsByDate(date));

        assertThat(intervals, hasSize(3));
        assertThat(intervals.get(0).isDefault(), equalTo(true));
        assertThat(intervals.get(1).isDefault(), equalTo(false));
        assertThat(intervals.get(2).isDefault(), equalTo(false));
    }

    @Test
    public void shouldNotSetDefaultIntervalWhenContainsOne() {
        CartResponse cartResponse = new CartResponse();
        DeliveryResponse deliveryOption = new DeliveryResponse();

        Date date = new Date();
        deliveryOption.setDeliveryDates(new DeliveryDates(date, date));
        RawDeliveryIntervalsCollection deliveryIntervals = deliveryOption.getRawDeliveryIntervals();
        LocalTime Am9 = LocalTime.of(9, 0);
        LocalTime Am10 = LocalTime.of(10, 0);
        LocalTime Am11 = LocalTime.of(11, 0);
        LocalTime Am12 = LocalTime.of(12, 0);
        deliveryIntervals.add(new RawDeliveryInterval(date, Am9, Am10));
        deliveryIntervals.add(new RawDeliveryInterval(date, Am10, Am11));
        deliveryIntervals.add(new RawDeliveryInterval(date, Am11, Am12, true));

        cartResponse.setDeliveryOptions(singletonList(deliveryOption));

        postprocessor.process(cart, cartResponse, cartContext);

        var actualDeliveryOptions = cartResponse.getDeliveryOptions();
        var rawDeliveryIntervals = actualDeliveryOptions.get(0).getRawDeliveryIntervals();
        var intervals = new LinkedList<>(rawDeliveryIntervals.getIntervalsByDate(date));

        assertThat(intervals, hasSize(3));
        assertThat(intervals.get(0).isDefault(), equalTo(false));
        assertThat(intervals.get(1).isDefault(), equalTo(false));
        assertThat(intervals.get(2).isDefault(), equalTo(true));
    }

    @Test
    public void shouldSetDefaultIntervalForEachDay() {
        CartResponse cartResponse = new CartResponse();
        DeliveryResponse deliveryOption = new DeliveryResponse();

        Date date = new Date();
        Date date2 = Date.from(LocalDate.now().plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant());
        deliveryOption.setDeliveryDates(new DeliveryDates(date, date2));
        RawDeliveryIntervalsCollection deliveryIntervals = deliveryOption.getRawDeliveryIntervals();
        LocalTime Am9 = LocalTime.of(9, 0);
        LocalTime Am10 = LocalTime.of(10, 0);
        LocalTime Am11 = LocalTime.of(11, 0);
        LocalTime Am12 = LocalTime.of(12, 0);
        deliveryIntervals.add(new RawDeliveryInterval(date, Am9, Am10));
        deliveryIntervals.add(new RawDeliveryInterval(date, Am10, Am11));
        deliveryIntervals.add(new RawDeliveryInterval(date, Am11, Am12));
        deliveryIntervals.add(new RawDeliveryInterval(date2, Am10, Am11));
        deliveryIntervals.add(new RawDeliveryInterval(date2, Am11, Am12));

        cartResponse.setDeliveryOptions(singletonList(deliveryOption));

        postprocessor.process(cart, cartResponse, cartContext);

        var actualDeliveryOptions = cartResponse.getDeliveryOptions();
        var rawDeliveryIntervals = actualDeliveryOptions.get(0).getRawDeliveryIntervals();

        var intervals = new LinkedList<>(rawDeliveryIntervals.getIntervalsByDate(date));
        assertThat(intervals, hasSize(3));
        assertThat(intervals.get(0).isDefault(), equalTo(true));
        assertThat(intervals.get(1).isDefault(), equalTo(false));
        assertThat(intervals.get(2).isDefault(), equalTo(false));

        var intervals2 = new LinkedList<>(rawDeliveryIntervals.getIntervalsByDate(date2));
        assertThat(intervals2, hasSize(2));
        assertThat(intervals2.get(0).isDefault(), equalTo(true));
        assertThat(intervals2.get(1).isDefault(), equalTo(false));
    }
}
