package ru.yandex.market.notifier.util.providers;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import ru.yandex.market.checkout.checkouter.order.ItemService;
import ru.yandex.market.checkout.checkouter.order.OfferItemKey;
import ru.yandex.market.checkout.checkouter.order.OrderAcceptMethod;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.PartnerPriceMarkup;
import ru.yandex.market.checkout.checkouter.order.PartnerPriceMarkupCoefficient;
import ru.yandex.market.checkout.checkouter.order.SupplierType;
import ru.yandex.market.checkout.checkouter.order.VatType;
import ru.yandex.market.checkout.checkouter.order.promo.ItemPromo;
import ru.yandex.market.checkout.findbugs.SuppressFBWarnings;
import ru.yandex.market.common.report.model.FeedOfferId;

import static org.apache.commons.codec.digest.DigestUtils.md5Hex;
import static ru.yandex.market.checkout.checkouter.order.VatType.VAT_18;
import static ru.yandex.market.notifier.util.providers.DeliveryProvider.MOCK_SORTING_CENTER_HARDCODED;

@SuppressWarnings("checkstyle:MagicNumber")
public abstract class OrderItemProvider {

    public static final String SHOW_INFO =
            "8-qH2tqoDtK8sKvSHArdFbfM_iCg5kyByYeXy3dlPCdNv1lid31JB8pvjHgl6MV1hIC11FlxrNuNItNe23jmzGYrfyzX" +
                    "-vxBC9gOKceGqET3eTYdWTjQctQvdoNPpXqW";
    public static final long FEED_ID = 1L;
    public static final String DEFAULT_WARE_MD5 = "-_40VqaS9BpXO1qaTtweBA";
    public static final String ANOTHER_WARE_MD5 = "k6iFKulm-OlRhVVYKzwngg";
    public static final String THIRD_WARE_MD5 = "O1iFKulm-OlRhVVYKurkam";
    public static final String ANOTHER_SHOW_INFO =
            "8-qH2tqoDtI8oFxOCmHLyET_0to7qDKi5qFJ6Vc1oW3iu0dLSUKzldwMoKuCGbYxtMaeKia6s__KxA-Lr38" +
                    "-iwG1pNYaKK9OeeksKmZjXbUwdwX5JdpyuRWrUH6-80cI";
    public static final String OTHER_WARE_MD5 = "INBviaa8PEWRR-mSbGWjog";
    public static final String OTHER_SHOW_INFO =
            "8-qH2tqoDtKkTQpQi-93JPET5x47gA0EbHuHccCk5TTaXVOhkfD4FBedJkoYTgX8WrtlPymqfNnKuVovF2u" +
                    "sYD0sI5zwmfcMiN47MvWVpda39kpElaDe-jm1HwmCvPKT";

    private static final int BYTELEN = 8;

    @Nonnull
    public static OrderItem getOrderItem() {
        return defaultOrderItem();
    }

    @Nonnull
    public static OrderItem getAnotherOrderItem() {
        OrderItem anotherOrderItem = buildOrderItem("2", 3);
        anotherOrderItem.setWareMd5(ANOTHER_WARE_MD5);
        anotherOrderItem.setShowInfo(ANOTHER_SHOW_INFO);
        return anotherOrderItem;
    }

    @Nonnull
    public static OrderItemBuilder applyDefaults(@Nonnull OrderItemBuilder itemBuilder) {
        return orderItemBuilder()
                .someId()
                .prepayEnabled(true)
                .feedId(FEED_ID)
                .categoryId(123)
                .modelId(13334267L)
                .wareMd5(DEFAULT_WARE_MD5)
                .feedCategoryId("123")
                .supplierId(123L)
                .vendorId(10545982L)
                .shopSku("shop_sku_test")
                .vat(VAT_18)
                .warehouseId(MOCK_SORTING_CENTER_HARDCODED.intValue())
                .price(BigDecimal.valueOf(250L))
                .postConfigurer(item -> {
                    //TODO уюрать - используется в паре тестов
                    item.getPrices().setPartnerPrice(BigDecimal.valueOf(100));
                    final PartnerPriceMarkup partnerPriceMarkup = new PartnerPriceMarkup();
                    partnerPriceMarkup.setCoefficients(Arrays.asList(
                            new PartnerPriceMarkupCoefficient("COOKIES", new BigDecimal("1")),
                            new PartnerPriceMarkupCoefficient("PANCAKES", new BigDecimal("1.5"))
                    ));
                    partnerPriceMarkup.setUpdateTime(System.currentTimeMillis());
                    item.getPrices().setPartnerPriceMarkup(partnerPriceMarkup);
                });
    }

    @Nonnull
    public static OrderItem defaultOrderItem() {
        return orderItemBuilder()
                .configure(OrderItemProvider::applyDefaults)
                .build();
    }

    @Nonnull
    public static OrderItem buildOrderItem(FeedOfferId feedOfferId) {
        OrderItem item = defaultOrderItem();
        item.setFeedOfferId(feedOfferId);
        return item;
    }

    @Nonnull
    public static OrderItem buildOrderItem(String offerId, Long itemId, int count, Long fulfilmentWarehouseId) {
        OrderItem item = defaultOrderItem();
        item.setId(itemId);
        item.setOfferId(offerId);
        item.setCount(count);
        item.setMsku((long) offerId.hashCode());
        item.setFulfilmentWarehouseId(fulfilmentWarehouseId);
        return item;
    }

    @Nonnull
    public static OrderItem buildOrderItem(String offerId, int count) {
        return buildOrderItem(offerId, null, count, null);
    }

    @Nonnull
    public static OrderItem buildOrderItem(String offerId) {
        return buildOrderItem(offerId, null, 1, null);
    }


    @Nonnull
    public static OrderItem buildOrderItem(String offerId, Long itemId, int count) {
        return buildOrderItem(offerId, itemId, count, null);
    }

    @Nonnull
    public static OrderItem buildOrderItem(String offerId, BigDecimal price, int count) {
        OrderItem item = defaultOrderItem();
        item.setOfferId(offerId);
        item.setCount(count);
        item.setPrice(price);
        item.setBuyerPrice(price);
        item.setQuantPrice(price);
        return item;
    }

    //TODO: move to concrete test
    @Deprecated
    public static OrderItem pushApiOrderItem() {
        return orderItemBuilder()
                .feedId(200305173L)
                .offerId("4")
                .count(4)
                .price(new BigDecimal("9.99"))
                .build();
    }

    /**
     * Возвращает поток {@link OrderItem}'ов с генерированными offerId вида:
     * <br/>
     * <i>orderIdPrefix</i>_0, <i>orderIdPrefix</i>_1, <i>orderIdPrefix</i>_2, ...
     *
     * @param offerIdPrefix префикс id оффера
     * @return созданный поток
     */
    public static Stream<OrderItem> stream(String offerIdPrefix) {
        AtomicInteger value = new AtomicInteger();

        return Stream.generate(() -> OrderItemProvider.buildOrderItem(offerIdPrefix + "_" + value.getAndIncrement()));
    }

    public static OrderItemBuilder orderItemBuilder() {
        return new OrderItemBuilder();
    }

    public static class OrderItemBuilder implements Cloneable {

        private Long id;
        private String label;
        private Long feedId;
        private Long vendorId;
        private Long modelId;
        private String name;
        private String description;
        private String offerId;
        private String wareMd5;
        private Integer categoryId;
        private Integer inputOrder;
        private String feedCategoryId;
        private String bundleId;
        private Boolean primaryInBundle;
        private Integer countInBundle;
        private String shopSku;
        private Long marketSku;
        private BigDecimal price;
        private BigDecimal clientPrice;
        private BigDecimal reportPrice;
        private Integer count;
        private Integer warehouseId;
        private Long fulfilmentWarehouseId;
        private Long supplierId;
        private SupplierType supplierType;
        private VatType vat;
        private boolean prepayEnabled;
        private boolean recommendedByVendor;
        private boolean preorder;
        private boolean atSupplierWarehouse;
        private OrderAcceptMethod orderAcceptMethod;
        private Consumer<OrderItem> postConfigurer;
        private List<ItemPromo> promos = new ArrayList<>();
        private Set<ItemService> services = new HashSet<>();

        private Long weight;
        private Long width;
        private Long height;
        private Long depth;

        private boolean digital;

        public OrderItemBuilder() {
            someOfferId();
            someCategoryId();
            someFeedCategoryId();
            name("OfferName");
            description("Description");
            count(1);
            weight(666);
            someShopSku();
        }

        @Nonnull
        public OrderItemBuilder configure(@Nonnull Function<OrderItemBuilder, OrderItemBuilder> configure) {
            return configure.apply(this);
        }

        @Nonnull
        public OrderItemBuilder postConfigurer(Consumer<OrderItem> postConfigurer) {
            this.postConfigurer = postConfigurer;
            return this;
        }

        public OrderItemBuilder id(Number id) {
            this.id = id == null ? null : id.longValue();
            return this;
        }

        public OrderItemBuilder emptyId() {
            return id(null);
        }

        public OrderItemBuilder someId() {
            return id(ThreadLocalRandom.current().nextInt(1, Integer.MAX_VALUE));
        }

        public OrderItemBuilder someShopSku() {
            return shopSku("shop-sku-" + String.valueOf(ThreadLocalRandom.current().nextInt(1, Integer.MAX_VALUE)));
        }

        public OrderItemBuilder categoryId(Number categoryId) {
            this.categoryId = categoryId == null ? null : categoryId.intValue();
            return this;
        }

        public OrderItemBuilder someCategoryId() {
            return categoryId(ThreadLocalRandom.current().nextInt(1, Integer.MAX_VALUE));
        }

        public OrderItemBuilder modelId(Number modelId) {
            this.modelId = modelId.longValue();
            return this;
        }

        public OrderItemBuilder vendorId(Number vendorId) {
            this.vendorId = vendorId.longValue();
            return this;
        }

        public OrderItemBuilder feedCategoryId(String feedCategoryId) {
            this.feedCategoryId = feedCategoryId;
            return this;
        }

        public OrderItemBuilder vat(VatType vat) {
            this.vat = vat;
            return this;
        }

        public OrderItemBuilder weight(Number weight) {
            this.weight = weight != null ? weight.longValue() : null;
            return this;
        }

        public OrderItemBuilder width(Number width) {
            this.width = width != null ? width.longValue() : null;
            return this;
        }

        public OrderItemBuilder height(Number height) {
            this.height = height != null ? height.longValue() : null;
            return this;
        }

        public OrderItemBuilder depth(Number depth) {
            this.depth = depth != null ? depth.longValue() : null;
            return this;
        }

        public OrderItemBuilder someFeedCategoryId() {
            return feedCategoryId(String.valueOf(ThreadLocalRandom.current().nextLong()));
        }

        public OrderItemBuilder name(String name) {
            this.name = name;
            return this;
        }

        public OrderItemBuilder description(String description) {
            this.description = description;
            return this;
        }

        public OrderItemBuilder someOfferId() {
            return offerId(String.valueOf(ThreadLocalRandom.current().nextInt(Integer.MAX_VALUE)));
        }

        @SuppressFBWarnings(justification = "md5 for this case is normal")
        public OrderItemBuilder offer(long feed, String offer) {
            return shopSku(offer)
                    .marketSku(feed + offer.hashCode())
                    .feedId(feed)
                    .wareMd5(md5Hex(offer))
                    .offerId(offer);
        }

        public OrderItemBuilder offer(String offer) {
            return offer(FEED_ID, offer);
        }

        public OrderItemBuilder feedId(Number feedId) {
            this.feedId = feedId.longValue();
            return this;
        }

        public OrderItemBuilder offerId(String offerId) {
            this.offerId = offerId;
            return this;
        }

        public OrderItemBuilder shopSku(String shopSku) {
            this.shopSku = shopSku;
            return this;
        }

        public OrderItemBuilder marketSku(Number marketSku) {
            if (marketSku != null) {
                this.marketSku = marketSku.longValue();
            }
            return this;
        }

        public OrderItemBuilder wareMd5(String wareMd5) {
            this.wareMd5 = wareMd5;
            return this;
        }

        public OrderItemBuilder orderAcceptMethod(OrderAcceptMethod orderAcceptMethod) {
            this.orderAcceptMethod = orderAcceptMethod;
            return this;
        }

        public OrderItemBuilder price(Number price) {
            this.price = BigDecimal.valueOf(price.longValue());
            return this;
        }

        public OrderItemBuilder prepayEnabled(boolean prepayEnabled) {
            this.prepayEnabled = prepayEnabled;
            return this;
        }

        public OrderItemBuilder count(Number count) {
            this.count = count.intValue();
            return this;
        }

        public OrderItemBuilder warehouseId(Number warehouseId) {
            this.warehouseId = warehouseId == null ? null : warehouseId.intValue();
            return this;
        }

        public OrderItemBuilder supplierId(Number supplierId) {
            this.supplierId = supplierId.longValue();
            return this;
        }

        public OrderItemBuilder promos(List<ItemPromo> promos) {
            this.promos = promos;
            return this;
        }

        public OrderItem build() {
            OfferItemKey itemKey = OfferItemKey.of(offerId, feedId, bundleId);
            OrderItem item = new OrderItem();
            item.setId(id);
            item.setLabel(label);
            item.setOfferName(name);
            item.setDescription(description);
            item.setOfferItemKey(itemKey);
            item.setWareMd5(wareMd5);
            item.setMsku(marketSku);
            item.setShopSku(shopSku);
            item.setCategoryId(categoryId);
            item.setModelId(modelId);
            item.setFeedCategoryId(feedCategoryId);
            item.setPrice(price);
            item.setBuyerPrice(price);
            item.setQuantPrice(price);
            item.setPrimaryInBundle(primaryInBundle);
            if (inputOrder != null) {
                item.setOriginalInputIndex(inputOrder);
            }

            if (orderAcceptMethod != null) {
                switch (orderAcceptMethod) {
                    case WEB_INTERFACE:
                        if (reportPrice == null) {
                            reportPrice = price;
                        }
                        break;
                    case PUSH_API:
                        atSupplierWarehouse = true;
                    default:
                        break;
                }
            }

            if (reportPrice != null) {
                item.getPrices().setReportPrice(reportPrice);
            }
            if (clientPrice != null) {
                item.getPrices().setClientBuyerPrice(clientPrice);
            }
            item.setCount(count);
            item.setQuantity(null == count ? null : BigDecimal.valueOf(count));
            item.setWarehouseId(warehouseId);
            item.setFulfilmentWarehouseId(fulfilmentWarehouseId);
            item.setSupplierId(supplierId);
            item.setSupplierType(supplierType);
            item.setAtSupplierWarehouse(atSupplierWarehouse);
            item.setCountInBundle(countInBundle);
            item.setVat(vat);
            item.setVendorId(vendorId);
            item.setPrepayEnabled(prepayEnabled);
            item.setRecommendedByVendor(recommendedByVendor);
            item.setWeight(weight);
            item.setWidth(width);
            item.setHeight(height);
            item.setDepth(depth);

            item.setClassifierMagicId("1ec5b43b86445f8b62e1b9eb084a9020");
            item.setFee(BigDecimal.valueOf(0.02));
            item.setFeeSum(BigDecimal.valueOf(12));
            item.setPp(1000);
            item.setShowUid("showUid");
            item.setCartShowUid("cartShowUid");
            item.setCartShowInfo(SHOW_INFO);
            item.setShowInfo(SHOW_INFO);
            item.setPreorder(preorder);

            for (ItemPromo promo : promos) {
                item.addOrReplacePromo(promo);
            }

            for (ItemService service : services) {
                item.addService(service);
            }

            if (postConfigurer != null) {
                postConfigurer.accept(item);
            }

            item.setDigital(digital);

            return item;
        }

        @Override
        public OrderItemBuilder clone() {
            try {
                return ((OrderItemBuilder) super.clone())
                        .promos(new ArrayList<>(promos))
                        .emptyId();
            } catch (CloneNotSupportedException e) {
                throw new RuntimeException("Unexpected exception", e);
            }
        }
    }
}
