package ru.yandex.market.marketpromo.core.test.generator;

import java.math.BigDecimal;

import javax.annotation.Nonnull;

import ru.yandex.market.marketpromo.model.BuildCustomizer;
import ru.yandex.market.marketpromo.model.CheapestAsGiftProperties;
import ru.yandex.market.marketpromo.model.DirectDiscountProperties;
import ru.yandex.market.marketpromo.model.DirectDiscountProperties.DirectDiscountPropertiesBuilder;

public final class PromoMechanics {

    private PromoMechanics() {
    }

    @Nonnull
    public static BuildCustomizer<DirectDiscountProperties, DirectDiscountPropertiesBuilder>
    minimalDiscountPercentSize(@Nonnull Number percent) {
        return b -> b.minimalDiscountPercentSize(BigDecimal.valueOf(percent.doubleValue()));
    }

    @Nonnull
    public static BuildCustomizer<CheapestAsGiftProperties, CheapestAsGiftProperties.CheapestAsGiftPropertiesBuilder>
    cheapestAsGiftDefaults() {
        return b -> b.quantityInBundle(3);
    }

    @Nonnull
    public static BuildCustomizer<CheapestAsGiftProperties, CheapestAsGiftProperties.CheapestAsGiftPropertiesBuilder>
    quantityInBundle(@Nonnull Number quantityInBundle) {
        return b -> b.quantityInBundle(quantityInBundle.intValue());
    }
}
