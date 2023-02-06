package ru.yandex.market.api.user.order.builders;

import ru.yandex.market.checkout.checkouter.cart.MultiCartTotals;
import ru.yandex.market.checkout.checkouter.order.promo.MultiCartPromo;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class MultiCartTotalsBuilder extends RandomBuilder<MultiCartTotals> {
    private BigDecimal buyerItemsTotal = BigDecimal.ZERO;
    private BigDecimal buyerTotal = BigDecimal.ZERO;
    private BigDecimal buyerDeliveryTotal = BigDecimal.ZERO;
    List<MultiCartPromo> promos = Collections.emptyList();

    @Override
    public RandomBuilder<MultiCartTotals> random() {
        buyerItemsTotal = random.getPrice(100, 50);
        buyerDeliveryTotal = random.getPrice(10, 50);
        buyerTotal = buyerItemsTotal.add(buyerDeliveryTotal);
        return this;
    }

    public MultiCartTotalsBuilder withBuyerItemsTotal(BigDecimal value) {
        this.buyerItemsTotal = value;
        return this;
    }

    public MultiCartTotalsBuilder withBuyerTotal(BigDecimal value) {
        this.buyerTotal = value;
        return this;
    }

    public MultiCartTotalsBuilder withBuyerDeliveryTotal(BigDecimal value) {
        this.buyerDeliveryTotal = value;
        return this;
    }

    public MultiCartTotalsBuilder withPromos(MultiCartPromo... promos) {
        this.promos = Arrays.asList(promos);
        return this;
    }

    @Override
    public MultiCartTotals build() {
        MultiCartTotals totals = new MultiCartTotals();
        totals.setBuyerItemsTotal(buyerItemsTotal);
        totals.setBuyerTotal(buyerTotal);
        totals.setBuyerDeliveryTotal(buyerDeliveryTotal);
        totals.setPromos(promos);
        return totals;
    }
}
