package ru.yandex.market.global.checkout.util;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;

import Market.DataCamp.DataCampOffer;
import Market.DataCamp.DataCampOfferContent;
import Market.DataCamp.DataCampOfferIdentifiers;
import Market.DataCamp.DataCampOfferMeta;
import Market.DataCamp.DataCampOfferPictures;
import Market.DataCamp.DataCampOfferPrice;
import Market.DataCamp.DataCampUnitedOffer;
import Market.DataCamp.PartnerCategoryOuterClass;
import Market.DataCamp.SyncAPI.SyncGetOffer;
import NMarketIndexer.Common.Common;
import io.github.benas.randombeans.EnhancedRandomBuilder;
import io.github.benas.randombeans.api.EnhancedRandom;
import io.github.benas.randombeans.api.Randomizer;

import ru.yandex.market.global.checkout.domain.promo.apply.PromoArgs;
import ru.yandex.market.global.checkout.domain.promo.apply.PromoState;
import ru.yandex.market.global.checkout.domain.promo.apply.fixed_discount.FixedDiscountArgs;
import ru.yandex.market.global.checkout.domain.promo.apply.fixed_discount.FixedDiscountCommonState;
import ru.yandex.market.global.checkout.domain.promo.apply.free_delivery.user.UserFreeDeliveryArgs;
import ru.yandex.market.global.checkout.domain.promo.apply.free_delivery.user.UserFreeDeliveryCommonState;
import ru.yandex.market.global.checkout.domain.promo.model.PromoType;
import ru.yandex.market.global.checkout.domain.promo.subject.PromoSubject;
import ru.yandex.market.global.common.jooq.Point;
import ru.yandex.market.global.common.jooq.Polygon;
import ru.yandex.market.global.common.test.RandomUtil;
import ru.yandex.market.global.common.test.TestClock;
import ru.yandex.market.global.db.jooq.embeddables.pojos.Address;
import ru.yandex.market.global.db.jooq.tables.pojos.Promo;
import ru.yandex.market.global.db.jooq.tables.pojos.PromoShop;
import ru.yandex.market.global.db.jooq.tables.pojos.PromoUser;

import static io.github.benas.randombeans.FieldPredicates.inClass;
import static io.github.benas.randombeans.FieldPredicates.named;
import static io.github.benas.randombeans.FieldPredicates.ofType;
import static io.github.benas.randombeans.randomizers.range.LongRangeRandomizer.aNewLongRangeRandomizer;
import static ru.yandex.market.global.common.test.RandomUtil.enumToStringRandomizer;
import static ru.yandex.market.global.common.test.RandomUtil.fixedValuesRandomizer;
import static ru.yandex.market.global.common.test.RandomUtil.fixedValuesStringRandomizer;
import static ru.yandex.market.global.common.test.RandomUtil.randomImplementationRandomizer;
import static ru.yandex.market.global.common.test.RandomUtil.someTimeNullRandomizer;
import static ru.yandex.market.gm.common.jooq.JooqRandomUtil.geoPointRandomizer;
import static ru.yandex.market.gm.common.jooq.JooqRandomUtil.geoPolygonPointsRandomizer;

/**
 * @author moskovkin@yandex-team.ru
 * @since 30.04.2020
 */
public class RandomDataGenerator {
    private RandomDataGenerator() {
    }

    public static void allGenerators(long seed, RandomUtil.EnhancedRandomEnhancedBuilder builder) {
        builder
                // Be careful here. There are no guaranty of order of applying random customizations.
                // For example [exclude "creationTime"] and [custom OffsetDateTime] randomizers some time
                // fill "creationTime" (has OffsetDateTime type) but some time exclude (named "creationTime").
                .apply(seed, RandomDataGenerator::excludeSpecialFieldsGenerator)
                .apply(seed, RandomDataGenerator::commonFieldsGenerator);
//                .apply(seed, RandomDataGenerator::testingDataGenerator);
    }

    public static void creationAndModificationTimeGenerator(
            long seed,
            RandomUtil.EnhancedRandomEnhancedBuilder builder
    ) {
        builder
                .randomize(named("createdAt"), RandomUtil.instantRangeRandomizer(
                        TestClock.INSTANCE.instant(),
                        TestClock.INSTANCE.instant().plus(1, ChronoUnit.DAYS),
                        seed
                ))
                .randomize(named("modifiedAt"), RandomUtil.instantRangeRandomizer(
                        TestClock.INSTANCE.instant().plus(2, ChronoUnit.DAYS),
                        TestClock.INSTANCE.instant().plus(3, ChronoUnit.DAYS),
                        seed
                ));
    }

    public static void excludeSpecialFieldsGenerator(long seed, RandomUtil.EnhancedRandomEnhancedBuilder builder
    ) {
        builder
                .excludeField(named("id"))
                .excludeField(named("finishedAt"))
                .excludeField(named("canceledAt"))
                .excludeField(named("mergedId"))
                .excludeField(named("referralId"));
    }

    public static void commonFieldsGenerator(long seed, RandomUtil.EnhancedRandomEnhancedBuilder builder) {
        builder
                .randomize(String.class, RandomUtil.stringFromUnicodeScriptRandomizer(seed, 1, 32,
                        Character.UnicodeScript.HEBREW,
                        Character.UnicodeScript.LATIN
                ))
                .randomize(named("uid"), aNewLongRangeRandomizer(10_000L, 100_000L, seed))
                .randomize(named("ownerUid"), aNewLongRangeRandomizer(10_000L, 100_000L, seed))
                .randomize(named("businessId"), aNewLongRangeRandomizer(1L, 100L, seed))
                .randomize(named("shopId"), aNewLongRangeRandomizer(100L, 1000L, seed))

                .randomize(named("vat").and(ofType(Double.class)), fixedValuesRandomizer(seed, 17d, 0d))
                .randomize(named("vat").and(ofType(BigDecimal.class)), fixedValuesRandomizer(seed,
                        BigDecimal.valueOf(17L), BigDecimal.valueOf(0L)))
                .randomize(named("deliveryCostForRecipient"), aNewLongRangeRandomizer(1000L, 2000L, seed))
                .randomize(named("deliveryCostForShop"), aNewLongRangeRandomizer(1000L, 2000L, seed))
                .randomize(named("totalItemsCost"), aNewLongRangeRandomizer(110_00L, 120_00L, seed))
                .randomize(named("totalItemsCostWithPromo"), aNewLongRangeRandomizer(100_00L, 110_00L, seed))
                .randomize(named("cost"), aNewLongRangeRandomizer(1000L, 10_000L, seed))
                .randomize(named("price"), aNewLongRangeRandomizer(1000L, 10_000L, seed))
                .randomize(named("totalCost"), aNewLongRangeRandomizer(100_00L, 110_00L, seed))
                .randomize(named("totalCostWithoutPromo"), aNewLongRangeRandomizer(110_00L, 120_00L, seed))
                .randomize(named("count"), aNewLongRangeRandomizer(1L, 5L, seed))
                .randomize(named("type").and(inClass(Promo.class)), enumToStringRandomizer(seed, PromoType.class))
                .randomize(named("points").and(inClass(Polygon.class)), geoPolygonPointsRandomizer(seed))
                .randomize(ofType(Point.class), geoPointRandomizer(seed))

                .randomize(ofType(PromoState.class), randomImplementationRandomizer(seed,
                        FixedDiscountCommonState.class, UserFreeDeliveryCommonState.class
                ))
                .randomize(ofType(PromoArgs.class), randomImplementationRandomizer(seed,
                        FixedDiscountArgs.class, UserFreeDeliveryArgs.class
                ))
                .randomize(ofType(PromoSubject.class), randomImplementationRandomizer(seed,
                        PromoUser.class, PromoShop.class
                ))
                .randomize(named("pictureUrl"), someTimeNullRandomizer(seed, fixedValuesStringRandomizer(seed,
                        "//avatars.mds.yandex.net/get-yabs_performance/1439906/pic9ea56f2912dcc82d5747ddbb84711904/",
                        "//avatars.mds.yandex.net/get-yabs_performance/99732/picec6e93397c4864590d02806ba5f9380a/",
                        "//avatars.mds.yandex.net/get-yabs_performance/1334820/pic2a57ec47a370bbe2c90aaf1519ac1047/"
                )));
    }

    public static RandomUtil.EnhancedRandomEnhancedBuilder dataRandom(Class<?> clazz) {
        long seed = RandomUtil.seed(clazz);
        return dataRandom(seed);
    }

    public static RandomUtil.EnhancedRandomEnhancedBuilder dataRandom(long seed) {
        return RandomUtil.baseRandom(seed).apply(seed, RandomDataGenerator::allGenerators);
    }
}
