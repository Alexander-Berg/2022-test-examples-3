package ru.yandex.market.loyalty.core.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import ru.yandex.market.loyalty.api.model.IdObject;
import ru.yandex.market.loyalty.api.model.MarketPlatform;
import ru.yandex.market.loyalty.api.model.OperationContextDto;
import ru.yandex.market.loyalty.api.model.coin.creation.DeviceInfoRequest;
import ru.yandex.market.loyalty.api.model.discount.MultiCartDiscountRequest;
import ru.yandex.market.loyalty.api.model.discount.OrderWithDeliveriesRequest;
import ru.yandex.market.loyalty.api.model.red.RedOrder;
import ru.yandex.market.loyalty.core.model.coin.CoinKey;

import static ru.yandex.market.loyalty.core.utils.OperationContextFactory.uidOperationContextDto;

public class DiscountRequestBuilder {
    private final List<OrderWithDeliveriesRequest> orderRequests;
    private OperationContextDto operationContext = uidOperationContextDto();
    private MarketPlatform platform = MarketPlatform.BLUE;
    private RedOrder redOrder = null;
    private final Set<IdObject> coins = new HashSet<>();
    private String certificateToken;
    private String coupon;
    private DeviceInfoRequest deviceInfoRequest;
    private Boolean isOptionalRulesEnabled;

    private DiscountRequestBuilder(OrderWithDeliveriesRequest request, OrderWithDeliveriesRequest... another) {
        this.orderRequests = new ArrayList<>(Arrays.asList(another));
        this.orderRequests.add(0, request);
    }

    private DiscountRequestBuilder(Collection<OrderWithDeliveriesRequest> requestOrders) {
        orderRequests = new ArrayList<>(requestOrders);
    }

    public static DiscountRequestBuilder builder(OrderWithDeliveriesRequest orderRequest,
                                                 OrderWithDeliveriesRequest... another) {
        return new DiscountRequestBuilder(orderRequest, another);
    }

    public static DiscountRequestBuilder builder(List<OrderWithDeliveriesRequest> orders) {
        return new DiscountRequestBuilder(orders);
    }

    public static DiscountRequestBuilder builder(MultiCartDiscountRequest request) {
        return builder(request.getOrders())
                .withOperationContext(request.getOperationContext())
                .withPlatform(request.getPlatform())
                .withRedOrder(request.getRedOrder())
                .withCoins((CoinKey) request.getCoins().stream().map(CoinKey::new).collect(Collectors.toList()))
                .withCertificate(request.getCertificateToken())
                .withCoupon(request.getCoupon())
                .withDeviceInfoRequest(request.getDeviceInfoRequest())
                .withOptionalRulesEnabled(request.getIsOptionalRulesEnabled());
    }

    public DiscountRequestBuilder withOperationContext(OperationContextDto operationContext) {
        this.operationContext = operationContext;
        return this;
    }

    public DiscountRequestBuilder withPlatform(MarketPlatform platform) {
        this.platform = platform;
        return this;
    }

    public DiscountRequestBuilder withRedOrder(RedOrder redOrder) {
        this.redOrder = redOrder;
        return this;
    }

    public DiscountRequestBuilder withCoins(CoinKey... coinKeys) {
        for (CoinKey coinKey : coinKeys) {
            this.coins.add(new IdObject(coinKey.getId()));
        }
        return this;
    }

    public DiscountRequestBuilder withCertificate(String certificateToken) {
        this.certificateToken = certificateToken;
        return this;
    }

    public DiscountRequestBuilder withCoupon(String coupon) {
        this.coupon = coupon;
        return this;
    }

    public DiscountRequestBuilder withCoins(Collection<CoinKey> coinKeys) {
        coins.addAll(coinKeys.stream().map(item -> new IdObject(item.getId())).collect(Collectors.toList()));
        return this;
    }

    public DiscountRequestBuilder withDeviceInfoRequest(DeviceInfoRequest deviceInfoRequest) {
        this.deviceInfoRequest = deviceInfoRequest;
        return this;
    }

    public DiscountRequestBuilder withOptionalRulesEnabled(boolean isOptionalRulesEnabled) {
        this.isOptionalRulesEnabled = isOptionalRulesEnabled;
        return this;
    }

    public MultiCartDiscountRequest build() {
        return new MultiCartDiscountRequest(
                orderRequests,
                operationContext,
                platform,
                redOrder,
                coins,
                certificateToken,
                coupon,
                false,
                deviceInfoRequest,
                isOptionalRulesEnabled
        );
    }
}
