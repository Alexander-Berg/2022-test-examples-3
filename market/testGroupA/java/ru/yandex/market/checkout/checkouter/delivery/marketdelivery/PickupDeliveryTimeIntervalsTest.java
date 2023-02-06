package ru.yandex.market.checkout.checkouter.delivery.marketdelivery;

import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import com.google.common.collect.Iterables;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.feature.type.common.BooleanFeatureType;
import ru.yandex.market.checkout.checkouter.feature.type.common.ComplexFeatureType;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.common.util.SwitchWithWhitelist;
import ru.yandex.market.checkout.helpers.YandexMarketDeliveryHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.test.providers.ActualDeliveryProvider;
import ru.yandex.market.checkout.util.Constants;
import ru.yandex.market.common.report.model.ActualDeliveryResult;
import ru.yandex.market.common.report.model.DeliveryTimeInterval;
import ru.yandex.market.common.report.model.OutletDeliveryTimeInterval;
import ru.yandex.market.common.report.model.PickupOption;

import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.INTAKE_AVAILABLE_DATE;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.MOCK_DELIVERY_SERVICE_ID;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.MOCK_POST_TERM_DELIVERY_SERVICE_ID;

public class PickupDeliveryTimeIntervalsTest extends AbstractWebTestBase {

    @Autowired
    private YandexMarketDeliveryHelper yandexMarketDeliveryHelper;

    private static Stream<Arguments> threeBooleansValueSpace() {
        return Stream.of(
                new Object[]{true, false, false},
                new Object[]{false, false, false},
                new Object[]{true, false, true},
                new Object[]{false, false, true},
                new Object[]{true, true, false},
                new Object[]{false, true, false},
                new Object[]{true, true, true},
                new Object[]{false, true, true}
        ).map(Arguments::of);
    }

    @ParameterizedTest
    @MethodSource("threeBooleansValueSpace")
    public void canCreateOrderWithoutIntervals(boolean enableTransfer,
                                               boolean isCombinatorFlow,
                                               boolean postTerm) {
        checkouterFeatureWriter.writeValue(BooleanFeatureType.ENABLE_TRANSFER_SET_PICKUP_DELIVERY_TIME, enableTransfer);
        setFixedTime(INTAKE_AVAILABLE_DATE);
        Parameters parameters = getParameters(null, isCombinatorFlow, postTerm);
        Order order = orderCreateHelper.createOrder(parameters);
        assertFalse(order.getDelivery().getDeliveryDates().hasTime());
    }

    @ParameterizedTest
    @MethodSource("threeBooleansValueSpace")
    public void canCreateOrderWithInvalidTimeIntervals(boolean enableTransfer,
                                                       boolean isCombinatorFlow,
                                                       boolean postTerm) {
        checkouterFeatureWriter.writeValue(BooleanFeatureType.ENABLE_TRANSFER_SET_PICKUP_DELIVERY_TIME, enableTransfer);
        setFixedTime(INTAKE_AVAILABLE_DATE);
        Parameters parameters = getParameters(
                singletonList(new OutletDeliveryTimeInterval(778L, LocalTime.of(14, 00), null)),
                isCombinatorFlow, postTerm
        );
        Order order = orderCreateHelper.createOrder(parameters);
        assertFalse(order.getDelivery().getDeliveryDates().hasTime());
    }

    @ParameterizedTest
    @MethodSource("threeBooleansValueSpace")
    public void canMapDeliveryIntervalsFromActualDelivery(boolean enableTransfer,
                                                          boolean isCombinatorFlow,
                                                          boolean postTerm) {
        checkouterFeatureWriter.writeValue(BooleanFeatureType.ENABLE_TRANSFER_SET_PICKUP_DELIVERY_TIME, enableTransfer);
        Long outletId = getOutletId(postTerm);
        LocalTime fromTime = LocalTime.of(14, 0);
        LocalTime toTime = LocalTime.of(18, 0);
        Parameters parameters = getParameters(
                singletonList(new OutletDeliveryTimeInterval(outletId, fromTime, toTime)),
                isCombinatorFlow, postTerm
        );
        MultiCart cart = orderCreateHelper.cart(parameters);
        Order order = Iterables.getOnlyElement(cart.getCarts());
        Delivery delivery = Iterables.getOnlyElement(order.getDeliveryOptions());
        var intervals = delivery.getOutletTimeIntervals();
        var deliveryInterval = Iterables.getOnlyElement(
                intervals
        );
        assertEquals(outletId, deliveryInterval.getOutletId());
        assertEquals(fromTime, deliveryInterval.getFrom());
        assertEquals(toTime, deliveryInterval.getTo());
    }

    @ParameterizedTest
    @MethodSource("threeBooleansValueSpace")
    public void shouldSetDeliveryIntervalFromDeliveryRoute(boolean enableTransfer,
                                                           boolean enablePickupDeliveryTime,
                                                           boolean postTerm) {
        checkouterFeatureWriter.writeValue(BooleanFeatureType.ENABLE_TRANSFER_SET_PICKUP_DELIVERY_TIME, enableTransfer);
        checkouterFeatureWriter.writeValue(BooleanFeatureType.ENABLE_PICKUP_DELIVERY_TIME, enablePickupDeliveryTime);
        setFixedTime(INTAKE_AVAILABLE_DATE);
        Long outletId = getOutletId(postTerm);
        LocalTime fromTime = LocalTime.of(14, 0);
        LocalTime toTime = LocalTime.of(18, 0);
        Parameters parameters = getParameters(
                singletonList(new OutletDeliveryTimeInterval(outletId, fromTime, toTime)),
                true, postTerm
        );
        parameters.getReportParameters().getDeliveryRoute().getResults().get(0)
                .getOption()
                .setTimeIntervals(singletonList(new DeliveryTimeInterval(fromTime, toTime)));

        reportMock.resetRequests();
        Order order = orderCreateHelper.createOrder(parameters);

        var deliveryDates = order.getDelivery().getDeliveryDates();
        if (enablePickupDeliveryTime) {
            assertEquals(fromTime, deliveryDates.getFromTime());
            assertEquals(toTime, deliveryDates.getToTime());
        } else {
            assertNull(deliveryDates.getFromTime());
            assertNull(deliveryDates.getToTime());
        }
    }

    @ParameterizedTest
    @MethodSource("threeBooleansValueSpace")
    public void shouldNotSetDeliveryIntervalWithoutThemInDeliveryRoute(boolean enableTransfer,
                                                                       boolean enablePickupDeliveryTime,
                                                                       boolean postTerm) {
        checkouterFeatureWriter.writeValue(BooleanFeatureType.ENABLE_TRANSFER_SET_PICKUP_DELIVERY_TIME, enableTransfer);
        checkouterFeatureWriter.writeValue(BooleanFeatureType.ENABLE_PICKUP_DELIVERY_TIME, enablePickupDeliveryTime);
        setFixedTime(INTAKE_AVAILABLE_DATE);
        Long outletId = getOutletId(postTerm);
        LocalTime fromTime = LocalTime.of(14, 0);
        LocalTime toTime = LocalTime.of(18, 0);
        Parameters parameters = getParameters(
                singletonList(new OutletDeliveryTimeInterval(outletId, fromTime, toTime)),
                true, postTerm
        );
        parameters.getReportParameters().getDeliveryRoute().getResults().get(0)
                .getOption()
                .setTimeIntervals(null);

        reportMock.resetRequests();
        Order order = orderCreateHelper.createOrder(parameters);

        var deliveryDates = order.getDelivery().getDeliveryDates();
        assertNull(deliveryDates.getFromTime());
        assertNull(deliveryDates.getToTime());
    }

    @ParameterizedTest
    @MethodSource("threeBooleansValueSpace")
    public void shouldNotSetInvalidDeliveryIntervalFromDeliveryRoute(boolean enableTransfer,
                                                                     boolean enablePickupDeliveryTime,
                                                                     boolean postTerm) {
        checkouterFeatureWriter.writeValue(BooleanFeatureType.ENABLE_TRANSFER_SET_PICKUP_DELIVERY_TIME, enableTransfer);
        checkouterFeatureWriter.writeValue(BooleanFeatureType.ENABLE_PICKUP_DELIVERY_TIME, enablePickupDeliveryTime);
        setFixedTime(INTAKE_AVAILABLE_DATE);
        Long outletId = getOutletId(postTerm);
        LocalTime fromTime = LocalTime.of(12, 0);
        LocalTime toTime = LocalTime.of(12, 0);
        Parameters parameters = getParameters(
                singletonList(new OutletDeliveryTimeInterval(outletId, fromTime, toTime)),
                true, postTerm
        );
        parameters.getReportParameters().getDeliveryRoute().getResults().get(0)
                .getOption()
                .setTimeIntervals(singletonList(new DeliveryTimeInterval(fromTime, toTime)));

        reportMock.resetRequests();
        Order order = orderCreateHelper.createOrder(parameters);

        var deliveryDates = order.getDelivery().getDeliveryDates();
        assertNull(deliveryDates.getFromTime());
        assertNull(deliveryDates.getToTime());
    }

    private Long getOutletId(boolean postTerm) {
        return postTerm ? ActualDeliveryProvider.POST_TERM_OUTLET_ID : ActualDeliveryProvider.PICKUP_OUTLET_ID;
    }

    private Parameters getParameters(List<OutletDeliveryTimeInterval> timeIntervals, boolean isCombinatorFlow,
                                     boolean postTerm) {
        if (isCombinatorFlow) {
            checkouterFeatureWriter.writeValue(ComplexFeatureType.COMBINATOR_FLOW,
                    new SwitchWithWhitelist(true, singleton(Constants.COMBINATOR_EXPERIMENT)));
        }

        var actualDeliveryBuilder = ActualDeliveryProvider.builder();
        if (postTerm) {
            actualDeliveryBuilder.addPostTerm(MOCK_POST_TERM_DELIVERY_SERVICE_ID);
        } else {
            actualDeliveryBuilder.addPickup(MOCK_DELIVERY_SERVICE_ID);
        }
        var builder = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withActualDelivery(actualDeliveryBuilder.build());
        if (isCombinatorFlow) {
            builder.withCombinator(true);
        }
        Parameters parameters = builder.buildParameters();
        ActualDeliveryResult actualDeliveryResult = Iterables.getOnlyElement(
                parameters.getReportParameters().getActualDelivery().getResults()
        );
        actualDeliveryResult.setPost(Collections.emptyList());
        actualDeliveryResult.setDelivery(Collections.emptyList());
        PickupOption pickupOption = Iterables.getOnlyElement(
                actualDeliveryResult.getPickup()
        );
        pickupOption.setOutletTimeIntervals(timeIntervals);
        if (isCombinatorFlow) {
            parameters.setExperiments(Constants.COMBINATOR_EXPERIMENT);
        }
        return parameters;
    }
}
