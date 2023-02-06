package ru.yandex.market.api.user.order.builders;

import ru.yandex.market.api.util.ApiCollections;
import ru.yandex.market.checkout.checkouter.order.promo.OrderPromo;
import ru.yandex.market.checkout.checkouter.order.promo.PromoDefinition;
import ru.yandex.market.checkout.checkouter.order.promo.PromoType;

import java.math.BigDecimal;
import java.util.Arrays;

public class OrderPromoBuilder  extends RandomBuilder<OrderPromo> {
    private PromoType type;
    private String marketPromoId;
    private String promoCode;
    private Long coinId;
    private BigDecimal buyerItemsDiscount;
    private BigDecimal deliveryDiscount;
    private BigDecimal subsidy;
    private BigDecimal buyerSubsidy;

    @Override
    public RandomBuilder<OrderPromo> random() {
        type = ApiCollections.randomElement(Arrays.asList(PromoType.values()), PromoType.MARKET_COIN);
        marketPromoId = random.getString();
        promoCode = random.getString();
        coinId = random.getLong();
        buyerItemsDiscount = BigDecimal.valueOf(random.getLong());
        deliveryDiscount = BigDecimal.valueOf(random.getLong());
        subsidy = BigDecimal.valueOf(random.getLong());
        buyerSubsidy = BigDecimal.valueOf(random.getLong());
        return this;
    }

    public OrderPromoBuilder withType(PromoType value) {
        this.type = value;
        return this;
    }
    public OrderPromoBuilder withMarketPromoId(String value) {
        this.marketPromoId = value;
        return this;
    }
    public OrderPromoBuilder withPromoCode(String value) {
        this.promoCode = value;
        return this;
    }
    public OrderPromoBuilder withCoinId(Long value) {
        this.coinId = value;
        return this;
    }
    public OrderPromoBuilder withBuyerItemsDiscount(BigDecimal value) {
        this.buyerItemsDiscount = value;
        return this;
    }
    public OrderPromoBuilder withDeliveryDiscount(BigDecimal value) {
        this.deliveryDiscount = value;
        return this;
    }
    public OrderPromoBuilder withSubsidy(BigDecimal value) {
        this.subsidy = value;
        return this;
    }
    public OrderPromoBuilder withBuyerSubsidy(BigDecimal value) {
        this.buyerSubsidy = value;
        return this;
    }

    @Override
    public OrderPromo build() {
        PromoDefinition promoDefinition = new PromoDefinition(type, marketPromoId, promoCode, coinId);
        OrderPromo promo = new OrderPromo(promoDefinition);
        promo.setBuyerItemsDiscount(buyerItemsDiscount);
        promo.setDeliveryDiscount(deliveryDiscount);
        promo.setSubsidy(subsidy);
        promo.setBuyerSubsidy(buyerSubsidy);
        return promo;
    }
}
