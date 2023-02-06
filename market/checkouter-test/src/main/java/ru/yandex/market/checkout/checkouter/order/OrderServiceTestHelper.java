package ru.yandex.market.checkout.checkouter.order;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Objects;
import java.util.function.Consumer;

import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.application.AbstractWebTestHelper;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.pay.PaymentType;
import ru.yandex.market.checkout.checkouter.shop.PrepayType;
import ru.yandex.market.checkout.checkouter.shop.ShopMetaData;
import ru.yandex.market.checkout.checkouter.shop.ShopService;
import ru.yandex.market.checkout.checkouter.tasks.queuedcalls.CheckouterQCType;
import ru.yandex.market.checkout.helpers.OrderPayHelper;
import ru.yandex.market.checkout.helpers.OrderStatusHelper;
import ru.yandex.market.checkout.test.providers.DeliveryProvider;
import ru.yandex.market.checkout.test.providers.OrderItemProvider;
import ru.yandex.market.checkout.test.providers.OrderProvider;
import ru.yandex.market.checkout.util.Has;
import ru.yandex.market.checkout.util.Holder;
import ru.yandex.market.checkout.util.balance.ShopSettingsHelper;
import ru.yandex.market.queuedcalls.QueuedCallService;

import static java.util.Optional.ofNullable;
import static ru.yandex.market.checkout.util.balance.ShopSettingsHelper.getDefaultMeta;
import static ru.yandex.market.checkout.util.balance.ShopSettingsHelper.getRedPrepayMeta;

/**
 * @author : poluektov
 * date: 10.07.17.
 * <p>
 * Генерилка готовых заказов для оплатных тестов.
 * В обход ручек /cart /checkout с изменением статуса.
 */
public class OrderServiceTestHelper extends AbstractWebTestHelper {

    public static final long FF_SHOP_ID = 431782L;

    @Autowired
    private OrderService orderService;
    @Autowired
    private OrderCreateService orderCreateService;
    @Autowired
    private OrderStatusHelper orderStatusHelper;
    @Autowired
    private ShopService shopService;
    @Autowired
    private OrderPayHelper payHelper;
    @Autowired
    private QueuedCallService queuedCallService;

    private Has<ShopMetaData> shopMetaData;
    private Has<Order> order;

    public OrderServiceTestHelper(AbstractWebTestBase test, Holder<Order> order, Holder<ShopMetaData> shopMetaData) {
        super(test);
        this.shopMetaData = shopMetaData;
        this.order = order;
    }

    public Order createOrderUnpaid(boolean withTaxData) {
        Order newOrder = OrderProvider.getPrepaidOrder();
        newOrder.setFulfilment(true);
        newOrder.getDelivery().setDeliveryPartnerType(DeliveryPartnerType.YANDEX_MARKET);
        newOrder.setRgb(Color.BLUE);
        if (!withTaxData) {
            newOrder.setTaxSystem(null);
        }
        return innerCreateOrderUnpaid(newOrder);
    }

    public Order createUnpaidBlueOrderWithPickupDelivery() {
        Order orderWithPickupDelivery = OrderProvider.getFulfillmentOrderWithPickupType();
        orderWithPickupDelivery.setRgb(Color.BLUE);
        orderWithPickupDelivery.setPaymentType(PaymentType.PREPAID);
        orderWithPickupDelivery.setPaymentMethod(PaymentMethod.YANDEX);

        return innerCreateOrderUnpaid(orderWithPickupDelivery, PrepayType.YANDEX_MARKET);
    }

    public Order createUnpaidBlueOrderWithPickupMardoDelivery() {
        Order orderWithPickupDelivery = OrderProvider.getFulfillmentOrderWithPickupType();
        orderWithPickupDelivery.getDelivery().setDeliveryPartnerType(DeliveryPartnerType.YANDEX_MARKET);
        orderWithPickupDelivery.setRgb(Color.BLUE);
        orderWithPickupDelivery.setFulfilment(true);
        orderWithPickupDelivery.setPaymentType(PaymentType.PREPAID);
        orderWithPickupDelivery.setPaymentMethod(PaymentMethod.YANDEX);

        return innerCreateOrderUnpaid(orderWithPickupDelivery, PrepayType.YANDEX_MARKET);
    }


    public Order createUnpaidBlueOrder(Consumer<Order> orderModifier) {
        Order newOrder = OrderProvider.getFulfilmentOrder();
        newOrder.setRgb(Color.BLUE);
        newOrder.setPaymentType(PaymentType.PREPAID);
        newOrder.getDelivery().setDeliveryPartnerType(DeliveryPartnerType.YANDEX_MARKET);
        newOrder.setPaymentMethod(PaymentMethod.YANDEX);
        if (!Objects.isNull(orderModifier)) {
            orderModifier.accept(newOrder);
        }
        return innerCreateOrderUnpaid(newOrder);
    }

    public Order createUnpaidBlueOrderWithShopDelivery(Consumer<Order> orderModifier) {
        Order newOrder = OrderProvider.getBlueOrder();
        newOrder.setFulfilment(false);
        newOrder.setPaymentType(PaymentType.PREPAID);
        newOrder.setPaymentMethod(PaymentMethod.YANDEX);
        newOrder.setDelivery(DeliveryProvider.shopSelfDelivery().build());
        if (!Objects.isNull(orderModifier)) {
            orderModifier.accept(newOrder);
        }
        return innerCreateOrderUnpaid(newOrder);
    }

    public Order createUnpaidBlue1POrder() {
        Order newOrder = OrderProvider.getFulfilmentOrder1PShopId();
        newOrder.setPaymentType(PaymentType.PREPAID);
        newOrder.setPaymentMethod(PaymentMethod.YANDEX);
        newOrder.getDelivery().setDeliveryPartnerType(DeliveryPartnerType.YANDEX_MARKET);
        newOrder.setRgb(Color.BLUE);
        return innerCreateOrderUnpaid(newOrder);
    }

    public Order createUnpaidBlue1POrder(Consumer<Order> orderModifier) {
        Order newOrder = OrderProvider.getFulfilmentOrder1PShopId();
        newOrder.setPaymentType(PaymentType.PREPAID);
        newOrder.setPaymentMethod(PaymentMethod.YANDEX);
        newOrder.getDelivery().setDeliveryPartnerType(DeliveryPartnerType.YANDEX_MARKET);
        newOrder.setRgb(Color.BLUE);
        ofNullable(orderModifier).ifPresent(modifier -> modifier.accept(newOrder));
        return innerCreateOrderUnpaid(newOrder);
    }

    private Order createPostpaidBlueOrder() {
        Order newOrder = OrderProvider.getFulfilmentOrder();
        newOrder.setRgb(Color.BLUE);
        newOrder.setPaymentType(PaymentType.POSTPAID);
        newOrder.getDelivery().setDeliveryPartnerType(DeliveryPartnerType.YANDEX_MARKET);
        newOrder.setPaymentMethod(PaymentMethod.CARD_ON_DELIVERY);
        initFFShopMetaData(newOrder);
        orderCreateService.createOrder(newOrder, ClientInfo.SYSTEM);
        orderStatusHelper.updateOrderStatus(newOrder.getId(), OrderStatus.RESERVED);
        orderStatusHelper.updateOrderStatus(newOrder.getId(), OrderStatus.PROCESSING);
        order.set(orderService.getOrder(newOrder.getId()));
        return order.get();
    }

    public Order createUnpaidFFOrderWithDiffShops() {
        Order newOrder = OrderProvider.getFulfilmentOrder();
        newOrder.setPaymentType(PaymentType.PREPAID);
        newOrder.getDelivery().setDeliveryPartnerType(DeliveryPartnerType.YANDEX_MARKET);
        newOrder.setPaymentMethod(PaymentMethod.YANDEX);
        newOrder.setRgb(Color.BLUE);
        return innerCreateOrderUnpaid(newOrder);
    }

    public Order createUnpaidFFBlueOrder() {
        Order newOrder = OrderProvider.getFulfilmentOrderSameShopId();
        newOrder.setPaymentType(PaymentType.PREPAID);
        newOrder.setPaymentMethod(PaymentMethod.YANDEX);
        newOrder.getDelivery().setDeliveryPartnerType(DeliveryPartnerType.YANDEX_MARKET);
        newOrder.setRgb(Color.BLUE);
        return innerCreateOrderUnpaid(newOrder);
    }

    public Order createDeliveredBlueOrder() {
        return createDeliveredBlueOrder(null);
    }

    @SuppressWarnings("checkstyle:HiddenField")
    public Order createPickupBlueOrder() {
        Order order = createUnpaidBlueOrder(o -> o.getDelivery().setType(DeliveryType.PICKUP));
        payHelper.payForOrder(order);
        processOrderToPickup(order);
        return orderService.getOrder(order.getId());
    }

    public Order createDeliveredBlueOrder(Consumer<Order> orderModifier) {
        return createPaidOrder(createUnpaidBlueOrder(orderModifier));
    }

    public Order createDeliveredBlueOrderWithCount(int count) {
        Order newOrder = OrderProvider.getPostPaidOrder(o -> {
            o.setFulfilment(true);

            OrderItem item1 = OrderItemProvider.buildOrderItem("1");
            item1.setSupplierId(123L);
            item1.setBuyerPrice(new BigDecimal("100.0"));
            item1.setQuantPrice(new BigDecimal("100.0"));
            item1.setCount(count);
            item1.setValidIntQuantity(count);

            o.setItems(Collections.singletonList(item1));
        });
        newOrder.setRgb(Color.BLUE);
        newOrder.setPaymentType(PaymentType.PREPAID);
        newOrder.setPaymentMethod(PaymentMethod.YANDEX);
        return createPaidOrder(innerCreateOrderUnpaid(newOrder));
    }

    @SuppressWarnings("checkstyle:HiddenField")
    public Order createDeliveredBluePostPaidOrder() {
        Order order = createDeliveredBluePostPaidOrderWithoutProcessingPayment();
        queuedCallService.executeQueuedCallBatch(CheckouterQCType.ORDER_CREATE_CASH_PAYMENT);
        return orderService.getOrder(order.getId());
    }

    @SuppressWarnings("checkstyle:HiddenField")
    public Order createDeliveredBluePostPaidOrderWithoutProcessingPayment() {
        Order order = createPostpaidBlueOrder();
        processOrderToDelivered(order);
        return orderService.getOrder(order.getId());
    }

    public Order createDeliveredBluePostPaid1POrder() {
        Order newOrder = OrderProvider.getFulfilmentOrderSameShopId();
        newOrder.setRgb(Color.BLUE);
        newOrder.getDelivery().setDeliveryPartnerType(DeliveryPartnerType.YANDEX_MARKET);
        newOrder.setPaymentType(PaymentType.POSTPAID);
        newOrder.setPaymentMethod(PaymentMethod.CARD_ON_DELIVERY);
        newOrder.getItems().forEach(item -> item.setSupplierType(SupplierType.FIRST_PARTY));
        initFFShopMetaData(newOrder);
        orderCreateService.createOrder(newOrder, ClientInfo.SYSTEM);
        orderStatusHelper.updateOrderStatus(newOrder.getId(), OrderStatus.RESERVED);
        orderStatusHelper.updateOrderStatus(newOrder.getId(), OrderStatus.PROCESSING);

        processOrderToDelivered(newOrder);
        queuedCallService.executeQueuedCallBatch(CheckouterQCType.ORDER_CREATE_CASH_PAYMENT);
        return orderService.getOrder(newOrder.getId());
    }

    private Order createPaidOrder(Order order) {
        payHelper.payForOrder(order);
        processOrderToDelivered(order);
        return orderService.getOrder(order.getId());
    }

    private void processOrderToPickup(Order order) {
        orderStatusHelper.updateOrderStatus(order.getId(), OrderStatus.DELIVERY);
        orderStatusHelper.updateOrderStatus(order.getId(), OrderStatus.PICKUP);
    }

    private void processOrderToDelivered(Order order) {
        orderStatusHelper.updateOrderStatus(order.getId(), OrderStatus.DELIVERY);
        orderStatusHelper.updateOrderStatus(order.getId(), OrderStatus.DELIVERED);
    }

    private Order innerCreateOrderUnpaid(Order newOrder) {
        return innerCreateOrderUnpaid(newOrder, PrepayType.YANDEX_MARKET);
    }

    @SuppressWarnings("checkstyle:HiddenField")
    private Order innerCreateOrderUnpaid(Order newOrder, PrepayType prepayType) {
        if (newOrder.isFulfilment()) {
            initFFShopMetaData(newOrder);
        } else {
            ShopMetaData shopMetaData;
            switch (prepayType) {
                case YANDEX_MARKET:
                    shopMetaData = ShopSettingsHelper.createShopSettings(shopService, newOrder.getShopId(),
                            getDefaultMeta());
                    break;
                case YANDEX_MARKET_AG:
                    shopMetaData = ShopSettingsHelper.createShopSettings(shopService, newOrder.getShopId(),
                            getRedPrepayMeta());
                    break;
                default:
                    throw new IllegalArgumentException("Unknown prepayType: " + prepayType);
            }

            newOrder.getItems().forEach(i -> i.setSupplierId(newOrder.getShopId()));
            newOrder.getItems().forEach(i -> i.setSupplierType(SupplierType.THIRD_PARTY));
            this.shopMetaData.set(shopMetaData);
        }
        shopService.updateMeta(newOrder.getShopId(), shopMetaData.get());
        orderCreateService.createOrder(newOrder, ClientInfo.SYSTEM);
        orderStatusHelper.updateOrderStatus(newOrder.getId(), OrderStatus.RESERVED);
        orderStatusHelper.updateOrderStatus(newOrder.getId(), OrderStatus.UNPAID, OrderSubstatus.WAITING_USER_INPUT);
        order.set(orderService.getOrder(newOrder.getId(), ClientInfo.SYSTEM,
                Collections.singleton(OptionalOrderPart.ITEM_SERVICES)));
        return order.get();
    }

    private void initFFShopMetaData(Order newOrder) {
        shopMetaData.set(shopService.getMeta(FF_SHOP_ID));
        shopService.updateMeta(newOrder.getShopId(), shopMetaData.get());
        newOrder.getItems().forEach(orderItem -> {
            Long ffshopId = orderItem.getSupplierId();
            shopService.updateMeta(ffshopId,
                    ShopSettingsHelper.createCustomNewPrepayMeta(ffshopId.intValue()));
        });
    }
}
