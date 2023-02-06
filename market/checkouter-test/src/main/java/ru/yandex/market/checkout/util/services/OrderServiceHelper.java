package ru.yandex.market.checkout.util.services;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.checkout.backbone.order.reservation.OrderCompletionService;
import ru.yandex.market.checkout.checkouter.balance.model.notifications.PaymentNotification;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.shipment.Parcel;
import ru.yandex.market.checkout.checkouter.delivery.tracking.Track;
import ru.yandex.market.checkout.checkouter.delivery.tracking.TrackCheckpoint;
import ru.yandex.market.checkout.checkouter.delivery.tracking.TrackId;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.OptionalOrderPart;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderCreateService;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.OrderService;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderUpdateService;
import ru.yandex.market.checkout.checkouter.order.delivery.track.checkpoint.TrackCheckpointService;
import ru.yandex.market.checkout.checkouter.pay.Payment;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.pay.PaymentOperations;
import ru.yandex.market.checkout.checkouter.pay.PaymentService;
import ru.yandex.market.checkout.checkouter.pay.cashier.CreatePaymentContext;
import ru.yandex.market.checkout.checkouter.shop.ShopService;
import ru.yandex.market.checkout.checkouter.storage.track.TrackReadingDao;
import ru.yandex.market.checkout.test.providers.BuyerProvider;
import ru.yandex.market.checkout.test.providers.DeliveryProvider;
import ru.yandex.market.checkout.test.providers.ItemServiceProvider;
import ru.yandex.market.checkout.test.providers.OrderItemProvider;
import ru.yandex.market.checkout.test.providers.OrderProvider;
import ru.yandex.market.checkout.test.providers.TrackCheckpointProvider;
import ru.yandex.market.checkout.test.providers.TrackProvider;
import ru.yandex.market.checkout.util.balance.ShopSettingsHelper;

import static ru.yandex.market.checkout.checkouter.pay.builders.PrepayPaymentBuilder.PLACEHOLDER_PAYMENT_ID;
import static ru.yandex.market.checkout.test.providers.ItemServiceProvider.builder;

/**
 * Алгоритмы для OrderService
 */
@TestComponent
public class OrderServiceHelper {

    public static final long DEFAULT_SHOP_ID = 45L;
    public static final long DEFAULT_BUSINESS_ID = 98765L;

    @Autowired
    private OrderService orderService;
    @Autowired
    private OrderCreateService orderCreateService;
    @Autowired
    private OrderUpdateService orderUpdateService;
    @Autowired
    private OrderCompletionService orderCompletionService;
    @Autowired
    private TrackCheckpointService trackCheckpointService;
    @Autowired
    private TransactionTemplate transactionTemplate;
    @Autowired
    private TrackReadingDao trackReadingDao;
    @Autowired
    private ShopService shopService;
    @Autowired
    private PaymentService paymentService;
    @Autowired
    @Qualifier("routingPaymentOperations")
    private PaymentOperations paymentOperations;

    public OrderServiceHelper() {
    }

    /**
     * Создает обычный заказ, но с предоплатой
     */
    public static long createPrepaidOrder(
            OrderCreateService orderCreateService,
            OrderUpdateService orderUpdateService,
            OrderCompletionService orderCompletionService
    ) {
        Order order = OrderProvider.getPrepaidOrder();
        return createPrepaidOrder(orderCreateService, orderUpdateService, orderCompletionService, order);
    }

    public static long createPrepaidOrder(
            OrderCreateService orderCreateService,
            OrderUpdateService orderUpdateService,
            OrderCompletionService orderCompletionService,
            Order prepaidOrder
    ) {
        long orderId = orderCreateService.createOrder(prepaidOrder, ClientInfo.SYSTEM);
        Order reservedOrder = orderUpdateService.reserveOrder(orderId,
                String.valueOf(orderId),
                prepaidOrder.getDelivery());
        orderCompletionService.completeOrder(reservedOrder, ClientInfo.SYSTEM);
        return orderId;
    }

    public static Order putTrackIntoOrder(long orderId, OrderUpdateService orderUpdateService) {
        return putTrackIntoOrder(orderId, TrackProvider.createTrack(), orderUpdateService);
    }

    public static Order putTrackIntoOrder(long orderId, long deliveryServiceId, OrderUpdateService orderUpdateService) {
        return putTrackIntoOrder(orderId, TrackProvider.createTrack(deliveryServiceId), orderUpdateService);
    }

    public static Order putTrackIntoOrder(long orderId, Track track, OrderUpdateService orderUpdateService) {
        Delivery delivery = new Delivery();
        Parcel shipment = new Parcel();
        shipment.addTrack(track);
        track.setOrderId(orderId);
        delivery.setParcels(Collections.singletonList(shipment));
        //
        return orderUpdateService.updateOrderDelivery(orderId, delivery, ClientInfo.SYSTEM);
    }

    public static long createPostPaidOrder(
            OrderCreateService orderCreateService,
            OrderUpdateService orderUpdateService,
            OrderCompletionService orderCompletionService
    ) {
        Order order = OrderProvider.getPostPaidOrder();
        long orderId = orderCreateService.createOrder(order, ClientInfo.SYSTEM);
        Order result = orderUpdateService.reserveOrder(orderId, "10204595", order.getDelivery());
        orderCompletionService.completeOrder(result, ClientInfo.SYSTEM);
        return orderId;
    }

    public static long createPrepaidOrder(
            OrderCreateService orderCreateService,
            OrderUpdateService orderUpdateService,
            OrderCompletionService orderCompletionService,
            ShopService shopService,
            PaymentService paymentService,
            PaymentOperations paymentOperations
    ) {
        Order order = OrderProvider.getPrepaidOrder();
        order.setFake(true);

        shopService.updateMeta(order.getShopId(), ShopSettingsHelper.getDefaultMeta());

        long orderId = orderCreateService.createOrder(order, ClientInfo.SYSTEM);
        Order order2 = orderUpdateService.reserveOrder(orderId, String.valueOf(orderId), order.getDelivery());
        orderCompletionService.completeOrder(order2, ClientInfo.SYSTEM);


        payOrder(paymentService, paymentOperations, orderId);
        return orderId;
    }

    private static void payOrder(PaymentService paymentService, PaymentOperations paymentOperations, long orderId) {
        ClientInfo payRole = new ClientInfo(ClientRole.USER, BuyerProvider.UID);
        Payment payment = paymentOperations.startPrepayPayment(
                orderId,
                payRole,
                "https://market-test.pepelac1ft.yandex.ru/payment/status/" +
                        PLACEHOLDER_PAYMENT_ID + "/",
                null,
                null,
                true,
                CreatePaymentContext.builder().build());
        paymentService.notifyPayment(new PaymentNotification("success", payment.getId(), true));
    }

    /**
     * Создает оплаченный заказ глобала с сэндобоксовым платежом.
     */
    @Deprecated
    public long createGlobalOrder() {
        Order order = OrderProvider.getPrepaidOrder();
        order.setDelivery(DeliveryProvider.getSelfDelivery());
        order.setFake(true);
        order.setGlobal(true);
        shopService.updateMeta(order.getShopId(), ShopSettingsHelper.getDefaultMeta());

        long order1 = orderCreateService.createOrder(order, ClientInfo.SYSTEM);
        Order order2 = orderUpdateService.reserveOrder(order1, "10204595", order.getDelivery());
        orderCompletionService.completeOrder(order2, ClientInfo.SYSTEM);

        shopService.updateMeta(order.getShopId(),
                ShopSettingsHelper.getOldPrepayMeta()
        );

        payOrder(paymentService, paymentOperations, order1);
        return order1;
    }

    public Order createOrder() {
        Order order = OrderProvider.getPostPaidOrder();
        order.setDelivery(DeliveryProvider.getSelfDelivery());
        order.setFake(true);
        order.setGlobal(true);
        order.setShopId(DEFAULT_SHOP_ID);
        order.setBusinessId(9876L);
        return saveOrder(order);
    }

    @Deprecated
    public Order prepareOrder(Color rgb) {
        final Order order = OrderProvider.getPostPaidOrder();
        order.setDelivery(DeliveryProvider.getSelfDelivery());
        order.setFake(true);
        order.setGlobal(true);
        order.setShopId(DEFAULT_SHOP_ID);
        order.setRgb(rgb);
        return order;
    }

    public Order createOrder(Color rgb) {
        final Order order = prepareOrder(rgb);
        return saveOrder(order);
    }

    public Order createOrder(boolean fulfilment, boolean global, Color rgb, Long shopId) {
        Order order = OrderProvider.getPostPaidOrder();
        order.setDelivery(DeliveryProvider.getSelfDelivery());
        order.setFake(true);
        order.setGlobal(global);
        order.setFulfilment(fulfilment);
        order.setShopId(shopId != null ? shopId : DEFAULT_SHOP_ID);
        order.setRgb(rgb);
        return saveOrder(order);
    }

    public Order createPostOrder() {
        return createPostOrder(order -> {
        });
    }

    public Order createPostOrder(Consumer<Order> configurer) {
        Order order = OrderProvider.getPostPaidOrder();
        order.setDelivery(DeliveryProvider.getPostalDelivery());
        order.setFake(true);
        order.setGlobal(false);
        configurer.accept(order);
        return saveOrder(order);
    }

    public Order saveOrder(Order order) {
        long orderId = orderCreateService.createOrder(order, ClientInfo.SYSTEM);
        Order order2 = orderUpdateService.reserveOrder(orderId, "10204595", order.getDelivery());
        return orderCompletionService.completeOrder(order2, ClientInfo.SYSTEM);
    }

    public void insertCheckpoint() {
        insertCheckpoint(
                new TrackId(TrackProvider.TRACK_CODE, TrackProvider.DELIVERY_SERVICE_ID),
                TrackCheckpointProvider.createCheckpoint()
        );
    }

    public void insertCheckpoint(TrackId trackId) {
        insertCheckpoint(
                trackId,
                TrackCheckpointProvider.createCheckpoint()
        );
    }

    public void insertCheckpoint(TrackId trackId,
                                 TrackCheckpoint checkpoint) {
        insertCheckpoints(trackId, Collections.singletonList(checkpoint));
    }

    public void insertCheckpoints(TrackId trackId,
                                  List<TrackCheckpoint> checkpoints) {
        Track track =
                transactionTemplate.execute(s -> trackReadingDao.findByBusinessIds(Collections.singletonList(trackId)))
                        .stream()
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException("Required track is not stored in DB"));
        trackCheckpointService.insertTrackCheckpoints(track, checkpoints);
    }

    @SuppressWarnings("checkstyle:HiddenField")
    public Long prepareOrderWithReturnStatusesCheckpoint(long deliveryServiceId) {
        var order = createPostOrder();
        orderUpdateService.updateOrderStatus(order.getId(), OrderStatus.PROCESSING);
        OrderServiceHelper.putTrackIntoOrder(order.getId(), deliveryServiceId, orderUpdateService);

        TrackId trackId = new TrackId(TrackProvider.TRACK_CODE, deliveryServiceId);
        long checkpointId = 124;
        insertCheckpoint(
                trackId, TrackCheckpointProvider.createLegacyCheckpoint()
        );
        insertCheckpoint(
                trackId, TrackCheckpointProvider.createCheckpoint(60, checkpointId++)
        );
        insertCheckpoint(
                trackId, TrackCheckpointProvider.createCheckpoint(70, checkpointId++)
        );
        insertCheckpoint(
                trackId, TrackCheckpointProvider.createCheckpoint(80, checkpointId)
        );
        return order.getId();
    }

    public Order prepareOrder() {
        long postOrder = createPostOrder().getId();
        return orderService.getOrder(postOrder, ClientInfo.SYSTEM, Set.of(OptionalOrderPart.ITEM_SERVICES));
    }

    public long createPrepaidBlueOrder() {
        return createPrepaidBlueOrder(true);
    }

    public long createPrepaidBlueOrder(boolean shouldBePayed) {
        OrderItem orderItem = OrderItemProvider.defaultOrderItem();
        orderItem.setServices(Set.of(builder()
                .configure(ItemServiceProvider::applyDefaults)
                .count(orderItem.getCount())
                .build()));
        Order order = OrderProvider.orderBuilder()
                .someBuyer()
                .someLabel()
                .deliveryBuilder(DeliveryProvider.yandexDelivery())
                .paymentMethod(PaymentMethod.YANDEX)
                .feeTotal(BigDecimal.valueOf(250))
                .total(BigDecimal.valueOf(410))
                .itemsTotal(BigDecimal.valueOf(250))
                .buyerTotal(BigDecimal.valueOf(410))
                .buyerItemsTotal(BigDecimal.valueOf(250))
                .item(orderItem)
                .isFake(true)
                .build();

        shopService.updateMeta(order.getShopId(), ShopSettingsHelper.getDefaultMeta());

        long orderId = orderCreateService.createOrder(order, ClientInfo.SYSTEM);
        order = orderUpdateService.reserveOrder(orderId, String.valueOf(orderId), order.getDelivery());
        orderCompletionService.completeOrder(order, ClientInfo.SYSTEM);
        if (shouldBePayed) {
            payOrder(paymentService, paymentOperations, orderId);
        }
        return orderId;
    }
}
