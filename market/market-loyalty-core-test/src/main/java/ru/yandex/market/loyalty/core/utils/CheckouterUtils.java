package ru.yandex.market.loyalty.core.utils;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.event.HistoryEventType;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.order.Buyer;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Context;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.OrderPropertyType;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.checkouter.pay.Payment;
import ru.yandex.market.checkout.checkouter.pay.PaymentType;
import ru.yandex.market.loyalty.api.exception.MarketLoyaltyException;
import ru.yandex.market.loyalty.api.model.coin.OrderStatusUpdatedRequest;
import ru.yandex.market.loyalty.core.model.order.ItemKey;
import ru.yandex.market.loyalty.core.model.trigger.event.OrderStatusUpdatedEvent;
import ru.yandex.market.loyalty.core.model.trigger.event.data.OrderEventInfo;

import static ru.yandex.market.checkout.checkouter.pay.PaymentType.PREPAID;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.ANOTHER_ITEM_KEY;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.DEFAULT_ITEM_KEY;
import static ru.yandex.market.loyalty.core.utils.UserDataFactory.DEFAULT_EMAIL;
import static ru.yandex.market.loyalty.core.utils.UserDataFactory.DEFAULT_MUID;
import static ru.yandex.market.loyalty.core.utils.UserDataFactory.DEFAULT_PHONE_ID;
import static ru.yandex.market.loyalty.core.utils.UserDataFactory.DEFAULT_UID;
import static ru.yandex.market.loyalty.core.utils.UserDataFactory.DEFAULT_UUID;
import static ru.yandex.market.loyalty.core.utils.UserDataFactory.DEFAULT_YANDEX_UID;

/**
 * @author ukchuvrus
 */
public final class CheckouterUtils {

    public static final String DEFAULT_OFFER_NAME = "чайник";
    public static final BigDecimal DEFAULT_ITEM_PRICE = BigDecimal.valueOf(100.00);
    public static final BigDecimal DEFAULT_ITEMS_COUNT = BigDecimal.valueOf(2);
    public static final BigDecimal ANOTHER_ITEM_PRICE = BigDecimal.valueOf(100.00);
    public static final BigDecimal ANOTHER_ITEMS_COUNT = BigDecimal.valueOf(2);
    public static final String DEFAULT_OFFER_WARE_ID = "DEFAULT_OFFER_WARE_ID";
    public static final int DEFAULT_CATEGORY_ID = 1;
    public static final int ANOTHER_CATEGORY_ID = 2;
    public static final long DEFAULT_VENDOR_ID = 1L;
    public static final long ANOTHER_VENDOR_ID = 2L;
    public static final long DEFAULT_MSKU = 1L;
    public static final long ANOTHER_MSKU = 2L;
    public static final long DEFAULT_ORDER_ID = 1231133L;
    public static final long ANOTHER_ORDER_ID = 1231134L;
    public static final String DEFAULT_REQUEST_ID = "request_id";
    public static final long DELIVERY_REGION = 213L;
    private static final Long SHOP_ID = 120L;

    private CheckouterUtils() {
    }

    public static OrderHistoryEvent getEvent(Order order, HistoryEventType historyEventType, Clock clock) {
        return getEvent(order, order, historyEventType, clock);
    }

    public static OrderHistoryEvent getEvent(
            Order orderBefore, Order orderAfter, HistoryEventType historyEventType, Clock clock
    ) {
        OrderHistoryEvent event = new OrderHistoryEvent();
        event.setOrderBefore(orderBefore);
        event.setOrderAfter(orderAfter);
        event.setType(historyEventType);
        event.setId(1L);
        event.setTranDate(Date.from(clock.instant()));
        event.setRequestId(DEFAULT_REQUEST_ID);
        return event;
    }

    public static OrderBuilder defaultOrder(OrderStatus orderStatus) {
        return new OrderBuilder()
                .setDeliveryRegion(DELIVERY_REGION)
                .setCreationDate(new Date())
                .setNoAuth(false)
                .setOrderId(DEFAULT_ORDER_ID)
                .setOrderStatus(orderStatus)
                .setOrderSubstatus(
                        Stream.of(OrderSubstatus.values())
                                .filter(ss -> ss.getStatus() == orderStatus)
                                .findFirst()
                                .orElse(null)
                )
                .setRgb(Color.BLUE)
                .setUserEmail(DEFAULT_EMAIL)
                .setPersonalPhoneId(DEFAULT_PHONE_ID)
                .setUuid(DEFAULT_UUID)
                .setMuid(DEFAULT_MUID)
                .setPaymentType(PREPAID)
                .setYandexUid(DEFAULT_YANDEX_UID)
                .setUid(DEFAULT_UID);
    }

    public static OrderBuilder defaultOrder(OrderStatus orderStatus, OrderSubstatus orderSubstatus) {
        return new OrderBuilder()
                .setCreationDate(new Date())
                .setDeliveryRegion(DELIVERY_REGION)
                .setNoAuth(false)
                .setOrderId(DEFAULT_ORDER_ID)
                .setOrderStatus(orderStatus)
                .setOrderSubstatus(orderSubstatus)
                .setRgb(Color.BLUE)
                .setUserEmail(DEFAULT_EMAIL)
                .setPersonalPhoneId(DEFAULT_PHONE_ID)
                .setUuid(DEFAULT_UUID)
                .setMuid(DEFAULT_MUID)
                .setPaymentType(PREPAID)
                .setYandexUid(DEFAULT_YANDEX_UID)
                .setUid(DEFAULT_UID);
    }

    public static OrderItemBuilder defaultOrderItem() {
        return new OrderItemBuilder()
                .setPrice(DEFAULT_ITEM_PRICE)
                .setCategoryId(DEFAULT_CATEGORY_ID)
                .setMsku(DEFAULT_MSKU)
                .setWareId(DEFAULT_OFFER_WARE_ID)
                .setItemKey(DEFAULT_ITEM_KEY)
                .setDiscount(null)
                .setCount(DEFAULT_ITEMS_COUNT)
                .setOfferName(DEFAULT_OFFER_NAME)
                .setVendorId(DEFAULT_VENDOR_ID);
    }

    public static OrderItemBuilder anotherOrderItem() {
        return new OrderItemBuilder()
                .setPrice(ANOTHER_ITEM_PRICE)
                .setCategoryId(ANOTHER_CATEGORY_ID)
                .setMsku(ANOTHER_MSKU)
                .setWareId(DEFAULT_OFFER_WARE_ID)
                .setItemKey(ANOTHER_ITEM_KEY)
                .setDiscount(null)
                .setCount(ANOTHER_ITEMS_COUNT)
                .setOfferName(DEFAULT_OFFER_NAME)
                .setVendorId(ANOTHER_VENDOR_ID);
    }

    private static BigDecimal getTotalCost(Order order) throws MarketLoyaltyException {
        return order.getItems().stream()
                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getCount())))
                .reduce(BigDecimal.valueOf(0, 2), BigDecimal::add);
    }

    public static OrderStatusUpdatedEvent createEvent(
            OrderEventInfo order, Long eventId
    ) {
        return OrderStatusUpdatedEvent.builderSingleOrderEventFromOrderInfo(order, eventId, DEFAULT_REQUEST_ID).build();
    }

    public static OrderStatusUpdatedRequest defaultStatusUpdatedRequest(long defaultOrderId) {
        return new OrderStatusUpdatedRequest(defaultOrderId, DEFAULT_UID, null, false);
    }

    public static class OrderBuilder {
        private Long orderId;
        private String multiOrderId;
        private Color rgb;
        private Long deliveryRegion;
        private String userEmail;
        private String personalPhoneId;
        private Long uid;
        private Boolean noAuth;
        private OrderStatus orderStatus;
        private OrderSubstatus orderSubstatus;
        private final List<OrderItem> items = new ArrayList<>();
        private String yandexUid;
        private String uuid;
        private Long muid;
        private PaymentType paymentType;
        @SuppressWarnings("rawtypes")
        private Map<OrderPropertyType, Object> properties;
        private DeliveryType deliveryType;
        private Payment payment;
        private Integer ordersCount;
        private DeliveryPartnerType deliveryPartnerType;
        private boolean isMarketBrandedDelivery = false;
        private Date creationDate;

        public OrderBuilder setMuid(Long muid) {
            this.muid = muid;
            return this;
        }

        public OrderBuilder setOrderId(Long orderId) {
            this.orderId = orderId;
            return this;
        }


        public OrderBuilder setCreationDate(Date creationDate) {
            this.creationDate = creationDate;
            return this;
        }

        public OrderBuilder setOrdersCount(Integer ordersCount) {
            this.ordersCount = ordersCount;
            return this;
        }

        public OrderBuilder setMultiOrderId(String multiOrderId) {
            this.multiOrderId = multiOrderId;
            return this;
        }

        public OrderBuilder setDeliveryPartnerType(DeliveryPartnerType deliveryPartnerType) {
            this.deliveryPartnerType = deliveryPartnerType;
            return this;
        }

        public OrderBuilder setRgb(Color rgb) {
            this.rgb = rgb;
            return this;
        }

        public OrderBuilder setDeliveryRegion(Long deliveryRegion) {
            this.deliveryRegion = deliveryRegion;
            return this;
        }

        public OrderBuilder setUserEmail(String userEmail) {
            this.userEmail = userEmail;
            return this;
        }

        public OrderBuilder setPersonalPhoneId(String personalPhoneId) {
            this.personalPhoneId = personalPhoneId;
            return this;
        }

        public OrderBuilder setUid(Long uid) {
            this.uid = uid;
            return this;
        }

        public OrderBuilder setNoAuth(Boolean noAuth) {
            this.noAuth = noAuth;
            return this;
        }

        public OrderBuilder setOrderStatus(OrderStatus orderStatus) {
            this.orderStatus = orderStatus;
            return this;
        }

        public OrderBuilder setOrderSubstatus(OrderSubstatus orderSubstatus) {
            this.orderSubstatus = orderSubstatus;
            return this;
        }

        public OrderBuilder addItem(OrderItem item) {
            this.items.add(item);
            return this;
        }

        public OrderBuilder addItems(Collection<OrderItem> items) {
            this.items.addAll(items);
            return this;
        }

        public OrderBuilder setYandexUid(String yandexUid) {
            this.yandexUid = yandexUid;
            return this;
        }

        public OrderBuilder setUuid(String uuid) {
            this.uuid = uuid;
            return this;
        }

        public OrderBuilder setPaymentType(PaymentType paymentType) {
            this.paymentType = paymentType;
            return this;
        }

        public <T> OrderBuilder setProperty(OrderPropertyType<T> property, T value) {
            if (this.properties == null) {
                this.properties = new HashMap<>();
            }
            this.properties.put(property, value);
            return this;
        }

        public OrderBuilder setDeliveryType(DeliveryType deliveryType) {
            this.deliveryType = deliveryType;
            return this;
        }

        public OrderBuilder setPayment(Payment payment) {
            this.payment = payment;
            return this;
        }

        public OrderBuilder setMarketBrandedDelivery(boolean marketBrandedDelivery) {
            isMarketBrandedDelivery = marketBrandedDelivery;
            return this;
        }

        public Order build() {
            Order order = new Order();
            order.setCreationDate(creationDate);
            order.setRgb(rgb);
            order.setId(orderId);
            order.setFake(false);
            order.setStatus(orderStatus);
            order.setSubstatus(orderSubstatus);
            order.setItems(items);
            order.setBuyerItemsTotal(getTotalCost(order));
            order.setShopId(SHOP_ID);
            order.setDelivery(new Delivery());
            order.getDelivery().setRegionId(deliveryRegion);
            order.getDelivery().setType(deliveryType);
            order.getDelivery().setDeliveryPartnerType(deliveryPartnerType);
            order.getDelivery().setMarketBranded(isMarketBrandedDelivery);
            order.setNoAuth(noAuth);
            order.setPaymentType(paymentType);
            order.setContext(Context.MARKET);

            Buyer buyer = new Buyer();
            if (!noAuth) {
                buyer.setUid(uid);
            }
            if (multiOrderId != null) {
                order.setProperty(OrderPropertyType.MULTI_ORDER_ID, multiOrderId);
            }
            if (ordersCount != null) {
                order.setProperty(OrderPropertyType.MULTI_ORDER_SIZE, ordersCount);
            }
            buyer.setYandexUid(yandexUid);
            buyer.setUuid(uuid);
            buyer.setMuid(muid);
            buyer.setEmail(userEmail);
            buyer.setPersonalPhoneId(personalPhoneId);
            order.setBuyer(buyer);
            if (properties != null) {
                properties.forEach(order::setProperty);
            }
            order.setPayment(payment);
            return order;
        }
    }

    public static class OrderItemBuilder {
        private Long id;
        private String wareId;
        private ItemKey offerKey;
        private BigDecimal discount;
        private BigDecimal count;
        private String offerName;
        private BigDecimal price;
        private Integer categoryId;
        private Long msku;
        private Long vendorId;

        public OrderItemBuilder setWareId(String wareId) {
            this.wareId = wareId;
            return this;
        }

        public OrderItemBuilder setItemKey(ItemKey offerKey) {
            this.offerKey = offerKey;
            return this;
        }

        public OrderItemBuilder setDiscount(BigDecimal discount) {
            this.discount = discount;
            return this;
        }

        public OrderItemBuilder setCount(BigDecimal count) {
            this.count = count;
            return this;
        }

        public OrderItemBuilder setCount(int count) {
            return setCount(BigDecimal.valueOf(count));
        }

        public OrderItemBuilder setOfferName(String offerName) {
            this.offerName = offerName;
            return this;
        }

        public OrderItemBuilder setPrice(BigDecimal price) {
            this.price = price;
            return this;
        }

        public OrderItemBuilder setPrice(int price) {
            return setPrice(BigDecimal.valueOf(price));
        }

        public OrderItemBuilder setCategoryId(Integer categoryId) {
            this.categoryId = categoryId;
            return this;
        }

        public OrderItemBuilder setMsku(Long msku) {
            this.msku = msku;
            return this;
        }

        public OrderItemBuilder setVendorId(Long vendorId) {
            this.vendorId = vendorId;
            return this;
        }

        public OrderItemBuilder setId(Long id) {
            this.id = id;
            return this;
        }

        public OrderItem build() {
            OrderItem orderItem = new OrderItem();
            orderItem.setWareMd5(wareId);
            orderItem.setFeedId(offerKey.getFeedId());
            orderItem.setOfferId(offerKey.getOfferId());
            orderItem.getPrices().setBuyerSubsidy(discount);
            orderItem.setCount(count.intValue());
            orderItem.setOfferName(offerName);
            orderItem.setPrice(price);
            orderItem.setBuyerPrice(price);
            orderItem.setCategoryId(categoryId);
            orderItem.setMsku(msku);
            orderItem.setVendorId(vendorId);
            orderItem.setId(id);
            return orderItem;
        }
    }
}
