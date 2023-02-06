package ru.yandex.market.loyalty.core.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import ru.yandex.market.loyalty.api.model.CashbackType;
import ru.yandex.market.loyalty.api.model.IdObject;
import ru.yandex.market.loyalty.api.model.MarketPlatform;
import ru.yandex.market.loyalty.api.model.OperationContextDto;
import ru.yandex.market.loyalty.api.model.bundle.OrderWithBundlesRequest;
import ru.yandex.market.loyalty.api.model.coin.creation.DeviceInfoRequest;
import ru.yandex.market.loyalty.api.model.discount.MultiCartWithBundlesDiscountRequest;
import ru.yandex.market.loyalty.api.model.discount.PaymentInfo;
import ru.yandex.market.loyalty.api.model.red.RedOrder;
import ru.yandex.market.loyalty.core.model.coin.CoinKey;

import static ru.yandex.market.loyalty.core.utils.OperationContextFactory.uidOperationContextDto;

public class DiscountRequestWithBundlesBuilder {
    private final List<OrderWithBundlesRequest> orderRequests;
    private OperationContextDto operationContext = uidOperationContextDto();
    private MarketPlatform platform = MarketPlatform.BLUE;
    private RedOrder redOrder = null;
    private final Set<IdObject> coins = new HashSet<>();
    private String certificateToken;
    private String coupon;
    private DeviceInfoRequest deviceInfoRequest;
    private String multiOrderId;
    private Boolean useInternalPromocode;
    private CashbackType cashbackOptionType = CashbackType.EMIT;
    private Boolean bnplSelected = false;
    private Boolean calculateOrdersSeparately = false;
    private PaymentInfo paymentInfo;
    private Boolean isOptionalRulesEnabled;

    private DiscountRequestWithBundlesBuilder(OrderWithBundlesRequest request, OrderWithBundlesRequest... another) {
        this.orderRequests = new ArrayList<>(Arrays.asList(another));
        this.orderRequests.add(0, request);
    }

    private DiscountRequestWithBundlesBuilder(Collection<OrderWithBundlesRequest> requestOrders) {
        orderRequests = new ArrayList<>(requestOrders);
    }

    public static DiscountRequestWithBundlesBuilder builder(
            OrderWithBundlesRequest orderRequest,
            OrderWithBundlesRequest... another
    ) {
        return new DiscountRequestWithBundlesBuilder(orderRequest, another);
    }

    public static DiscountRequestWithBundlesBuilder builder(List<OrderWithBundlesRequest> orders) {
        return new DiscountRequestWithBundlesBuilder(orders);
    }

    public DiscountRequestWithBundlesBuilder withOperationContext(OperationContextDto operationContext) {
        this.operationContext = operationContext;
        return this;
    }

    public DiscountRequestWithBundlesBuilder withPlatform(MarketPlatform platform) {
        this.platform = platform;
        return this;
    }

    public DiscountRequestWithBundlesBuilder withRedOrder(RedOrder redOrder) {
        this.redOrder = redOrder;
        return this;
    }

    public DiscountRequestWithBundlesBuilder withCoins(CoinKey... coinKeys) {
        for (CoinKey coinKey : coinKeys) {
            this.coins.add(new IdObject(coinKey.getId()));
        }
        return this;
    }

    public DiscountRequestWithBundlesBuilder withCertificate(String certificateToken) {
        this.certificateToken = certificateToken;
        return this;
    }

    public DiscountRequestWithBundlesBuilder withCoupon(String coupon) {
        this.coupon = coupon;
        return this;
    }

    public DiscountRequestWithBundlesBuilder withCoins(Collection<CoinKey> coinKeys) {
        coins.addAll(coinKeys.stream().map(item -> new IdObject(item.getId())).collect(Collectors.toList()));
        return this;
    }

    public DiscountRequestWithBundlesBuilder withDeviceInfoRequest(DeviceInfoRequest deviceInfoRequest) {
        this.deviceInfoRequest = deviceInfoRequest;
        return this;
    }

    public DiscountRequestWithBundlesBuilder withMultiOrderId(String multiOrderId) {
        this.multiOrderId = multiOrderId;
        return this;
    }

    public DiscountRequestWithBundlesBuilder useInternalPromocode(Boolean useInternalPromocode) {
        this.useInternalPromocode = useInternalPromocode;
        return this;
    }

    public DiscountRequestWithBundlesBuilder withCashbackOptionType(CashbackType cashbackOptionType) {
        this.cashbackOptionType = cashbackOptionType;
        return this;
    }

    public DiscountRequestWithBundlesBuilder withBnplSelected(Boolean bnplSelected) {
        this.bnplSelected = bnplSelected;
        return this;
    }

    public DiscountRequestWithBundlesBuilder withOptionalRulesEnabled(boolean isOptionalRulesEnabled) {
        this.isOptionalRulesEnabled = isOptionalRulesEnabled;
        return this;
    }

    public DiscountRequestWithBundlesBuilder withCalculateOrdersSeparately(Boolean calculateOrdersSeparately) {
        this.calculateOrdersSeparately = calculateOrdersSeparately;
        return this;
    }

    public DiscountRequestWithBundlesBuilder withPaymentInfo(PaymentInfo paymentInfo) {
        this.paymentInfo = paymentInfo;
        return this;
    }

    public MultiCartWithBundlesDiscountRequest build() {
        return new MultiCartWithBundlesDiscountRequest(
                orderRequests,
                operationContext,
                platform,
                redOrder,
                coins,
                certificateToken,
                coupon,
                false,
                multiOrderId,
                deviceInfoRequest,
                useInternalPromocode,
                cashbackOptionType,
                bnplSelected,
                calculateOrdersSeparately,
                paymentInfo,
                isOptionalRulesEnabled
        );
    }
}
