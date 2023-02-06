package ru.yandex.market.marketpromo.core.test.generator;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import javax.annotation.Nonnull;

import ru.yandex.market.marketpromo.model.BuildCustomizer;
import ru.yandex.market.marketpromo.model.DirectDiscountOfferPropertiesCore;
import ru.yandex.market.marketpromo.model.DirectDiscountOfferPropertiesCore.BaseBuilder;
import ru.yandex.market.marketpromo.model.LocalOfferPromo;
import ru.yandex.market.marketpromo.model.MechanicsOfferProperties;
import ru.yandex.market.marketpromo.model.MechanicsType;
import ru.yandex.market.marketpromo.model.OfferPromoBase;

import static ru.yandex.market.marketpromo.model.BuildCustomizer.Util.customize;

public final class LocalOfferPromoMechanics {

    private LocalOfferPromoMechanics() {
    }

    @Nonnull
    public static LocalOfferPromo.LocalOfferPromoBuilder cheapestAsGift(
            @Nonnull String id
    ) {
        return LocalOfferPromo.builderOf(OfferPromoBase.builder()
                .id(id)
                .updatedAt(LocalDateTime.now())
                .build()
        )
                .mechanicsProperties(MechanicsOfferProperties.emptyOf(MechanicsType.CHEAPEST_AS_GIFT));
    }

    @Nonnull
    @SafeVarargs
    public static LocalOfferPromo.LocalOfferPromoBuilder directDiscount(
            @Nonnull String id,
            BuildCustomizer<DirectDiscountOfferPropertiesCore, BaseBuilder<DirectDiscountOfferPropertiesCore, ?>>... customizers
    ) {
        return LocalOfferPromo.builderOf(
                OfferPromoBase.builder()
                        .id(id)
                        .updatedAt(LocalDateTime.now())
                        .build()
        )
                .mechanicsProperties(customize(DirectDiscountOfferPropertiesCore::builder, customizers).build());
    }

    @Nonnull
    public static BuildCustomizer<DirectDiscountOfferPropertiesCore, BaseBuilder<DirectDiscountOfferPropertiesCore, ?>>
    basePrice(
            @Nonnull BigDecimal price
    ) {
        return b -> b.fixedBasePrice(price);
    }

    @Nonnull
    public static BuildCustomizer<DirectDiscountOfferPropertiesCore, BaseBuilder<DirectDiscountOfferPropertiesCore, ?>>
    price(
            @Nonnull BigDecimal price
    ) {
        return b -> b.fixedPrice(price);
    }

}
