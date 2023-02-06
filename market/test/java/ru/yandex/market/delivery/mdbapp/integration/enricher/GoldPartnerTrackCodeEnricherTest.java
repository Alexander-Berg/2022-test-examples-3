package ru.yandex.market.delivery.mdbapp.integration.enricher;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;

import org.assertj.core.api.SoftAssertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import steps.utils.TestableClock;

import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryServiceType;
import ru.yandex.market.checkout.checkouter.delivery.shipment.Parcel;
import ru.yandex.market.checkout.checkouter.delivery.tracking.Track;
import ru.yandex.market.checkout.checkouter.delivery.tracking.TrackStatus;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatusNotAllowedException;
import ru.yandex.market.checkout.checkouter.shipment.ShipmentUpdateNotAllowedException;
import ru.yandex.market.delivery.mdbapp.components.service.checkouter.client.CheckouterParcelService;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class GoldPartnerTrackCodeEnricherTest {

    private static final long ORDER_ID = 1L;
    private static final long PARCEL_ID = 2L;
    private static final String SHOP_ORDER_ID = "shopOrderId";
    private static final Long DELIVERY_SERVICE_ID = 100L;
    private static final Long INTERNAL_DELIVERY_ID = 1000L;
    private static final String TRACK_CODE_PREFIX = "track_code_";
    private final SoftAssertions softAssertions = new SoftAssertions();

    @Mock
    private CheckouterParcelService checkouterParcelService;
    private TestableClock clock = new TestableClock();

    @Captor
    private ArgumentCaptor<List<Parcel>> parcelsCaptor;

    private GoldPartnerTrackCodeEnricher enricher;

    @Before
    public void setUp() {
        enricher = new GoldPartnerTrackCodeEnricher(checkouterParcelService, clock);
        clock.setFixed(Instant.parse("2019-07-20T00:00:00Z"), ZoneOffset.UTC);
    }

    @After
    public void tearDown() {
        softAssertions.assertAll();
    }

    @Test
    public void enrich() {
        OrderHistoryEvent event = createOrderHistoryEvent();

        enricher.enrich(event);

        verify(checkouterParcelService)
            .replaceParcels(eq(ORDER_ID), parcelsCaptor.capture(), eq(ClientInfo.SYSTEM));

        List<Parcel> parcels = parcelsCaptor.getValue();

        softAssertions.assertThat(parcels).hasSize(1);
        softAssertions.assertThat(parcels.get(0).getTracks()).hasSize(1);

        Track track = parcels.get(0).getTracks().get(0);

        softAssertions.assertThat(track.getOrderId()).isEqualTo(ORDER_ID);
        softAssertions.assertThat(track.getTrackCode()).isEqualTo(TRACK_CODE_PREFIX + ORDER_ID);
        softAssertions.assertThat(track.getDeliveryServiceId()).isEqualTo(DELIVERY_SERVICE_ID);
        softAssertions.assertThat(track.getDeliveryServiceType()).isEqualTo(DeliveryServiceType.CARRIER);
        softAssertions.assertThat(track.getCreationDate()).isEqualTo(Date.from(clock.instant()));
        softAssertions.assertThat(track.getStatus()).isEqualTo(TrackStatus.NEW);
        softAssertions.assertThat(track.getDeliveryId()).isEqualTo(INTERNAL_DELIVERY_ID);
    }

    @Test
    public void enrichWhenCheckouterClientThrowsOrderStatusNotAllowedException() {
        OrderHistoryEvent event = createOrderHistoryEvent();

        Mockito.doThrow(new OrderStatusNotAllowedException("TEST", "TEST", 0))
            .when(checkouterParcelService).replaceParcels(eq(ORDER_ID), anyList(), eq(ClientInfo.SYSTEM));

        enricher.enrich(event);
    }

    @Test
    public void enrichWhenCheckouterClientThrowsShipmentUpdateNotAllowedException() {
        OrderHistoryEvent event = createOrderHistoryEvent();

        Mockito.doThrow(new ShipmentUpdateNotAllowedException("TEST", "TEST", 0))
            .when(checkouterParcelService).replaceParcels(eq(ORDER_ID), anyList(), eq(ClientInfo.SYSTEM));

        enricher.enrich(event);
    }

    private OrderHistoryEvent createOrderHistoryEvent() {
        OrderHistoryEvent orderHistoryEvent = new OrderHistoryEvent();
        orderHistoryEvent.setOrderAfter(createOrder());

        return orderHistoryEvent;
    }

    private Order createOrder() {
        Order order = new Order();
        order.setShopOrderId(SHOP_ORDER_ID);
        order.setDelivery(createDelivery());
        order.setInternalDeliveryId(INTERNAL_DELIVERY_ID);
        order.setId(ORDER_ID);

        return order;
    }

    private Delivery createDelivery() {
        Delivery delivery = new Delivery();
        delivery.setDeliveryServiceId(DELIVERY_SERVICE_ID);

        return delivery;
    }
}
