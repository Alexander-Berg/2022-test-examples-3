package ru.yandex.market.checkout.checkouter.delivery.parcel;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.Calendar;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.delivery.AddressImpl;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryDates;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryUpdateStatusValidator;
import ru.yandex.market.checkout.checkouter.delivery.SelfDeliveryPossibleUpdateProvider;
import ru.yandex.market.checkout.checkouter.delivery.shipment.Parcel;
import ru.yandex.market.checkout.checkouter.delivery.tracking.Track;
import ru.yandex.market.checkout.checkouter.feature.CheckouterFeatureReader;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.delivery.ParcelUpdateActions;
import ru.yandex.market.checkout.common.util.DeliveryChange;
import ru.yandex.market.checkout.test.providers.OrderProvider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.DELIVERY;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.PICKUP;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.PROCESSING;

/**
 * @author apershukov
 */
public class ShopDeliveryParcelsUpdateProcessorTest {

    private static final long OLD_SHIPMENT_ID = 45;
    private static final long ORDER_ITEM_ID = 1;

    private ShopDeliveryParcelsUpdateProcessor processor;
    @Mock
    private CheckouterFeatureReader checkouterFeatureReader;

    private Order order;

    @BeforeEach
    public void setUp() throws Exception {
        processor = new ShopDeliveryParcelsUpdateProcessor(
                new SelfDeliveryPossibleUpdateProvider(),
                Clock.systemDefaultZone(),
                checkouterFeatureReader
        );

        AddressImpl oldShopAddress = new AddressImpl();
        oldShopAddress.setCountry("Россия");
        oldShopAddress.setCity("Москва");
        oldShopAddress.setHouse("15");
        oldShopAddress.setRecipient("Лев Толстой");
        oldShopAddress.setPhone("+79272403522");

        Delivery oldDelivery = new Delivery();
        oldDelivery.setType(DeliveryType.DELIVERY);
        oldDelivery.setServiceName("ООО Рога и Копыта");
        oldDelivery.setPrice(BigDecimal.valueOf(100));
        oldDelivery.setBuyerPrice(BigDecimal.valueOf(100));
        oldDelivery.setShopAddress(oldShopAddress);

        Parcel oldShipment = new Parcel();
        oldShipment.setId(OLD_SHIPMENT_ID);
        oldDelivery.setParcels(Collections.singletonList(oldShipment));

        Calendar fromCalendar = Calendar.getInstance();
        fromCalendar.set(2017, Calendar.JANUARY, 1);

        Calendar toCalendar = Calendar.getInstance();
        toCalendar.set(2017, Calendar.JANUARY, 2);

        oldDelivery.setDeliveryDates(new DeliveryDates(fromCalendar.getTime(), toCalendar.getTime()));

        order = OrderProvider.getBlueOrder();
        order.setId(135135L);
        order.setStatus(OrderStatus.PROCESSING);
        order.setDelivery(oldDelivery);

        OrderItem item = new OrderItem();
        item.setId(ORDER_ITEM_ID);
        item.setCount(5);
        order.addItem(item);
    }

    /**
     * Не измененные треки должны помещяться в список для на привязку
     */
    @Test
    public void shouldMarkNotChangedTrackAsTrackToRebind() {
        order.setStatus(PROCESSING);

        Track oldTrack = new Track();
        oldTrack.setDeliveryServiceId(99L);
        oldTrack.setId(111L);
        oldTrack.setTrackCode("99");

        Track newTrack = new Track();
        newTrack.setDeliveryServiceId(99L);
        newTrack.setTrackCode("99");

        ParcelUpdateActions actions = new ParcelUpdateActions(order.getDelivery().getParcels().get(0));
        processor.processTracks(
                Collections.singletonList(oldTrack),
                Collections.singletonList(newTrack),
                1,
                ClientInfo.SYSTEM,
                new DeliveryUpdateStatusValidator<>(
                        EnumSet.of(PROCESSING, DELIVERY, PICKUP), order, false
                ), actions);

        assertEquals(1, actions.getTracksToRebind().size());
        assertNull(actions.getTracksToInsert());
        assertNull(actions.getTracksToUpdate());
    }

    /**
     * Отредактированные треки должны помещаться в очередь на обновление
     */
    @Test
    public void shouldMarkChangedTrackAsTrackToUpdate() {
        order.setStatus(PROCESSING);

        DeliveryUpdateStatusValidator<DeliveryChange> detector = new DeliveryUpdateStatusValidator<>(
                EnumSet.of(PROCESSING, DELIVERY, PICKUP), order, false
        );

        Track oldTrack = new Track();
        oldTrack.setId(111L);
        oldTrack.setDeliveryServiceId(99L);
        oldTrack.setTrackCode("99");

        List<Track> oldTracks = Collections.singletonList(oldTrack);

        Track newTrack = new Track();
        newTrack.setDeliveryServiceId(99L);
        newTrack.setTrackCode("99");
        newTrack.setTrackerId(135135L);

        List<Track> newTracks = Collections.singletonList(newTrack);

        ParcelUpdateActions actions = new ParcelUpdateActions(order.getDelivery().getParcels().get(0));
        processor.processTracks(oldTracks, newTracks, 1, ClientInfo.SYSTEM, detector, actions);

        assertEquals(1, actions.getTracksToUpdate().size());
        assertNull(actions.getTracksToInsert());
        assertNull(actions.getTracksToRebind());
        assertTrue(detector.getDetector().isChanged());
    }

    /**
     * При добавлении треков к существующему отправлению новые треки должны
     * помещаться в список на добавление
     */
    @Test
    public void shouldMarkNewTrackAsTrackToInsert() {
        order.setStatus(PROCESSING);

        DeliveryUpdateStatusValidator<DeliveryChange> detector = new DeliveryUpdateStatusValidator<>(
                EnumSet.of(PROCESSING, DELIVERY, PICKUP), order, false
        );

        List<Track> oldTracks = Collections.emptyList();

        Track newTrack = new Track();
        newTrack.setDeliveryServiceId(99L);
        newTrack.setTrackCode("99");

        List<Track> newTracks = Collections.singletonList(newTrack);

        ParcelUpdateActions actions = new ParcelUpdateActions(order.getDelivery().getParcels().get(0));
        processor.processTracks(oldTracks, newTracks, 1, ClientInfo.SYSTEM, detector, actions);

        assertEquals(1, actions.getTracksToInsert().size());
        assertNull(actions.getTracksToRebind());
        assertNull(actions.getTracksToUpdate());
    }
}
