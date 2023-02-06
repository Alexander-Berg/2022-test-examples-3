package ru.yandex.market.checkout.util.loyalty.response;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.lang3.BooleanUtils;

import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.test.providers.OrderItemProvider.OrderItemBuilder;
import ru.yandex.market.common.report.model.FeedOfferId;
import ru.yandex.market.loyalty.api.model.ItemPromoResponse;
import ru.yandex.market.loyalty.api.model.PromoType;
import ru.yandex.market.loyalty.api.model.bundle.BundleDestroyReason;
import ru.yandex.market.loyalty.api.model.bundle.BundleDestroyReason.ReasonType;
import ru.yandex.market.loyalty.api.model.bundle.OrderBundle;
import ru.yandex.market.loyalty.api.model.bundle.OrderBundleDestroyed;
import ru.yandex.market.loyalty.api.model.bundle.OrderBundleItem;

import static ru.yandex.market.checkout.test.providers.OrderItemProvider.FEED_ID;

public final class OrderBundleBuilder {

    private String bundleId;
    private String promoKey;
    private Long clientId;
    private String distributorPromoId;
    private String anaplanId;
    private Long quantity = 1L;
    private boolean restrictReturn;
    private final Set<OrderBundleItem> items = new LinkedHashSet<>();
    private final Map<FeedOfferId, Number> itemsDiscount = new HashMap<>();
    private BundleDestroyReason destroyReason;
    private PromoType promoType = PromoType.GENERIC_BUNDLE;

    private OrderBundleBuilder() {
    }

    public static OrderBundleBuilder create() {
        return new OrderBundleBuilder();
    }

    public OrderBundleBuilder promoType(@Nonnull PromoType promoType) {
        this.promoType = promoType;
        return this;
    }

    public OrderBundleBuilder bundleId(String bundleId) {
        this.bundleId = bundleId;
        return this;
    }

    public OrderBundleBuilder clientId(Long clientId) {
        this.clientId = clientId;
        return this;
    }

    public OrderBundleBuilder promo(String promoKey, String distributorPromoId) {
        this.promoKey = promoKey;
        this.distributorPromoId = distributorPromoId;
        return this;
    }

    public OrderBundleBuilder promo(String distributorPromoId) {
        return promo(distributorPromoId, distributorPromoId);
    }

    public OrderBundleBuilder anaplanId(String anaplanId) {
        this.anaplanId = anaplanId;
        return this;
    }

    public OrderBundleBuilder quantity(Number quantity) {
        this.quantity = quantity.longValue();
        return this;
    }

    public OrderBundleBuilder restrictReturn() {
        this.restrictReturn = true;
        return this;
    }

    public OrderBundleBuilder item(Long feedId, String offerId, Number countInBundles) {
        return item(feedId, offerId, null, countInBundles, null);
    }

    public OrderBundleBuilder item(Long feedId, String offerId, Boolean primaryInBundle, Number countInBundles,
                                   Number discount) {
        items.add(new OrderBundleItem(offerId, feedId, countInBundles.intValue(),
                BooleanUtils.isTrue(primaryInBundle)));
        if (discount != null) {
            itemsDiscount.put(new FeedOfferId(offerId, feedId), discount);
        }
        return this;
    }

    public OrderBundleBuilder item(OrderItem item, Number countInBundles, Number discount) {
        return item(item.getFeedId(), item.getOfferId(), item.getPrimaryInBundle(), countInBundles, discount);
    }

    public OrderBundleBuilder item(String offerId) {
        return item(FEED_ID, offerId, 1);
    }

    public OrderBundleBuilder item(String offerId, Number countInBundles) {
        return item(FEED_ID, offerId, countInBundles);
    }

    public OrderBundleBuilder item(String offerId, Number countInBundles, Number discount) {
        return item(FEED_ID, offerId, null, countInBundles, discount);
    }

    public OrderBundleBuilder item(String offerId, Boolean primaryInBundles, Number countInBundles, Number discount) {
        return item(FEED_ID, offerId, primaryInBundles, countInBundles, discount);
    }

    public OrderBundleBuilder item(OrderItemBuilder itemBuilder) {
        return item(itemBuilder, 1, null);
    }

    public OrderBundleBuilder item(OrderItemBuilder itemBuilder, Number countInBundles) {
        return item(itemBuilder, countInBundles, null);
    }

    public OrderBundleBuilder item(OrderItemBuilder itemBuilder, Number countInBundles, Number discount) {
        OrderItem item = itemBuilder.build();
        return item(item, countInBundles, discount);
    }

    public OrderBundleBuilder item(OrderItem item) {
        return item(item, 1, null);
    }

    public OrderBundleBuilder item(OrderItem item, Number countInBundles) {
        return item(item, countInBundles, null);
    }

    public OrderBundleBuilder destroyReason(ReasonType reason) {
        destroyReason = new BundleDestroyReason(reason, null);
        return this;
    }

    @Nonnull
    private ItemPromoResponse discountResponse(
            @Nonnull String promo,
            @Nonnull String shopPromoId,
            @Nullable Long clientId,
            @Nullable String anaplanId,
            @Nonnull Number discount
    ) {
        return new ItemPromoResponse(
                BigDecimal.valueOf(discount.longValue()),
                promoType,
                null,
                promo,
                shopPromoId,
                clientId,
                anaplanId,
                null,
                null,
                null,
                null,
                null
        );
    }

    public OrderBundleResponse build() {
        return new OrderBundleResponse(
                new OrderBundle(bundleId,
                        promoKey,
                        distributorPromoId,
                        quantity,
                        items,
                        restrictReturn),
                itemsDiscount.entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, entry -> discountResponse(
                                promoKey,
                                distributorPromoId,
                                clientId,
                                anaplanId,
                                entry.getValue()
                        ))),
                promoType,
                anaplanId
        );
    }

    public OrderBundleDestroyed buildDestroyed() {
        return new OrderBundleDestroyed(
                bundleId,
                promoKey,
                distributorPromoId,
                quantity,
                items,
                destroyReason
        );
    }
}
