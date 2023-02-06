package ru.yandex.market.loyalty.core.utils;

import ru.yandex.market.loyalty.core.model.flash.FlashPromoDescription;
import ru.yandex.market.loyalty.core.model.flash.FlashPromoDescription.FlashPromoDescriptionBuilder;
import ru.yandex.market.loyalty.core.model.flash.FlashPromoRestrictions;
import ru.yandex.market.loyalty.core.model.flash.FlashPromoStatus;

import javax.annotation.Nonnull;

import java.time.LocalDateTime;

import static ru.yandex.market.loyalty.core.model.flash.FlashPromoRestrictions.FlashPromoRestrictionsBuilder;
import static ru.yandex.market.loyalty.core.utils.BuildCustomizer.Util.customize;

public final class FlashPromoUtils {
    private FlashPromoUtils() {
    }

    @SafeVarargs
    public static FlashPromoDescription flashDescription(
            BuildCustomizer<FlashPromoDescription, FlashPromoDescriptionBuilder>... customizers
    ) {
        return customize(FlashPromoDescription::builder, customizers).build();
    }

    public static BuildCustomizer<FlashPromoDescription, FlashPromoDescriptionBuilder> starts(
            LocalDateTime dateTime
    ) {
        return b -> b.startTime(dateTime);
    }

    public static BuildCustomizer<FlashPromoDescription, FlashPromoDescriptionBuilder> ends(LocalDateTime dateTime) {
        return b -> b.endTime(dateTime);
    }

    public static BuildCustomizer<FlashPromoDescription, FlashPromoDescriptionBuilder> feedId(Number feedId) {
        return b -> b.feedId(feedId.longValue());
    }

    public static BuildCustomizer<FlashPromoDescription, FlashPromoDescriptionBuilder> promoKey(String promo) {
        return b -> b.promoKey(promo);
    }

    public static BuildCustomizer<FlashPromoDescription, FlashPromoDescriptionBuilder> source(String source) {
        return b -> b.source(source);
    }

    public static BuildCustomizer<FlashPromoDescription, FlashPromoDescriptionBuilder> shopPromoId(String promo) {
        return b -> b.shopPromoId(promo);
    }

    public static BuildCustomizer<FlashPromoDescription, FlashPromoDescriptionBuilder> anaplanId(String promo) {
        return b -> b.anaplanId(promo);
    }

    public static BuildCustomizer<FlashPromoDescription, FlashPromoDescriptionBuilder> status(FlashPromoStatus status) {
        return b -> b.status(status);
    }

    public static BuildCustomizer<FlashPromoDescription, FlashPromoDescriptionBuilder> landingUrl(
            @Nonnull String landingUrl
    ) {
        return b -> b.landingUrl(landingUrl);
    }

    public static BuildCustomizer<FlashPromoDescription, FlashPromoDescriptionBuilder> promoSource(Number number) {
        return b -> b.promoSource(number.intValue());
    }

    @SafeVarargs
    public static BuildCustomizer<FlashPromoDescription, FlashPromoDescriptionBuilder> restrictions(
            BuildCustomizer<FlashPromoRestrictions, FlashPromoRestrictionsBuilder>... customizers
    ) {
        return b -> b.restrictions(customize(FlashPromoRestrictions::builder, customizers).build());
    }

    public static BuildCustomizer<FlashPromoRestrictions, FlashPromoRestrictionsBuilder> restrictBerubonus() {
        return b -> b.allowBerubonus(false);
    }

    public static BuildCustomizer<FlashPromoRestrictions, FlashPromoRestrictionsBuilder> restrictPromocode() {
        return b -> b.allowPromocode(false);
    }
}
