package ru.yandex.market.api.user.order.builders;

import com.google.common.collect.Lists;
import ru.yandex.market.checkout.checkouter.cart.ConsolidatedCarts;
import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.cart.MultiCartTotals;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.PresetInfo;
import ru.yandex.market.checkout.checkouter.validation.ValidationResult;

import java.math.BigDecimal;
import java.util.Arrays;

/**
 * @author Kirill Sulim sulim@yandex-team.ru
 */
public class MultiCartBuilder extends RandomBuilder<MultiCart> {

    private final MultiCart multiCart = new MultiCart();

    @Override
    public MultiCartBuilder random() {
        multiCart.setBuyerRegionId((long) random.getInt(1, 1000));
        return this;
    }

    public MultiCartBuilder withBuyerRegionId(int regionId) {
        multiCart.setBuyerRegionId((long) regionId);
        return this;
    }

    public MultiCartBuilder withOrder(Order order) {
        multiCart.setCarts(Lists.newArrayList(order));
        return this;
    }

    public MultiCartBuilder withErrors(ValidationResult ... results) {
        multiCart.setValidationErrors(Arrays.asList(results));
        return this;
    }

    public MultiCartBuilder withWarnings(ValidationResult ... results) {
        multiCart.setValidationWarnings(Arrays.asList(results));
        return this;
    }

    public MultiCartBuilder withTotals(MultiCartTotals value) {
        multiCart.setTotals(value);
        return this;
    }

    public MultiCartBuilder withPriceLeftForFreeDelivery(BigDecimal priceLeft) {
        multiCart.setPriceLeftForFreeDelivery(priceLeft);
        return this;
    }

    public MultiCartBuilder withPresets(PresetInfo ... presets) {
        multiCart.setPresets(Arrays.asList(presets));
        return this;
    }

    public MultiCartBuilder withCashback(ru.yandex.market.checkout.checkouter.cashback.model.Cashback cashback) {
        multiCart.setCashback(cashback);
        return this;
    }
    public MultiCartBuilder withGrouping(ConsolidatedCarts... consolidatedCarts) {
        multiCart.setGrouping(Arrays.asList(consolidatedCarts));
        return this;
    }

    @Override
    public MultiCart build() {
        return multiCart;
    }
}
