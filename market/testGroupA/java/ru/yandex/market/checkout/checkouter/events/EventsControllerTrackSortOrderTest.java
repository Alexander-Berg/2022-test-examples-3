package ru.yandex.market.checkout.checkouter.events;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.google.common.collect.Iterables;
import io.qameta.allure.Epic;
import io.qameta.allure.Story;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.allure.Epics;
import ru.yandex.market.checkout.allure.Stories;
import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.OrderFilter;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.shipment.Parcel;
import ru.yandex.market.checkout.checkouter.delivery.tracking.Track;
import ru.yandex.market.checkout.checkouter.delivery.tracking.TrackId;
import ru.yandex.market.checkout.checkouter.event.HistoryEventType;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvents;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.helpers.EventsQueueGetHelper;
import ru.yandex.market.checkout.helpers.OrderDeliveryHelper;
import ru.yandex.market.checkout.helpers.OrderPayHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.test.providers.TrackProvider;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;

public class EventsControllerTrackSortOrderTest extends AbstractWebTestBase {

    public static final String ANOTHER_TRACK_CODE = "defdef";

    @Autowired
    private OrderDeliveryHelper orderDeliveryHelper;
    @Autowired
    private EventsQueueGetHelper eventsQueueGetHelper;
    @Autowired
    private OrderPayHelper orderPayHelper;

    @Epic(Epics.GET_ORDER)
    @Story(Stories.ORDERS_EVENTS)
    @DisplayName("Треки должны сортироваться в порядке добавления")
    @Test
    public void testSortingOrder() throws Exception {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        Order order = orderCreateHelper.createOrder(parameters);

        orderPayHelper.payForOrder(order);

        Track track = TrackProvider.createTrack();
        Track anotherTrack = TrackProvider.createTrack(ANOTHER_TRACK_CODE, TrackProvider.DELIVERY_SERVICE_ID);

        Parcel orderShipment = new Parcel();
        orderShipment.setTracks(Arrays.asList(track, anotherTrack));

        Delivery delivery = new Delivery();
        delivery.setParcels(Collections.singletonList(orderShipment));

        Order updated = orderDeliveryHelper.updateOrderDelivery(order.getId(), ClientInfo.SYSTEM, delivery);

        orderUpdateService.updateTrackSetTrackerId(order.getId(), new TrackId(TrackProvider.TRACK_CODE,
                TrackProvider.DELIVERY_SERVICE_ID), 123L);

        OrderHistoryEvents events = eventsQueueGetHelper.getOrderHistoryEvents(null, null, null,
                OrderFilter.builder().setRgb(Color.BLUE).build());

        List<OrderHistoryEvent> deliveryUpdateEvents = events.getContent()
                .stream()
                .filter(ohe -> ohe.getType() == HistoryEventType.ORDER_DELIVERY_UPDATED)
                .collect(Collectors.toList());

        assertThat(deliveryUpdateEvents, hasSize(2));
        OrderHistoryEvent addTracksEvent = deliveryUpdateEvents.get(0);
        checkEvent(addTracksEvent);

        OrderHistoryEvent updateTrackSetTrackerId = deliveryUpdateEvents.get(1);
        checkEvent(updateTrackSetTrackerId);
    }

    private void checkEvent(OrderHistoryEvent deliveryUpdateEvent) {
        List<String> trackCodes =
                Iterables.getOnlyElement(deliveryUpdateEvent.getOrderAfter().getDelivery().getParcels())
                .getTracks()
                .stream().map(Track::getTrackCode)
                .collect(Collectors.toList());

        assertThat(trackCodes, Matchers.contains(TrackProvider.TRACK_CODE, ANOTHER_TRACK_CODE));
    }
}
