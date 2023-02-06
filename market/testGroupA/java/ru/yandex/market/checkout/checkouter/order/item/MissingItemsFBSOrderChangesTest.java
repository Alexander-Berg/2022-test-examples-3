package ru.yandex.market.checkout.checkouter.order.item;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.shipment.Parcel;
import ru.yandex.market.checkout.checkouter.delivery.shipment.ParcelItem;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderEditRequest;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.changerequest.ChangeRequest;
import ru.yandex.market.checkout.checkouter.order.changerequest.ChangeRequestStatus;
import ru.yandex.market.checkout.checkouter.order.changerequest.itemsremoval.MissingItemsNotification;
import ru.yandex.market.checkout.checkouter.pay.PaymentType;
import ru.yandex.market.checkout.helpers.DropshipDeliveryHelper;
import ru.yandex.market.checkout.helpers.OrderDeliveryHelper;
import ru.yandex.market.checkout.helpers.ParcelBoxHelper;
import ru.yandex.market.checkout.test.providers.DeliveryProvider;
import ru.yandex.market.checkout.test.providers.OrderItemProvider;
import ru.yandex.market.checkout.test.providers.OrderProvider;
import ru.yandex.market.checkout.test.providers.ParcelBoxItemProvider;
import ru.yandex.market.checkout.test.providers.ParcelBoxProvider;
import ru.yandex.market.checkout.test.providers.ParcelProvider;
import ru.yandex.market.checkout.util.services.OrderServiceHelper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.checkout.checkouter.event.HistoryEventReason.USER_REQUESTED_REMOVE;

public class MissingItemsFBSOrderChangesTest extends MissingItemsAbstractTest {

    @Autowired
    private OrderServiceHelper orderServiceHelper;
    @Autowired
    private OrderDeliveryHelper orderDeliveryHelper;
    @Autowired
    private ParcelBoxHelper parcelBoxHelper;

    @Test
    @DisplayName("Успешно удалить item из fbs заказа в PICKUP с parcelBoxItems")
    void successfullyRemoveItemsFromOrder() throws Exception {
        Order order = createFBSOrderWithTwoItemsAndParcelsWithBoxes();
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.PICKUP);

        assertEquals(2, order.getItems().size());
        assertTrue(order.getItems().stream().allMatch(item -> item.getCount() == 1));

        // удалим item с наименьшей ценой
        OrderItem itemToRemove = Collections.min(order.getItems(), Comparator.comparing(OrderItem::getPrice));
        OrderItem itemToRemain = Collections.max(order.getItems(), Comparator.comparing(OrderItem::getPrice));

        OrderEditRequest editRequest = new OrderEditRequest();
        editRequest.setMissingItemsNotification(new MissingItemsNotification(true, List.of(
                toItemInfo(itemToRemain, 1),
                toItemInfo(itemToRemove, 0)),
                USER_REQUESTED_REMOVE, true));

        List<ChangeRequest> changeRequests =
                client.editOrder(order.getId(), ClientRole.PICKUP_SERVICE, null, List.of(Color.BLUE), editRequest);

        assertEquals(1, changeRequests.size());
        assertEquals(ChangeRequestStatus.APPLIED, changeRequests.get(0).getStatus());

        Order updatedOrder = orderService.getOrder(order.getId());
        assertEquals(1, updatedOrder.getItems().size());

        Parcel updatedParcel = updatedOrder.getDelivery().getParcels().get(0);
        assertEquals(1, updatedParcel.getParcelItems().size());
        assertEquals(1, updatedParcel.getBoxes().size());
        assertEquals(1, updatedParcel.getBoxes().get(0).getItems().size());
    }

    private Order createFBSOrderWithTwoItemsAndParcelsWithBoxes() throws Exception {
        OrderItem firstItem = OrderItemProvider.orderItemBuilder()
                .configure(OrderItemProvider::applyDefaults)
                .shopSku("testShopSKU1")
                .weight(null)
                .supplierId(DropshipDeliveryHelper.DROPSHIP_SHOP_ID)
                .offerId("first_offer")
                .price(2500)
                .atSupplierWarehouse(true)
                .warehouseId(DropshipDeliveryHelper.DROPSHIP_WAREHOUSE_ID)
                .count(1)
                .build();
        OrderItem secondItem = OrderItemProvider.orderItemBuilder()
                .configure(OrderItemProvider::applyDefaults)
                .shopSku("testShopSKU2")
                .weight(null)
                .supplierId(DropshipDeliveryHelper.DROPSHIP_SHOP_ID)
                .offerId("second_offer")
                .price(100)
                .atSupplierWarehouse(true)
                .warehouseId(DropshipDeliveryHelper.DROPSHIP_WAREHOUSE_ID)
                .count(1)
                .build();

        Order orderToSave = OrderProvider.getBlueOrder(o -> {
            o.setItems(List.of(firstItem, secondItem));
            o.setShopId(DropshipDeliveryHelper.DROPSHIP_SHOP_ID);
            o.setPaymentType(PaymentType.POSTPAID);
            o.setDelivery(DeliveryProvider.yandexPickupDelivery().build());
        });

        // создаем заказ с 2 items
        Order order = orderServiceHelper.saveOrder(orderToSave);
        long orderId = order.getId();
        orderStatusHelper.updateOrderStatus(orderId, OrderStatus.PROCESSING);

        // добавляем в него посылку c ParcelItems
        Delivery deliveryUpdateWithParcels = order.getDelivery().clone();
        deliveryUpdateWithParcels.setParcels(List.of(
                ParcelProvider.createParcel(
                        new ParcelItem(firstItem.getId(), firstItem.getCount()),
                        new ParcelItem(secondItem.getId(), secondItem.getCount()))
        ));

        Order orderWithParcel =
                orderDeliveryHelper.updateOrderDelivery(orderId, ClientInfo.SYSTEM, deliveryUpdateWithParcels);

        // кладем в посылку boxes с ParcelBoxItems
        parcelBoxHelper.putBoxes(
                orderId, orderWithParcel.getDelivery().getParcels().get(0).getId(),
                List.of(
                        ParcelBoxProvider.buildBox(
                                ParcelBoxItemProvider.parcelBoxItem(firstItem.getId(), firstItem.getCount()),
                                ParcelBoxItemProvider.parcelBoxItem(secondItem.getId(), secondItem.getCount())
                        )
                ), new ClientInfo(ClientRole.SHOP, order.getShopId())
        );

        return orderService.getOrder(orderId);
    }

}
