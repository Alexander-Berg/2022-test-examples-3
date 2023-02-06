package ru.yandex.market.checkout.util.loyalty.response;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.loyalty.api.model.CashbackResponse;
import ru.yandex.market.loyalty.api.model.bundle.BundledOrderItemResponse;
import ru.yandex.market.loyalty.api.model.bundle.OrderWithBundlesResponse;
import ru.yandex.market.loyalty.api.model.delivery.DeliveryResponse;
import ru.yandex.market.loyalty.api.model.discount.ExternalItemDiscountFault.ExternalItemDiscountFaultBuilder;
import ru.yandex.market.loyalty.api.model.discount.OrderWithDeliveriesResponse;

public class OrderResponseBuilder {

    private String orderId;
    private String cartId;
    private final List<DeliveryResponse> deliveries = new ArrayList<>();
    private final List<OrderItemResponseBuilder> items = new ArrayList<>();
    private final List<OrderBundleBuilder> bundles = new ArrayList<>();
    private final List<OrderBundleBuilder> bundlesToDestroy = new ArrayList<>();
    private final List<ExternalItemDiscountFaultBuilder> externalItemDiscountFaults = new ArrayList<>();
    private CashbackResponse cashbackResponse;

    private OrderResponseBuilder() {
    }

    public static OrderResponseBuilder create() {
        return new OrderResponseBuilder();
    }

    public static OrderResponseBuilder createFrom(Order order) {
        OrderResponseBuilder builder = create()
                .withCartId(order.getLabel())
                .withOrderId(String.valueOf(order.getId()));
        order.getItems().stream()
                .map(OrderItemResponseBuilder::createFrom)
                .forEach(builder::withItem);
        return builder;
    }

    public OrderResponseBuilder withOrderId(String orderId) {
        this.orderId = orderId;
        return this;
    }

    public OrderResponseBuilder withCartId(String cartId) {
        this.cartId = cartId;
        return this;
    }

    public OrderResponseBuilder withDelivery(DeliveryResponse delivery) {
        this.deliveries.add(delivery);
        return this;
    }

    public OrderResponseBuilder withItem(OrderItemResponseBuilder item) {
        this.items.add(item);
        return this;
    }

    public OrderResponseBuilder withBundle(OrderBundleBuilder bundleBuilder) {
        bundles.add(bundleBuilder);
        return this;
    }

    public OrderResponseBuilder withDestroyedBundle(OrderBundleBuilder bundleBuilder) {
        bundlesToDestroy.add(bundleBuilder);
        return this;
    }

    public OrderResponseBuilder withExternalItemDiscountFaults(
            ExternalItemDiscountFaultBuilder externalItemDiscountFault
    ) {
        externalItemDiscountFaults.add(externalItemDiscountFault);
        return this;
    }

    public OrderResponseBuilder withCashbackResponse(CashbackResponse cashbackResponse) {
        this.cashbackResponse = cashbackResponse;
        return this;
    }

    public OrderWithDeliveriesResponse build() {
        return new OrderWithDeliveriesResponse(
                items.stream()
                        .map(OrderItemResponseBuilder::build)
                        .collect(Collectors.toUnmodifiableList()),
                cartId,
                orderId,
                deliveries
        );
    }

    public OrderWithBundlesResponse buildResponseWithBundles() {
        return new OrderWithBundlesResponse(
                cartId,
                orderId,
                items.stream()
                        .map(OrderItemResponseBuilder::build)
                        .map(BundledOrderItemResponse.class::cast)
                        .collect(Collectors.toUnmodifiableList()),
                deliveries,
                bundles.stream()
                        .map(OrderBundleBuilder::build)
                        .map(OrderBundleResponse::getBundle)
                        .collect(Collectors.toUnmodifiableList()),
                bundlesToDestroy.stream()
                        .map(OrderBundleBuilder::buildDestroyed)
                        .collect(Collectors.toUnmodifiableList()),
                externalItemDiscountFaults.stream()
                        .map(ExternalItemDiscountFaultBuilder::build)
                        .collect(Collectors.toUnmodifiableList()),
                cashbackResponse
        );
    }
}
