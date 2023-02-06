package ru.yandex.market.loyalty.core.utils;

import ru.yandex.market.loyalty.core.model.bundle.PromoBundleDescription;
import ru.yandex.market.loyalty.core.model.bundle.PromoBundleDescription.PromoBundleDescriptionBuilder;
import ru.yandex.market.loyalty.core.model.bundle.PromoBundleItemDescription;
import ru.yandex.market.loyalty.core.model.bundle.PromoBundleItemDescription.PromoBundleItemDescriptionBuilder;
import ru.yandex.market.loyalty.core.model.bundle.PromoBundleRestrictions;
import ru.yandex.market.loyalty.core.model.bundle.PromoBundleStatus;
import ru.yandex.market.loyalty.core.model.bundle.PromoBundleStrategy;
import ru.yandex.market.loyalty.core.service.ConfigurationService;
import ru.yandex.market.loyalty.core.service.bundle.BundleConstructionStrategyCondition;
import ru.yandex.market.loyalty.core.service.bundle.calculation.OfferDiscountCondition;
import ru.yandex.market.loyalty.core.service.bundle.calculation.OfferDiscountCondition.OfferDiscountDescriptionBuilder;
import ru.yandex.market.loyalty.core.service.bundle.strategy.condition.BlueSetPromoCondition;
import ru.yandex.market.loyalty.core.service.bundle.strategy.condition.CheapestAsGiftCondition;
import ru.yandex.market.loyalty.core.service.bundle.strategy.condition.FeedSskuSet;
import ru.yandex.market.loyalty.core.service.bundle.strategy.condition.GiftWithPurchaseCondition;

import javax.annotation.Nonnull;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static ru.yandex.market.loyalty.core.model.bundle.PromoBundleRestrictions.PromoBundleRestrictionsBuilder;
import static ru.yandex.market.loyalty.core.utils.BuildCustomizer.Util.customize;
import static ru.yandex.market.loyalty.core.utils.BuildCustomizer.Util.mixin;
import static ru.yandex.market.loyalty.core.utils.BuildCustomizer.Util.same;

public final class PromoBundleUtils {
    private PromoBundleUtils() {
    }

    @SafeVarargs
    public static PromoBundleDescription bundleDescription(
            BuildCustomizer<PromoBundleDescription, PromoBundleDescriptionBuilder>... customizers
    ) {
        return customize(PromoBundleDescription::builder, customizers).build();
    }

    @SuppressWarnings("unchecked")
    public static PromoBundleDescription bundleDescription(
            Stream<BuildCustomizer<PromoBundleDescription, PromoBundleDescriptionBuilder>> customizersStream
    ) {
        return customize(
                PromoBundleDescription::builder, customizersStream.toArray(BuildCustomizer[]::new)).build();
    }

    @SafeVarargs
    public static PromoBundleDescription changeDescriptionOf(
            PromoBundleDescription from,
            BuildCustomizer<PromoBundleDescription, PromoBundleDescriptionBuilder>... customizers
    ) {
        return customize(same(from.toBuilder()), customizers).build();
    }

    public static BuildCustomizer<PromoBundleDescription, PromoBundleDescriptionBuilder> strategy(
            PromoBundleStrategy strategy
    ) {
        return b -> b.promoBundlesStrategyType(strategy);
    }

    public static BuildCustomizer<PromoBundleDescription, PromoBundleDescriptionBuilder> starts(
            LocalDateTime dateTime
    ) {
        return b -> b.startTime(dateTime);
    }

    public static BuildCustomizer<PromoBundleDescription, PromoBundleDescriptionBuilder> ends(LocalDateTime dateTime) {
        return b -> b.endTime(dateTime);
    }

    public static BuildCustomizer<PromoBundleDescription, PromoBundleDescriptionBuilder> feedId(Number feedId) {
        return b -> b.feedId(feedId.longValue());
    }

    public static BuildCustomizer<PromoBundleDescription, PromoBundleDescriptionBuilder> source(String source) {
        return b -> b.source(source);
    }

    public static BuildCustomizer<PromoBundleDescription, PromoBundleDescriptionBuilder> promoKey(String promo) {
        return b -> b.promoKey(promo);
    }

    public static BuildCustomizer<PromoBundleDescription, PromoBundleDescriptionBuilder> shopPromoId(
            String promo
    ) {
        return b -> b.shopPromoId(promo);
    }

    public static BuildCustomizer<PromoBundleDescription, PromoBundleDescriptionBuilder> anaplanId(
            String promo
    ) {
        return b -> b.anaplanId(promo);
    }

    public static BuildCustomizer<PromoBundleDescription, PromoBundleDescriptionBuilder> promoId(String promo) {
        return mixin(
                promoKey(promo),
                shopPromoId(promo)
        );
    }

    public static BuildCustomizer<PromoBundleDescription, PromoBundleDescriptionBuilder> status(
            PromoBundleStatus status
    ) {
        return b -> b.status(status);
    }

    @SafeVarargs
    public static BuildCustomizer<PromoBundleDescription, PromoBundleDescriptionBuilder> restrictions(
            BuildCustomizer<PromoBundleRestrictions, PromoBundleRestrictionsBuilder>... customizers
    ) {
        return b -> b.restrictions(customize(PromoBundleRestrictions::builder, customizers).build());
    }

    public static BuildCustomizer<PromoBundleRestrictions, PromoBundleRestrictionsBuilder> restrictReturn() {
        return b -> b.restrictReturn(true);
    }

    public static BuildCustomizer<PromoBundleRestrictions, PromoBundleRestrictionsBuilder> restrictBerubonus() {
        return b -> b.allowBerubonus(false);
    }

    public static BuildCustomizer<PromoBundleRestrictions, PromoBundleRestrictionsBuilder> restrictPromocode() {
        return b -> b.allowPromocode(false);
    }

    public static BuildCustomizer<PromoBundleRestrictions, PromoBundleRestrictionsBuilder> minQuantity(Number number) {
        return b -> b.minQuantity(number == null ? null : BigDecimal.valueOf(number.longValue()));
    }

    public static BuildCustomizer<PromoBundleRestrictions, PromoBundleRestrictionsBuilder> maxQuantity(Number number) {
        return b -> b.maxQuantity(number == null ? null : BigDecimal.valueOf(number.longValue()));
    }

    @SafeVarargs
    public static BuildCustomizer<PromoBundleDescription, PromoBundleDescriptionBuilder> item(
            BuildCustomizer<PromoBundleItemDescription, PromoBundleItemDescriptionBuilder>... customizers
    ) {
        return b -> b.item(customize(PromoBundleItemDescription::builder, customizers).build());
    }

    @SafeVarargs
    public static BuildCustomizer<PromoBundleDescription, PromoBundleDescriptionBuilder> changeItems(
            BuildCustomizer<PromoBundleDescription, PromoBundleDescriptionBuilder>... customizers
    ) {
        return b -> {
            b.clearItems();
            customize(same(b), customizers);
        };
    }

    @SafeVarargs
    public static BuildCustomizer<PromoBundleItemDescription, PromoBundleItemDescriptionBuilder> itemRestrictions(
            BuildCustomizer<PromoBundleRestrictions, PromoBundleRestrictionsBuilder>... customizers
    ) {
        return b -> b.withRestrictions(customize(PromoBundleRestrictions::builder, customizers).build());
    }

    public static BuildCustomizer<PromoBundleItemDescription, PromoBundleItemDescriptionBuilder> condition(
            BundleConstructionStrategyCondition condition
    ) {
        return b -> b.withCondition(condition);
    }

    public static BuildCustomizer<PromoBundleItemDescription, PromoBundleItemDescriptionBuilder> quantityInBundle(
            Number number
    ) {
        return b -> b.withQuantityInBundle(BigDecimal.valueOf(number.longValue()));
    }

    public static BuildCustomizer<PromoBundleDescription, PromoBundleDescriptionBuilder> withQuantityInBundle(
            Number number
    ) {
        return b -> b.quantityInBundle(BigDecimal.valueOf(number.longValue()));
    }

    public static BuildCustomizer<PromoBundleDescription, PromoBundleDescriptionBuilder> promoSource(Number number) {
        return b -> b.promoSource(number.intValue());
    }

    public static BuildCustomizer<PromoBundleItemDescription, PromoBundleItemDescriptionBuilder> primary() {
        return b -> b.withPrimary(true);
    }

    public static BuildCustomizer<PromoBundleDescription, PromoBundleDescriptionBuilder> primaryItem(
            long feedId, String... shopSkus
    ) {
        return item(
                condition(giftWithPurchase(feedId, shopSkus)),
                primary()
        );
    }

    public static BuildCustomizer<PromoBundleDescription, PromoBundleDescriptionBuilder> primaryItem(
            long feedId, OfferDiscountCondition... discountConditions
    ) {
        return item(
                condition(giftWithPurchase(feedId, discountConditions)),
                primary()
        );
    }

    public static BuildCustomizer<PromoBundleDescription, PromoBundleDescriptionBuilder> giftItem(
            long feedId, OfferDiscountCondition... discountConditions
    ) {
        return item(
                condition(giftWithPurchase(
                        feedId,
                        discountConditions
                ))
        );
    }

    public static GiftWithPurchaseCondition giftWithPurchase(long feedId, String... shopSkus) {
        return GiftWithPurchaseCondition.builder()
                .withFeedId(feedId)
                .withOfferDiscountDescriptions(Arrays.stream(shopSkus)
                        .map(ssku -> OfferDiscountCondition.builder()
                                .withSsku(ssku)
                                .build())
                        .collect(Collectors.toList()))
                .build();
    }

    public static GiftWithPurchaseCondition giftWithPurchase(
            long feedId,
            OfferDiscountCondition... directionalMapping
    ) {
        return GiftWithPurchaseCondition.builder()
                .withFeedId(feedId)
                .withOfferDiscountDescriptions(Arrays.asList(directionalMapping))
                .build();
    }

    @SafeVarargs
    public static OfferDiscountCondition directionalMapping(
            BuildCustomizer<OfferDiscountCondition, OfferDiscountDescriptionBuilder>... customizers
    ) {
        return customize(OfferDiscountCondition::builder, customizers).build();
    }

    public static BuildCustomizer<OfferDiscountCondition, OfferDiscountDescriptionBuilder> when(
            @Nonnull String shopSku
    ) {
        return b -> b.withRequiredSsku(shopSku);
    }

    public static BuildCustomizer<OfferDiscountCondition, OfferDiscountDescriptionBuilder> then(
            @Nonnull String shopSku
    ) {
        return b -> b.withSsku(shopSku);
    }

    public static BuildCustomizer<OfferDiscountCondition, OfferDiscountDescriptionBuilder> fixedPrice(
            @Nonnull Number price
    ) {
        return b -> b.withFixedPrice(BigDecimal.valueOf(price.doubleValue()));
    }

    public static BuildCustomizer<OfferDiscountCondition, OfferDiscountDescriptionBuilder> proportion(
            @Nonnull Number proportion
    ) {
        return b -> b.withProportion(BigDecimal.valueOf(proportion.doubleValue()));
    }

    public static OfferDiscountCondition proportion(
            @Nonnull String ssku,
            @Nonnull Number proportion
    ) {
        return OfferDiscountCondition.builder()
                .withSsku(ssku)
                .withProportion(BigDecimal.valueOf(proportion.doubleValue()))
                .build();
    }

    public static CheapestAsGiftCondition cheapestAsGift(FeedSskuSet... sskuSets) {
        return CheapestAsGiftCondition.builder()
                .withFeedSskuSets(Arrays.stream(sskuSets)
                        .collect(Collectors.toUnmodifiableList()))
                .build();
    }

    public static BlueSetPromoCondition blueSet(long feedId, OfferDiscountCondition... conditions) {
        return BlueSetPromoCondition.builder()
                .withFeedId(feedId)
                .withOfferDiscountDescriptions(Arrays.asList(conditions))
                .build();
    }

    public static void enableAllBundleFeatures(@Nonnull ConfigurationService configurationService) {
        //add some temporary off features here
    }

    public static void disableAllBundleFeatures(@Nonnull ConfigurationService configurationService) {
        //add some temporary off features here
    }
}
