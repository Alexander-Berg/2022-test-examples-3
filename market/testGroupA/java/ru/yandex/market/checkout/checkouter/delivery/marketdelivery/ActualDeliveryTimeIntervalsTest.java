package ru.yandex.market.checkout.checkouter.delivery.marketdelivery;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.google.common.collect.Iterables;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.date.DateUtil;
import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.delivery.RawDeliveryInterval;
import ru.yandex.market.checkout.checkouter.delivery.RawDeliveryIntervalsCollection;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.MultiOrder;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.common.util.DeliveryOptionPartnerType;
import ru.yandex.market.checkout.helpers.YandexMarketDeliveryHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.common.report.model.ActualDeliveryOption;
import ru.yandex.market.common.report.model.ActualDeliveryResult;
import ru.yandex.market.common.report.model.DeliveryTimeInterval;

import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.INTAKE_AVAILABLE_DATE;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.MOCK_DELIVERY_SERVICE_ID;
import static ru.yandex.market.checkout.test.providers.OrderProvider.SHOP_ID_WITH_SORTING_CENTER;

public class ActualDeliveryTimeIntervalsTest extends AbstractWebTestBase {

    @Autowired
    private YandexMarketDeliveryHelper yandexMarketDeliveryHelper;

    @Test
    public void canCreateOrderWithoutIntervals() {
        setFixedTime(INTAKE_AVAILABLE_DATE);
        Parameters parameters = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withColor(Color.BLUE)
                .withDeliveryType(DeliveryType.DELIVERY)
                .withShopId(SHOP_ID_WITH_SORTING_CENTER)
                .withShipmentDay(1)
                .buildParameters();
        ActualDeliveryResult actualDeliveryResult = Iterables.getOnlyElement(
                parameters.getReportParameters().getActualDelivery().getResults()
        );
        actualDeliveryResult.setPost(Collections.emptyList());
        actualDeliveryResult.setPickup(Collections.emptyList());
        Iterables.getOnlyElement(
                actualDeliveryResult.getDelivery()
        ).setTimeIntervals(null);
        Order order = orderCreateHelper.createOrder(parameters);
        assertFalse(order.getDelivery().getDeliveryDates().hasTime());
    }

    @Test
    public void canCreateOrderWithInvalidTimeIntervals() {
        setFixedTime(INTAKE_AVAILABLE_DATE);
        Parameters parameters = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withColor(Color.BLUE)
                .withDeliveryType(DeliveryType.DELIVERY)
                .withShopId(SHOP_ID_WITH_SORTING_CENTER)
                .withShipmentDay(1)
                .buildParameters();
        ActualDeliveryResult actualDeliveryResult = Iterables.getOnlyElement(
                parameters.getReportParameters().getActualDelivery().getResults()
        );
        actualDeliveryResult.setPost(Collections.emptyList());
        actualDeliveryResult.setPickup(Collections.emptyList());
        Iterables.getOnlyElement(
                actualDeliveryResult.getDelivery()
        ).setTimeIntervals(singletonList(new DeliveryTimeInterval(LocalTime.of(14, 00), null)));
        Order order = orderCreateHelper.createOrder(parameters);
        assertFalse(order.getDelivery().getDeliveryDates().hasTime());
    }

    @Test
    public void canMapDeliveryIntervalsFromActualDelivery() {
        LocalTime fromTime = LocalTime.of(14, 00);
        LocalTime toTime = LocalTime.of(18, 00);
        Parameters parameters = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withColor(Color.BLUE)
                .withDeliveryType(DeliveryType.DELIVERY)
                .withShopId(SHOP_ID_WITH_SORTING_CENTER)
                .buildParameters();
        ActualDeliveryResult actualDeliveryResult = Iterables.getOnlyElement(
                parameters.getReportParameters().getActualDelivery().getResults()
        );
        actualDeliveryResult.setPost(Collections.emptyList());
        actualDeliveryResult.setPickup(Collections.emptyList());
        Iterables.getOnlyElement(
                actualDeliveryResult.getDelivery()
        ).setTimeIntervals(
                singletonList(new DeliveryTimeInterval(fromTime, toTime))
        );
        MultiCart cart = orderCreateHelper.cart(parameters);
        Order order = Iterables.getOnlyElement(cart.getCarts());
        Delivery delivery = Iterables.getOnlyElement(order.getDeliveryOptions());
        RawDeliveryIntervalsCollection intervals = delivery.getRawDeliveryIntervals();
        RawDeliveryInterval deliveryInterval = Iterables.getOnlyElement(
                intervals.getIntervalsByDate(delivery.getDeliveryDates().getFromDate())
        );
        assertEquals(deliveryInterval.getDate(), delivery.getDeliveryDates().getFromDate());
        assertEquals(fromTime, deliveryInterval.getFromTime());
        assertEquals(toTime, deliveryInterval.getToTime());
    }

    @Test
    public void canMapDeliveryIntervalsIsDefaultMarkFromActualDelivery() {
        LocalTime fromTime = LocalTime.of(14, 00);
        LocalTime toTime = LocalTime.of(18, 00);
        LocalTime defaultIntervalFromTime = LocalTime.of(10, 00);
        LocalTime defaultIntervalToTime = LocalTime.of(20, 00);
        Parameters parameters = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withColor(Color.BLUE)
                .withDeliveryType(DeliveryType.DELIVERY)
                .withShopId(SHOP_ID_WITH_SORTING_CENTER)
                .buildParameters();
        ActualDeliveryResult actualDeliveryResult = Iterables.getOnlyElement(
                parameters.getReportParameters().getActualDelivery().getResults()
        );
        actualDeliveryResult.setPost(Collections.emptyList());
        actualDeliveryResult.setPickup(Collections.emptyList());
        Iterables.getOnlyElement(
                actualDeliveryResult.getDelivery()
        ).setTimeIntervals(Arrays.asList(
                new DeliveryTimeInterval(fromTime, toTime),
                new DeliveryTimeInterval(defaultIntervalFromTime, defaultIntervalToTime, true)
        ));
        MultiCart cart = orderCreateHelper.cart(parameters);
        Order order = Iterables.getOnlyElement(cart.getCarts());
        Delivery delivery = Iterables.getOnlyElement(order.getDeliveryOptions());
        RawDeliveryIntervalsCollection intervals = delivery.getRawDeliveryIntervals();
        assertThat(intervals.getDates(), hasSize(1));
        assertThat(intervals.getIntervalsByDate(delivery.getDeliveryDates().getFromDate()), hasSize(2));
        RawDeliveryInterval defaultDeliveryInterval = Iterables.getOnlyElement(
                intervals.getIntervalsByDate(delivery.getDeliveryDates().getFromDate())
                        .stream()
                        .filter(RawDeliveryInterval::isDefault)
                        .collect(toList())
        );
        assertEquals(defaultDeliveryInterval.getDate(), delivery.getDeliveryDates().getFromDate());
        assertEquals(defaultIntervalFromTime, defaultDeliveryInterval.getFromTime());
        assertEquals(defaultIntervalToTime, defaultDeliveryInterval.getToTime());
        assertTrue(defaultDeliveryInterval.isDefault());
    }

    @Test
    public void canCreateOrderWithDeliveryIntervalFromActualDelivery() {
        setFixedTime(INTAKE_AVAILABLE_DATE);
        LocalTime fromTime = LocalTime.of(14, 00);
        LocalTime toTime = LocalTime.of(18, 00);
        Parameters parameters = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withColor(Color.BLUE)
                .withDeliveryType(DeliveryType.DELIVERY)
                .withShopId(SHOP_ID_WITH_SORTING_CENTER)
                .withShipmentDay(1)
                .buildParameters();
        ActualDeliveryResult actualDeliveryResult = Iterables.getOnlyElement(
                parameters.getReportParameters().getActualDelivery().getResults()
        );
        actualDeliveryResult.setPost(Collections.emptyList());
        actualDeliveryResult.setPickup(Collections.emptyList());
        Iterables.getOnlyElement(
                actualDeliveryResult.getDelivery()
        ).setTimeIntervals(
                singletonList(new DeliveryTimeInterval(fromTime, toTime))
        );
        Order order = orderCreateHelper.createOrder(parameters);
        assertTrue(order.getDelivery().getDeliveryDates().hasTime());
        assertEquals(fromTime, order.getDelivery().getDeliveryDates().getFromTime());
        assertEquals(toTime, order.getDelivery().getDeliveryDates().getToTime());
    }

    @Test
    public void canCreateOrderWithDeliveryIntervalFromActualDeliveryWithMultipleIntervals() {
        setFixedTime(INTAKE_AVAILABLE_DATE);
        LocalTime fromTime = LocalTime.of(12, 00);
        LocalTime toTime = LocalTime.of(13, 00);
        Parameters parameters = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withColor(Color.BLUE)
                .withDeliveryType(DeliveryType.DELIVERY)
                .withShopId(SHOP_ID_WITH_SORTING_CENTER)
                .withShipmentDay(1)
                .buildParameters();
        ActualDeliveryResult actualDeliveryResult = Iterables.getOnlyElement(
                parameters.getReportParameters().getActualDelivery().getResults()
        );
        actualDeliveryResult.setPost(Collections.emptyList());
        actualDeliveryResult.setPickup(Collections.emptyList());
        Iterables.getOnlyElement(
                actualDeliveryResult.getDelivery()
        ).setTimeIntervals(
                Arrays.asList(
                        new DeliveryTimeInterval(fromTime, toTime),
                        new DeliveryTimeInterval(LocalTime.of(13, 00), LocalTime.of(18, 00)),
                        new DeliveryTimeInterval(LocalTime.of(16, 00), LocalTime.of(20, 00))
                )
        );
        Order order = orderCreateHelper.createOrder(parameters);
        assertTrue(order.getDelivery().getDeliveryDates().hasTime());
        assertEquals(fromTime, order.getDelivery().getDeliveryDates().getFromTime());
        assertEquals(toTime, order.getDelivery().getDeliveryDates().getToTime());
    }

    @Test
    public void canCreateOrderWithMultipleDeliveryOptionsFromActualDeliveryWithMultipleIntervals() {
        setFixedTime(INTAKE_AVAILABLE_DATE);
        LocalTime fromTime = LocalTime.of(12, 00);
        LocalTime toTime = LocalTime.of(13, 00);
        Parameters parameters = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withColor(Color.BLUE)
                .withDeliveryType(DeliveryType.DELIVERY)
                .withShopId(SHOP_ID_WITH_SORTING_CENTER)
                .withShipmentDay(1)
                .buildParameters();
        ActualDeliveryResult actualDeliveryResult = Iterables.getOnlyElement(
                parameters.getReportParameters().getActualDelivery().getResults()
        );
        actualDeliveryResult.setPost(Collections.emptyList());
        actualDeliveryResult.setPickup(Collections.emptyList());
        actualDeliveryResult.setDelivery(
                buildDeliveryOptions()
        );
        Order order = orderCreateHelper.createOrder(parameters);
        assertTrue(order.getDelivery().getDeliveryDates().hasTime());
        assertEquals(fromTime, order.getDelivery().getDeliveryDates().getFromTime());
        assertEquals(toTime, order.getDelivery().getDeliveryDates().getToTime());
    }

    @Test
    public void canNotCreateOrderWithDatesOutsideOfDeliveryOptionDates() throws Exception {
        setFixedTime(INTAKE_AVAILABLE_DATE);
        Parameters parameters = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withColor(Color.BLUE)
                .withDeliveryType(DeliveryType.DELIVERY)
                .withShopId(SHOP_ID_WITH_SORTING_CENTER)
                .withShipmentDay(1)
                .buildParameters();
        ActualDeliveryResult actualDeliveryResult = Iterables.getOnlyElement(
                parameters.getReportParameters().getActualDelivery().getResults()
        );
        actualDeliveryResult.setPost(Collections.emptyList());
        actualDeliveryResult.setPickup(Collections.emptyList());
        actualDeliveryResult.setDelivery(
                buildDeliveryOptions()
        );
        MultiCart cart = orderCreateHelper.cart(parameters);
        MultiOrder multiOrder = orderCreateHelper.mapCartToOrder(cart, parameters);
        Iterables.getOnlyElement(multiOrder.getCarts()).getDelivery().getDeliveryDates()
                .setFromDate(DateUtil.addDay(DateUtil.getToday(), 7));
        Iterables.getOnlyElement(multiOrder.getCarts()).getDelivery().getDeliveryDates()
                .setToDate(DateUtil.addDay(DateUtil.getToday(), 7));
        parameters.setCheckOrderCreateErrors(false);
        multiOrder = orderCreateHelper.checkout(multiOrder, parameters);
    }

    private List<ActualDeliveryOption> buildDeliveryOptions() {
        ActualDeliveryOption multipleDaysDeliveryOption = new ActualDeliveryOption();
        multipleDaysDeliveryOption.setDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID);
        multipleDaysDeliveryOption.setDayFrom(1);
        multipleDaysDeliveryOption.setDayTo(5);
        multipleDaysDeliveryOption.setPaymentMethods(Collections.singleton(PaymentMethod.YANDEX.name()));
        multipleDaysDeliveryOption.setPrice(new BigDecimal("100"));
        multipleDaysDeliveryOption.setPartnerType(DeliveryOptionPartnerType.MARKET_DELIVERY.getReportName());
        multipleDaysDeliveryOption.setShipmentDay(1);
        multipleDaysDeliveryOption.setTimeIntervals(
                singletonList(new DeliveryTimeInterval(LocalTime.of(12, 00),
                        LocalTime.of(13, 00)))
        );

        ActualDeliveryOption singleDayDeliveryOption = new ActualDeliveryOption();
        singleDayDeliveryOption.setDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID);
        singleDayDeliveryOption.setDayFrom(5);
        singleDayDeliveryOption.setDayTo(5);
        singleDayDeliveryOption.setPaymentMethods(Collections.singleton(PaymentMethod.YANDEX.name()));
        singleDayDeliveryOption.setPrice(new BigDecimal("100"));
        singleDayDeliveryOption.setPartnerType(DeliveryOptionPartnerType.MARKET_DELIVERY.getReportName());
        singleDayDeliveryOption.setShipmentDay(1);
        singleDayDeliveryOption.setTimeIntervals(
                Arrays.asList(
                        new DeliveryTimeInterval(LocalTime.of(13, 00), LocalTime.of(18, 00)),
                        new DeliveryTimeInterval(LocalTime.of(16, 00), LocalTime.of(20, 00))
                )
        );

        return Arrays.asList(
                multipleDaysDeliveryOption,
                singleDayDeliveryOption
        );
    }
}
