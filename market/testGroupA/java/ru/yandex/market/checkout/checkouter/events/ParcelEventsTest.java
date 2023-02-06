package ru.yandex.market.checkout.checkouter.events;

import java.math.RoundingMode;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.delivery.shipment.Parcel;
import ru.yandex.market.checkout.checkouter.delivery.shipment.ParcelStatus;
import ru.yandex.market.checkout.checkouter.event.HistoryEventType;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.helpers.OrderDeliveryHelper;
import ru.yandex.market.checkout.helpers.OrderPayHelper;
import ru.yandex.market.checkout.helpers.ParcelHelper;
import ru.yandex.market.checkout.helpers.YandexMarketDeliveryHelper;
import ru.yandex.market.checkout.helpers.utils.EventsTestUtils;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.common.report.model.ActualDeliveryResult;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.INTAKE_AVAILABLE_DATE;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.MOCK_DELIVERY_SERVICE_ID;
import static ru.yandex.market.checkout.test.providers.OrderProvider.SHOP_ID_WITH_SORTING_CENTER;

/**
 * @author mmetlov
 */
public class ParcelEventsTest extends AbstractEventsControllerTestBase {

    @Autowired
    private YandexMarketDeliveryHelper yandexMarketDeliveryHelper;
    @Autowired
    private OrderPayHelper payHelper;
    @Autowired
    private OrderDeliveryHelper orderDeliveryHelper;
    @Autowired
    private ParcelHelper parcelHelper;

    public static Stream<Arguments> parameterizedTestData() {
        return EventsTestUtils.parameters(Color.BLUE).stream().map(Arguments::of);
    }


    @ParameterizedTest(name = "{0}")
    @MethodSource("parameterizedTestData")
    public void eventHasParcelBox(String caseName, EventsTestUtils.EventGetter eventGetter) throws Exception {
        setFixedTime(INTAKE_AVAILABLE_DATE);
        Parameters parameters = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withDeliveryType(DeliveryType.DELIVERY)
                .withShopId(SHOP_ID_WITH_SORTING_CENTER)
                .withColor(Color.BLUE)
                .buildParameters();
        Order order = orderCreateHelper.createOrder(parameters);

        payHelper.payForOrder(order);

        Delivery deliveryChangeRequest = new Delivery();
        Parcel parcel = new Parcel();
        parcel.setStatus(ParcelStatus.CREATED);
        deliveryChangeRequest.setParcels(Collections.singletonList(parcel));
        orderDeliveryHelper.updateOrderDelivery(order.getId(), deliveryChangeRequest);

        ActualDeliveryResult actualDeliveryResult =
                parameters.getReportParameters().getActualDelivery().getResults().get(0);
        Collection<OrderHistoryEvent> events = eventGetter.getEvents(order.getId(), mockMvc, serializationService);
        events.stream()
                .flatMap(e -> Stream.of(e.getOrderAfter(), e.getOrderBefore()))
                .filter(Objects::nonNull)
                .forEach(o ->
                        assertThat(o.getDelivery().getParcels().get(0).getBoxes(),
                                contains(allOf(
                                        hasProperty("id", notNullValue()),
                                        hasProperty(
                                                "weight", is(actualDeliveryResult.getWeight().movePointRight(3)
                                                        .setScale(0, RoundingMode.CEILING).longValue())),
                                        hasProperty("width",
                                                is(actualDeliveryResult.getDimensions().get(1).longValue())),
                                        hasProperty("height",
                                                is(actualDeliveryResult.getDimensions().get(2).longValue())),
                                        hasProperty("depth",
                                                is(actualDeliveryResult.getDimensions().get(0).longValue()))
                                ))));
    }


    @ParameterizedTest(name = "{0}")
    @MethodSource("parameterizedTestData")
    public void hasDeliveredAtUpdatedEvent(String caseName, EventsTestUtils.EventGetter eventGetter) throws Exception {
        setFixedTime(INTAKE_AVAILABLE_DATE);
        Parameters parameters = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withDeliveryType(DeliveryType.DELIVERY)
                .withShopId(SHOP_ID_WITH_SORTING_CENTER)
                .withColor(Color.BLUE)
                .buildParameters();
        Order order = orderCreateHelper.createOrder(parameters);
        payHelper.payForOrder(order);

        Optional<Parcel> originParcelOptional = order.getDelivery().getParcels().stream().findFirst();
        assertTrue(originParcelOptional.isPresent());
        Parcel originParcel = originParcelOptional.get();
        assertNull(originParcel.getDeliveredAt());

        // action
        Instant expectedDeliveredAtTS = Instant.now();
        parcelHelper.updateParcelDeliveredAt(order, originParcel, expectedDeliveredAtTS)
                .andExpect(status().isOk());

        // check
        Collection<OrderHistoryEvent> events = eventGetter.getEvents(order.getId(), mockMvc, serializationService);
        assertThat(
                events,
                hasItem(
                        hasProperty(
                                "type",
                                is(HistoryEventType.PARCEL_DELIVERED_AT_UPDATED))));
    }
}
