package ru.yandex.market.marketpromo.core.test.generator;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import javax.annotation.Nonnull;

import com.google.common.collect.Sets;

import ru.yandex.market.marketpromo.core.utils.RequestContextUtils;
import ru.yandex.market.marketpromo.model.BuildCustomizer;
import ru.yandex.market.marketpromo.model.CategoryIdWithDiscount;
import ru.yandex.market.marketpromo.model.CheapestAsGiftProperties;
import ru.yandex.market.marketpromo.model.CheapestAsGiftProperties.CheapestAsGiftPropertiesBuilder;
import ru.yandex.market.marketpromo.model.DirectDiscountProperties;
import ru.yandex.market.marketpromo.model.DirectDiscountProperties.DirectDiscountPropertiesBuilder;
import ru.yandex.market.marketpromo.model.MechanicsType;
import ru.yandex.market.marketpromo.model.Promo;
import ru.yandex.market.marketpromo.model.PromoKey;
import ru.yandex.market.marketpromo.model.PromoStatus;
import ru.yandex.market.marketpromo.model.User;
import ru.yandex.market.marketpromo.utils.IdentityUtils;

import static ru.yandex.market.marketpromo.core.test.generator.Offers.DEFAULT_WAREHOUSE_ID;
import static ru.yandex.market.marketpromo.core.test.generator.PromoMechanics.cheapestAsGiftDefaults;
import static ru.yandex.market.marketpromo.model.BuildCustomizer.Util.customize;
import static ru.yandex.market.marketpromo.model.BuildCustomizer.Util.mixin;

public final class Promos {

    public static final String DEFAULT_PROMO_ID = "some promo";
    public static final long CATEGORY_1 = 123L;
    public static final long CATEGORY_2 = 125L;
    public static final String LOGIN_1 = "login_1";
    public static final String LOGIN_2 = "login_2";
    public static final PromoKey DD_PROMO_KEY =
            IdentityUtils.decodePromoId("direct-discount$" + IdentityUtils.hashId("direct discount promo"));
    public static final PromoKey ANOTHER_DD_PROMO_KEY =
            IdentityUtils.decodePromoId("direct-discount$" + IdentityUtils.hashId("another direct discount promo"));
    public static final PromoKey CAG_PROMO_KEY =
            IdentityUtils.decodePromoId("cheapest-as-gift$" + IdentityUtils.hashId("cheapest as gift promo"));
    public static final PromoKey ANOTHER_CAG_PROMO_KEY =
            IdentityUtils.decodePromoId("cheapest-as-gift$" + IdentityUtils.hashId("another cheapest as gift promo"));
    public static long DEFAULT_CATEGORY_ID = 12;

    private Promos() {
    }

    @SafeVarargs
    @Nonnull
    public static Promo promo(BuildCustomizer<Promo, Promo.PromoBuilder>... customizers) {
        return customize(Promo::builder, mixin(defaults(), customizers)).build();
    }

    @Nonnull
    public static BuildCustomizer<Promo, Promo.PromoBuilder> defaults() {
        final String promoId = UUID.randomUUID().toString();
        return b -> b.category(DEFAULT_CATEGORY_ID)
                .warehouseId(DEFAULT_WAREHOUSE_ID)
                .promoId(promoId)
                .id(IdentityUtils.hashId(promoId))
                .source("some source")
                .startDate(LocalDateTime.now())
                .status(PromoStatus.CREATED)
                .name(DEFAULT_PROMO_ID)
                .warehouseId(DEFAULT_WAREHOUSE_ID)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .createRequestId(RequestContextUtils.currentRequestId())
                .updateRequestId(RequestContextUtils.currentRequestId());
    }

    @Nonnull
    public static BuildCustomizer<Promo, Promo.PromoBuilder> id(@Nonnull String id) {
        return b -> b.id(id).promoId(IdentityUtils.decodeHashId(id));
    }

    @Nonnull
    public static BuildCustomizer<Promo, Promo.PromoBuilder> promoId(@Nonnull String promoId) {
        return b -> b.promoId(promoId);
    }

    @Nonnull
    public static BuildCustomizer<Promo, Promo.PromoBuilder> promoName(@Nonnull String promoName) {
        return b -> b.name(promoName);
    }

    @Nonnull
    public static BuildCustomizer<Promo, Promo.PromoBuilder> trade(@Nonnull User user) {
        return b -> b.trade(user);
    }

    @Nonnull
    public static BuildCustomizer<Promo, Promo.PromoBuilder> warehouse(@Nonnull Number warehouseId) {
        return b -> b.warehouseId(warehouseId.longValue());
    }

    @Nonnull
    public static BuildCustomizer<Promo, Promo.PromoBuilder> categories(@Nonnull Long... categories) {
        return b -> b.categories(Sets.newHashSet(categories));
    }

    @Nonnull
    public static BuildCustomizer<Promo, Promo.PromoBuilder> system(boolean system) {
        return b -> b.system(system);
    }

    @Nonnull
    public static BuildCustomizer<Promo, Promo.PromoBuilder> category(@Nonnull Long category,
                                                                      @Nonnull Number minimalDiscountPercentSize) {
        return b -> b.category(category)
                .categoryWithDiscount(CategoryIdWithDiscount.of(category,
                        BigDecimal.valueOf(minimalDiscountPercentSize.doubleValue())));
    }

    @SafeVarargs
    @Nonnull
    public static BuildCustomizer<Promo, Promo.PromoBuilder> directDiscount(
            @Nonnull BuildCustomizer<DirectDiscountProperties,
                    DirectDiscountPropertiesBuilder>... customizers
    ) {
        return b -> b.mechanicsType(MechanicsType.DIRECT_DISCOUNT)
                .mechanicsProperties(customize(DirectDiscountProperties::builder, customizers).build());
    }

    @SafeVarargs
    @Nonnull
    public static BuildCustomizer<Promo, Promo.PromoBuilder> cheapestAsGift(
            @Nonnull BuildCustomizer<CheapestAsGiftProperties,
                    CheapestAsGiftPropertiesBuilder>... customizers
    ) {
        return b -> b.mechanicsType(MechanicsType.CHEAPEST_AS_GIFT)
                .mechanicsProperties(customize(CheapestAsGiftProperties::builder,
                        mixin(cheapestAsGiftDefaults(), customizers)).build());
    }

    @SafeVarargs
    @Nonnull
    public static BuildCustomizer<Promo, Promo.PromoBuilder> cheapestAsGiftLocal(
            @Nonnull BuildCustomizer<CheapestAsGiftProperties,
                    CheapestAsGiftPropertiesBuilder>... customizers
    ) {
        return b -> b.mechanicsType(MechanicsType.CHEAPEST_AS_GIFT)
                .source(NMarket.Common.Promo.Promo.ESourceType.CATEGORYIFACE.name())
                .mechanicsProperties(customize(CheapestAsGiftProperties::builder,
                        mixin(cheapestAsGiftDefaults(), customizers)).build());
    }

    @Nonnull
    public static BuildCustomizer<Promo, Promo.PromoBuilder> assortmentAutopublication(

    ) {
        return b -> b.assortmentAutopublication(true);
    }

    @Nonnull
    public static BuildCustomizer<Promo, Promo.PromoBuilder> starts(@Nonnull LocalDateTime localDateTime) {
        return b -> b.startDate(localDateTime);
    }

    @Nonnull
    public static BuildCustomizer<Promo, Promo.PromoBuilder> ends(@Nonnull LocalDateTime localDateTime) {
        return b -> b.endDate(localDateTime);
    }

    @Nonnull
    public static BuildCustomizer<Promo, Promo.PromoBuilder> deadline(@Nonnull LocalDateTime localDateTime) {
        return b -> b.deadlineAt(localDateTime);
    }

    @Nonnull
    public static BuildCustomizer<Promo, Promo.PromoBuilder> published(@Nonnull LocalDateTime localDateTime) {
        return b -> b.publishDate(localDateTime);
    }

    @Nonnull
    public static BuildCustomizer<Promo, Promo.PromoBuilder> status(@Nonnull PromoStatus status) {
        return b -> b.status(status);
    }

    @Nonnull
    public static BuildCustomizer<Promo, Promo.PromoBuilder> categoryMinimalDiscountPercentSize(
            @Nonnull Number categoryId,
            @Nonnull Number discount
    ) {
        return b -> b.categoryWithDiscount(CategoryIdWithDiscount.of(
                categoryId.longValue(),
                BigDecimal.valueOf(discount.doubleValue())
        ));
    }
}
