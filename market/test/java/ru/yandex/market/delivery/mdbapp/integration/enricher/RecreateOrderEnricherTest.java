package ru.yandex.market.delivery.mdbapp.integration.enricher;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.shipment.Parcel;
import ru.yandex.market.checkout.checkouter.delivery.tracking.Track;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.order.Order;

import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class RecreateOrderEnricherTest {

    private static final String DUMMY_TRACK_CODE = "dummy_track_code";

    private RecreateOrderEnricher recreateOrderEnricher;

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Parameterized.Parameter
    public OrderHistoryEvent incomingEvent;

    @Parameterized.Parameter(1)
    public boolean expectException;

    @Before
    public void setUp() {
        recreateOrderEnricher = new RecreateOrderEnricher();
    }

    @Test
    public void testEnrich() throws Exception {
        if (expectException) {
            thrown.expect(EnrichmentFailException.class);
        }

        OrderHistoryEvent enrichedEvent = recreateOrderEnricher.enrich(incomingEvent);
        assertEquals(
            "RecreateOrderEnricher cannot enrich event correctly",
            DUMMY_TRACK_CODE,
            Optional.of(enrichedEvent)
                .map(OrderHistoryEvent::getOrderAfter)
                .map(Order::getDelivery)
                .map(Delivery::getParcels)
                .flatMap(p -> p.stream().findFirst())
                .map(Parcel::getTracks)
                .orElse(Collections.emptyList())
                .stream()
                .findFirst()
                .map(Track::getTrackCode)
                .get()
        );
    }


    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
            {
                makeEventWithoutTrackCodeInOrderAfter(),
                true
            },
            {
                makeEventWithTrackCodeInShipments(),
                false
            },
            {
                makeEventWithoutTrackCodeWithNonExistingOrderShipmentId(),
                true
            },
        });
    }

    private static OrderHistoryEvent makeEventWithoutTrackCodeInOrderAfter() {
        Track track = new Track();
        track.setTrackCode(DUMMY_TRACK_CODE);

        Delivery deliveryForOrderBefore = new Delivery();
        Parcel parcel = new Parcel();
        parcel.addTrack(track);

        deliveryForOrderBefore.addParcel(parcel);

        Delivery deliveryForOrderAfter = new Delivery();

        Order orderBefore = new Order();
        orderBefore.setDelivery(deliveryForOrderBefore);

        Order orderAfter = new Order();
        orderAfter.setDelivery(deliveryForOrderAfter);

        OrderHistoryEvent orderHistoryEvent = new OrderHistoryEvent();
        orderHistoryEvent.setOrderBefore(orderBefore);
        orderHistoryEvent.setOrderAfter(orderAfter);

        return orderHistoryEvent;
    }

    private static OrderHistoryEvent makeEventWithTrackCodeInShipments() {
        Track track = new Track();
        track.setTrackCode(DUMMY_TRACK_CODE);

        Delivery deliveryForOrderBefore = new Delivery();
        Parcel shipmentForOrderBefore = new Parcel();

        shipmentForOrderBefore.setTracks(Collections.singletonList(track));

        deliveryForOrderBefore.setParcels(Collections.singletonList(shipmentForOrderBefore));
        Order orderBefore = new Order();
        orderBefore.setDelivery(deliveryForOrderBefore);


        Order orderAfter = new Order();
        Delivery deliveryForOrderAfter = new Delivery();
        orderAfter.setDelivery(deliveryForOrderAfter);
        deliveryForOrderAfter.setParcels(Collections.singletonList(shipmentForOrderBefore));

        OrderHistoryEvent orderHistoryEvent = new OrderHistoryEvent();
        orderHistoryEvent.setOrderBefore(orderBefore);
        orderHistoryEvent.setOrderAfter(orderAfter);

        return orderHistoryEvent;
    }


    private static OrderHistoryEvent makeEventWithoutTrackCodeWithNonExistingOrderShipmentId() {
        Delivery deliveryForOrderAfter = new Delivery();
        Order orderAfter = new Order();
        orderAfter.setDelivery(deliveryForOrderAfter);
        Order orderBefore = new Order();

        OrderHistoryEvent orderHistoryEvent = new OrderHistoryEvent();
        orderHistoryEvent.setOrderBefore(orderBefore);
        orderHistoryEvent.setOrderAfter(orderAfter);
        orderHistoryEvent.getOrderBefore().setId(3L); //not existing ID

        return orderHistoryEvent;

    }
}
