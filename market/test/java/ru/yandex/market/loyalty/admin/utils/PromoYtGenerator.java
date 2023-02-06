package ru.yandex.market.loyalty.admin.utils;

import com.google.protobuf.Message;

import ru.yandex.market.loyalty.core.model.ReportPromoType;
import Market.Promo.Promo.PromoDetails;
import Market.Promo.Promo.PromoDetails.BlueFlash;
import Market.Promo.Promo.PromoDetails.BlueSet;
import Market.Promo.Promo.PromoDetails.CheapestAsGift;
import Market.Promo.Promo.PromoDetails.FeedOfferId;
import Market.Promo.Promo.PromoDetails.GenericBundle;

import javax.annotation.Nonnull;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.function.Supplier;

import static ru.yandex.market.loyalty.admin.utils.PromoYtGenerator.BuildCustomizer.Util.customize;

public final class PromoYtGenerator {
    private static final int MONEY_CORRECTION = 100;

    private PromoYtGenerator() {
    }

    public interface PromoDetailsDSL {
        static BuildCustomizer<PromoDetails, PromoDetails.Builder> shopPromoId(@Nonnull String shopPromoId) {
            return b -> b.setShopPromoId(shopPromoId);
        }

        static BuildCustomizer<PromoDetails, PromoDetails.Builder> starts(@Nonnull ZonedDateTime start) {
            return b -> b.setStartDate(start.toEpochSecond());
        }

        static BuildCustomizer<PromoDetails, PromoDetails.Builder> ends(@Nonnull ZonedDateTime end) {
            return b -> b.setEndDate(end.toEpochSecond());
        }
    }

    public interface GenericBundleDSL {
        static BuildCustomizer<GenericBundle.PromoItem, GenericBundle.PromoItem.Builder> ssku(
                @Nonnull String ssku
        ) {
            return b -> b.setOfferId(ssku);
        }

        static BuildCustomizer<GenericBundle.PromoItem, GenericBundle.PromoItem.Builder> quantityInBundle(
                @Nonnull Number count
        ) {
            return b -> b.setCount(count.intValue());
        }

        @SafeVarargs
        static BuildCustomizer<GenericBundle.BundleContent, GenericBundle.BundleContent.Builder> primary(
                BuildCustomizer<GenericBundle.PromoItem, GenericBundle.PromoItem.Builder>... customizers
        ) {
            return b -> b.setPrimaryItem(customize(GenericBundle.PromoItem::newBuilder, customizers));
        }

        @SafeVarargs
        static BuildCustomizer<GenericBundle.SecondaryItem, GenericBundle.SecondaryItem.Builder> item(
                BuildCustomizer<GenericBundle.PromoItem, GenericBundle.PromoItem.Builder>... customizers
        ) {
            return b -> b.setItem(customize(GenericBundle.PromoItem::newBuilder, customizers));
        }

        static BuildCustomizer<GenericBundle.SecondaryItem, GenericBundle.SecondaryItem.Builder> fixedPrice(
                @Nonnull Number price
        ) {
            return b -> b.setDiscountPrice(PromoDetails.Money.newBuilder()
                    .setValue(price.longValue() * MONEY_CORRECTION)
                    .setCurrency("RUB")
                    .build());
        }

        @SafeVarargs
        static BuildCustomizer<GenericBundle.BundleContent, GenericBundle.BundleContent.Builder> gift(
                BuildCustomizer<GenericBundle.SecondaryItem, GenericBundle.SecondaryItem.Builder>... customizers
        ) {
            return b -> b.setSecondaryItem(customize(GenericBundle.SecondaryItem::newBuilder, customizers));
        }

        static BuildCustomizer<GenericBundle.BundleContent, GenericBundle.BundleContent.Builder> proportion(
                @Nonnull Number proportion
        ) {
            return b -> b.setSpreadDiscount(proportion.doubleValue());
        }

        @SafeVarargs
        static BuildCustomizer<GenericBundle, GenericBundle.Builder> relation(
                BuildCustomizer<GenericBundle.BundleContent, GenericBundle.BundleContent.Builder>... customizers
        ) {
            return b -> b.addBundlesContent(customize(GenericBundle.BundleContent::newBuilder, customizers));
        }

        static BuildCustomizer<GenericBundle, GenericBundle.Builder> allowBerubonus(boolean allowBerubonus) {
            return b -> b.setAllowBerubonus(allowBerubonus);
        }

        static BuildCustomizer<GenericBundle, GenericBundle.Builder> allowPromocode(boolean allowPromocode) {
            return b -> b.setAllowPromocode(allowPromocode);
        }

        static BuildCustomizer<GenericBundle, GenericBundle.Builder> restrictRefund(boolean restrictRefund) {
            return b -> b.setRestrictRefund(restrictRefund);
        }
    }

    public interface CheapestAsGiftDSL {

        static BuildCustomizer<CheapestAsGift, CheapestAsGift.Builder> ssku(
                @Nonnull Number feed,
                @Nonnull String ssku
        ) {
            return b -> b
                    .addFeedOfferIds(FeedOfferId.newBuilder()
                            .setFeedId(feed.intValue())
                            .setOfferId(ssku)
                            .build());
        }

        static BuildCustomizer<CheapestAsGift, CheapestAsGift.Builder> quantityInBundle(
                @Nonnull Number quantityInBundle
        ) {
            return b -> b.setCount(quantityInBundle.intValue());
        }

        static BuildCustomizer<CheapestAsGift, CheapestAsGift.Builder> allowBerubonus(boolean allowBerubonus) {
            return b -> b.setAllowBerubonus(allowBerubonus);
        }

        static BuildCustomizer<CheapestAsGift, CheapestAsGift.Builder> allowPromocode(boolean allowPromocode) {
            return b -> b.setAllowPromocode(allowPromocode);
        }
    }

    public interface BlueSetDSL {
        static BuildCustomizer<BlueSet.SetContent.SetItem, BlueSet.SetContent.SetItem.Builder> ssku(
                @Nonnull String ssku
        ) {
            return b -> b.setOfferId(ssku);
        }

        static BuildCustomizer<BlueSet.SetContent.SetItem, BlueSet.SetContent.SetItem.Builder> discountProportion(
                @Nonnull Number proportion
        ) {
            return b -> b.setDiscount(proportion.doubleValue());
        }

        static BuildCustomizer<BlueSet.SetContent.SetItem, BlueSet.SetContent.SetItem.Builder> count(
                @Nonnull Number count
        ) {
            return b -> b.setCount(count.intValue());
        }

        @SafeVarargs
        static BuildCustomizer<BlueSet.SetContent, BlueSet.SetContent.Builder> item(
                BuildCustomizer<BlueSet.SetContent.SetItem, BlueSet.SetContent.SetItem.Builder>... customizers
        ) {
            return b -> b.addItems(customize(BlueSet.SetContent.SetItem::newBuilder, customizers));
        }

        @SafeVarargs
        static BuildCustomizer<BlueSet, BlueSet.Builder> set(
                BuildCustomizer<BlueSet.SetContent, BlueSet.SetContent.Builder>... customizers
        ) {
            return b -> b.addSetsContent(customize(BlueSet.SetContent::newBuilder, customizers));
        }

        static BuildCustomizer<BlueSet, BlueSet.Builder> allowBerubonus(boolean allowBerubonus) {
            return b -> b.setAllowBerubonus(allowBerubonus);
        }

        static BuildCustomizer<BlueSet, BlueSet.Builder> allowPromocode(boolean allowPromocode) {
            return b -> b.setAllowPromocode(allowPromocode);
        }

        static BuildCustomizer<BlueSet, BlueSet.Builder> restrictRefund(boolean restrictRefund) {
            return b -> b.setRestrictRefund(restrictRefund);
        }
    }

    public interface BlueFlashDSL {

        static BuildCustomizer<BlueFlash.FlashItem, BlueFlash.FlashItem.Builder> offer(
                @Nonnull Number feedId,
                @Nonnull String ssku
        ) {
            return b -> b.setOffer(FeedOfferId.newBuilder()
                    .setFeedId(feedId.intValue())
                    .setOfferId(ssku)
                    .build());
        }

        static BuildCustomizer<BlueFlash.FlashItem, BlueFlash.FlashItem.Builder> fixedPrice(
                @Nonnull Number price
        ) {
            return b -> b.setPrice(PromoDetails.Money.newBuilder()
                    .setValue(price.longValue() * MONEY_CORRECTION)
                    .setCurrency("RUB").build());
        }

        static BuildCustomizer<BlueFlash.FlashItem, BlueFlash.FlashItem.Builder> moneyBudget(
                @Nonnull Number count
        ) {
            return b -> b.setBudgetLimit(PromoDetails.Money.newBuilder()
                    .setValue(count.longValue() * MONEY_CORRECTION)
                    .setCurrency("RUB").build());
        }

        static BuildCustomizer<BlueFlash.FlashItem, BlueFlash.FlashItem.Builder> quantityBudget(
                @Nonnull Number quantity
        ) {
            return b -> b.setQuantityLimit(quantity.intValue());
        }

        @SafeVarargs
        static BuildCustomizer<BlueFlash, BlueFlash.Builder> item(
                BuildCustomizer<BlueFlash.FlashItem, BlueFlash.FlashItem.Builder>... customizers
        ) {
            return b -> b.addItems(customize(BlueFlash.FlashItem::newBuilder, customizers));
        }

        static BuildCustomizer<BlueFlash, BlueFlash.Builder> allowBerubonus(boolean allowBerubonus) {
            return b -> b.setAllowBerubonus(allowBerubonus);
        }

        static BuildCustomizer<BlueFlash, BlueFlash.Builder> allowPromocode(boolean allowPromocode) {
            return b -> b.setAllowPromocode(allowPromocode);
        }
    }

    @SafeVarargs
    @Nonnull
    public static PromoDetails.Builder promoDetails(
            BuildCustomizer<PromoDetails, PromoDetails.Builder>... customizers
    ) {
        return customize(PromoDetails::newBuilder, customizers);
    }

    @SafeVarargs
    @Nonnull
    public static BuildCustomizer<PromoDetails, PromoDetails.Builder> giftWithPurchaseDetails(
            BuildCustomizer<GenericBundle, GenericBundle.Builder>... customizers
    ) {
        return b -> b.setType(ReportPromoType.GENERIC_BUNDLE.getCode())
                .setGenericBundle(customize(GenericBundle::newBuilder, customizers));
    }

    @SafeVarargs
    @Nonnull
    public static BuildCustomizer<PromoDetails, PromoDetails.Builder> cheapestAsGiftDetails(
            BuildCustomizer<CheapestAsGift, CheapestAsGift.Builder>... customizers
    ) {
        return b -> b.setType(ReportPromoType.CHEAPEST_AS_GIFT.getCode())
                .setCheapestAsGift(customize(CheapestAsGift::newBuilder, customizers));
    }

    @SafeVarargs
    @Nonnull
    public static BuildCustomizer<PromoDetails, PromoDetails.Builder> blueSetDetails(
            BuildCustomizer<BlueSet, BlueSet.Builder>... customizers
    ) {
        return b -> b.setType(ReportPromoType.BLUE_SET.getCode())
                .setBlueSet(customize(BlueSet::newBuilder, customizers));
    }

    @SafeVarargs
    @Nonnull
    public static BuildCustomizer<PromoDetails, PromoDetails.Builder> blueFlashDetails(
            BuildCustomizer<BlueFlash, BlueFlash.Builder>... customizers
    ) {
        return b -> b.setType(ReportPromoType.BLUE_FLASH.getCode())
                .setBlueFlash(customize(BlueFlash::newBuilder, customizers));
    }

    @FunctionalInterface
    public interface BuildCustomizer<T extends Message, B extends Message.Builder> {
        void change(B builder);

        class Util {
            @Nonnull
            @SafeVarargs
            public static <T extends Message, B extends Message.Builder> B customize(
                    @Nonnull Supplier<B> supplier, BuildCustomizer<T, B>... customizers
            ) {
                B builder = supplier.get();
                Arrays.stream(customizers).forEach(c -> c.change(builder));
                return builder;
            }

            @Nonnull
            public static <T extends Message, B extends Message.Builder> Supplier<B> same(@Nonnull B builder) {
                return () -> builder;
            }

            @Nonnull
            @SafeVarargs
            @SuppressWarnings("unchecked")
            public static <T extends Message, B extends Message.Builder> BuildCustomizer<T, B> mixin(
                    BuildCustomizer<T, B>... customizers
            ) {
                return b -> customize(same(b), customizers);
            }
        }
    }


}
