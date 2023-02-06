package ru.yandex.market.checkout.test.providers;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.google.protobuf.InvalidProtocolBufferException;
import org.apache.commons.collections4.CollectionUtils;

import ru.yandex.common.util.ObjectUtils;
import ru.yandex.market.checkout.checkouter.cart.ItemChange;
import ru.yandex.market.checkout.checkouter.order.ItemService;
import ru.yandex.market.checkout.checkouter.order.OfferItemKey;
import ru.yandex.market.checkout.checkouter.order.OrderAcceptMethod;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.PartnerPriceMarkup;
import ru.yandex.market.checkout.checkouter.order.PartnerPriceMarkupCoefficient;
import ru.yandex.market.checkout.checkouter.order.SupplierType;
import ru.yandex.market.checkout.checkouter.order.VatType;
import ru.yandex.market.checkout.checkouter.order.promo.ItemPromo;
import ru.yandex.market.checkout.cipher.CipherService;
import ru.yandex.market.checkout.findbugs.SuppressFBWarnings;
import ru.yandex.market.common.report.model.FeedOfferId;
import ru.yandex.market.common.report.model.QuantityLimits;
import ru.yandex.market.report.MarketSearchSession.CpaContextRecord;

import static org.apache.commons.codec.digest.DigestUtils.md5Hex;
import static ru.yandex.market.checkout.checkouter.order.VatType.VAT_18;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.MOCK_SORTING_CENTER_HARDCODED;
import static ru.yandex.market.checkout.test.providers.OrderProvider.SHOP_ID_WITH_SORTING_CENTER;

@SuppressWarnings("checkstyle:MagicNumber")
public abstract class OrderItemProvider {

    public static final String TEST_SHOP_SKU = "testShopSKU";
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

    public static OrderItem getAnotherWarehouseOrderItem() {
        OrderItem result = getAnotherOrderItem();
        result.setWarehouseId(DeliveryProvider.ANOTHER_WAREHOUSE_ID.intValue());
        return result;
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
                .shopSku(TEST_SHOP_SKU)
                .vat(VAT_18)
                .warehouseId(MOCK_SORTING_CENTER_HARDCODED.intValue())
                .price(BigDecimal.valueOf(250L))
                .quantPrice(BigDecimal.valueOf(250L))
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
        item.setValidIntQuantity(count);
        item.setMsku((long) offerId.hashCode());
        item.setFulfilmentWarehouseId(fulfilmentWarehouseId);
        return item;
    }

    @Nonnull
    public static OrderItem buildOrderItem(String offerId, int feeInt, Long feedId) {
        BigDecimal fee = BigDecimal.valueOf(feeInt)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        OrderItem item = defaultOrderItem();
        item.setFeedId(feedId);
        item.setOfferId(offerId);
        item.setCount(1);
        item.setValidIntQuantity(1);
        item.setMsku((long) offerId.hashCode());
        item.setFeeInt(feeInt);
        item.setFee(fee);
        item.setFeeSum(fee);
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
        item.setValidIntQuantity(count);
        item.setPrice(price);
        item.setQuantPrice(price);
        item.setBuyerPrice(price);
        return item;
    }

    @Nonnull
    public static OrderItem buildOrderItem(Integer warehouseId) {
        OrderItem orderItem = defaultOrderItem();
        orderItem.setWarehouseId(warehouseId);
        return orderItem;
    }

    @Nonnull
    public static OrderItem buildOrderItemDigital(String offerId) {
        var item = buildOrderItem(offerId);
        item.setWarehouseId(null);
        item.setDigital(true);
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

    public static void patchShowInfo(OrderItem orderItem, CipherService reportCipherService) {
        orderItem.setShowInfo(generateShowInfo(cpaRecord(
                SHOW_INFO,
                orderItem.getFee(),
                orderItem.getFeeSum(),
                orderItem.getShowUid(),
                orderItem.getWareMd5(),
                orderItem.getPp(),
                reportCipherService
        ), reportCipherService));
    }

    public static String generateShowInfo(CpaContextRecord cpaContextRecord, CipherService reportCipherService) {
        byte[] raw = cpaContextRecord.toByteArray();
        int pad = raw.length % BYTELEN;
        if (pad != 0) {
            raw = Arrays.copyOf(raw, raw.length + BYTELEN - pad);
            for (int i = 1; i <= BYTELEN - pad; i++) {
                raw[raw.length - i] = 0;
            }
        }
        return reportCipherService.cipher(raw);
    }

    public static CpaContextRecord cpaRecord(
            String showInfo, BigDecimal fee, BigDecimal feeSum, String showUid, String wareMd5, Integer pp,
            CipherService reportCipherService
    ) {
        try {
            CpaContextRecord.Builder builder = CpaContextRecord.newBuilder()
                    .mergeFrom(reportCipherService.performUnpadding(reportCipherService.decipher(showInfo)))
                    .setWareMd5(wareMd5);
            if (fee != null) {
                builder.setFee(fee.toString());
            }
            if (feeSum != null) {
                builder.setFeeSum(feeSum.toString());
            }
            if (showUid != null) {
                builder.setShowUid(showUid);
            }
            if (pp != null) {
                builder.setPp(pp);
            }
            return builder.build();
        } catch (InvalidProtocolBufferException e) {
            throw new RuntimeException(e);
        }
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

    public static Collection<OrderItem> getDefaultItems() {
        return Arrays.asList(
                OrderItemProvider.buildOrderItem("1", 5),
                OrderItemProvider.getAnotherOrderItem()
        );
    }

    public static OrderItemBuilder orderItemBuilder() {
        return new OrderItemBuilder();
    }

    public static OrderItemBuilder similar(OrderItemBuilder item) {
        return item.clone();
    }

    //TODO: рефакторинг - переосмыслить использование, неочевидное
    public static OrderItemBuilder orderItemWithSortingCenter() {
        return orderItemBuilder()
                .configure(OrderItemProvider::applyDefaults)
                .someDimensions()
                .supplierId(SHOP_ID_WITH_SORTING_CENTER)
                .warehouseId(MOCK_SORTING_CENTER_HARDCODED);
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
        private Set<ItemChange> changes = EnumSet.noneOf(ItemChange.class);
        private QuantityLimits quantityLimits;

        private Long weight;
        private Long width;
        private Long height;
        private Long depth;
        private BigDecimal quantPrice;
        private BigDecimal quantity;
        private boolean digital;
        private Set<Integer> cargoTypes = Collections.emptySet();

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

        public OrderItemBuilder label(String label) {
            this.label = label;
            return this;
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

        public OrderItemBuilder someDimensions() {
            return weight(666)
                    .width(500)
                    .height(200)
                    .depth(400);
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

        public OrderItemBuilder clientPrice(Number price) {
            this.clientPrice = BigDecimal.valueOf(price.longValue());
            return this;
        }

        public OrderItemBuilder reportPrice(Number reportPrice) {
            this.reportPrice = BigDecimal.valueOf(reportPrice.longValue());
            return this;
        }

        public OrderItemBuilder prepayEnabled(boolean prepayEnabled) {
            this.prepayEnabled = prepayEnabled;
            return this;
        }

        public OrderItemBuilder preorder(boolean preorder) {
            this.preorder = preorder;
            return this;
        }

        public OrderItemBuilder atSupplierWarehouse(boolean atSupplierWarehouse) {
            this.atSupplierWarehouse = atSupplierWarehouse;
            return this;
        }

        public OrderItemBuilder recommendedByVendor(boolean recommendedByVendor) {
            this.recommendedByVendor = recommendedByVendor;
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

        public OrderItemBuilder fulfilmentWarehouseId(long warehouseId) {
            this.fulfilmentWarehouseId = warehouseId;
            return this;
        }

        public OrderItemBuilder supplierId(Number supplierId) {
            this.supplierId = supplierId.longValue();
            return this;
        }

        public OrderItemBuilder inputOrder(Number inputOrder) {
            this.inputOrder = inputOrder.intValue();
            return this;
        }

        public OrderItemBuilder supplierType(SupplierType supplierType) {
            this.supplierType = supplierType;
            return this;
        }

        public OrderItemBuilder setNoPromoBundle() {
            this.bundleId = null;
            this.countInBundle = null;
            this.primaryInBundle = null;
            return this;
        }

        public OrderItemBuilder promoBundle(String bundleId) {
            this.bundleId = bundleId;
            return this;
        }

        public OrderItemBuilder primaryInBundle(Boolean primaryInBundle) {
            this.primaryInBundle = primaryInBundle;
            return this;
        }

        public OrderItemBuilder promo(ItemPromo itemPromo) {
            promos.add(itemPromo);
            return this;
        }

        public OrderItemBuilder promos(List<ItemPromo> promos) {
            this.promos = promos;
            return this;
        }

        public OrderItemBuilder noPromos() {
            promos.clear();
            return this;
        }

        public OrderItemBuilder services(Set<ItemService> services) {
            this.services = services;
            return this;
        }

        public OrderItemBuilder digital(boolean value) {
            this.digital = value;
            return this;
        }

        public OrderItemBuilder quantPrice(BigDecimal quantPrice) {
            this.quantPrice = quantPrice;
            return this;
        }

        public OrderItemBuilder quantity(BigDecimal quantity) {
            this.quantity = quantity;
            return this;
        }

        public OrderItemBuilder cargoTypes(Set<Integer> cargoTypes) {
            this.cargoTypes = cargoTypes;
            return this;
        }

        public OrderItemBuilder quantityLimits(QuantityLimits quantityLimits) {
            this.quantityLimits = quantityLimits;
            return this;
        }

        public OrderItemBuilder change(ItemChange itemChange) {
            this.changes.add(itemChange);
            return this;
        }

        public OrderItem build() {
            OfferItemKey itemKey = OfferItemKey.of(offerId, feedId, bundleId);
            OrderItem item = new OrderItem();
            item.setId(id);
            item.setLabel(label);
            item.setOfferName(name);
            if (CollectionUtils.isNotEmpty(changes)) {
                item.setChanges(changes);
            }
            item.setDescription(description);
            item.setOfferItemKey(itemKey);
            item.setWareMd5(wareMd5);
            item.setMsku(marketSku);
            item.setShopSku(shopSku);
            item.setCategoryId(categoryId);
            item.setModelId(modelId);
            item.setFeedCategoryId(feedCategoryId);
            item.setPrice(price);
            item.setQuantPrice(quantPrice);
            item.setBuyerPrice(price);
            item.setPrimaryInBundle(primaryInBundle);
            item.setQuantityLimits(quantityLimits);
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
            item.setQuantity(ObjectUtils.defaultIfNull(quantity, BigDecimal.valueOf(count)));
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
            item.setCargoTypes(cargoTypes);

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
