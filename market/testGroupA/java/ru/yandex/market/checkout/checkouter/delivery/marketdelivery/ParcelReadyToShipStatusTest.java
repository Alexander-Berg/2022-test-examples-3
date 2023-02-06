package ru.yandex.market.checkout.checkouter.delivery.marketdelivery;

import java.util.Collections;
import java.util.function.Consumer;

import com.google.common.collect.Iterables;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.shipment.Parcel;
import ru.yandex.market.checkout.checkouter.delivery.shipment.ParcelStatus;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderStatusNotAllowedException;
import ru.yandex.market.checkout.helpers.OrderPayHelper;
import ru.yandex.market.checkout.helpers.YandexMarketDeliveryHelper;
import ru.yandex.market.checkout.test.providers.TrackProvider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.yandex.market.checkout.checkouter.delivery.shipment.ParcelStatus.CREATED;
import static ru.yandex.market.checkout.checkouter.delivery.shipment.ParcelStatus.NEW;
import static ru.yandex.market.checkout.checkouter.delivery.shipment.ParcelStatus.READY_TO_SHIP;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.MOCK_DELIVERY_SERVICE_ID;

public class ParcelReadyToShipStatusTest extends AbstractWebTestBase {

    @Autowired
    private YandexMarketDeliveryHelper yandexMarketDeliveryHelper;
    @Autowired
    private OrderPayHelper orderPayHelper;

    @Test
    public void movesCreatedParcelToReadyToShipForDeliveryOrderWithLabelUrlWithTracks() {
        Order order = yandexMarketDeliveryHelper.createMarDoOrder(MOCK_DELIVERY_SERVICE_ID, false);
        orderPayHelper.payForOrder(order);
        assertEquals(
                NEW,
                Iterables.getOnlyElement(order.getDelivery().getParcels()).getStatus()
        );
        ParcelStatus parcelStatus = changeStatusToCreated(
                order,
                parcel -> {
                    parcel.setLabelURL("some-label-url");
                    parcel.setTracks(Collections.singletonList(TrackProvider.createTrack()));
                }
        );
        assertEquals(READY_TO_SHIP, parcelStatus);
    }

    @Test
    public void doesNotMoveCreatedParcelToReadyToShipForDeliveryOrderWithLabelUrlWithoutTracks() {
        Order order = yandexMarketDeliveryHelper.createMarDoOrder(MOCK_DELIVERY_SERVICE_ID, false);
        orderPayHelper.payForOrder(order);
        assertEquals(
                NEW,
                Iterables.getOnlyElement(order.getDelivery().getParcels()).getStatus()
        );
        ParcelStatus parcelStatus = changeStatusToCreated(
                order,
                parcel -> parcel.setLabelURL("some-label-url")
        );
        assertEquals(CREATED, parcelStatus);
    }

    @Test
    public void doesNotMoveCreatedParcelToReadyToShipForDeliveryOrderWithoutLabelUrlWithTracks() {
        Order order = yandexMarketDeliveryHelper.createMarDoOrder(MOCK_DELIVERY_SERVICE_ID, false);
        orderPayHelper.payForOrder(order);
        assertEquals(
                NEW,
                Iterables.getOnlyElement(order.getDelivery().getParcels()).getStatus()
        );
        ParcelStatus parcelStatus = changeStatusToCreated(
                order,
                parcel -> parcel.setTracks(Collections.singletonList(TrackProvider.createTrack()))
        );
        assertEquals(CREATED, parcelStatus);
    }

    @Test
    public void doesNotMoveCreatedParcelToReadyToShipForDeliveryOrderWithoutLabelUrlWithoutTracks() {
        Order order = yandexMarketDeliveryHelper.createMarDoOrder(MOCK_DELIVERY_SERVICE_ID, false);
        orderPayHelper.payForOrder(order);
        assertEquals(
                NEW,
                Iterables.getOnlyElement(order.getDelivery().getParcels()).getStatus()
        );
        ParcelStatus parcelStatus = changeStatusToCreated(
                order,
                parcel -> {
                }
        );
        assertEquals(CREATED, parcelStatus);
    }

    @Test
    public void movesCreatedParcelToReadyToShipForProcessingOrderWithLabelUrlWithTracks() {
        Order order = yandexMarketDeliveryHelper.createMarDoOrder(MOCK_DELIVERY_SERVICE_ID, false);
        orderPayHelper.payForOrder(order);
        order = orderService.getOrder(order.getId());
        assertEquals(OrderStatus.PROCESSING, order.getStatus());
        assertEquals(
                NEW,
                Iterables.getOnlyElement(order.getDelivery().getParcels()).getStatus()
        );
        ParcelStatus parcelStatus = changeStatusToCreated(
                order,
                parcel -> {
                    parcel.setLabelURL("some-label-url");
                    parcel.setTracks(Collections.singletonList(TrackProvider.createTrack()));
                }
        );
        assertEquals(READY_TO_SHIP, parcelStatus);
    }

    @Test
    public void doesNotMoveCreatedParcelToReadyToShipForProcessingOrderWithLabelUrlWithoutTracks() {
        Order order = yandexMarketDeliveryHelper.createMarDoOrder(MOCK_DELIVERY_SERVICE_ID, false);
        orderPayHelper.payForOrder(order);
        order = orderService.getOrder(order.getId());
        assertEquals(OrderStatus.PROCESSING, order.getStatus());
        assertEquals(
                NEW,
                Iterables.getOnlyElement(order.getDelivery().getParcels()).getStatus()
        );
        ParcelStatus parcelStatus = changeStatusToCreated(
                order,
                parcel -> parcel.setLabelURL("some-label-url")
        );
        assertEquals(CREATED, parcelStatus);
    }

    @Test
    public void movesCreatedParcelToReadyToShipForProcessingOrderOnSettingLabelUrl() {
        Order order = yandexMarketDeliveryHelper.createMarDoOrder(MOCK_DELIVERY_SERVICE_ID, false);
        orderPayHelper.payForOrder(order);
        order = orderService.getOrder(order.getId());
        assertEquals(OrderStatus.PROCESSING, order.getStatus());
        assertEquals(
                NEW,
                Iterables.getOnlyElement(order.getDelivery().getParcels()).getStatus()
        );
        ParcelStatus parcelStatus = changeStatusToCreated(
                order,
                parcel -> parcel.setTracks(Collections.singletonList(TrackProvider.createTrack()))
        );
        assertEquals(CREATED, parcelStatus);

        parcelStatus = changeStatusToCreated(
                order,
                parcel -> parcel.setLabelURL("some-label-url")
        );
        assertEquals(READY_TO_SHIP, parcelStatus);
    }

    @Test
    public void movesCreatedParcelToReadyToShipForProcessingOrderOnSettingTracks() {
        Order order = yandexMarketDeliveryHelper.createMarDoOrder(MOCK_DELIVERY_SERVICE_ID, false);
        orderPayHelper.payForOrder(order);
        order = orderService.getOrder(order.getId());
        assertEquals(OrderStatus.PROCESSING, order.getStatus());
        assertEquals(
                NEW,
                Iterables.getOnlyElement(order.getDelivery().getParcels()).getStatus()
        );
        ParcelStatus parcelStatus = changeStatusToCreated(
                order,
                parcel -> parcel.setLabelURL("some-label-url")
        );
        assertEquals(CREATED, parcelStatus);

        parcelStatus = changeStatusToCreated(
                order,
                parcel -> parcel.setTracks(Collections.singletonList(TrackProvider.createTrack()))
        );
        assertEquals(READY_TO_SHIP, parcelStatus);
    }

    @Test
    public void forbidsUpdateForDeliveredOrder() {
        Assertions.assertThrows(OrderStatusNotAllowedException.class, () -> {
            Order order = yandexMarketDeliveryHelper.createMarDoOrder(MOCK_DELIVERY_SERVICE_ID, false);
            orderPayHelper.payForOrder(order);

            Delivery delivery = new Delivery();
            Parcel labelUrlUpdate = new Parcel();
            labelUrlUpdate.setLabelURL("some-label-url");
            delivery.setParcels(Collections.singletonList(labelUrlUpdate));
            order = client.updateOrderDelivery(order.getId(), ClientRole.SYSTEM, 0L, delivery);

            client.updateOrderStatus(
                    order.getId(), ClientRole.SYSTEM, 0L, null, OrderStatus.DELIVERED, null);
            assertEquals(
                    NEW,
                    Iterables.getOnlyElement(order.getDelivery().getParcels()).getStatus()
            );
            changeStatusToCreated(
                    order,
                    parcel -> {
                    }
            );
        });
    }

    private ParcelStatus changeStatusToCreated(Order order, Consumer<Parcel> parcelModifier) {
        Delivery delivery = new Delivery();
        Parcel parcel = new Parcel();
        parcel.setStatus(CREATED);
        parcelModifier.accept(parcel);
        delivery.setParcels(Collections.singletonList(parcel));
        order = client.updateOrderDelivery(order.getId(), ClientRole.SYSTEM, 0L, delivery);
        return Iterables.getOnlyElement(order.getDelivery().getParcels()).getStatus();
    }
}
