package ru.yandex.market.checkout.storage.track;

import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryServiceType;
import ru.yandex.market.checkout.checkouter.delivery.shipment.Parcel;
import ru.yandex.market.checkout.checkouter.delivery.tracking.Track;
import ru.yandex.market.checkout.checkouter.delivery.tracking.TrackId;
import ru.yandex.market.checkout.checkouter.delivery.tracking.TrackStatus;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.storage.shipment.ParcelWritingDao;
import ru.yandex.market.checkout.checkouter.storage.track.TrackReadingDao;
import ru.yandex.market.checkout.checkouter.storage.track.TrackWritingDao;
import ru.yandex.market.checkout.checkouter.storage.track.history.TrackHistoryDao;
import ru.yandex.market.checkout.common.rest.DuplicateKeyException;
import ru.yandex.market.checkout.helpers.OrderDeliveryHelper;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.test.providers.OrderProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.MOCK_DELIVERY_SERVICE_ID;

public class TrackWritingDaoTest extends AbstractWebTestBase {

    @Autowired
    TrackWritingDao trackWritingDao;
    @Autowired
    TrackReadingDao trackReadingDao;
    @Autowired
    ParcelWritingDao parcelWritingDao;
    @Autowired
    TrackHistoryDao trackHistoryDao;
    @Autowired
    OrderDeliveryHelper orderDeliveryHelper;

    @Test
    void insertTracksShouldWork() {
        // given:
        Order order = createOrder();
        long orderId = order.getId();
        Parcel parcel = order.getDelivery().getParcels().get(0);
        long parcelId = parcel.getId();
        TrackId trackId = new TrackId("track-code", MOCK_DELIVERY_SERVICE_ID);
        Track track = new Track(orderId, trackId);
        track.setDeliveryServiceType(DeliveryServiceType.FULFILLMENT);
        track.setTrackerId(849843L);
        track.setActualCheckpointId(56835L);
        track.setStatus(TrackStatus.NEW);
        long deliveryId = getDeliveryId(orderId);

        // when:
        transactionTemplate.execute(t -> {
            trackWritingDao.insertTracks(List.of(track), orderId, deliveryId, parcelId, 123L);
            return null;
        });

        // then:
        Collection<Track> tracks = trackReadingDao.findByBusinessIds(List.of(trackId));
        assertThat(tracks).hasSize(1);
        Track fetchedTrack = tracks.iterator().next();
        assertThat(fetchedTrack.getTrackCode()).isEqualTo(trackId.getTrackCode());
        assertThat(fetchedTrack.getDeliveryServiceId()).isEqualTo(MOCK_DELIVERY_SERVICE_ID);
        assertThat(fetchedTrack.getOrderId()).isEqualTo(orderId);
        assertThat(fetchedTrack.getDeliveryId()).isEqualTo(deliveryId);
        assertThat(fetchedTrack.getShipmentId()).isEqualTo(parcelId);
        assertThat(fetchedTrack.getBusinessId()).isEqualTo(trackId);
        assertThat(fetchedTrack.getCreationDate()).isCloseTo(new Date(), 5000);
        assertThat(fetchedTrack.getTrackerId()).isEqualTo(track.getTrackerId());
        assertThat(fetchedTrack.getStatus()).isEqualTo(track.getStatus());
    }

    @Test
    void insertTracksShouldFailIfTrackAlreadyExitsForAnotherOrder() {
        // given:
        Order order1 = createOrder();
        long orderId1 = order1.getId();
        long parcelId1 = order1.getDelivery().getParcels().get(0).getId();
        TrackId commonTrackId = new TrackId("track-code", MOCK_DELIVERY_SERVICE_ID);
        Track track1 = new Track(orderId1, commonTrackId);
        track1.setDeliveryServiceType(DeliveryServiceType.FULFILLMENT);
        long deliveryId1 = getDeliveryId(orderId1);
        transactionTemplate.execute(t -> {
            trackWritingDao.insertTracks(List.of(track1), orderId1, deliveryId1, parcelId1, 123L);
            return null;
        });
        Order order2 = createOrder();
        long orderId2 = order2.getId();
        long parcelId2 = order2.getDelivery().getParcels().get(0).getId();
        Track track2 = new Track(orderId2, commonTrackId);
        track2.setDeliveryServiceType(DeliveryServiceType.FULFILLMENT);
        long deliveryId2 = getDeliveryId(orderId2);

        // when:
        Executable exec = () -> transactionTemplate.execute(t -> {
            trackWritingDao.insertTracks(List.of(track2), orderId2, deliveryId2, parcelId2, 123L);
            return null;
        });

        // then:
        DuplicateKeyException exception = assertThrows(DuplicateKeyException.class, exec);
        assertThat(exception.getMessage()).contains("There are tracks with the same trackCode and deliveryServiceId " +
                "in orders: ");

    }

    @Test
    void insertTracksShouldNotFailIfTrackAlreadyExitsForSameOrder() {
        // given:
        Order order1 = createOrder();
        long orderId1 = order1.getId();
        long parcelId1 = order1.getDelivery().getParcels().get(0).getId();
        TrackId trackId = new TrackId("track-code", MOCK_DELIVERY_SERVICE_ID);
        Track track1 = new Track(orderId1, trackId);
        track1.setDeliveryServiceType(DeliveryServiceType.FULFILLMENT);
        long deliveryId1 = getDeliveryId(orderId1);
        transactionTemplate.execute(t -> {
            trackWritingDao.insertTracks(List.of(track1), orderId1, deliveryId1, parcelId1, 123L);
            return null;
        });

        // when:
        transactionTemplate.execute(t -> {
            trackWritingDao.insertTracks(List.of(track1), orderId1, deliveryId1, parcelId1, 123L);
            return null;
        });

        // then:
        Collection<Track> tracks = trackReadingDao.findByBusinessIds(List.of(trackId));
        assertThat(tracks).hasSize(2);
    }

    private Order createOrder() {
        Order order = OrderProvider.getBlueOrder();
        return orderCreateHelper.createOrder(BlueParametersProvider.defaultBlueOrderParameters(order));

    }

    private long getDeliveryId(long orderId) {
        return masterJdbcTemplate.queryForObject("SELECT delivery_id FROM orders WHERE id = " + orderId, Long.class);
    }

    @Test
    void updateTrackShouldWork() {
        // given:
        Order order = createOrder();
        long orderId = order.getId();
        Parcel parcel = order.getDelivery().getParcels().get(0);
        long parcelId = parcel.getId();
        TrackId trackId = new TrackId("ABC1917", MOCK_DELIVERY_SERVICE_ID);
        Track track = new Track(orderId, trackId);
        track.setDeliveryServiceType(DeliveryServiceType.FULFILLMENT);
        track.setTrackerId(563453L);
        track.setActualCheckpointId(1235253L);
        track.setStatus(TrackStatus.NEW);
        long deliveryId = getDeliveryId(orderId);
        transactionTemplate.execute(t -> {
            trackWritingDao.insertTracks(List.of(track), orderId, deliveryId, parcelId, 123L);
            return null;
        });
        Order order2 = createOrder();
        long orderId2 = order2.getId();
        long parcelId2 = order2.getDelivery().getParcels().get(0).getId();
        long deliveryId2 = getDeliveryId(orderId2);
        Track savedTrack = trackReadingDao.findByBusinessIds(List.of(trackId)).iterator().next();

        // when:
        transactionTemplate.execute(t -> {
            trackWritingDao.updateTracks(deliveryId2, parcelId2, List.of(savedTrack), 342342L);
            return null;
        });

        // then:
        Collection<Track> tracks = trackReadingDao.findByBusinessIds(List.of(trackId));
        assertThat(tracks).hasSize(1);
        Track fetchedTrack = tracks.iterator().next();
        assertThat(fetchedTrack.getDeliveryId()).isEqualTo(deliveryId2);
        assertThat(fetchedTrack.getShipmentId()).isEqualTo(parcelId2);
    }

    @Test
    void bindTrackShouldWork() {
        // given:
        Order order = createOrder();
        long orderId = order.getId();
        Parcel parcel = order.getDelivery().getParcels().get(0);
        long parcelId = parcel.getId();
        TrackId trackId = new TrackId("LIDI3488043", MOCK_DELIVERY_SERVICE_ID);
        Track track = new Track(orderId, trackId);
        track.setDeliveryServiceType(DeliveryServiceType.FULFILLMENT);
        track.setStatus(TrackStatus.NEW);
        long deliveryId = getDeliveryId(orderId);
        transactionTemplate.execute(t -> {
            trackWritingDao.insertTracks(List.of(track), orderId, deliveryId, parcelId, 123L);
            return null;
        });
        Order order2 = createOrder();
        long orderId2 = order2.getId();
        long parcelId2 = order2.getDelivery().getParcels().get(0).getId();
        long deliveryId2 = getDeliveryId(orderId2);
        Track savedTrack = trackReadingDao.findByBusinessIds(List.of(trackId)).iterator().next();

        // when:
        transactionTemplate.execute(t -> {
            trackWritingDao.bindTracks(deliveryId2, parcelId2, List.of(savedTrack), 342342L);
            return null;
        });

        // then:
        Collection<Track> tracks = trackReadingDao.findByBusinessIds(List.of(trackId));
        assertThat(tracks).hasSize(1);
        Track fetchedTrack = tracks.iterator().next();
        assertThat(fetchedTrack.getDeliveryId()).isEqualTo(deliveryId2);
        assertThat(fetchedTrack.getShipmentId()).isEqualTo(parcelId2);
    }

    @Test
    void bindTrackToDlieveryShouldWork() {
        // given:
        Order order = createOrder();
        long orderId = order.getId();
        Parcel parcel = order.getDelivery().getParcels().get(0);
        long parcelId = parcel.getId();
        TrackId trackId = new TrackId("LIDI3488043", MOCK_DELIVERY_SERVICE_ID);
        Track track = new Track(orderId, trackId);
        track.setDeliveryServiceType(DeliveryServiceType.FULFILLMENT);
        track.setStatus(TrackStatus.NEW);
        long deliveryId = getDeliveryId(orderId);
        transactionTemplate.execute(t -> {
            trackWritingDao.insertTracks(List.of(track), orderId, deliveryId, parcelId, 123L);
            return null;
        });
        Order order2 = createOrder();
        long orderId2 = order2.getId();
        long deliveryId2 = getDeliveryId(orderId2);
        Track savedTrack = trackReadingDao.findByBusinessIds(List.of(trackId)).iterator().next();

        // when:
        transactionTemplate.execute(t -> {
            trackWritingDao.bindTracksToDelivery(List.of(savedTrack), deliveryId2, 342342L);
            return null;
        });

        // then:
        Collection<Track> tracks = trackReadingDao.findByBusinessIds(List.of(trackId));
        assertThat(tracks).hasSize(1);
        Track fetchedTrack = tracks.iterator().next();
        assertThat(fetchedTrack.getDeliveryId()).isEqualTo(deliveryId2);
    }

}
