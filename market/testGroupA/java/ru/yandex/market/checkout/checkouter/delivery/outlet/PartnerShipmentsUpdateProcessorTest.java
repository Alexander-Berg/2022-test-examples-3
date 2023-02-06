package ru.yandex.market.checkout.checkouter.delivery.outlet;

import java.time.Clock;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryUpdateStatusValidator;
import ru.yandex.market.checkout.checkouter.delivery.ParcelsUpdateActions;
import ru.yandex.market.checkout.checkouter.delivery.PartnerDeliveryParcelValidator;
import ru.yandex.market.checkout.checkouter.delivery.parcel.MarketDeliveryParcelsUpdateProcessor;
import ru.yandex.market.checkout.checkouter.delivery.shipment.Parcel;
import ru.yandex.market.checkout.checkouter.delivery.shipment.ParcelItem;
import ru.yandex.market.checkout.checkouter.delivery.shipment.ParcelStatus;
import ru.yandex.market.checkout.checkouter.delivery.tracking.CheckpointStatus;
import ru.yandex.market.checkout.checkouter.delivery.tracking.Track;
import ru.yandex.market.checkout.checkouter.delivery.tracking.TrackCheckpoint;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.delivery.ParcelUpdateActions;
import ru.yandex.market.checkout.common.rest.InvalidRequestException;
import ru.yandex.market.checkout.common.util.DeliveryChange;
import ru.yandex.market.checkout.test.providers.OrderProvider;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.DELIVERY;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.PICKUP;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.PROCESSING;

/**
 * @author apershukov
 */
public class PartnerShipmentsUpdateProcessorTest {

    private MarketDeliveryParcelsUpdateProcessor processor;
    private DeliveryUpdateStatusValidator<DeliveryChange> detector;

    private Order order;
    private Delivery newDelivery;

    @BeforeEach
    public void setUp() throws Exception {
        processor = new MarketDeliveryParcelsUpdateProcessor(
                null,
                mock(PartnerDeliveryParcelValidator.class),
                new ParcelStatusGraph(),
                Clock.systemDefaultZone()
        );

        Delivery oldDelivery = new Delivery();

        order = OrderProvider.getBlueOrder();
        order.setId(135135L);
        order.setStatus(OrderStatus.PROCESSING);
        order.setDelivery(oldDelivery);

        detector = new DeliveryUpdateStatusValidator<>(
                EnumSet.of(PROCESSING, DELIVERY, PICKUP), order, false
        );

        newDelivery = new Delivery();
    }

    @Test
    public void testMultipleShipmentsNotAllowed() {
        Assertions.assertThrows(InvalidRequestException.class, () -> {
            newDelivery.setParcels(Arrays.asList(new Parcel(), new Parcel()));
            processShipments();
        });
    }

    @Test
    public void testDroppingShipmentsNotAllowed() {
        Assertions.assertThrows(InvalidRequestException.class, () -> {
            newDelivery.setParcels(Collections.emptyList());
            processShipments();
        });
    }

    @Test
    public void testInsertNewShipment() {
        newDelivery.setParcels(Collections.singletonList(new Parcel()));

        ParcelsUpdateActions actions = processShipments();

        assertThat(actions.getParcelsToInsert(), hasSize(1));
        assertNull(actions.getParcelsToRebind());
        assertNull(actions.getParcelUpdates());
    }

    @Test
    public void testEditShipment() {
        Parcel oldShipment = new Parcel();
        oldShipment.setStatus(ParcelStatus.NEW);
        oldShipment.setId(111L);
        order.getDelivery().setParcels(Collections.singletonList(oldShipment));

        Parcel newShipment = new Parcel();
        newShipment.setStatus(ParcelStatus.CREATED);
        newDelivery.setParcels(Collections.singletonList(newShipment));

        ParcelsUpdateActions actions = processShipments();

        assertNull(actions.getParcelsToInsert());
        assertNull(actions.getParcelsToRebind());
        assertThat(actions.getParcelUpdates(), hasSize(1));

        Parcel shipment = actions.getParcelUpdates().get(0).getParcel();
        assertEquals(ParcelStatus.CREATED, shipment.getStatus());
    }

    @Test
    public void testRebindShipmentIfNullSpecified() {
        Parcel oldShipment = new Parcel();
        oldShipment.setStatus(ParcelStatus.NEW);
        oldShipment.setId(111L);
        order.getDelivery().setParcels(Collections.singletonList(oldShipment));

        ParcelsUpdateActions actions = processShipments();
        assertNull(actions.getParcelsToInsert());
        assertNull(actions.getParcelUpdates());
        assertThat(actions.getParcelsToRebind(), hasSize(1));

        assertEquals(111L, (long) actions.getParcelsToRebind().get(0).getId());
    }

    /**
     * При редактировании свойств отгрузки, отгрузка попадает в список на редактирование
     * а её треки и товары в список привязки
     */
    @Test
    public void testEditShipmentProperties() {
        Parcel newShipment = new Parcel();
        newShipment.setId(1111L);
        newShipment.setParcelItems(null);
        newShipment.setTracks(null);
        newShipment.setStatus(ParcelStatus.NEW);
        newDelivery.setParcels(Collections.singletonList(newShipment));

        Parcel oldShipment = new Parcel();
        oldShipment.setId(1111L);
        oldShipment.addParcelItem(new ParcelItem(1010L, 2));
        order.getDelivery().setParcels(Collections.singletonList(oldShipment));

        Track track = new Track("iddqd", 123L);
        track.addCheckpoint(
                new TrackCheckpoint(
                        234L,
                        "asd",
                        "def",
                        "erer",
                        "yyy",
                        CheckpointStatus.DELIVERED,
                        "123",
                        new Date(),
                        123
                )
        );
        oldShipment.addTrack(track);

        ParcelsUpdateActions actions = processShipments();
        assertTrue(actions.notEmpty());

        List<ParcelUpdateActions> parcelUpdateActions = actions.getParcelUpdates();
        assertThat(parcelUpdateActions, hasSize(1));

        ParcelUpdateActions shipmentActions = parcelUpdateActions.get(0);
        assertThat(shipmentActions.getTracksToRebind(), hasSize(1));
        assertThat(shipmentActions.getItemsToRebind(), hasSize(1));

        Parcel toSave = shipmentActions.getParcel();
        assertEquals(ParcelStatus.NEW, toSave.getStatus());
    }

    private ParcelsUpdateActions processShipments() {
        return processor.processParcels(order, newDelivery.getParcels(), ClientInfo.SYSTEM, detector);
    }
}
