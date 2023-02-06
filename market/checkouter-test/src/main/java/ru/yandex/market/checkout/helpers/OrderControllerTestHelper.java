package ru.yandex.market.checkout.helpers;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.context.WebApplicationContext;

import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.delivery.outlet.ShopOutlet;
import ru.yandex.market.checkout.checkouter.delivery.outlet.ShopOutletPhone;
import ru.yandex.market.checkout.checkouter.delivery.shipment.Parcel;
import ru.yandex.market.checkout.checkouter.delivery.shipment.ParcelItem;
import ru.yandex.market.checkout.checkouter.delivery.shipment.ParcelStatus;
import ru.yandex.market.checkout.checkouter.delivery.tracking.Track;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderUpdateService;
import ru.yandex.market.checkout.common.WebTestHelper;
import ru.yandex.market.checkout.helpers.utils.MockMvcAware;
import ru.yandex.market.checkout.test.providers.DeliveryProvider;
import ru.yandex.market.checkout.test.providers.OrderItemProvider;
import ru.yandex.market.checkout.test.providers.OrderProvider;
import ru.yandex.market.checkout.test.providers.TrackProvider;
import ru.yandex.market.checkout.util.serialization.TestSerializationService;
import ru.yandex.market.checkout.util.services.OrderServiceHelper;
import ru.yandex.market.common.report.model.FeedOfferId;

@WebTestHelper
public class OrderControllerTestHelper extends MockMvcAware {

    private static AtomicLong deliveryServiceIdCounter = new AtomicLong(1L);
    private static AtomicLong trackCounter = new AtomicLong(1L);
    private static final long SHOP_ID = 4545L;
    public static final long SELF_DELIVERY_SERVICE_ID = 123L;

    @Autowired
    private OrderServiceHelper orderServiceHelper;
    @Autowired
    private OrderUpdateService orderUpdateService;
    @Autowired
    private OrderStatusHelper orderStatusHelper;

    public OrderControllerTestHelper(WebApplicationContext webApplicationContext,
                                     TestSerializationService testSerializationService) {
        super(webApplicationContext, testSerializationService);
    }


    public Order createOrderWithSelfDelivery(boolean global) {
        Order order = createOrder(global);

        order.setItems(Arrays.asList(
                OrderItemProvider.buildOrderItem("qwerty-1", 5),
                OrderItemProvider.buildOrderItem("qwerty-2", 5)
        ));

        order = orderServiceHelper.saveOrder(order);
        long itemId1 = order.getItem(new FeedOfferId("qwerty-1", 1L)).getId();
        long itemId2 = order.getItem(new FeedOfferId("qwerty-2", 1L)).getId();

        Parcel shipment1 = new Parcel();
        shipment1.addParcelItem(new ParcelItem(itemId2, 3));
        shipment1.addTrack(new Track("iddqd-1", SELF_DELIVERY_SERVICE_ID));

        Parcel shipment2 = new Parcel();
        shipment2.addParcelItem(new ParcelItem(itemId2, 2));
        shipment2.addParcelItem(new ParcelItem(itemId1, 5));
        shipment2.addTrack(new Track("iddqd-2", SELF_DELIVERY_SERVICE_ID));

        Delivery delivery = new Delivery();
        delivery.setParcels(Arrays.asList(shipment1, shipment2));

        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.PROCESSING);

        return orderUpdateService.updateOrderDelivery(order.getId(), delivery, ClientInfo.SYSTEM);
    }

    public static Order createOrder(boolean global) {
        Order order = OrderProvider.getPostPaidOrder();
        order.setDelivery(DeliveryProvider.getSelfDelivery());
        order.setFake(true);
        order.setGlobal(global);
        order.setShopId(SHOP_ID);

        OrderItem item = OrderItemProvider.buildOrderItem("qwerty", 5);
        order.setItems(Collections.singleton(item));
        return order;
    }

    public Order createOrderWithPartnerDelivery(long shopId) {
        long deliveryServiceId = deliveryServiceIdCounter.incrementAndGet();
        Order order = makeOrder(shopId, deliveryServiceId, -1L);
        order.setDelivery(DeliveryProvider.shopSelfDelivery().build());
        Parcel parcel = makeParcel(deliveryServiceId);
        ShopOutlet shopOutlet = makeShopOutlet();
        fillDelivery(order, shopOutlet);
        order.getDelivery().setParcels(Collections.singletonList(parcel));

        return orderServiceHelper.saveOrder(order);
    }

    public Order createBlueOrderWithPartnerDelivery(long shopId) {
        long deliveryServiceId = deliveryServiceIdCounter.incrementAndGet();
        Order order = makeOrder(shopId, deliveryServiceId, deliveryServiceId);
        Parcel parcel = makeParcel(deliveryServiceId);
        ShopOutlet shopOutlet = makeShopOutlet();
        fillDelivery(order, shopOutlet);
        order.getDelivery().setParcels(Collections.singletonList(parcel));
        order.setRgb(Color.BLUE);
        return orderServiceHelper.saveOrder(order);
    }

    public Order createOrderWithPartnerDeliverySortingCenter(long shopId) {
        long deliveryServiceId = deliveryServiceIdCounter.incrementAndGet();
        long fulfilmentWarehouseId = deliveryServiceIdCounter.incrementAndGet();
        Order order = makeOrder(shopId, deliveryServiceId, fulfilmentWarehouseId);
        Parcel parcel = makeParcel(deliveryServiceId);
        ShopOutlet shopOutlet = makeShopOutlet();
        fillDelivery(order, shopOutlet);
        parcel.addTrack(TrackProvider.createTrack(generateTrackCode(), fulfilmentWarehouseId)); //sc track
        order.getDelivery().setParcels(Collections.singletonList(parcel));

        return orderServiceHelper.saveOrder(order);
    }

    //creates GREEN order
    @Deprecated
    private static Order makeOrder(long shopId, long deliveryServiceId, long fulfilmentWarehouseId) {
        Order order = OrderProvider.getPostPaidOrder();
        order.setDelivery(DeliveryProvider.getPostalDelivery());
        order.getDelivery().setDeliveryServiceId(deliveryServiceId);
        order.setFake(true);
        order.setGlobal(false);
        order.setShopId(shopId);

        order.setItems(Collections.singleton(
                OrderItemProvider.buildOrderItem("qwerty", null, 5, fulfilmentWarehouseId)
        ));
        return order;
    }

    private static Parcel makeParcel(long deliveryServiceId) {
        Parcel parcel = new Parcel();
        parcel.addTrack(TrackProvider.createTrack(generateTrackCode(), deliveryServiceId));
        parcel.setStatus(ParcelStatus.READY_TO_SHIP);
        parcel.setLabelURL("https://some.site.ru/label");
        return parcel;
    }

    private static ShopOutlet makeShopOutlet() {
        ShopOutlet shopOutlet = new ShopOutlet();
        shopOutlet.setId(2121L);
        shopOutlet.setShipmentDate(new Date());
        shopOutlet.setInletId(1212L);
        shopOutlet.setRegionId(213);
        shopOutlet.setPhones(Collections.singletonList(
                new ShopOutletPhone("+7", "992", "222-22-22", "24"))
        );
        shopOutlet.setCity("Moscow");
        shopOutlet.setName("Outlet");
        return shopOutlet;
    }

    private static void fillDelivery(Order order, ShopOutlet shopOutlet) {
        Delivery delivery = order.getDelivery();
        delivery.setOutlet(shopOutlet);
        delivery.setType(DeliveryType.PICKUP);
        delivery.setOutletId(2121L);
        delivery.setOutletCode("outlet");
    }

    private static String generateTrackCode() {
        return String.format("%07d", trackCounter.incrementAndGet());
    }
}
