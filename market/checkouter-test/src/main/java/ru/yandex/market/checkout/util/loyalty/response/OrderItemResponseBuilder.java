package ru.yandex.market.checkout.util.loyalty.response;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import ru.yandex.market.checkout.checkouter.order.OfferItemKey;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.test.providers.OrderItemProvider;
import ru.yandex.market.common.report.model.FeedOfferId;
import ru.yandex.market.loyalty.api.model.CashbackResponse;
import ru.yandex.market.loyalty.api.model.ItemPromoResponse;
import ru.yandex.market.loyalty.api.model.bundle.BundledOrderItemResponse;

public class OrderItemResponseBuilder {

    private String offerId;
    private Long feedId;
    private String bundleId;
    private Boolean primaryInBundle;
    private BigDecimal price;
    private BigDecimal quantity;
    private boolean downloadable;
    private List<ItemPromoResponse> promos = new ArrayList<>();
    private CashbackResponse cashbackResponse;

    private OrderItemResponseBuilder() {
    }

    public static OrderItemResponseBuilder create() {
        return new OrderItemResponseBuilder();
    }

    public static OrderItemResponseBuilder createFrom(OrderItem item) {
        return create()
                .offer(item.getFeedId(), item.getOfferId(), item.getBundleId())
                .price(item.getPrice())
                .quantity(BigDecimal.valueOf(item.getCount()))
                .downloadable(item.isDigital());
    }

    public OrderItemResponseBuilder bundleId(String bundleId) {
        this.bundleId = bundleId;
        return this;
    }

    public OrderItemResponseBuilder primaryInBundle(Boolean primaryInBundle) {
        this.primaryInBundle = primaryInBundle;
        return this;
    }

    public OrderItemResponseBuilder offer(Long feedId, String offerId, String bundleId) {
        this.feedId = feedId;
        this.offerId = offerId;
        this.bundleId = bundleId;
        return this;
    }

    public OrderItemResponseBuilder offer(Long feedId, String offerId) {
        return offer(feedId, offerId, null);
    }

    public OrderItemResponseBuilder offer(String offerId, String bundleId) {
        return offer(OrderItemProvider.FEED_ID, offerId, bundleId);
    }

    public OrderItemResponseBuilder price(BigDecimal price) {
        this.price = price;
        return this;
    }

    public OrderItemResponseBuilder quantity(Number quantity) {
        this.quantity = BigDecimal.valueOf(quantity.longValue());
        return this;
    }

    public OrderItemResponseBuilder promos(ItemPromoResponse... promos) {
        this.promos = Stream.of(promos).collect(Collectors.toList());
        return this;
    }

    public OrderItemResponseBuilder promo(ItemPromoResponse promo) {
        this.promos.add(promo);
        return this;
    }

    public OrderItemResponseBuilder cashback(CashbackResponse cashbackResponse) {
        this.cashbackResponse = cashbackResponse;
        return this;
    }

    public OrderItemResponseBuilder downloadable(Boolean downloadable) {
        this.downloadable = downloadable;
        return this;
    }

    public BundledOrderItemResponse build() {
        return new BundledOrderItemResponse(
                offerId,
                feedId,
                price,
                quantity,
                downloadable,
                promos,
                bundleId,
                primaryInBundle,
                cashbackResponse
        );
    }

    public FeedOfferId feedOfferId() {
        return new FeedOfferId(offerId, feedId);
    }

    public OfferItemKey offerItemKey() {
        return OfferItemKey.of(offerId, feedId, bundleId);
    }
}
