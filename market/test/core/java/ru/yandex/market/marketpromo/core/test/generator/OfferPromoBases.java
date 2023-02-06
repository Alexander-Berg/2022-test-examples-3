package ru.yandex.market.marketpromo.core.test.generator;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import javax.annotation.Nonnull;

import ru.yandex.market.marketpromo.model.BuildCustomizer;
import ru.yandex.market.marketpromo.model.OfferPromoBase;

import static ru.yandex.market.marketpromo.model.BuildCustomizer.Util.customize;
import static ru.yandex.market.marketpromo.model.BuildCustomizer.Util.mixin;

public final class OfferPromoBases {

    private OfferPromoBases() {
    }

    @Nonnull
    @SafeVarargs
    public static OfferPromoBase promoBase(
            @Nonnull BuildCustomizer<OfferPromoBase, OfferPromoBase.OfferPromoBaseBuilder> first,
            BuildCustomizer<OfferPromoBase, OfferPromoBase.OfferPromoBaseBuilder>... customizers
    ) {
        return customize(OfferPromoBase::builder,
                mixin(b -> b.updatedAt(LocalDateTime.now()), mixin(first, customizers))).build();
    }

    @Nonnull
    public static BuildCustomizer<OfferPromoBase, OfferPromoBase.OfferPromoBaseBuilder> id(@Nonnull String id) {
        return b -> b.id(id);
    }

    @Nonnull
    public static BuildCustomizer<OfferPromoBase, OfferPromoBase.OfferPromoBaseBuilder> updatedAt(
            @Nonnull LocalDateTime updatedAt
    ) {
        return b -> b.updatedAt(updatedAt);
    }

    @Nonnull
    public static BuildCustomizer<OfferPromoBase, OfferPromoBase.OfferPromoBaseBuilder>
    basePrice(@Nonnull BigDecimal price) {
        return b -> b.basePrice(price);
    }
}
