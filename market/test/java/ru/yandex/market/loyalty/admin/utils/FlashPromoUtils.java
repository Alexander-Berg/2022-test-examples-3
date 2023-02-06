package ru.yandex.market.loyalty.admin.utils;

import ru.yandex.market.loyalty.core.model.flash.FlashPromoDescription;
import ru.yandex.market.loyalty.core.utils.BuildCustomizer;

import javax.annotation.Nonnull;

import java.time.LocalDateTime;

import static NMarket.Common.Promo.Promo.ESourceType.LOYALTY_VALUE;
import static ru.yandex.market.loyalty.core.utils.BuildCustomizer.Util.mixin;
import static ru.yandex.market.loyalty.core.utils.FlashPromoUtils.ends;
import static ru.yandex.market.loyalty.core.utils.FlashPromoUtils.feedId;
import static ru.yandex.market.loyalty.core.utils.FlashPromoUtils.flashDescription;
import static ru.yandex.market.loyalty.core.utils.FlashPromoUtils.promoSource;
import static ru.yandex.market.loyalty.core.utils.FlashPromoUtils.starts;

public final class FlashPromoUtils {
    public static final long FEED_ID = 123;

    private FlashPromoUtils() {
    }

    @Nonnull
    @SafeVarargs
    public static FlashPromoDescription flashPromo(
            BuildCustomizer<FlashPromoDescription, FlashPromoDescription.FlashPromoDescriptionBuilder>... customizers
    ) {
        LocalDateTime current = LocalDateTime.now().minusDays(1);
        return flashDescription(
                promoSource(LOYALTY_VALUE),
                feedId(FEED_ID),
                starts(current),
                ends(current.plusYears(10)),
                mixin(customizers)
        );
    }
}
