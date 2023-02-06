package ru.yandex.market.checkout.util.loyalty.response;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import ru.yandex.market.checkout.test.providers.OrderItemProvider;
import ru.yandex.market.loyalty.api.model.CashbackResponse;
import ru.yandex.market.loyalty.api.model.CouponError;
import ru.yandex.market.loyalty.api.model.IdObject;
import ru.yandex.market.loyalty.api.model.cart.CartFlag;
import ru.yandex.market.loyalty.api.model.coin.CoinError;
import ru.yandex.market.loyalty.api.model.coin.UserCoinResponse;
import ru.yandex.market.loyalty.api.model.discount.FreeDeliveryReason;
import ru.yandex.market.loyalty.api.model.discount.FreeDeliveryStatus;
import ru.yandex.market.loyalty.api.model.discount.MultiCartWithBundlesDiscountResponse;
import ru.yandex.market.loyalty.api.model.discount.PriceLeftForFreeDeliveryResponseV3;
import ru.yandex.market.loyalty.api.model.promocode.PromocodeError;

public class DiscountResponseBuilder {

    private final List<OrderResponseBuilder> orders = new ArrayList<>();
    private List<UserCoinResponse> coins;
    private List<IdObject> unusedCoins;
    private List<CoinError> coinErrors;
    private CouponError couponError;
    private BigDecimal priceLeftForFreeDelivery;
    private BigDecimal threshold;
    private FreeDeliveryReason freeDeliveryReason = FreeDeliveryReason.UNKNOWN;
    private FreeDeliveryStatus freeDeliveryStatus = FreeDeliveryStatus.UNKNOWN;
    private CashbackResponse cashbackResponse;
    private Set<PromocodeError> promocodeErrors = new HashSet<>();
    private Set<String> unusedPromocodes = new HashSet<>();
    private Map<FreeDeliveryReason, PriceLeftForFreeDeliveryResponseV3> deliveryDiscountMap = new HashMap<>();
    private CartFlag cartFlag = CartFlag.UNKNOWN;

    private DiscountResponseBuilder() {
    }

    public static DiscountResponseBuilder create() {
        return new DiscountResponseBuilder();
    }

    public DiscountResponseBuilder withOrder(OrderResponseBuilder order) {
        this.orders.add(order);
        return this;
    }

    public DiscountResponseBuilder withCoins(List<UserCoinResponse> coins) {
        this.coins = coins;
        return this;
    }

    public DiscountResponseBuilder withUnusedCoins(List<IdObject> unusedCoins) {
        this.unusedCoins = unusedCoins;
        return this;
    }

    public DiscountResponseBuilder withCoinErrors(List<CoinError> coinErrors) {
        this.coinErrors = coinErrors;
        return this;
    }

    public DiscountResponseBuilder withCouponError(CouponError couponError) {
        this.couponError = couponError;
        return this;
    }

    public DiscountResponseBuilder withPriceLeftForFreeDelivery(BigDecimal priceLeftForFreeDelivery) {
        this.priceLeftForFreeDelivery = priceLeftForFreeDelivery;
        return this;
    }

    public DiscountResponseBuilder withThreshold(BigDecimal threshold) {
        this.threshold = threshold;
        return this;
    }

    public DiscountResponseBuilder withFreeDeliveryReason(FreeDeliveryReason freeDeliveryReason) {
        this.freeDeliveryReason = freeDeliveryReason;
        return this;
    }

    public DiscountResponseBuilder withFreeDeliveryStatus(FreeDeliveryStatus freeDeliveryStatus) {
        this.freeDeliveryStatus = freeDeliveryStatus;
        return this;
    }

    public DiscountResponseBuilder withCashbackResponse(CashbackResponse cashbackResponse) {
        this.cashbackResponse = cashbackResponse;
        return this;
    }

    public DiscountResponseBuilder withPromocodeError(@Nonnull PromocodeError promocodeError) {
        this.promocodeErrors.add(promocodeError);
        return this;
    }

    public DiscountResponseBuilder withPromocodeErrors(@Nonnull Set<PromocodeError> promocodeErrors) {
        this.promocodeErrors = new HashSet<>(promocodeErrors);
        return this;
    }

    public DiscountResponseBuilder withUnusedPromocode(@Nonnull String promocode) {
        this.unusedPromocodes.add(promocode);
        return this;
    }

    public DiscountResponseBuilder withUnusedPromocodes(@Nonnull Set<String> promocodes) {
        this.unusedPromocodes = new HashSet<>(promocodes);
        return this;
    }

    public DiscountResponseBuilder setDeliveryDiscountMap(
            Map<FreeDeliveryReason, PriceLeftForFreeDeliveryResponseV3> deliveryDiscountMap) {
        this.deliveryDiscountMap = deliveryDiscountMap;
        return this;
    }

    public DiscountResponseBuilder addDeliveryDiscount(FreeDeliveryReason reason,
                                                       PriceLeftForFreeDeliveryResponseV3 priceLeft) {
        this.deliveryDiscountMap.put(reason, priceLeft);
        return this;
    }

    public DiscountResponseBuilder withCartFlag(CartFlag cartFlag) {
        this.cartFlag = cartFlag;
        return this;
    }

    public MultiCartWithBundlesDiscountResponse buildResponseWithBundles() {
        return new MultiCartWithBundlesDiscountResponse(
                orders.stream()
                        .map(OrderResponseBuilder::buildResponseWithBundles)
                        .collect(Collectors.toList()),
                coins,
                unusedCoins,
                coinErrors,
                couponError,
                promocodeErrors,
                unusedPromocodes,
                priceLeftForFreeDelivery,
                threshold,
                freeDeliveryReason,
                freeDeliveryStatus,
                deliveryDiscountMap,
                cashbackResponse,
                cartFlag
        );
    }

    public OrderItemProvider.OrderItemBuilder clone() {
        try {
            return ((OrderItemProvider.OrderItemBuilder) super.clone()).emptyId();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException("Unexpected exception", e);
        }
    }
}
