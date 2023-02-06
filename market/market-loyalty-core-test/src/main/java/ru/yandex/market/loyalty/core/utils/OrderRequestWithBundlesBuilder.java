package ru.yandex.market.loyalty.core.utils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import ru.yandex.market.loyalty.api.model.MarketPlatform;
import ru.yandex.market.loyalty.api.model.PaymentSystem;
import ru.yandex.market.loyalty.api.model.PaymentType;
import ru.yandex.market.loyalty.api.model.bundle.BundledOrderItemRequest;
import ru.yandex.market.loyalty.api.model.bundle.OrderExtraChargeDeliveryParams;
import ru.yandex.market.loyalty.api.model.bundle.OrderWithBundleAdditionalFlags;
import ru.yandex.market.loyalty.api.model.bundle.OrderWithBundlesRequest;
import ru.yandex.market.loyalty.api.model.delivery.DeliveryRequest;
import ru.yandex.market.loyalty.core.utils.OrderRequestUtils.OrderItemBuilder;

import static ru.yandex.market.loyalty.core.utils.BuildCustomizer.Util.customize;
import static ru.yandex.market.loyalty.core.utils.DeliveryRequestUtils.SELECTED;

public class OrderRequestWithBundlesBuilder implements Builder<OrderWithBundlesRequest> {
    public static final String DEFAULT_ORDER_ID = "1";

    private final List<BundledOrderItemRequest> items = new ArrayList<>();
    private String orderId = DEFAULT_ORDER_ID;
    private List<DeliveryRequest> deliveries = Collections.singletonList(
            DeliveryRequestUtils.courierDelivery(SELECTED));
    private String cartId = UUID.randomUUID().toString();
    private PaymentType paymentType;
    private MarketPlatform platform;
    private BigDecimal weight;
    private Long volume;
    private Boolean isLargeSize;
    private PaymentSystem paymentSystem;
    private Map<OrderWithBundleAdditionalFlags, String> additionalFlags = new HashMap<>();
    private OrderExtraChargeDeliveryParams extraChargeDeliveryParams;

    public OrderRequestWithBundlesBuilder() {
    }

    public OrderRequestWithBundlesBuilder withCartId(String cartId) {
        this.cartId = cartId;
        return this;
    }

    public OrderRequestWithBundlesBuilder withOrderId(String orderId) {
        this.orderId = orderId;
        return this;
    }

    @SafeVarargs
    public final OrderRequestWithBundlesBuilder withOrderItem(
            BuildCustomizer<BundledOrderItemRequest, OrderItemBuilder>... customizers
    ) {
        OrderItemBuilder customized = customize(OrderRequestUtils::orderItemBuilder, customizers);
        return withOrderItem(customized.build());
    }

    public final OrderRequestWithBundlesBuilder withOrderItem(
            Collection<BuildCustomizer<BundledOrderItemRequest, OrderItemBuilder>> customizers
    ) {
        OrderItemBuilder customized = customize(OrderRequestUtils::orderItemBuilder, customizers);
        return withOrderItem(customized.build());
    }

    public OrderRequestWithBundlesBuilder withOrderItem(BundledOrderItemRequest orderItemRequest) {
        items.add(orderItemRequest);
        return this;
    }

    public OrderRequestWithBundlesBuilder withPaymentType(PaymentType paymentType) {
        this.paymentType = paymentType;
        return this;
    }

    public OrderRequestWithBundlesBuilder withPaymentSystem(PaymentSystem paymentSystem) {
        this.paymentSystem = paymentSystem;
        return this;
    }

    public OrderRequestWithBundlesBuilder withPlatform(MarketPlatform platform) {
        this.platform = platform;
        return this;
    }

    public OrderRequestWithBundlesBuilder withDeliveries(DeliveryRequest... deliveryRequests) {
        this.deliveries = Arrays.asList(deliveryRequests);
        return this;
    }

    public OrderRequestWithBundlesBuilder withWeight(BigDecimal weight) {
        this.weight = weight;
        return this;
    }

    public OrderRequestWithBundlesBuilder withVolume(Long volume) {
        this.volume = volume;
        return this;
    }

    public OrderRequestWithBundlesBuilder withLargeSize(Boolean isLargeSize) {
        this.isLargeSize = isLargeSize;
        return this;
    }

    public OrderRequestWithBundlesBuilder withAdditionalFlags(
            Map<OrderWithBundleAdditionalFlags, String> additionalFlags
    ) {
        this.additionalFlags = new HashMap<>(additionalFlags);
        return this;
    }

    public OrderRequestWithBundlesBuilder withExtraChargeDeliveryParams(OrderExtraChargeDeliveryParams extraChargeDeliveryParams) {
        this.extraChargeDeliveryParams = extraChargeDeliveryParams;
        return this;
    }

    public static OrderRequestWithBundlesBuilder builder() {
        return new OrderRequestWithBundlesBuilder();
    }

    public OrderWithBundlesRequest build() {
        return OrderWithBundlesRequest.Builder.builder()
                .setOrderId(orderId)
                .setCartId(cartId)
                .setPaymentType(paymentType)
                .setWeight(weight)
                .setVolume(volume)
                .setItems(items)
                .setLargeSize(isLargeSize)
                .setPlatform(platform)
                .setDeliveries(deliveries)
                .setPaymentSystemType(paymentSystem)
                .setAdditionalFlags(additionalFlags)
                .setExtraChargeDeliveryParams(extraChargeDeliveryParams)
                .build();
    }
}
