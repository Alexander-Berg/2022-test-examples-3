package util;

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

import ru.yandex.market.global.common.test.RandomUtil;
import ru.yandex.market.global.common.test.TestClock;

import static io.github.benas.randombeans.FieldPredicates.named;

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
                .build();
    }

    public static Randomizer<SyncGetOffer.GetUnitedOffersResponse> getUnitedOffersResponseRandomizer(
            long seed, long businessId, long shopId, String offerId, long price
    ) {
        return new Randomizer<>() {
            private final EnhancedRandom random = EnhancedRandomBuilder.aNewEnhancedRandomBuilder()
                    .seed(seed)
                    .build();

            @Override
            public SyncGetOffer.GetUnitedOffersResponse getRandomValue() {
                return SyncGetOffer.GetUnitedOffersResponse.newBuilder()
                        .addOffers(DataCampUnitedOffer.UnitedOffer.newBuilder()
                                .setBasic(DataCampOffer.Offer.newBuilder()
                                        .setIdentifiers(DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                                                .setOfferId(offerId)
                                                .setBusinessId((int) businessId)
                                                .setShopId((int) shopId)
                                                .build())
                                        .setContent(DataCampOfferContent.OfferContent.newBuilder()
                                                .setPartner(DataCampOfferContent.PartnerContent.newBuilder()
                                                        .setOriginal(DataCampOfferContent.OriginalSpecification.newBuilder()
                                                                .setName(DataCampOfferMeta.StringValue.newBuilder()
                                                                        .setValue(random.nextObject(String.class))
                                                                        .build())
                                                                .setDescription(DataCampOfferMeta.StringValue.newBuilder()
                                                                        .setValue(random.nextObject(String.class))
                                                                        .build())
                                                                .setVendor(DataCampOfferMeta.StringValue.newBuilder()
                                                                        .setValue(random.nextObject(String.class))
                                                                        .build())
                                                                .setCategory(PartnerCategoryOuterClass.PartnerCategory.newBuilder()
                                                                        .setId(random.nextLong())
                                                                        .build())
                                                                .build())
                                                        .build())
                                                .build())
                                        .setPictures(DataCampOfferPictures.OfferPictures.newBuilder()
                                                .setPartner(DataCampOfferPictures.PartnerPictures.newBuilder()
                                                        .putActual(
                                                                random.nextObject(String.class),
                                                                DataCampOfferPictures.MarketPicture.newBuilder()
                                                                        .setOriginal(DataCampOfferPictures.MarketPicture.Picture.newBuilder()
                                                                                .setUrl(random.nextObject(String.class))
                                                                                .build())
                                                                        .build())
                                                        .build())
                                                .build())
                                        .build())
                                .putService((int) shopId, DataCampOffer.Offer.newBuilder()
                                        .setIdentifiers(DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                                                .setBusinessId((int) businessId)
                                                .setShopId((int) shopId)
                                                .setOfferId(offerId)
                                                .buildPartial())
                                        .setPrice(DataCampOfferPrice.OfferPrice.newBuilder()
                                                .setBasic(DataCampOfferPrice.PriceBundle.newBuilder()
                                                        .setBinaryPrice(Common.PriceExpression.newBuilder()
                                                                .setPrice(price * 100_000)
                                                                .build())
                                                        .build())
                                                .build())
                                        .build())
                                .build()
                        ).build();
            }
        };
    }

    public static RandomUtil.EnhancedRandomEnhancedBuilder dataRandom(Class<?> clazz) {
        long seed = RandomUtil.seed(clazz);
        return dataRandom(seed);
    }

    public static RandomUtil.EnhancedRandomEnhancedBuilder dataRandom(long seed) {
        return RandomUtil.baseRandom(seed).apply(seed, RandomDataGenerator::allGenerators);
    }
}
