package ru.yandex.market.logistics.logistics4shops.factory;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import lombok.RequiredArgsConstructor;
import org.mockito.ArgumentCaptor;
import org.springframework.stereotype.Component;

import ru.yandex.market.checkout.checkouter.client.CheckouterAPI;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryServiceType;
import ru.yandex.market.checkout.checkouter.delivery.parcel.ParcelBox;
import ru.yandex.market.checkout.checkouter.delivery.parcel.ParcelBoxItem;
import ru.yandex.market.checkout.checkouter.delivery.shipment.Parcel;
import ru.yandex.market.checkout.checkouter.delivery.tracking.Track;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.OrderNotFoundException;
import ru.yandex.market.checkout.checkouter.order.OrderSearchRequest;
import ru.yandex.market.checkout.checkouter.request.OrderRequest;
import ru.yandex.market.checkout.checkouter.request.RequestClientInfo;
import ru.yandex.market.checkout.common.rest.Pager;
import ru.yandex.market.common.report.model.FeedOfferId;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.safeRefEq;

@Component
@RequiredArgsConstructor
@ParametersAreNonnullByDefault
public class CheckouterFactory {

    public static final long CHECKOUTER_SHOP_ID = 200100;
    public static final long CHECKOUTER_ORDER_ID = 123456L;
    public static final long CHECKOUTER_PARCEL_ID = 101L;
    public static final long DS_ID = 123L;
    public static final String DS_TRACK_CODE = "external-track-ds";
    public static final RequestClientInfo SYSTEM_USER = RequestClientInfo.builder(ClientRole.SYSTEM).build();

    private final CheckouterAPI checkouterAPI;

    @Nonnull
    public AutoCloseable mockGetOrderTracks(
        Long orderId,
        Long shopId,
        @Nullable List<Track> tracks
    ) {
        when(checkouterAPI.getTracksByOrderId(orderId, ClientRole.SHOP, shopId)).thenReturn(tracks);
        return () -> verify(checkouterAPI).getTracksByOrderId(orderId, ClientRole.SHOP, shopId);
    }

    @Nonnull
    public AutoCloseable mockGetOrderTracksNotFound(
        Long orderId,
        Long shopId
    ) {
        when(checkouterAPI.getTracksByOrderId(orderId, ClientRole.SHOP, shopId))
            .thenThrow(new OrderNotFoundException(orderId));
        return () -> verify(checkouterAPI).getTracksByOrderId(orderId, ClientRole.SHOP, shopId);
    }

    @Nonnull
    public AutoCloseable mockPutOrderTracks(
        Long orderId,
        Long shopId,
        ArgumentCaptor<List<Track>> tracksCaptor
    ) {
        when(checkouterAPI.updateDeliveryTracks(
            eq(orderId),
            eq(CheckouterFactory.CHECKOUTER_PARCEL_ID),
            anyList(),
            eq(ClientRole.SHOP),
            eq(shopId)
        )).thenAnswer(invocation -> invocation.<List<Track>>getArgument(2));
        return () -> verify(checkouterAPI).updateDeliveryTracks(
            eq(orderId),
            eq(CheckouterFactory.CHECKOUTER_PARCEL_ID),
            tracksCaptor.capture(),
            eq(ClientRole.SHOP),
            eq(shopId)
        );
    }

    @Nonnull
    public AutoCloseable mockGetOrder(
        RequestClientInfo requestClientInfo,
        Long orderId,
        Order returnValue
    ) {
        OrderRequest orderRequest = OrderRequest.builder(orderId).build();
        when(checkouterAPI.getOrder(safeRefEq(requestClientInfo), safeRefEq(orderRequest)))
            .thenReturn(returnValue);
        return () -> verify(checkouterAPI).getOrder(safeRefEq(requestClientInfo), safeRefEq(orderRequest));
    }

    @Nonnull
    public AutoCloseable mockGetOrderNotFound(
        RequestClientInfo requestClientInfo,
        Long orderId
    ) {
        OrderRequest orderRequest = OrderRequest.builder(orderId).build();
        when(checkouterAPI.getOrder(safeRefEq(requestClientInfo), safeRefEq(orderRequest)))
            .thenThrow(new OrderNotFoundException(orderId));
        return () -> verify(checkouterAPI).getOrder(safeRefEq(requestClientInfo), safeRefEq(orderRequest));
    }

    @Nonnull
    public AutoCloseable mockPutOrderBoxes(
        RequestClientInfo requestClientInfo,
        Long orderId,
        Long parcelId,
        List<ParcelBox> request
    ) {
        when(checkouterAPI.putParcelBoxes(eq(orderId), eq(parcelId), eq(request), safeRefEq(requestClientInfo)))
            .thenReturn(request);
        return () -> verify(checkouterAPI).putParcelBoxes(
            eq(orderId),
            eq(parcelId),
            eq(request),
            safeRefEq(requestClientInfo)
        );
    }

    @Nonnull
    public RequestClientInfo systemUserInfo() {
        return SYSTEM_USER;
    }

    @Nonnull
    public RequestClientInfo shopUserInfo(long shopId) {
        return new RequestClientInfo(ClientRole.SHOP, shopId);
    }

    @Nonnull
    public static Order buildOrder(Long orderId, long shopId) {
        return buildOrderWithBoxes(
            orderId,
            shopId,
            List.of(buildParcelBox(orderId + "-1"))
        );
    }

    @Nonnull
    public static Order buildOrderWithBoxes(
        Long orderId,
        long shopId,
        List<ParcelBox> boxes
    ) {
        Order order = new Order();
        order.setId(orderId);
        order.setShopId(shopId);

        Delivery delivery = buildDelivery();

        Parcel parcel = new Parcel();
        parcel.setId(CHECKOUTER_PARCEL_ID);
        parcel.setBoxes(boxes);

        parcel.setTracks(List.of(buildTrack(orderId)));

        delivery.setParcels(List.of(parcel));
        order.setDelivery(delivery);

        order.setItems(List.of(
            buildOrderItem(9091L, 300L, 42544, 1),
            buildOrderItem(9092L, null, 42584, 5),
            buildOrderItem(9093L, 42524L, 42524, 1)
        ));

        return order;
    }

    @Nonnull
    public static Track buildTrack(Long orderId) {
        Track track = new Track();
        track.setOrderId(orderId);
        track.setDeliveryServiceId(DS_ID);
        track.setDeliveryServiceType(DeliveryServiceType.CARRIER);
        track.setTrackCode(DS_TRACK_CODE);
        return track;
    }

    @Nonnull
    public static OrderItem buildOrderItem(Long itemId, Long fulfilmentWarehouseId, int warehouseId, int count) {
        OrderItem item = new OrderItem(
            new FeedOfferId(String.valueOf(itemId * 10), 9L),
            BigDecimal.valueOf(1000L),
            count
        );
        item.setId(itemId);
        item.setFulfilmentWarehouseId(fulfilmentWarehouseId);
        item.setWarehouseId(warehouseId);
        return item;
    }

    @Nonnull
    public static ParcelBox buildParcelBox(String fulfilmentId) {
        ParcelBox parcelBox = new ParcelBox();
        parcelBox.setId(9100L);
        parcelBox.setWeight(901L);
        parcelBox.setWidth(902L);
        parcelBox.setHeight(903L);
        parcelBox.setDepth(904L);
        parcelBox.setFulfilmentId(fulfilmentId);
        parcelBox.setItems(List.of(buildBoxItem(9091L, 9), buildBoxItem(9092L, 99)));
        return parcelBox;
    }

    @Nonnull
    public static ParcelBoxItem buildBoxItem(long itemId, int count) {
        ParcelBoxItem item = new ParcelBoxItem();
        item.setItemId(itemId);
        item.setCount(count);
        return item;
    }

    @Nonnull
    public static Delivery buildDelivery() {
        Delivery delivery = new Delivery();
        delivery.setDeliveryServiceId(51L);
        return delivery;
    }

    @Nonnull
    public static OrderSearchRequest orderSearchRequest(Long... orderIds) {
        return OrderSearchRequest.builder()
            .withRgbs(Color.BLUE, Color.WHITE)
            .withPageInfo(Pager.atPage(1, 50).setTotal(Integer.MAX_VALUE))
            .withOrderIds(orderIds)
            .build();
    }

    @Nonnull
    public static Order createOrder(long orderId, List<String> parcelBoxIds) {
        var parcels = parcelBoxIds.stream()
            .map(CheckouterFactory::buildParcelBox)
            .map(box -> {
                var parcel = new Parcel();
                parcel.setBoxes(List.of(box));
                return parcel;
            })
            .collect(Collectors.toList());

        var delivery = buildDelivery();
        delivery.setParcels(parcels);

        var order = new Order();
        order.setId(orderId);
        order.setDelivery(new Delivery(delivery));
        return order;
    }

    @Nonnull
    public static OrderRequest orderRequest() {
        return orderRequest(CHECKOUTER_ORDER_ID);
    }

    @Nonnull
    public static OrderRequest orderRequest(Long orderId) {
        return OrderRequest.builder(orderId).build();
    }
}
