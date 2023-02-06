package ru.yandex.market.notifier.util.providers;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import org.apache.commons.collections4.CollectionUtils;

import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.shipment.Parcel;
import ru.yandex.market.checkout.checkouter.delivery.tracking.Track;
import ru.yandex.market.checkout.checkouter.order.Buyer;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Context;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderAcceptMethod;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.OrderPropertyType;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.TaxSystem;
import ru.yandex.market.checkout.checkouter.order.promo.OrderPromo;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;

import static ru.yandex.common.util.currency.Currency.RUR;
import static ru.yandex.market.checkout.checkouter.order.Context.MARKET;
import static ru.yandex.market.checkout.checkouter.order.OrderAcceptMethod.DEFAULT;
import static ru.yandex.market.checkout.checkouter.order.OrderAcceptMethod.WEB_INTERFACE;

@SuppressWarnings("checkstyle:MagicNumber")
public abstract class OrderProvider {

    private static final String SECOND_TRACK_CODE = "qwerty";
    private static final Integer TEST_WAREHOUSE_ID = 1;

    private static final String FIRST_TRACK_CODE = "iddqd";
    public static final long SHOP_ID = 774L;
    public static final long BUSINESS_ID = 98765L;

    @Nonnull
    public static Order getColorOrder(@Nonnull Color color) {
        return applyDefaults(orderBuilder())
                .item(OrderItemProvider.getOrderItem())
                .color(color)
                .build();
    }

    @Deprecated
    public static Order getRedOrder() {
        return getColorOrder(Color.RED);
    }

    /**
     * @return Синий Фулфилмент Заказ с яндекс куръеркой.
     */
    @Nonnull
    public static Order getBlueOrder() {
        Order order = getColorOrder(Color.BLUE);
        // Временный хак, на время перевода тестов на синий цвет
        order.getItems().forEach(item -> item.setMsku(123L));
        return order;
    }

    @Nonnull
    public static Order getWhiteOrder() {
        Order order = getColorOrder(Color.WHITE);
        // Временный хак, на время перевода тестов на синий цвет
        order.getItems().forEach(item -> item.setMsku(null));
        return order;
    }

    @Nonnull
    public static OrderBuilder applyDefaults(@Nonnull OrderBuilder builder) {
        return builder
                .label(UUID.randomUUID().toString())
                .acceptMethod(WEB_INTERFACE)
                .delivery(DeliveryProvider.getYandexMarketDelivery(false))
                .someBuyer()
                .isFake(false)
                //TODO: убрать - не должно быть в реквесте, но завязалось много тестов
                .feeTotal(BigDecimal.valueOf(250))
                .total(BigDecimal.valueOf(250))
                .itemsTotal(BigDecimal.valueOf(250))
                .buyerTotal(BigDecimal.valueOf(260))
                .buyerItemsTotal(BigDecimal.valueOf(250));
    }

    @Deprecated
    public static Order getOrderWithTracking() {
        Order order = OrderProvider.getPostPaidOrder();
        order.setDelivery(DeliveryProvider.getSelfDelivery());
        order.setFake(false);
        order.setGlobal(true);

        Track track1 = TrackProvider.createTrack(FIRST_TRACK_CODE, 123L);
        track1.setTrackerId(100500L);

        Parcel shipment1 = new Parcel();
        shipment1.addTrack(track1);

        Track track2 = TrackProvider.createTrack(SECOND_TRACK_CODE, 123L);
        track2.setTrackerId(100501L);

        Parcel shipment2 = new Parcel();
        shipment2.addTrack(track2);
        order.getDelivery().setParcels(Arrays.asList(shipment1, shipment2));
        return order;
    }

    public static Order getPostPaidOrder() {
        Order order = getBlueOrder();
        order.setPaymentMethod(PaymentMethod.CASH_ON_DELIVERY);
        return order;
    }

    public static Order getPostPaidOrder(Consumer<Order> configurer) {
        return configureOrder(OrderProvider::getPostPaidOrder, configurer);
    }

    @Deprecated
    public static Order getOrderWithPickupDelivery() {
        Order order = getBlueOrder();
        order.setDelivery(DeliveryProvider.getShopDeliveryWithPickupType());
        return order;
    }

    // TODO: 10.03.2021 item.buyerPrice отличается от item.price
    public static Order getFulfilmentOrder() {
        return getPostPaidOrder(o -> {
            o.setFulfilment(true);

            OrderItem item1 = OrderItemProvider.buildOrderItem("1");
            item1.setSupplierId(123L);
            item1.setBuyerPrice(new BigDecimal("100.0"));
            item1.setQuantPrice(new BigDecimal("100.0"));
            item1.setWarehouseId(TEST_WAREHOUSE_ID);

            OrderItem item2 = OrderItemProvider.buildOrderItem("2");
            item2.setSupplierId(124L);
            item2.setBuyerPrice(new BigDecimal("200.0"));
            item2.setQuantPrice(new BigDecimal("200.0"));
            item2.setWarehouseId(TEST_WAREHOUSE_ID);

            o.setItems(Arrays.asList(item1, item2));
        });
    }

    private static Order configureOrder(Supplier<Order> orderSupplier, Consumer<Order> configurer) {
        Order order = orderSupplier.get();
        configurer.accept(order);
        return order;
    }

    public static OrderBuilder orderBuilder() {
        return new OrderBuilder();
    }

    public static class OrderBuilder {

        private static final Date DEFAULT_CREATION_DATE = defaultCreationDate();

        private Long id;
        private Long shopId;
        private Long businessId;
        private String label;
        private Color color;
        private OrderStatus status;
        private Date createDate;
        private Currency currency;
        private PaymentMethod paymentMethod;
        private boolean isFake;
        private Context context;
        private List<OrderItemProvider.OrderItemBuilder> itemBuilders = new ArrayList<>();
        private List<OrderItem> items = new ArrayList<>();
        private Delivery delivery;
        private DeliveryProvider.DeliveryBuilder deliveryBuilder;
        private Buyer buyer;
        private OrderAcceptMethod orderAcceptMethod;
        private BigDecimal exchangeRate;
        private BigDecimal feeTotal;
        private BigDecimal total;
        private BigDecimal itemsTotal;
        private BigDecimal buyerTotal;
        private BigDecimal buyerItemsTotal;
        private List<OrderPromo> promos = new ArrayList<>();
        private Map<OrderPropertyType, Object> orderProperties = new HashMap<>();

        private OrderBuilder() {
            color(Color.BLUE);
            shopId(SHOP_ID);
            businessId(BUSINESS_ID);
            createDate(DEFAULT_CREATION_DATE);
            context(MARKET);
            isFake(false);
            currency(RUR);
            acceptMethod(DEFAULT);
            exchangeRate(BigDecimal.ONE);
            deliveryBuilder(DeliveryProvider.deliveryBuilder());
        }

        private static Date defaultCreationDate() {
            var sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

            try {
                return sdf.parse("2018-06-19 10:05:00");
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
        }

        public OrderBuilder id(Number id) {
            this.id = id == null ? null : id.longValue();
            return this;
        }

        public OrderBuilder color(Color color) {
            this.color = color;
            return this;
        }

        public OrderBuilder shopId(Long shopId) {
            this.shopId = shopId;
            return this;
        }

        public OrderBuilder businessId(Long businessId) {
            this.businessId = businessId;
            return this;
        }

        public OrderBuilder label(Object label) {
            this.label = label.toString();
            return this;
        }

        public OrderBuilder status(OrderStatus status) {
            this.status = status;
            return this;
        }

        public OrderBuilder createDate(Date createDate) {
            this.createDate = createDate;
            return this;
        }

        public OrderBuilder currency(Currency currency) {
            this.currency = currency;
            return this;
        }

        public OrderBuilder isFake(boolean fake) {
            isFake = fake;
            return this;
        }

        public OrderBuilder context(Context context) {
            this.context = context;
            return this;
        }

        public OrderBuilder item(OrderItem item) {
            items.add(item);
            return this;
        }

        public OrderBuilder deliveryBuilder(DeliveryProvider.DeliveryBuilder deliveryBuilder) {
            this.deliveryBuilder = deliveryBuilder;
            return this;
        }

        public OrderBuilder delivery(Delivery delivery) {
            this.delivery = delivery;
            return this;
        }

        public OrderBuilder buyer(Buyer buyer) {
            this.buyer = buyer;
            return this;
        }

        public OrderBuilder someBuyer() {
            this.buyer = BuyerProvider.getBuyer();
            return this;
        }

        public OrderBuilder acceptMethod(OrderAcceptMethod orderAcceptMethod) {
            this.orderAcceptMethod = orderAcceptMethod;
            return this;
        }

        public OrderBuilder pushApi() {
            return acceptMethod(OrderAcceptMethod.PUSH_API);
        }

        public OrderBuilder exchangeRate(BigDecimal exchangeRate) {
            this.exchangeRate = exchangeRate;
            return this;
        }

        public OrderBuilder feeTotal(BigDecimal feeTotal) {
            this.feeTotal = feeTotal;
            return this;
        }

        public OrderBuilder total(BigDecimal total) {
            this.total = total;
            return this;
        }

        public OrderBuilder itemsTotal(BigDecimal itemsTotal) {
            this.itemsTotal = itemsTotal;
            return this;
        }

        public OrderBuilder buyerTotal(BigDecimal buyerTotal) {
            this.buyerTotal = buyerTotal;
            return this;
        }

        public OrderBuilder buyerItemsTotal(BigDecimal buyerItemsTotal) {
            this.buyerItemsTotal = buyerItemsTotal;
            return this;
        }

        public OrderBuilder promo(OrderPromo promo) {
            this.promos.add(promo);
            return this;
        }

        public Order build() {
            if (delivery == null && deliveryBuilder != null) {
                delivery = deliveryBuilder.build();
            }
            Order order = new Order();
            order.setId(id);
            order.setShopId(shopId);
            order.setBusinessId(businessId);
            order.setLabel(label);
            order.setStatus(status);
            order.setRgb(color);
            order.setContext(context);
            order.setCreationDate(createDate);
            order.setCurrency(currency);
            order.setBuyerCurrency(currency);
            order.setPaymentType(paymentMethod == null ? null : paymentMethod.getPaymentType());
            order.setPaymentMethod(paymentMethod);
            order.setFake(isFake);
            if (CollectionUtils.isNotEmpty(itemBuilders)) {
                order.setItems(itemBuilders.stream()
                        .map(OrderItemProvider.OrderItemBuilder::build)
                        .collect(Collectors.toList()));
            } else if (CollectionUtils.isNotEmpty(items)) {
                order.setItems(items);
            }
            order.setDelivery(delivery);
            order.setBuyer(buyer);
            order.setAcceptMethod(orderAcceptMethod);
            order.setExchangeRate(exchangeRate);
            order.setFeeTotal(feeTotal);
            order.setTotal(total);
            order.setItemsTotal(itemsTotal);
            order.setBuyerTotal(buyerTotal);
            order.setBuyerItemsTotal(buyerItemsTotal);

            orderProperties.forEach(order::setProperty);

            order.setNoAuth(false);
            order.setTaxSystem(TaxSystem.OSN);
            for (OrderPromo promo : promos) {
                order.addPromo(promo);
            }
            return order;
        }
    }
}
