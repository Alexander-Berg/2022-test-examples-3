package ru.yandex.market.global.partner.util;

import java.time.OffsetTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;

import ru.yandex.market.global.common.jooq.Point;
import ru.yandex.market.global.common.jooq.Polygon;
import ru.yandex.market.global.common.test.RandomUtil;
import ru.yandex.market.global.common.test.TestClock;
import ru.yandex.market.global.db.jooq.tables.pojos.Business;
import ru.yandex.market.global.db.jooq.tables.pojos.Shop;
import ru.yandex.market.global.db.jooq.tables.pojos.ShopSchedule;
import ru.yandex.market.global.partner.domain.shop.model.ShopModel;
import ru.yandex.mj.generated.server.model.ReturnPolicyDto;
import ru.yandex.mj.generated.server.model.ScheduleItemDto;

import static io.github.benas.randombeans.FieldPredicates.inClass;
import static io.github.benas.randombeans.FieldPredicates.named;
import static io.github.benas.randombeans.FieldPredicates.ofType;
import static io.github.benas.randombeans.randomizers.range.LongRangeRandomizer.aNewLongRangeRandomizer;
import static io.github.benas.randombeans.randomizers.range.OffsetTimeRangeRandomizer.aNewOffsetTimeRangeRandomizer;
import static java.util.function.Predicate.not;
import static ru.yandex.market.global.common.test.RandomUtil.fixedValuesRandomizer;
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
    }

    public static void creationAndModificationTimeGenerator(
            long seed,
            RandomUtil.EnhancedRandomEnhancedBuilder builder
    ) {
        builder
                .randomize(named("creationTime"), RandomUtil.instantRangeRandomizer(
                        TestClock.INSTANCE.instant(),
                        TestClock.INSTANCE.instant().plus(1, ChronoUnit.DAYS),
                        seed
                ))
                .randomize(named("modificationTime"), RandomUtil.instantRangeRandomizer(
                        TestClock.INSTANCE.instant().plus(2, ChronoUnit.DAYS),
                        TestClock.INSTANCE.instant().plus(3, ChronoUnit.DAYS),
                        seed
                ));
    }

    public static void excludeSpecialFieldsGenerator(long seed, RandomUtil.EnhancedRandomEnhancedBuilder builder
    ) {
        builder
                .excludeField(named("id").and(not(inClass(Business.class).or(inClass(Shop.class)))));
    }

    public static void commonFieldsGenerator(long seed, RandomUtil.EnhancedRandomEnhancedBuilder builder) {
        builder
                .randomize(String.class, RandomUtil.stringFromUnicodeScriptRandomizer(seed, 1, 32,
                        Character.UnicodeScript.HEBREW,
                        Character.UnicodeScript.LATIN
                ))
                .randomize(named("permissions").and(inClass(ShopModel.class)), fixedValuesRandomizer(seed,
                        new ArrayList<>()))
                .randomize(named("id").and(inClass(Business.class)), aNewLongRangeRandomizer(1L, 10_000L, seed))
                .randomize(named("id").and(inClass(Shop.class)), aNewLongRangeRandomizer(10_000L, 10_000_000L, seed))
                .randomize(named("uid"), aNewLongRangeRandomizer(10_000L, 10_000_0L, seed))
                .randomize(named("ownerUid"), aNewLongRangeRandomizer(10_000L, 100_000L, seed))
                .randomize(named("businessId"), aNewLongRangeRandomizer(1L, 100L, seed))
                .randomize(named("deliveryRegionId"), fixedValuesRandomizer(seed, 1L))
                .randomize(named("shopId"), aNewLongRangeRandomizer(100L, 1000L, seed))
                .randomize(named("startAt").and(inClass(ScheduleItemDto.class)),
                        fixedValuesRandomizer(seed, "11:00:00", "10:00:00", "01:00:43")
                )
                .randomize(named("endAt").and(inClass(ScheduleItemDto.class)),
                        fixedValuesRandomizer(seed, "21:00:00", "20:00:00", "23:17:42")
                )
                .randomize(named("startAt").and(inClass(ShopSchedule.class)), aNewOffsetTimeRangeRandomizer(
                        OffsetTime.of(10, 0, 0, 0, ZoneOffset.UTC),
                        OffsetTime.of(11, 0, 0, 0, ZoneOffset.UTC),
                        seed
                ))
                .randomize(named("endAt").and(inClass(ShopSchedule.class)), aNewOffsetTimeRangeRandomizer(
                        OffsetTime.of(20, 0, 0, 0, ZoneOffset.UTC),
                        OffsetTime.of(21, 0, 0, 0, ZoneOffset.UTC),
                        seed
                ))
                .randomize(named("day").and(inClass(ShopSchedule.class).or(inClass(ScheduleItemDto.class))),
                        fixedValuesRandomizer(seed, "SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT")
                )
                .randomize(named("points").and(inClass(Polygon.class)), geoPolygonPointsRandomizer(seed))
                .randomize(named("returnPolicy").and(inClass(Shop.class)), someTimeNullRandomizer(
                        seed, ReturnPolicyDto.class
                ))
                .randomize(ofType(Point.class), geoPointRandomizer(seed));
    }

    public static RandomUtil.EnhancedRandomEnhancedBuilder dataRandom(Class<?> clazz) {
        long seed = RandomUtil.seed(clazz);
        return dataRandom(seed);
    }

    public static RandomUtil.EnhancedRandomEnhancedBuilder dataRandom(long seed) {
        return RandomUtil.baseRandom(seed).apply(seed, RandomDataGenerator::allGenerators);
    }
}
