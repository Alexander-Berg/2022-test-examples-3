package ru.yandex.market.ff.util;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import javax.annotation.Nonnull;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.parcel.ParcelBox;
import ru.yandex.market.checkout.checkouter.delivery.parcel.ParcelBoxItem;
import ru.yandex.market.checkout.checkouter.delivery.shipment.Parcel;
import ru.yandex.market.checkout.checkouter.event.HistoryEventReason;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvents;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.PagedOrders;
import ru.yandex.market.checkout.common.rest.Pager;
import ru.yandex.market.logistics.lom.model.dto.OrderDto;
import ru.yandex.market.logistics.lom.model.dto.StorageUnitDto;
import ru.yandex.market.logistics.lom.model.enums.StorageUnitType;

import static java.util.Collections.singletonList;

public class CreateCheckouterOrdersForReturnUtils {

    private static final Long ITEM_ID = 433252345L;
    private static final LocalDateTime NOW =
            LocalDateTime.of(2019, 10, 3, 10, 55, 12);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private CreateCheckouterOrdersForReturnUtils() {
        throw new AssertionError();
    }

    public static Order createUnpaidOrder(long orderId) {
        Order order = new Order();

        ParcelBoxItem parcelItem = new ParcelBoxItem();
        parcelItem.setItemId(ITEM_ID);
        parcelItem.setCount(1);

        ParcelBox box = new ParcelBox();
        box.setFulfilmentId("ff2");
        box.setExternalId("boxExtrId");

        ParcelBox box2 = new ParcelBox();
        box2.setFulfilmentId("ff");
        box2.setExternalId("box1");

        Parcel parcel = new Parcel();
        parcel.setBoxes(List.of(box, box2));
        parcel.setShipmentDate(NOW.toLocalDate());

        Delivery delivery = new Delivery();
        delivery.setParcels(singletonList(parcel));

        order.setDelivery(delivery);
        order.setItems(singletonList(createOrderItem(ITEM_ID, 1, getArrayNodeForUnpaid())));
        order.setId(orderId);

        return order;
    }

    private static OrderItem createOrderItem(long id, int count, ArrayNode instances) {
        OrderItem orderItem = new OrderItem();
        orderItem.setId(id);
        orderItem.setSupplierId(11L);
        orderItem.setShopSku("shopsku1");
        orderItem.setInstances(instances);
        orderItem.setCount(count);

        return orderItem;
    }

    private static ArrayNode getArrayNodeForUnpaid()  {
        try {
            String json = FileContentUtils.getFileContent(
                    "service/returns/checkouter-items-fetching/unpaid_order_item_instances.json");
            return (ArrayNode) MAPPER.readTree(json);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static OrderHistoryEvents createOrderHistoryEvents() {

        OrderHistoryEvents orderHistoryEvents = new OrderHistoryEvents(createHistoryEvents(getArrayNodeForFashion()));

        return orderHistoryEvents;
    }

    private static Collection<OrderHistoryEvent> createHistoryEvents(ArrayNode instances) {
        OrderHistoryEvent eventDelivery = new OrderHistoryEvent();
        OrderHistoryEvent eventPickup = new OrderHistoryEvent();

        eventDelivery.setReason(HistoryEventReason.USER_REQUESTED_REMOVE);
        eventPickup.setReason(HistoryEventReason.USER_REQUESTED_REMOVE);

        ClientInfo authorDeliveryService = new ClientInfo(ClientRole.DELIVERY_SERVICE, 1L);
        ClientInfo authorPickupService = new ClientInfo(ClientRole.PICKUP_SERVICE, 2L);

        eventDelivery.setAuthor(authorDeliveryService);
        eventPickup.setAuthor(authorPickupService);

        eventDelivery.setOrderAfter(createOrderAfter(5, instances));
        eventPickup.setOrderAfter(createOrderAfter(10, null));

        eventDelivery.setOrderBefore(createOrderBefore(5, instances));
        eventPickup.setOrderBefore(createOrderBefore(10, null));

        return List.of(eventDelivery, eventPickup);
    }

    private static Order createOrderAfter(long orderId, ArrayNode instances) {
        Order order = new Order();

        order.setItems(singletonList(createOrderItem(ITEM_ID, 4, instances)));
        order.setId(orderId);

        return order;
    }

    private static Order createOrderBefore(long orderId, ArrayNode instances) {
        Order order = new Order();

        order.setItems(singletonList(createOrderItem(ITEM_ID, 5, instances)));
        order.setId(orderId);

        return order;
    }

    private static ArrayNode getArrayNodeForFashion()  {
        try {
            String json = FileContentUtils.getFileContent(
                    "service/returns/checkouter-items-fetching/fashion_order_item_instances.json");
            return (ArrayNode) MAPPER.readTree(json);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static OrderDto createLomOrderDto() {
        OrderDto dto = new OrderDto();
        dto.setId(222L);
        dto.setBarcode("1"); // unique order id
        dto.setUnits(List.of(
                StorageUnitDto.builder().externalId("root").type(StorageUnitType.ROOT).build(),
                StorageUnitDto.builder().externalId("not-a-P-ff2").type(StorageUnitType.PLACE).build(),
                StorageUnitDto.builder().externalId("not-a-P-ff").type(StorageUnitType.PLACE).build()
        ));
        return dto;
    }

    @Nonnull
    public static PagedOrders createPagedOrders(long orderId) {
        return new PagedOrders(singletonList(createUnpaidOrder(orderId)), new Pager(2, 1, 3, 20, 1, 1));
    }
}
