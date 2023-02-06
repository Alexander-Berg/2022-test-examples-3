package ru.yandex.market.loyalty.core.utils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.apache.commons.collections.CollectionUtils;

import ru.yandex.market.loyalty.api.model.CashbackPromoRequest;
import ru.yandex.market.loyalty.api.model.CashbackRequest;
import ru.yandex.market.loyalty.api.model.ItemPromoResponse;
import ru.yandex.market.loyalty.api.model.MarketPlatform;
import ru.yandex.market.loyalty.api.model.OrderItemRequest;
import ru.yandex.market.loyalty.api.model.OrderItemResponse;
import ru.yandex.market.loyalty.api.model.OrderRequestWithoutOrderId;
import ru.yandex.market.loyalty.api.model.PaymentType;
import ru.yandex.market.loyalty.api.model.UsageClientDeviceType;
import ru.yandex.market.loyalty.api.model.bundle.BundledOrderItemRequest;
import ru.yandex.market.loyalty.api.model.delivery.DeliveryRequest;
import ru.yandex.market.loyalty.api.model.discount.ExternalItemDiscount;
import ru.yandex.market.loyalty.api.model.discount.MultiCartDiscountRequest;
import ru.yandex.market.loyalty.api.model.discount.MultiCartWithBundlesDiscountRequest;
import ru.yandex.market.loyalty.api.model.discount.OrderWithDeliveriesRequest;
import ru.yandex.market.loyalty.api.model.report.InternalSpec;
import ru.yandex.market.loyalty.api.model.report.Specs;
import ru.yandex.market.loyalty.api.model.spread.CountBound;
import ru.yandex.market.loyalty.api.model.spread.ReceiptBound;
import ru.yandex.market.loyalty.api.model.spread.SpreadDiscountCountSpecification;
import ru.yandex.market.loyalty.api.model.spread.SpreadDiscountReceiptSpecification;
import ru.yandex.market.loyalty.api.model.spread.SpreadDiscountSpecification;
import ru.yandex.market.loyalty.core.model.order.ItemKey;
import ru.yandex.market.loyalty.core.service.discount.constants.DeliveryPartnerType;

import static ru.yandex.market.loyalty.core.test.SupplementaryDataLoader.ALCOHOL_CATEGORY;
import static ru.yandex.market.loyalty.core.test.SupplementaryDataLoader.STICK_CATEGORY;
import static ru.yandex.market.loyalty.core.utils.BuildCustomizer.Util.customize;
import static ru.yandex.market.loyalty.core.utils.DeliveryRequestUtils.SELECTED;

/**
 * @author dinyat
 * 05/06/2017
 */
public class OrderRequestUtils {
    public static final long DEFAULT_FEED_ID = 200321470L;
    public static final ItemKey DEFAULT_ITEM_KEY = ItemKey.ofFeedOffer(DEFAULT_FEED_ID, "65");
    public static final int DEFAULT_SUPPLIER_ID = 1000;
    public static final ItemKey ANOTHER_ITEM_KEY = ItemKey.ofFeedOffer(2131L, "45666");
    public static final ItemKey THIRD_ITEM_KEY = ItemKey.ofFeedOffer(1312312L, "12331");
    public static final BigDecimal DEFAULT_QUANTITY = BigDecimal.ONE;
    public static final BigDecimal DEFAULT_PRICE = BigDecimal.valueOf(1000);
    private static final int DEFAULT_CATEGORY_ID = 123213;
    public static final String DEFAULT_ORDER_ID = "1";
    public static final Long DEFAULT_ORDER_ID_LONG = Long.parseLong(DEFAULT_ORDER_ID);
    public static final String ANOTHER_ORDER_ID = "2";
    public static final Long ANOTHER_ORDER_ID_LONG = Long.parseLong(ANOTHER_ORDER_ID);
    public static final String DEFAULT_MSKU = "111";
    public static final String ANOTHER_MSKU = "AAA";
    public static final String DEFAULT_SSKU = "222";
    public static final int MARKET_WAREHOUSE_ID = 145;
    public static final int SUPPLIER_WAREHOUSE_ID = 100;
    public static final int EXCLUDED_SUPPLIER_ID = 1015449;
    public static final long DEFAULT_VENDOR_ID = 1L;
    public static final Integer DEFAULT_WAREHOUSE_ID = 123;
    public static final long MOSCOW_REGION = 213L;
    public static final String DEFAULT_MULTI_ORDER_ID = "multi-order";

    private OrderRequestUtils() {
    }

    public static String offerId(long feedId, String ssku) {
        return feedId + "." + ssku;
    }

    public static BigDecimal sumPriceWithDiscount(OrderItemResponse item) {
        return item.getPrice().subtract(totalDiscount(item)).multiply(item.getQuantity());
    }

    public static BigDecimal totalDiscount(OrderItemResponse item) {
        return item.getPromos().stream()
                .map(ItemPromoResponse::getDiscount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public static BigDecimal totalPrice(MultiCartDiscountRequest request) {
        return request.getOrders().stream()
                .flatMap(o -> o.getItems().stream())
                .map(i -> i.getPrice().multiply(i.getQuantity()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public static BigDecimal totalPrice(MultiCartWithBundlesDiscountRequest request) {
        return request.getOrders().stream()
                .flatMap(o -> o.getItems().stream())
                .map(i -> i.getPrice().multiply(i.getQuantity()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public static OrderRequestBuilder orderRequestBuilder() {
        return new OrderRequestBuilder();
    }

    public static OrderRequestWithBundlesBuilder orderRequestWithBundlesBuilder() {
        return new OrderRequestWithBundlesBuilder();
    }

    @SafeVarargs
    public static OrderItemBuilder orderItemBuilder(
            BuildCustomizer<BundledOrderItemRequest, OrderItemBuilder>... customizers
    ) {
        return customize(OrderRequestUtils::defaultItemRequest, customizers);
    }

    public static Function<Long, ItemKey> keyOf() {
        return (index) -> ItemKey.ofFeedOffer(index, String.valueOf(index));
    }

    public static BuildCustomizer<BundledOrderItemRequest, OrderItemBuilder> itemKey(ItemKey itemKey) {
        return b -> b.setItemKey(itemKey);
    }

    public static BuildCustomizer<BundledOrderItemRequest, OrderItemBuilder> cashbackPromo(
            CashbackPromoRequest... promos
    ) {
        ImmutableList.Builder<CashbackPromoRequest> builder = ImmutableList.builder();
        builder.addAll(Arrays.asList(promos));
        ImmutableList<CashbackPromoRequest> requests = builder.build();
        return b -> b.setCashbackPromos(requests);
    }

    public static BuildCustomizer<BundledOrderItemRequest, OrderItemBuilder> nullCashbackPromo(
    ) {
        return b -> b.setCashbackPromos(null);
    }

    public static BuildCustomizer<BundledOrderItemRequest, OrderItemBuilder> itemKey(long feedId, String offerId) {
        return b -> b.setItemKey(ItemKey.ofFeedOffer(feedId, offerId));
    }

    public static BuildCustomizer<BundledOrderItemRequest, OrderItemBuilder> downloadable(boolean isDownloadable) {
        return b -> b.setDownloadable(isDownloadable);
    }

    public static BuildCustomizer<BundledOrderItemRequest, OrderItemBuilder> vendor(Long vendorId) {
        return b -> b.setVendorId(vendorId);
    }

    public static BuildCustomizer<BundledOrderItemRequest, OrderItemBuilder> dropship() {
        return b -> b.setDropship(true);
    }

    public static BuildCustomizer<BundledOrderItemRequest, OrderItemBuilder> dropship(boolean isDropship) {
        return b -> b.setDropship(isDropship);
    }

    public static BuildCustomizer<BundledOrderItemRequest, OrderItemBuilder> quantity(int quantity) {
        return b -> b.setQuantity(quantity);
    }

    public static BuildCustomizer<BundledOrderItemRequest, OrderItemBuilder> quantity(BigDecimal quantity) {
        return b -> b.setQuantity(quantity);
    }

    public static BuildCustomizer<BundledOrderItemRequest, OrderItemBuilder> cashback(String promoKey, int version) {
        return b -> b.setCashback(promoKey, version);
    }

    public static BuildCustomizer<BundledOrderItemRequest, OrderItemBuilder> specs(Specs specs) {
        return b -> b.setSpecs(specs);
    }

    public static Specs ofSpecs(String... typesValues) {
        if (typesValues.length % 2 != 0) {
            throw new AssertionError("Odd length: " + typesValues.length);
        }
        ImmutableSet.Builder<InternalSpec> internalSpecsBuilder = ImmutableSet.builder();
        for (int i = 0; i < typesValues.length; i += 2) {
            internalSpecsBuilder.add(new InternalSpec(typesValues[i], typesValues[i + 1]));
        }
        return new Specs(internalSpecsBuilder.build());
    }

    private static OrderItemBuilder defaultItemRequest() {
        return new OrderItemBuilder().setItemKey(DEFAULT_ITEM_KEY)
                .setQuantity(DEFAULT_QUANTITY)
                .setPrice(DEFAULT_PRICE)
                .setMsku(DEFAULT_MSKU)
                .setSsku(DEFAULT_SSKU)
                .setVendorId(DEFAULT_VENDOR_ID)
                .setCategoryId(DEFAULT_CATEGORY_ID)
                .setWarehouseId(DEFAULT_WAREHOUSE_ID)
                .setDropship(false);
    }

    public static BuildCustomizer<BundledOrderItemRequest, OrderItemBuilder> price(Number price) {
        return b -> b.setPrice(BigDecimal.valueOf(price.longValue()));
    }

    public static BuildCustomizer<BundledOrderItemRequest, OrderItemBuilder> price(BigDecimal price) {
        return b -> b.setPrice(price);
    }

    public static BuildCustomizer<BundledOrderItemRequest, OrderItemBuilder> discount(
            @Nonnull String promo,
            @Nonnull Number discount
    ) {
        return b -> b.addPromoDiscounts(ExternalItemDiscount.builder()
                .withPromoKey(promo)
                .withDiscount(BigDecimal.valueOf(discount.floatValue()))
                .build());
    }

    public static BuildCustomizer<BundledOrderItemRequest, OrderItemBuilder> discount(
            @Nonnull String promo,
            @Nonnull String promoType,
            @Nonnull Number discount
    ) {
        return b -> b.addPromoDiscounts(ExternalItemDiscount.builder()
                .withPromoKey(promo)
                .withPromoType(promoType)
                .withDiscount(BigDecimal.valueOf(discount.floatValue()))
                .build());
    }

    public static BuildCustomizer<BundledOrderItemRequest, OrderItemBuilder> price(long price) {
        return b -> b.setPrice(price);
    }

    public static BuildCustomizer<BundledOrderItemRequest, OrderItemBuilder> msku(String msku) {
        return b -> b.setMsku(msku);
    }

    public static BuildCustomizer<BundledOrderItemRequest, OrderItemBuilder> ssku(String ssku) {
        return b -> b.setSsku(ssku);
    }

    public static BuildCustomizer<BundledOrderItemRequest, OrderItemBuilder> warehouse(Number warehouseId) {
        return b -> b.setWarehouseId(warehouseId == null ? null : warehouseId.intValue());
    }

    public static BuildCustomizer<BundledOrderItemRequest, OrderItemBuilder> atSupplierWarehouse(
            Boolean atSupplierWarehouse
    ) {
        return b -> b.atSupplierWarehouse(atSupplierWarehouse);
    }

    public static BuildCustomizer<BundledOrderItemRequest, OrderItemBuilder> loyaltyProgramPartner(
            Boolean loyaltyProgramPartner
    ) {
        return b -> b.loyaltyProgramPartner(loyaltyProgramPartner);
    }

    public static BuildCustomizer<BundledOrderItemRequest, OrderItemBuilder> supplier(Number supplierId) {
        return b -> b.setSupplierId(supplierId);
    }

    public static BuildCustomizer<BundledOrderItemRequest, OrderItemBuilder> categoryId(Integer categoryId) {
        return b -> b.setCategoryId(categoryId);
    }

    public static BuildCustomizer<BundledOrderItemRequest, OrderItemBuilder> excludedAlcoholCategoryId() {
        return b -> b.setCategoryId(ALCOHOL_CATEGORY);
    }

    public static BuildCustomizer<BundledOrderItemRequest, OrderItemBuilder> excludedStickCategoryId() {
        return b -> b.setCategoryId(STICK_CATEGORY);
    }

    public static BuildCustomizer<BundledOrderItemRequest, OrderItemBuilder> bundle(String bundleId) {
        return b -> b.setBundleId(bundleId);
    }

    public static BuildCustomizer<BundledOrderItemRequest, OrderItemBuilder> promoKeys(String... keys) {
        return b -> b.addPromoKeys(keys);
    }

    public static BuildCustomizer<BundledOrderItemRequest, OrderItemBuilder> spreadDiscountCountBound(
            int count,
            BigDecimal discount,
            boolean isPercent
    ) {
        return b -> b.addSpreadDiscountCountBound(
                new CountBound(count, isPercent ? null : discount, isPercent ? discount : null, null)
        );
    }

    public static BuildCustomizer<BundledOrderItemRequest, OrderItemBuilder> spreadDiscountCountPromoKey(
            String promoKey
    ) {
        return b -> b.setSpreadDiscountCountPromoKey(promoKey);
    }

    public static BuildCustomizer<BundledOrderItemRequest, OrderItemBuilder> spreadDiscountReceiptBound(
            BigDecimal receiptBound,
            BigDecimal discount,
            boolean isPercent
    ) {
        return b -> b.addSpreadDiscountReceiptBound(
                new ReceiptBound(receiptBound, isPercent ? null : discount, isPercent ? discount : null, null)
        );
    }

    public static BuildCustomizer<BundledOrderItemRequest, OrderItemBuilder> spreadDiscountReceiptPromoKey(
            String promoKey
    ) {
        return b -> b.setSpreadDiscountReceiptPromoKey(promoKey);
    }

    public static BuildCustomizer<BundledOrderItemRequest, OrderItemBuilder> payByYaPlus(Integer payByYaPlus) {
        return b -> b.setPayByYaPlus(payByYaPlus);
    }

    public static BuildCustomizer<BundledOrderItemRequest, OrderItemBuilder> cpa(String cpa) {
        return b -> b.setCpa(cpa);
    }

    public static BuildCustomizer<BundledOrderItemRequest, OrderItemBuilder> platform(MarketPlatform platform) {
        return b -> b.setPlatform(platform);
    }

    public static BuildCustomizer<BundledOrderItemRequest, OrderItemBuilder> allowedPaymentTypes(
            Set<PaymentType> allowedPaymentTypes
    ) {
        return b -> b.setAllowedPaymentTypes(allowedPaymentTypes);
    }

    public static class OrderRequestBuilder implements Builder<OrderWithDeliveriesRequest> {
        private final List<OrderItemRequest> items = new ArrayList<>();
        private String orderId = DEFAULT_ORDER_ID;
        private List<DeliveryRequest> deliveryRequests = Collections.singletonList(
                DeliveryRequestUtils.courierDelivery(SELECTED));
        private String cartId = UUID.randomUUID().toString();
        private UsageClientDeviceType clientDeviceType;
        private PaymentType paymentType;
        private BigDecimal weight;
        private Long volume;

        private OrderRequestBuilder() {
        }

        public OrderRequestBuilder withDeliveries(DeliveryRequest... deliveryRequests) {
            this.deliveryRequests = Arrays.asList(deliveryRequests);
            return this;
        }

        public OrderRequestBuilder withCartId(String cartId) {
            this.cartId = cartId;
            return this;
        }

        public OrderRequestBuilder withOrderId(String orderId) {
            this.orderId = orderId;
            return this;
        }

        public OrderRequestBuilder withOrderId(Long orderId) {
            this.orderId = Long.toString(orderId);
            return this;
        }

        @SafeVarargs
        public final OrderRequestBuilder withOrderItem(
                BuildCustomizer<BundledOrderItemRequest, OrderItemBuilder>... customizers
        ) {
            return withOrderItem(customize(OrderRequestUtils::orderItemBuilder, customizers).build());
        }

        public OrderRequestBuilder withOrderItem(OrderItemRequest orderItemRequest) {
            items.add(orderItemRequest);
            return this;
        }

        public OrderRequestBuilder withClientDeviceType(UsageClientDeviceType clientDeviceType) {
            this.clientDeviceType = clientDeviceType;
            return this;
        }

        public OrderRequestBuilder withPaymentType(PaymentType paymentType) {
            this.paymentType = paymentType;
            return this;
        }

        public OrderRequestBuilder withWeight(BigDecimal weight) {
            this.weight = weight;
            return this;
        }

        public OrderRequestBuilder withVolume(Long volume) {
            this.volume = volume;
            return this;
        }


        public OrderWithDeliveriesRequest build() {
            return new OrderWithDeliveriesRequest(
                    orderId, cartId, paymentType, weight, volume, items, deliveryRequests);
        }

        public OrderRequestWithoutOrderId buildWithoutOrderId() {
            return new OrderRequestWithoutOrderId(clientDeviceType, items);
        }
    }

    public static class OrderItemBuilder implements Builder<BundledOrderItemRequest> {
        protected ItemKey itemKey;
        protected Integer categoryId;
        protected BigDecimal oldMinPrice;
        protected String msku;
        protected String ssku;
        protected Long supplierId;
        protected Long vendorId;
        protected BigDecimal price;
        protected BigDecimal quantity;
        protected boolean isDownloadable;
        protected Boolean largeDimensionItem;
        protected Boolean isDropship;
        protected String bundleId;
        protected Integer warehouseId;
        protected Boolean atSupplierWarehouse;
        protected Boolean loyaltyProgramPartner;
        protected Set<String> promoKeys = new HashSet<>();
        protected Set<ExternalItemDiscount> promoDiscounts = new HashSet<>();
        protected CashbackRequest cashback;
        protected List<CashbackPromoRequest> cashbackPromoRequests;
        private MarketPlatform platform;
        protected Specs specs;
        private Set<CountBound> countBounds = new HashSet<>();
        private Set<ReceiptBound> receiptBounds = new HashSet<>();
        private String spreadDiscountCountPromoKey;
        private String spreadDiscountReceiptPromoKey;
        private Integer payByYaPlus;
        private String cpa;
        private Set<PaymentType> allowedPaymentTypes;
        private Boolean isExpress;

        public OrderItemBuilder setItemKey(ItemKey itemKey) {
            this.itemKey = itemKey;
            return this;
        }

        public OrderItemBuilder setCategoryId(Integer categoryId) {
            this.categoryId = categoryId;
            return this;
        }

        public OrderItemBuilder setOldMinPrice(BigDecimal oldMinPrice) {
            this.oldMinPrice = oldMinPrice;
            return this;
        }

        public OrderItemBuilder setMsku(String msku) {
            this.msku = msku;
            return this;
        }

        public OrderItemBuilder setSsku(String ssku) {
            this.ssku = ssku;
            return this;
        }

        public OrderItemBuilder setSpecs(Specs specs) {
            this.specs = specs;
            return this;
        }

        public OrderItemBuilder setCpa(String cpa) {
            this.cpa = cpa;
            return this;
        }

        public OrderItemBuilder setSupplierId(Number supplierId) {
            this.supplierId = supplierId.longValue();
            return this;
        }

        public OrderItemBuilder setVendorId(Long vendorId) {
            this.vendorId = vendorId;
            return this;
        }

        public OrderItemBuilder setPrice(long price) {
            return setPrice(BigDecimal.valueOf(price));
        }

        public OrderItemBuilder setPrice(BigDecimal price) {
            this.price = price;
            return this;
        }

        public OrderItemBuilder setQuantity(int quantity) {
            return setQuantity(BigDecimal.valueOf(quantity));
        }

        public OrderItemBuilder setDownloadable(boolean isDownloadable) {
            this.isDownloadable = isDownloadable;
            return this;
        }

        public OrderItemBuilder setQuantity(BigDecimal quantity) {
            this.quantity = quantity;
            return this;
        }

        public OrderItemBuilder setLargeDiminsionItem(boolean largeDimensionItem) {
            this.largeDimensionItem = largeDimensionItem;
            return this;
        }

        public OrderItemBuilder setDropship(Boolean dropship) {
            this.isDropship = dropship;
            return this;
        }

        public OrderItemBuilder setBundleId(String bundleId) {
            this.bundleId = bundleId;
            return this;
        }

        public OrderItemBuilder setWarehouseId(Integer warehouseId) {
            this.warehouseId = warehouseId;
            return this;
        }

        public OrderItemBuilder atSupplierWarehouse(Boolean atSupplierWarehouse) {
            this.atSupplierWarehouse = atSupplierWarehouse;
            return this;
        }

        public OrderItemBuilder setPlatform(MarketPlatform platform) {
            this.platform = platform;
            return this;
        }

        public OrderItemBuilder loyaltyProgramPartner(Boolean loyaltyProgramPartner) {
            this.loyaltyProgramPartner = loyaltyProgramPartner;
            return this;
        }

        public void addPromoKeys(String... promoKeys) {
            this.promoKeys.addAll(Arrays.asList(promoKeys));
        }

        public void addPromoDiscounts(ExternalItemDiscount... discounts) {
            this.promoDiscounts.addAll(Arrays.asList(discounts));
        }

        public OrderItemBuilder setCashback(String promoKey, int version) {
            this.cashback = new CashbackRequest(promoKey, version);
            return this;
        }

        public OrderItemBuilder setCashbackPromos(List<CashbackPromoRequest> cashbackPromoRequests) {
            this.cashbackPromoRequests = cashbackPromoRequests;
            return this;
        }

        public OrderItemBuilder setSpreadDiscountCountPromoKey(String promoKey) {
            this.spreadDiscountCountPromoKey = promoKey;
            return this;
        }

        public OrderItemBuilder addSpreadDiscountCountBound(CountBound countBound) {
            this.countBounds.add(countBound);
            return this;
        }

        public OrderItemBuilder setSpreadDiscountReceiptPromoKey(String promoKey) {
            this.spreadDiscountReceiptPromoKey = promoKey;
            return this;
        }


        public OrderItemBuilder setPayByYaPlus(Integer payByYaPlus) {
            this.payByYaPlus = payByYaPlus;
            return this;
        }

        public OrderItemBuilder addSpreadDiscountReceiptBound(ReceiptBound receiptBound) {
            this.receiptBounds.add(receiptBound);
            return this;
        }

        public OrderItemBuilder setAllowedPaymentTypes(Set<PaymentType> allowedPaymentTypes) {
            this.allowedPaymentTypes = allowedPaymentTypes;
            return this;
        }

        public BundledOrderItemRequest build() {
            return new BundledOrderItemRequest(
                    itemKey.getOfferId(),
                    itemKey.getFeedId(),
                    price,
                    quantity,
                    isDownloadable,
                    categoryId,
                    oldMinPrice,
                    msku,
                    vendorId,
                    largeDimensionItem != null && largeDimensionItem ? ImmutableSet.of(300) : Collections.emptySet(),
                    Boolean.TRUE.equals(isDropship)
                            ? ImmutableSet.of(DeliveryPartnerType.SHOP.getCode())
                            : ImmutableSet.of(DeliveryPartnerType.YANDEX_MARKET.getCode()),
                    bundleId,
                    warehouseId,
                    atSupplierWarehouse,
                    supplierId,
                    ssku,
                    promoKeys,
                    promoDiscounts,
                    platform,
                    cashback,
                    cashbackPromoRequests,
                    loyaltyProgramPartner,
                    specs,
                    CollectionUtils.isNotEmpty(countBounds) || CollectionUtils.isNotEmpty(receiptBounds) ?
                            new SpreadDiscountSpecification(
                                    new SpreadDiscountCountSpecification(
                                            countBounds, spreadDiscountCountPromoKey
                                    ),
                                    new SpreadDiscountReceiptSpecification(
                                            receiptBounds, spreadDiscountReceiptPromoKey
                                    )
                            ) : null,
                    payByYaPlus,
                    cpa,
                    allowedPaymentTypes
            );
        }
    }
}
