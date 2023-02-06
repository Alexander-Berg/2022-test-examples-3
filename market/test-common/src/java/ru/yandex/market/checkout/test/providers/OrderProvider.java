package ru.yandex.market.checkout.test.providers;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import java.util.function.Function;
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
import ru.yandex.market.checkout.checkouter.order.ItemService;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderAcceptMethod;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.OrderPropertyType;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.SupplierType;
import ru.yandex.market.checkout.checkouter.order.TaxSystem;
import ru.yandex.market.checkout.checkouter.order.promo.OrderPromo;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.pay.PaymentType;
import ru.yandex.market.checkout.test.providers.DeliveryProvider.DeliveryBuilder;
import ru.yandex.market.checkout.test.providers.OrderItemProvider.OrderItemBuilder;

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
    public static final long SHOP_ID_WITH_SORTING_CENTER = 10773L;
    public static final long FF_SHOP_ID = 5525662L;
    public static final long FF_ONLY_DELIVERY_SHOP_ID = 5525663L;
    public static final long FAKE_SHOP_ID = 999;

    @Nonnull
    public static Order getColorOrder(@Nonnull Color color) {
        return applyDefaults(orderBuilder())
                .item(OrderItemProvider.getOrderItem())
                .color(color)
                .build();
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
    public static Order getBlueOrder(Consumer<Order> configurer) {
        return configureOrder(OrderProvider::getBlueOrder, configurer);
    }

    @Nonnull
    public static Order getWhiteOrder(Consumer<Order> configurer) {
        return configureOrder(OrderProvider::getWhiteOrder, configurer);
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

    @Nonnull
    public static OrderBuilder applyWhiteDbsDefaults(@Nonnull OrderBuilder builder) {
        return builder
                .color(Color.WHITE)
                .pushApi()
                .delivery(DeliveryProvider.getShopDelivery())
                .someBuyer()
                .isFake(false);
    }

    @Deprecated
    public static Order getPrepaidOrder() {
        Order order = getBlueOrder();
        order.setPaymentMethod(PaymentMethod.YANDEX);
        order.setPaymentType(PaymentType.PREPAID);
        OrderItem item = order.getItems().iterator().next();
        ItemService itemService = ItemServiceProvider.defaultItemService();
        itemService.setCount(item.getCount());
        itemService.setPaymentType(PaymentType.PREPAID);
        itemService.setPaymentMethod(PaymentMethod.YANDEX);
        item.setServices(Set.of(itemService));
        return order;
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

    public static Order getPrepaidOrder(Consumer<Order> configurer) {
        return configureOrder(OrderProvider::getPrepaidOrder, configurer);
    }

    public static Order getPostPaidOrder() {
        Order order = getBlueOrder();
        order.setPaymentMethod(PaymentMethod.CASH_ON_DELIVERY);
        return order;
    }

    public static Order getBluePostPaidOrder() {
        Order order = getBlueOrder();
        order.setPaymentMethod(PaymentMethod.CASH_ON_DELIVERY);
        order.setPaymentType(PaymentType.POSTPAID);
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

    @Nonnull
    public static Order getFulfillmentOrderWithYandexDelivery() {
        Order order = getBlueOrder();
        order.setAcceptMethod(WEB_INTERFACE);
        order.setDelivery(DeliveryProvider.getYandexMarketPickupDelivery());
        order.setFulfilment(true);
        return order;
    }

    @Nonnull
    public static Order getFulfillmentOrderWithPickupType() {
        Order order = getBlueOrder();
        order.setAcceptMethod(WEB_INTERFACE);
        order.setDelivery(DeliveryProvider.getShopDeliveryWithPickupType());
        order.setFulfilment(true);
        return order;
    }

    public static Order getFulfillmentOrderWithPostType() {
        Order order = getBlueOrder();
        order.setAcceptMethod(WEB_INTERFACE);
        order.setDelivery(DeliveryProvider.getShopDeliveryWithPostType());
        order.setFulfilment(true);

        return order;
    }

    public static Order getOrderWithYandexMarketDelivery() {
        Order order = getBlueOrder();
        order.setDelivery(DeliveryProvider.getYandexMarketPickupDelivery());
        return order;
    }

    public static Order getOrderWithServices() {
        Order order = getBlueOrder();
        order.getItems().iterator().next().getServices().add(ItemServiceProvider.defaultItemService());
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

    // TODO: 10.03.2021 item.buyerPrice отличается от item.price
    public static Order getFulfilmentOrderSameShopId() {
        return getPostPaidOrder((o) -> {
            o.setFulfilment(true);

            OrderItem item1 = OrderItemProvider.buildOrderItem("1");
            item1.setSupplierId(123L);
            item1.setBuyerPrice(new BigDecimal("100.0"));
            item1.setQuantPrice(new BigDecimal("100.0"));

            OrderItem item2 = OrderItemProvider.buildOrderItem("2");
            item2.setSupplierId(123L);
            item2.setBuyerPrice(new BigDecimal("200.0"));
            item2.setQuantPrice(new BigDecimal("200.0"));

            OrderItem item3 = OrderItemProvider.buildOrderItem("3");
            item3.setSupplierId(123L);
            item3.setBuyerPrice(new BigDecimal("300.0"));
            item3.setQuantPrice(new BigDecimal("300.0"));

            o.setItems(Arrays.asList(item1, item2, item3));
        });
    }

    public static Order getCrossborderBlueOrder() {
        Order order = getBlueOrder();
        order.setAcceptMethod(WEB_INTERFACE);
        order.setDelivery(DeliveryProvider.getShopDeliveryWithPostType());
        order.setFulfilment(false);
        order.setDropship(true);
        order.setProperty(OrderPropertyType.IS_CROSSBORDER, true);
        return order;
    }

    // TODO: 10.03.2021 item.buyerPrice отличается от item.price
    public static Order getFulfilmentOrder1PShopId() {
        return getPostPaidOrder((o) -> {
            o.setFulfilment(true);

            OrderItem item1 = OrderItemProvider.buildOrderItem("1");
            item1.setSupplierId(123L);
            item1.setSupplierType(SupplierType.FIRST_PARTY);
            item1.setBuyerPrice(new BigDecimal("100.0"));
            item1.setQuantPrice(new BigDecimal("100.0"));

            OrderItem item2 = OrderItemProvider.buildOrderItem("2");
            item2.setSupplierId(123L);
            item2.setSupplierType(SupplierType.FIRST_PARTY);
            item2.setBuyerPrice(new BigDecimal("200.0"));
            item2.setQuantPrice(new BigDecimal("200.0"));

            o.setItems(Arrays.asList(item1, item2));
        });
    }


    private static Order configureOrder(Supplier<Order> orderSupplier, Consumer<Order> configurer) {
        Order order = orderSupplier.get();
        configurer.accept(order);
        return order;
    }

    private static Order getBlueFulfilmentCart() {
        return configureOrder(OrderProvider::getFulfilmentOrder, o -> {
            o.setRgb(Color.BLUE);
            o.setPaymentOptions(EnumSet.of(
                    PaymentMethod.YANDEX,
                    PaymentMethod.APPLE_PAY,
                    PaymentMethod.GOOGLE_PAY,
                    PaymentMethod.CASH_ON_DELIVERY,
                    PaymentMethod.CARD_ON_DELIVERY
            ));
        });
    }

    public static Order getBlueFulfilmentCart(Consumer<Order> configurer) {
        return configureOrder(OrderProvider::getBlueFulfilmentCart, configurer);
    }

    public static Order getBlueOrderForBusinessClient() {
        var order = OrderProvider.getBlueOrder();
        order.getBuyer().setBusinessBalanceId(123L);
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
        private List<OrderItemBuilder> itemBuilders = new ArrayList<>();
        private List<OrderItem> items = new ArrayList<>();
        private Delivery delivery;
        private DeliveryBuilder deliveryBuilder;
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

        @Nonnull
        public OrderBuilder configure(@Nonnull Function<OrderBuilder, OrderBuilder> configure) {
            return configure.apply(this);
        }

        public OrderBuilder id(Number id) {
            this.id = id == null ? null : id.longValue();
            return this;
        }

        public OrderBuilder someId() {
            return id(ThreadLocalRandom.current().nextInt(1, Integer.MAX_VALUE));
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

        public OrderBuilder someLabel() {
            return label(UUID.randomUUID());
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

        public OrderBuilder paymentMethod(PaymentMethod paymentMethod) {
            this.paymentMethod = paymentMethod;
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

        public OrderBuilder itemBuilder(OrderItemBuilder item) {
            itemBuilders.add(item);
            item.orderAcceptMethod(orderAcceptMethod);
            item.supplierId(shopId);
            return this;
        }

        public OrderBuilder item(OrderItem item) {
            items.add(item);
            return this;
        }

        public <P> OrderBuilder property(OrderPropertyType<P> property, P value) {
            orderProperties.put(property, value);
            return this;
        }

        public OrderBuilder deliveryBuilder(DeliveryBuilder deliveryBuilder) {
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

        public OrderBuilder stubApi() {
            return acceptMethod(WEB_INTERFACE);
        }

        public OrderBuilder pushApi() {
            return acceptMethod(OrderAcceptMethod.PUSH_API);
        }

        public OrderBuilder sandbox() {
            return acceptMethod(OrderAcceptMethod.PUSHAPI_SANDBOX);
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
                        .map(OrderItemBuilder::build)
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
